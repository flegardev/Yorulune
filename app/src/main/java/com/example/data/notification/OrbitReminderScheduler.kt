package com.example.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalPreferencesEntity
import com.example.data.local.LocalTaskEntity
import com.example.data.local.OrbitLocalDatabase
import com.example.data.model.NativeHomeWidgetVariant
import com.example.data.model.OrbitTask
import com.example.data.model.SmartTaskParser
import com.example.widget.OrbitHomeWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object OrbitReminderScheduler {
    const val CHANNEL_STANDARD = "orbit_reminders_standard"
    const val CHANNEL_CRITICAL = "orbit_reminders_critical"
    const val CHANNEL_SPECIAL = "orbit_reminders_special"
    const val CHANNEL_FOCUS = "orbit_reminders_focus"

    const val ACTION_FIRE_REMINDER = "com.example.orbit.ACTION_FIRE_REMINDER"
    const val ACTION_COMPLETE_TASK = "com.example.orbit.ACTION_COMPLETE_TASK"
    const val ACTION_SNOOZE_TASK = "com.example.orbit.ACTION_SNOOZE_TASK"
    const val ACTION_RESCHEDULE_TOMORROW = "com.example.orbit.ACTION_RESCHEDULE_TOMORROW"
    const val ACTION_CYCLE_WIDGET_VARIANT = "com.example.orbit.ACTION_CYCLE_WIDGET_VARIANT"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    /**
     * Default minimum number of reminder offset slots checked during cancellation,
     * supplemented dynamically by [scheduledOffsetCountByTaskId] so tasks with any
     * number of reminder offsets are 100% cancelled and rescheduled cleanly.
     */
    const val DEFAULT_MAX_REMINDER_SLOTS = 32

    private val scheduledOffsetCountByTaskId = ConcurrentHashMap<String, Int>()

    fun requestCodeForOffset(taskId: String, index: Int): Int =
        (taskId.hashCode() * 31) + index

    /** Extras are not part of PendingIntent identity. Keep colliding task hashes independent. */
    fun reminderIntent(context: Context, taskId: String, index: Int): Intent =
        Intent(context, OrbitReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            data = taskActionUri(taskId, "offset-$index")
            putExtra(EXTRA_TASK_ID, taskId)
        }

    private fun taskActionUri(taskId: String, action: String): Uri = Uri.Builder()
        .scheme("yorulune").authority("local-reminder").appendPath(taskId).appendPath(action).build()

    fun activeTrackedOffsetCount(taskId: String): Int =
        scheduledOffsetCountByTaskId[taskId] ?: 0

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val standardChannel = NotificationChannel(
                CHANNEL_STANDARD,
                "Yorulune Task & Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Time-window, deadline, and recurring reminders"
            }
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL,
                "Yorulune Escalated & Persistent Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority persistent reminders for critical deadlines"
            }
            val specialChannel = NotificationChannel(
                CHANNEL_SPECIAL,
                "Yorulune Birthdays, Bills & Medication",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dedicated alerts for birthdays, anniversaries, monthly bills, and medication"
            }
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "Yorulune Focus Cockpit Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pomodoro and deep work session completion alerts"
            }
            manager.createNotificationChannel(standardChannel)
            manager.createNotificationChannel(criticalChannel)
            manager.createNotificationChannel(specialChannel)
            manager.createNotificationChannel(focusChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactReminders(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Honest description of Android alarm delivery behavior (never promises exact delivery
     * when OS battery optimization, Doze mode, or missing SCHEDULE_EXACT_ALARM applies).
     */
    fun describeAlarmDeliveryGuarantee(context: Context): String {
        return if (canScheduleExactReminders(context)) {
            "Idle-capable alarm scheduled (subject to Android OS Doze and battery policies)"
        } else {
            "Battery-optimized windowed alarm scheduled (Android may defer delivery during Doze)"
        }
    }

    /**
     * Computes the UTC epoch millisecond trigger time for a task reminder using [ZonedDateTime],
     * properly accounting for Daylight Saving Time (DST) spring-forward gaps, fall-back overlaps,
     * and time-zone changes.
     */
    fun computeTriggerMillis(
        targetEpochDay: Long,
        hour: Int,
        minute: Int,
        offsetMinutes: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val localDate = LocalDate.ofEpochDay(targetEpochDay)
        val localTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val zonedBase = ZonedDateTime.of(localDate, localTime, zoneId)
        return zonedBase.minusMinutes(offsetMinutes.toLong()).toInstant().toEpochMilli()
    }

    /**
     * Cancels any previously scheduled reminders for [task] and schedules all future
     * reminder offsets in [OrbitTask.multipleReminderOffsets].
     * Returns the list of scheduled UTC trigger timestamps in milliseconds.
     */
    fun scheduleTaskReminder(
        context: Context,
        task: OrbitTask,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Long> {
        if (task.id.isBlank()) return emptyList()

        val rawOffsets = task.multipleReminderOffsets.ifEmpty {
            if (task.reminderOffsetMinutes >= 0) listOf(task.reminderOffsetMinutes) else emptyList()
        }.distinct().sorted()

        // Always cancel any prior alarms for this task (including higher offset indices) first
        cancelTaskReminder(context, task.id, knownOffsetCount = rawOffsets.size)

        if (task.isCompleted || task.isTrashed || rawOffsets.isEmpty()) {
            return emptyList()
        }

        val targetDay = when {
            task.scheduledDateEpochDay > 0L -> task.scheduledDateEpochDay
            task.dueDateEpochDay > 0L -> task.dueDateEpochDay
            else -> return emptyList()
        }
        val hour = if (task.scheduledStartHour in 0..23) task.scheduledStartHour else 9
        val minute = task.scheduledStartMinute.coerceIn(0, 59)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return emptyList()
        val scheduledTimes = mutableListOf<Long>()

        rawOffsets.forEachIndexed { idx, offsetMins ->
            val triggerMillis = computeTriggerMillis(
                targetEpochDay = targetDay,
                hour = hour,
                minute = minute,
                offsetMinutes = offsetMins,
                zoneId = zoneId
            )
            if (triggerMillis > nowMillis) {
                val intent = reminderIntent(context, task.id, idx).apply {
                    putExtra(EXTRA_OFFSET_MINUTES, offsetMins)
                }
                val requestCode = requestCodeForOffset(task.id, idx)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } catch (_: SecurityException) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                scheduledTimes.add(triggerMillis)
            }
        }

        scheduledOffsetCountByTaskId[task.id] = rawOffsets.size
        return scheduledTimes
    }

    /**
     * Cancels all scheduled reminder offsets for [taskId], checking the maximum of
     * [DEFAULT_MAX_REMINDER_SLOTS], [knownOffsetCount], and the tracked offset count in
     * [scheduledOffsetCountByTaskId] so tasks with >5 offsets never leave orphaned alarms.
     */
    fun cancelTaskReminder(
        context: Context,
        taskId: String,
        knownOffsetCount: Int = DEFAULT_MAX_REMINDER_SLOTS
    ) {
        if (taskId.isBlank()) return
        val tracked = scheduledOffsetCountByTaskId.remove(taskId) ?: 0
        val totalSlots = maxOf(DEFAULT_MAX_REMINDER_SLOTS, knownOffsetCount, tracked)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        for (idx in 0 until totalSlots) {
            val intent = reminderIntent(context, taskId, idx)
            val requestCode = requestCodeForOffset(taskId, idx)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager?.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            // Remove alarms from the pre-launch request-code-only implementation on upgrade.
            val legacyIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
                action = ACTION_FIRE_REMINDER
                putExtra(EXTRA_TASK_ID, taskId)
            }
            PendingIntent.getBroadcast(context, requestCode, legacyIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { legacy ->
                alarmManager?.cancel(legacy)
                legacy.cancel()
            }
        }
        NotificationManagerCompat.from(context).cancel(taskId, 0)
        NotificationManagerCompat.from(context).cancel(taskId.hashCode()) // Retire legacy untagged notifications.
    }

    fun triggerImmediateReminder(
        context: Context,
        task: OrbitTask,
        quietHoursActive: Boolean
    ): String {
        ensureChannels(context)
        if (quietHoursActive && task.reminderEscalation != "critical" && task.priority != "p1") {
            return "Queued in Quiet Hours Digest (DND active — only P1/Critical reminders bypass)"
        }
        if (!hasNotificationPermission(context)) {
            return "In-App Banner Active (Grant Notification permission in Settings for system alerts)"
        }

        val channelId = when {
            task.assigneeName in listOf("Birthday", "Anniversary", "Bill", "Medication") -> CHANNEL_SPECIAL
            task.reminderEscalation == "critical" || task.priority == "p1" -> CHANNEL_CRITICAL
            else -> CHANNEL_STANDARD
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            data = taskActionUri(task.id, ACTION_COMPLETE_TASK)
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val completePending = PendingIntent.getBroadcast(
            context,
            task.id.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            data = taskActionUri(task.id, ACTION_SNOOZE_TASK)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_SNOOZE_MINUTES, 15)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            task.id.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tomorrowIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
            action = ACTION_RESCHEDULE_TOMORROW
            data = taskActionUri(task.id, ACTION_RESCHEDULE_TOMORROW)
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val tomorrowPending = PendingIntent.getBroadcast(
            context,
            task.id.hashCode() + 3,
            tomorrowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryPrefix = if (task.assigneeName.isNotBlank()) "${task.assigneeName} • " else ""
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Yorulune • $categoryPrefix${task.priority.uppercase()}")
            .setContentText(task.title)
            .setSubText("${task.projectName} • ${describeAlarmDeliveryGuarantee(context)}")
            .setContentIntent(openPending)
            .setGroup("orbit_task_reminders")
            .setPriority(
                if (channelId == CHANNEL_CRITICAL || channelId == CHANNEL_SPECIAL) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setOngoing(task.reminderEscalation == "persistent")
            .setAutoCancel(true)
            .addAction(0, "Complete", completePending)
            .addAction(0, "Snooze 15m", snoozePending)
            .addAction(0, "Tomorrow", tomorrowPending)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(task.id, 0, notification)
            "Notification delivered (Complete / Snooze 15m / Tomorrow actions ready)"
        } catch (e: SecurityException) {
            "In-App Reminder Active (System notification permission restricted)"
        }
    }
}

/**
 * Handles background notification actions (Complete, Snooze, Reschedule Tomorrow),
 * home-screen widget task completion & mode cycling, and device reboot / time-zone
 * reminder rescheduling.
 */
class OrbitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleIntentSuspend(context, intent)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        /**
         * Suspendable entry point used by both [onReceive] in production and deterministic
         * Robolectric tests verifying notification actions, recurring task progression,
         * reboot/time-zone rescheduling, and home-screen widget synchronization.
         */
        suspend fun handleIntentSuspend(
            context: Context,
            intent: Intent,
            daoOverride: com.example.data.local.OrbitDao? = null,
            todayEpochDay: Long = LocalDate.now().toEpochDay()
        ) {
            val action = intent.action.orEmpty()
            val taskId = intent.getStringExtra(OrbitReminderScheduler.EXTRA_TASK_ID).orEmpty()
            val dao = daoOverride ?: OrbitLocalDatabase.getInstance(context).orbitDao()

            when (action) {
                OrbitReminderScheduler.ACTION_FIRE_REMINDER -> {
                    val entity = dao.getTaskById(taskId)
                    val prefs = dao.getPreferencesOnce()?.toDomain()
                    if (entity != null) {
                        val task = entity.toDomain()
                        if (!task.isCompleted && !task.isTrashed) {
                            OrbitReminderScheduler.triggerImmediateReminder(
                                context = context,
                                task = task,
                                quietHoursActive = prefs?.quietHoursEnabled == true
                            )
                        }
                    }
                }

                OrbitReminderScheduler.ACTION_COMPLETE_TASK -> {
                    val entity = dao.getTaskById(taskId)
                    if (entity != null) {
                        val task = entity.toDomain()
                        val updatedHistory = (task.historyLog + "Completed via notification/widget").takeLast(20)
                        dao.upsertTask(
                            LocalTaskEntity.fromDomain(
                                task.copy(
                                    status = "done",
                                    historyLog = updatedHistory
                                )
                            )
                        )
                        // Unlock any dependent tasks that were waiting on this task
                        val dependents = dao.getAllTasksOnce().map { it.toDomain() }
                            .filter { it.dependsOnTaskId == task.id && !it.isCompleted && !it.isTrashed }
                        dependents.forEach { dep ->
                            dao.upsertTask(
                                LocalTaskEntity.fromDomain(
                                    dep.copy(
                                        dependsOnTaskId = "",
                                        status = if (dep.status == "blocked") "todo" else dep.status,
                                        historyLog = (dep.historyLog + "Prerequisite '${task.title}' completed").takeLast(20)
                                    )
                                )
                            )
                        }

                        // If recurring, deterministically schedule the next occurrence
                        if (task.recurrenceRule.isNotBlank()) {
                            val baseDay = if (task.recurrenceRule == "AFTER_COMPLETION") todayEpochDay
                            else if (task.dueDateEpochDay > 0) task.dueDateEpochDay
                            else todayEpochDay
                            val nextDay = SmartTaskParser.calculateNextRecurrenceDay(baseDay, task.recurrenceRule)
                            val nextTask = task.copy(
                                id = "",
                                status = "todo",
                                dueDateEpochDay = nextDay,
                                scheduledDateEpochDay = nextDay,
                                subtasks = task.parsedSubtasks().map { it.copy(isDone = false).encode() },
                                createdAt = null,
                                updatedAt = null
                            )
                            val nextEntity = LocalTaskEntity.fromDomain(nextTask)
                            dao.upsertTask(nextEntity)
                            OrbitReminderScheduler.scheduleTaskReminder(context, nextEntity.toDomain())
                        }
                        OrbitReminderScheduler.cancelTaskReminder(context, task.id, task.multipleReminderOffsets.size)
                        OrbitHomeWidgetProvider.requestUpdate(context)
                    }
                }

                OrbitReminderScheduler.ACTION_SNOOZE_TASK -> {
                    val snoozeMins = intent.getIntExtra(OrbitReminderScheduler.EXTRA_SNOOZE_MINUTES, 15).coerceAtLeast(5)
                    val entity = dao.getTaskById(taskId)
                    if (entity != null) {
                        val task = entity.toDomain()
                        val now = LocalTime.now().plusMinutes(snoozeMins.toLong())
                        val updated = task.copy(
                            scheduledDateEpochDay = todayEpochDay,
                            scheduledStartHour = now.hour,
                            scheduledStartMinute = now.minute,
                            reminderOffsetMinutes = 0,
                            multipleReminderOffsets = listOf(0),
                            postponeCount = (task.postponeCount + 1).coerceAtMost(999)
                        )
                        dao.upsertTask(LocalTaskEntity.fromDomain(updated))
                        OrbitReminderScheduler.cancelTaskReminder(context, task.id, task.multipleReminderOffsets.size)
                        OrbitReminderScheduler.scheduleTaskReminder(context, updated)
                        OrbitHomeWidgetProvider.requestUpdate(context)
                    }
                }

                OrbitReminderScheduler.ACTION_RESCHEDULE_TOMORROW -> {
                    val entity = dao.getTaskById(taskId)
                    if (entity != null) {
                        val task = entity.toDomain()
                        val tomorrow = todayEpochDay + 1
                        val updated = task.copy(
                            dueDateEpochDay = tomorrow,
                            scheduledDateEpochDay = tomorrow,
                            scheduledStartHour = 9,
                            scheduledStartMinute = 0,
                            postponeCount = (task.postponeCount + 1).coerceAtMost(999)
                        )
                        dao.upsertTask(LocalTaskEntity.fromDomain(updated))
                        OrbitReminderScheduler.cancelTaskReminder(context, task.id, task.multipleReminderOffsets.size)
                        OrbitReminderScheduler.scheduleTaskReminder(context, updated)
                        OrbitHomeWidgetProvider.requestUpdate(context)
                    }
                }

                OrbitReminderScheduler.ACTION_CYCLE_WIDGET_VARIANT -> {
                    val currentPrefs = dao.getPreferencesOnce()?.toDomain() ?: com.example.data.model.UserPreferencesDoc()
                    val variants = NativeHomeWidgetVariant.entries
                    val currentIdx = variants.indexOfFirst { it.key == currentPrefs.homeWidgetVariant }.coerceAtLeast(0)
                    val nextVariant = variants[(currentIdx + 1) % variants.size]
                    dao.upsertPreferences(
                        LocalPreferencesEntity.fromDomain(
                            currentPrefs.copy(homeWidgetVariant = nextVariant.key)
                        )
                    )
                    OrbitHomeWidgetProvider.requestUpdate(context)
                }

                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    val allActive = dao.getAllTasksOnce().map { it.toDomain() }
                        .filter { !it.isCompleted && !it.isTrashed }
                    allActive.forEach { task ->
                        OrbitReminderScheduler.scheduleTaskReminder(context, task)
                    }
                    OrbitHomeWidgetProvider.requestUpdate(context)
                }
            }
        }
    }
}

