package com.example.widget

import androidx.compose.ui.graphics.toArgb
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.OrbitLocalDatabase
import com.example.data.model.NativeHomeWidgetVariant
import com.example.data.model.SmartTaskParser
import com.example.data.notification.OrbitReminderReceiver
import com.example.data.notification.OrbitReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OrbitHomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        updateAllWidgets(context, manager, intArrayOf(id))
    }

    companion object {
        fun requestUpdate(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, OrbitHomeWidgetProvider::class.java))
            if (ids != null && ids.isNotEmpty()) {
                updateAllWidgets(appContext, manager, ids)
            }
        }

        fun requestPinToHomeScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, OrbitHomeWidgetProvider::class.java)
                if (manager.isRequestPinAppWidgetSupported) {
                    return manager.requestPinAppWidget(provider, null, null)
                }
            }
            return false
        }

        private fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = OrbitLocalDatabase.getInstance(context).orbitDao()
                val today = LocalDate.now().toEpochDay()
                val allTasks = dao.getAllTasksOnce().map { it.toDomain() }.filter { !it.isTrashed }
                val openTasks = allTasks.filter { !it.isCompleted }.sortedBy { it.priority }
                val doneCount = allTasks.count { it.isCompleted }
                val totalCount = allTasks.size.coerceAtLeast(1)
                val pct = (doneCount * 100) / totalCount
                val habits = dao.getAllHabitsOnce().map { it.toDomain() }
                val projects = dao.getAllProjectsOnce().map { it.toDomain() }
                val focusSessions = dao.getAllFocusSessionsOnce().map { it.toDomain() }
                val prefs = dao.getPreferencesOnce()?.toDomain()
                val variant = NativeHomeWidgetVariant.fromKey(prefs?.homeWidgetVariant ?: "today_checklist")

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val launchPending = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val cycleIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
                    action = OrbitReminderScheduler.ACTION_CYCLE_WIDGET_VARIANT
                }
                val cyclePending = PendingIntent.getBroadcast(
                    context,
                    9001,
                    cycleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.orbit_appwidget)
                    val height = appWidgetManager.getAppWidgetOptions(widgetId)
                        .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 240)
                    views.setViewVisibility(R.id.widget_header_title, if (height < 130) android.view.View.GONE else android.view.View.VISIBLE)
                    for (id in listOf(R.id.widget_task_line_1, R.id.widget_task_line_2, R.id.widget_task_line_3)) {
                        views.setViewVisibility(id, if (height < 160) android.view.View.GONE else android.view.View.VISIBLE)
                    }
                    val padding = ((if (height < 160) 8 else 14) * context.resources.displayMetrics.density).toInt()
                    views.setViewPadding(R.id.widget_root_container, padding, padding, padding, padding)
                    views.setContentDescription(R.id.widget_root_container, "Yorulune ${variant.title}")
                    val systemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    val mode = prefs?.themeMode ?: "system"
                    val dark = mode == "dark" || mode == "oled" || (mode in listOf("system", "dynamic") && systemDark)
                    val palette = com.example.ui.theme.getAccentPalette(prefs?.accentPreset ?: "indigo_night")
                    val bg = if (mode == "oled") 0xFF000000.toInt() else if (dark) 0xFF161D30.toInt() else palette.softBgLight.toArgb()
                    val ink = if (dark) 0xFFF6F1E9.toInt() else 0xFF221F1B.toInt()
                    val accent = com.example.ui.theme.parseKissatenAccent(prefs?.accentPreset.orEmpty()) ?: palette.primary
                    views.setInt(R.id.widget_root_container, "setBackgroundColor", bg)
                    for (id in listOf(R.id.widget_summary_headline, R.id.widget_task_line_1, R.id.widget_task_line_2, R.id.widget_task_line_3)) views.setTextColor(id, ink)
                    views.setTextColor(R.id.widget_header_title, ink)
                    views.setTextColor(R.id.widget_progress_badge, ink)
                    views.setInt(R.id.widget_progress_badge, "setBackgroundColor", bg)
                    views.setInt(R.id.widget_footer_action, "setBackgroundColor", accent.toArgb())
                    views.setTextColor(R.id.widget_footer_action, com.example.ui.theme.kissatenInk(accent).toArgb())

                    views.setTextViewText(
                        R.id.widget_header_title,
                        "YORULUNE • ${variant.title.uppercase()} (${variant.number}/10)"
                    )
                    views.setTextViewText(
                        R.id.widget_progress_badge,
                        "Mode ↻ • $pct%"
                    )

                    when (variant) {
                        NativeHomeWidgetVariant.TODAY_CHECKLIST -> {
                            val todayOpen = openTasks.filter { it.dueDateEpochDay <= today || it.scheduledDateEpochDay == today }
                                .ifEmpty { openTasks }
                            views.setTextViewText(
                                R.id.widget_summary_headline,
                                if (todayOpen.isEmpty()) "All clear! Zero open tasks today"
                                else "${todayOpen.size} Today Tasks (Tap item to complete):"
                            )
                            val t1 = todayOpen.getOrNull(0)
                            val t2 = todayOpen.getOrNull(1)
                            val t3 = todayOpen.getOrNull(2)
                            views.setTextViewText(R.id.widget_task_line_1, t1?.let { "◯ [${it.priority.uppercase()}] ${it.title}" } ?: "✓ All top tasks done")
                            views.setTextViewText(R.id.widget_task_line_2, t2?.let { "◯ [${it.priority.uppercase()}] ${it.title}" } ?: "• Tap + below to quick capture")
                            views.setTextViewText(R.id.widget_task_line_3, t3?.let { "◯ [${it.priority.uppercase()}] ${it.title}" } ?: "• $doneCount/$totalCount completed ($pct%)")
                            bindTaskCompleteClick(context, views, R.id.widget_task_line_1, t1?.id, launchPending)
                            bindTaskCompleteClick(context, views, R.id.widget_task_line_2, t2?.id, launchPending)
                            bindTaskCompleteClick(context, views, R.id.widget_task_line_3, t3?.id, launchPending)
                        }

                        NativeHomeWidgetVariant.QUICK_CAPTURE -> {
                            val inboxCount = openTasks.count { it.status == "inbox" }
                            views.setTextViewText(R.id.widget_summary_headline, "Quick offline task capture")
                            views.setTextViewText(R.id.widget_task_line_1, "• Inbox Queue: $inboxCount items awaiting triage")
                            views.setTextViewText(R.id.widget_task_line_2, "• Supports #tags, @projects, p1..p4 & times")
                            views.setTextViewText(R.id.widget_task_line_3, "• Example: \"Call Sarah tomorrow at 4pm p1\"")
                            views.setOnClickPendingIntent(R.id.widget_task_line_1, launchPending)
                            views.setOnClickPendingIntent(R.id.widget_task_line_2, launchPending)
                            views.setOnClickPendingIntent(R.id.widget_task_line_3, launchPending)
                        }

                        NativeHomeWidgetVariant.MINI_CALENDAR -> {
                            val dateHeader = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                            val timedToday = openTasks.filter { it.scheduledStartHour >= 0 }.take(3)
                            views.setTextViewText(R.id.widget_summary_headline, "📅 $dateHeader")
                            views.setTextViewText(
                                R.id.widget_task_line_1,
                                timedToday.getOrNull(0)?.let { "${SmartTaskParser.formatHourMinute(it.scheduledStartHour, it.scheduledStartMinute)} • ${it.title}" }
                                    ?: "09:00 • Open Focus Slot"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_2,
                                timedToday.getOrNull(1)?.let { "${SmartTaskParser.formatHourMinute(it.scheduledStartHour, it.scheduledStartMinute)} • ${it.title}" }
                                    ?: "14:00 • Open Deep Work Slot"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_3,
                                timedToday.getOrNull(2)?.let { "${SmartTaskParser.formatHourMinute(it.scheduledStartHour, it.scheduledStartMinute)} • ${it.title}" }
                                    ?: "17:30 • Evening Review"
                            )
                        }

                        NativeHomeWidgetVariant.UPCOMING_REMINDERS -> {
                            val special = openTasks.filter { it.assigneeName.isNotBlank() || it.reminderOffsetMinutes >= 0 }.take(3)
                            views.setTextViewText(R.id.widget_summary_headline, "🔔 Next Scheduled Reminders")
                            views.setTextViewText(
                                R.id.widget_task_line_1,
                                special.getOrNull(0)?.let { "• ${it.title} (${SmartTaskParser.formatEpochDay(it.dueDateEpochDay, today)})" }
                                    ?: "• No pending alarms"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_2,
                                special.getOrNull(1)?.let { "• ${it.title} (${SmartTaskParser.formatEpochDay(it.dueDateEpochDay, today)})" }
                                    ?: "• Quiet hours: ${if (prefs?.quietHoursEnabled == true) "Active" else "Off"}"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_3,
                                special.getOrNull(2)?.let { "• ${it.title} (${SmartTaskParser.formatEpochDay(it.dueDateEpochDay, today)})" }
                                    ?: "• AlarmManager boot-synced"
                            )
                        }

                        NativeHomeWidgetVariant.HABIT_PROGRESS -> {
                            val doneHabits = habits.count { it.isCompletedOn(today) }
                            views.setTextViewText(
                                R.id.widget_summary_headline,
                                "🔥 Habits: $doneHabits of ${habits.size} Completed Today"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_1,
                                habits.getOrNull(0)?.let { "${if (it.isCompletedOn(today)) "✓" else "◯"} ${it.title} (🔥 ${it.currentStreak}d)" }
                                    ?: "• Morning meditation"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_2,
                                habits.getOrNull(1)?.let { "${if (it.isCompletedOn(today)) "✓" else "◯"} ${it.title} (🔥 ${it.currentStreak}d)" }
                                    ?: "• Drink 8 glasses of water"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_3,
                                habits.getOrNull(2)?.let { "${if (it.isCompletedOn(today)) "✓" else "◯"} ${it.title} (🔥 ${it.currentStreak}d)" }
                                    ?: "• Read 30 minutes"
                            )
                        }

                        NativeHomeWidgetVariant.PROGRESS_RING -> {
                            val todayFocusMins = focusSessions.filter { it.completedEpochDay == today }.sumOf { it.durationMinutes }
                            val doneHabits = habits.count { it.isCompletedOn(today) }
                            views.setTextViewText(R.id.widget_summary_headline, "◎ Daily Score: $pct% Complete")
                            views.setTextViewText(R.id.widget_task_line_1, "• Tasks: $doneCount / $totalCount completed")
                            views.setTextViewText(R.id.widget_task_line_2, "• Focus Cockpit: ${todayFocusMins}m / ${prefs?.dailyFocusGoalMinutes ?: 120}m")
                            views.setTextViewText(R.id.widget_task_line_3, "• Habit Rituals: $doneHabits / ${habits.size} checked in")
                        }

                        NativeHomeWidgetVariant.IMPORTANT_TASKS -> {
                            val p1p2 = openTasks.filter { it.priority in listOf("p1", "p2") || it.isImportant }
                            views.setTextViewText(R.id.widget_summary_headline, "🎯 ${p1p2.size} High-Impact Priorities (P1/P2)")
                            views.setTextViewText(R.id.widget_task_line_1, p1p2.getOrNull(0)?.let { "❗ [${it.priority.uppercase()}] ${it.title}" } ?: "✓ All P1/P2 priorities clear!")
                            views.setTextViewText(R.id.widget_task_line_2, p1p2.getOrNull(1)?.let { "❗ [${it.priority.uppercase()}] ${it.title}" } ?: "• Stay focused on top outcomes")
                            views.setTextViewText(R.id.widget_task_line_3, p1p2.getOrNull(2)?.let { "❗ [${it.priority.uppercase()}] ${it.title}" } ?: "• Zero distractions")
                        }

                        NativeHomeWidgetVariant.DEADLINE_COUNTDOWN -> {
                            val upcomingDeadlines = openTasks.filter { it.dueDateEpochDay >= today }.sortedBy { it.dueDateEpochDay }
                            views.setTextViewText(R.id.widget_summary_headline, "⏳ Milestone & Deadline Countdowns")
                            views.setTextViewText(
                                R.id.widget_task_line_1,
                                upcomingDeadlines.getOrNull(0)?.let { "T-${(it.dueDateEpochDay - today).coerceAtLeast(0)}d • ${it.title}" }
                                    ?: "• No upcoming deadlines"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_2,
                                upcomingDeadlines.getOrNull(1)?.let { "T-${(it.dueDateEpochDay - today).coerceAtLeast(0)}d • ${it.title}" }
                                    ?: "• Add deadlines in Task Inspector"
                            )
                            views.setTextViewText(
                                R.id.widget_task_line_3,
                                upcomingDeadlines.getOrNull(2)?.let { "T-${(it.dueDateEpochDay - today).coerceAtLeast(0)}d • ${it.title}" }
                                    ?: "• 100% deterministic tracking"
                            )
                        }

                        NativeHomeWidgetVariant.FOCUS_STATUS -> {
                            val totalMins = focusSessions.sumOf { it.durationMinutes }
                            val todayMins = focusSessions.filter { it.completedEpochDay == today }.sumOf { it.durationMinutes }
                            views.setTextViewText(R.id.widget_summary_headline, "⏱ Focus Cockpit • ${todayMins}m Today")
                            views.setTextViewText(R.id.widget_task_line_1, "• Total Deep Work Logged: ${totalMins}m (${focusSessions.size} sessions)")
                            views.setTextViewText(R.id.widget_task_line_2, "• Default Pomodoro: ${prefs?.defaultPomodoroMinutes ?: 25}m / Break: ${prefs?.defaultBreakMinutes ?: 5}m")
                            views.setTextViewText(R.id.widget_task_line_3, "• Procedural Audio: Cosmic Rain, Brown, 40Hz")
                        }

                        NativeHomeWidgetVariant.PROJECT_PROGRESS -> {
                            views.setTextViewText(R.id.widget_summary_headline, "📁 Active Projects (${projects.size})")
                            projects.take(3).forEachIndexed { index, proj ->
                                val projTasks = allTasks.filter { it.projectId == proj.id }
                                val projDone = projTasks.count { it.isCompleted }
                                val projPct = if (projTasks.isEmpty()) 100 else (projDone * 100) / projTasks.size
                                val lineId = when (index) {
                                    0 -> R.id.widget_task_line_1
                                    1 -> R.id.widget_task_line_2
                                    else -> R.id.widget_task_line_3
                                }
                                views.setTextViewText(lineId, "• ${proj.name}: $projDone/${projTasks.size} ($projPct%)")
                            }
                        }
                    }

                    views.setOnClickPendingIntent(R.id.widget_progress_badge, cyclePending)
                    views.setOnClickPendingIntent(R.id.widget_root_container, launchPending)
                    views.setOnClickPendingIntent(R.id.widget_footer_action, launchPending)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }

        private fun bindTaskCompleteClick(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            taskId: String?,
            fallbackPending: PendingIntent
        ) {
            if (taskId.isNullOrBlank()) {
                views.setOnClickPendingIntent(viewId, fallbackPending)
                return
            }
            val completeIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
                action = OrbitReminderScheduler.ACTION_COMPLETE_TASK
                putExtra(OrbitReminderScheduler.EXTRA_TASK_ID, taskId)
            }
            val completePending = PendingIntent.getBroadcast(
                context,
                taskId.hashCode() + viewId,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(viewId, completePending)
        }
    }
}