/**
 * Synthesizes clean, zero-dependency, royalty-free procedural ambient soundscapes
 * (Cosmic Rain, Deep Space Brown, 40Hz Gamma Flow, Forest Breeze, Warm Vinyl Static)
 * with live volume mixing using Android AudioTrack.
 */
class AmbientSoundEngine {
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var volumeGain: Float = 0.7f

    fun setVolume(volume: Float) {
        volumeGain = volume.coerceIn(0.05f, 1.0f)
    }

    fun start(soundType: String, scope: CoroutineScope, volume: Float = volumeGain) {
        stop()
        volumeGain = volume.coerceIn(0.05f, 1.0f)
        if (soundType == "Silent" || soundType.isBlank()) return

        audioJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            val buffer = ShortArray(2048)
            var lastOut = 0.0
            var phase1 = 0.0
            var phase2 = 0.0

            try {
                track.play()
                while (isActive) {
                    val gain = (volumeGain * 5200.0)
                    for (i in buffer.indices) {
                        val sample = when (soundType) {
                            "Rainy Tokyo Café" -> {
                                phase1 += 2.0 * PI * 110.0 / sampleRate
                                val white = Random.nextDouble(-1.0, 1.0)
                                val vinylDust = if (Random.nextDouble() > 0.997) Random.nextDouble(-0.45, 0.45) else 0.0
                                lastOut = (lastOut + (0.05 * white)) / 1.05
                                val warmChord = sin(phase1) * 0.05 + sin(phase1 * 1.5) * 0.04
                                ((lastOut * 2.0) + warmChord + vinylDust).coerceIn(-0.85, 0.85)
                            }
                            "Night Train Ride" -> {
                                phase1 += 2.0 * PI * 1.35 / sampleRate
                                val railPulse = if (sin(phase1) > 0.92 || sin(phase1 + 0.55) > 0.94) 1.65 else 0.75
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.025 * white)) / 1.025
                                (lastOut * 2.6 * railPulse).coerceIn(-0.85, 0.85)
                            }
                            "Kyoto Bamboo Wind" -> {
                                phase1 += 2.0 * PI * 0.18 / sampleRate
                                phase2 += 2.0 * PI * 528.0 / sampleRate
                                val sway = 0.5 + 0.4 * sin(phase1)
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.035 * white)) / 1.035
                                val softChime = sin(phase2) * 0.03 * sway
                                ((lastOut * 2.5 * sway) + softChime).coerceIn(-0.85, 0.85)
                            }
                            "Deep Space Brown" -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.02 * white)) / 1.02
                                (lastOut * 3.5).coerceIn(-0.9, 0.9)
                            }
                            "40Hz Gamma Flow" -> {
                                phase1 += 2.0 * PI * 200.0 / sampleRate
                                phase2 += 2.0 * PI * 240.0 / sampleRate
                                (sin(phase1) * 0.15 + sin(phase2) * 0.15)
                            }
                            "Forest Breeze" -> {
                                phase1 += 2.0 * PI * 0.25 / sampleRate
                                val mod = 0.55 + 0.35 * sin(phase1)
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.04 * white)) / 1.04
                                (lastOut * 2.8 * mod).coerceIn(-0.85, 0.85)
                            }
                            "Warm Vinyl Static" -> {
                                val white = Random.nextDouble(-1.0, 1.0)
                                val crackle = if (Random.nextDouble() > 0.996) Random.nextDouble(-0.6, 0.6) else 0.0
                                lastOut = (lastOut + (0.08 * white)) / 1.08
                                ((lastOut * 1.6) + crackle).coerceIn(-0.85, 0.85)
                            }
                            else -> { // "Cosmic Rain" soft filtered pink-brown rain
                                val white = Random.nextDouble(-1.0, 1.0)
                                lastOut = (lastOut + (0.06 * white)) / 1.06
                                (lastOut * 2.2).coerceIn(-0.8, 0.8)
                            }
                        }
                        buffer[i] = (sample * gain).toInt().toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (ignored: Exception) {
                // Audio unavailable on headless environment
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    fun stop() {
        audioJob?.cancel()
        audioJob = null
        audioTrack = null
    }
}
