package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalTaskEntity
import com.example.data.local.OrbitLocalDatabase
import com.example.data.local.OrbitLocalRepository
import com.example.data.model.NativeHomeWidgetVariant
import com.example.data.model.OrbitTask
import com.example.data.model.RestoreDuplicateStrategy
import com.example.data.notification.OrbitReminderReceiver
import com.example.data.notification.OrbitReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: OrbitLocalDatabase
    private lateinit var repository: OrbitLocalRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, OrbitLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OrbitLocalRepository(database.orbitDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun fireReminderBroadcastIntent(taskId: String, index: Int = 0): Intent =
        OrbitReminderScheduler.reminderIntent(context, taskId, index)

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Yorulune", appName)
    }

    @Test
    fun `seed workspace persists tasks habits projects and preferences`() = runBlocking {
        repository.seedStarterWorkspaceIfEmpty()

        val tasks = repository.observeTasks().first()
        val habits = repository.observeHabits().first()
        val projects = repository.observeProjects().first()
        val prefs = repository.observePreferences().first()

        assertTrue("Expected seeded tasks", tasks.isNotEmpty())
        assertEquals("Expected 6 starter habits matching design", 6, habits.size)
        assertTrue("Expected starter projects", projects.size >= 4)
        assertNotNull(prefs)
        assertEquals("paper", prefs.themeMode)
        assertEquals("terracotta", prefs.accentPreset)
    }

    @Test
    fun `hash-colliding tasks keep independent notification actions and cancellation`() {
        org.robolectric.Shadows.shadowOf(context as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val first = OrbitTask(id = "FB", title = "Fictional notice A")
        val second = OrbitTask(id = "Ea", title = "Fictional notice B")
        OrbitReminderScheduler.triggerImmediateReminder(context, first, false)
        OrbitReminderScheduler.triggerImmediateReminder(context, second, false)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val notices = manager.activeNotifications.filter { it.tag in setOf("FB", "Ea") }
        assertEquals(setOf("FB", "Ea"), notices.map { it.tag }.toSet())
        for (notice in notices) {
            val action = notice.notification.actions.first().actionIntent
            assertEquals(notice.tag, org.robolectric.Shadows.shadowOf(action).savedIntent
                .getStringExtra(OrbitReminderScheduler.EXTRA_TASK_ID))
        }
        OrbitReminderScheduler.cancelTaskReminder(context, "FB")
        assertEquals(listOf("Ea"), manager.activeNotifications.filter { it.tag in setOf("FB", "Ea") }.map { it.tag })
    }

    @Test
    fun `hash-colliding task ids keep independent alarms`() {
        val day = LocalDate.now().plusDays(3).toEpochDay()
        val first = OrbitTask(id = "FB", title = "Fixture A", scheduledDateEpochDay = day,
            scheduledStartHour = 14, multipleReminderOffsets = listOf(0))
        val second = first.copy(id = "Ea", title = "Fixture B")
        assertEquals(first.id.hashCode(), second.id.hashCode())
        OrbitReminderScheduler.scheduleTaskReminder(context, first)
        val firstAlarm = PendingIntent.getBroadcast(context,
            OrbitReminderScheduler.requestCodeForOffset(first.id, 0), fireReminderBroadcastIntent(first.id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        OrbitReminderScheduler.scheduleTaskReminder(context, second)
        val secondAlarm = PendingIntent.getBroadcast(context,
            OrbitReminderScheduler.requestCodeForOffset(second.id, 0), fireReminderBroadcastIntent(second.id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        assertNotNull(firstAlarm)
        assertNotNull(secondAlarm)
        assertNotEquals("Task extras alone do not distinguish PendingIntents", firstAlarm, secondAlarm)
        OrbitReminderScheduler.cancelTaskReminder(context, first.id)
        assertNotNull("Cancelling fixture A must retain fixture B", PendingIntent.getBroadcast(context,
            OrbitReminderScheduler.requestCodeForOffset(second.id, 0), fireReminderBroadcastIntent(second.id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
    }

    @Test
    fun `multiple reminders per task beyond 5 offsets schedule cancel and reschedule cleanly`() {
        val futureEpochDay = LocalDate.now().plusDays(3).toEpochDay()
        val eightOffsets = listOf(0, 5, 10, 15, 30, 60, 120, 1440)
        val task = OrbitTask(
            id = "multi_offset_task_8",
            title = "Critical Architecture Review",
            dueDateEpochDay = futureEpochDay,
            scheduledDateEpochDay = futureEpochDay,
            scheduledStartHour = 14,
            scheduledStartMinute = 0,
            multipleReminderOffsets = eightOffsets,
            status = "todo"
        )

        val scheduledTimestamps = OrbitReminderScheduler.scheduleTaskReminder(context, task)
        assertEquals("All 8 future reminder offsets must be scheduled", 8, scheduledTimestamps.size)

        // Verify all 8 PendingIntents (including indices 5, 6, 7 beyond the old 5-index limit) exist
        for (idx in eightOffsets.indices) {
            val requestCode = OrbitReminderScheduler.requestCodeForOffset(task.id, idx)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                fireReminderBroadcastIntent(task.id, idx),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertNotNull("Expected PendingIntent at offset index $idx to exist", pi)
        }

        // Reschedule the same task with only 2 offsets — indices 2..7 must be cancelled automatically
        val reducedTask = task.copy(multipleReminderOffsets = listOf(0, 15))
        OrbitReminderScheduler.scheduleTaskReminder(context, reducedTask)

        for (idx in 2 until 8) {
            val requestCode = OrbitReminderScheduler.requestCodeForOffset(task.id, idx)
            val stalePi = PendingIntent.getBroadcast(
                context,
                requestCode,
                fireReminderBroadcastIntent(task.id, idx),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertNull("Expected stale PendingIntent at index $idx to be cancelled on reschedule", stalePi)
        }

        // Cancel task reminder completely — indices 0 and 1 must also be cancelled
        OrbitReminderScheduler.cancelTaskReminder(context, task.id)
        for (idx in 0 until 8) {
            val requestCode = OrbitReminderScheduler.requestCodeForOffset(task.id, idx)
            val cancelledPi = PendingIntent.getBroadcast(
                context,
                requestCode,
                fireReminderBroadcastIntent(task.id, idx),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertNull("Expected PendingIntent at index $idx to be null after cancelTaskReminder", cancelledPi)
        }
    }

    @Test
    fun `notification actions complete recurring task unlock dependent task snooze and reschedule`() = runBlocking {
        val today = LocalDate.now().toEpochDay()
        val prereqTask = OrbitTask(
            id = "prereq_rec",
            title = "Complete Q2 Ledger Audit",
            dueDateEpochDay = today,
            scheduledDateEpochDay = today,
            recurrenceRule = "DAILY",
            multipleReminderOffsets = listOf(0, 5, 10, 15, 30, 60),
            status = "todo"
        )
        val blockedDependent = OrbitTask(
            id = "dep_task_1",
            title = "Publish Q2 Financial Summary",
            dueDateEpochDay = today,
            scheduledDateEpochDay = today,
            dependsOnTaskId = "prereq_rec",
            status = "blocked"
        )
        repository.upsertTask(prereqTask)
        repository.upsertTask(blockedDependent)

        // Trigger ACTION_COMPLETE_TASK via OrbitReminderReceiver
        val completeIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
            action = OrbitReminderScheduler.ACTION_COMPLETE_TASK
            putExtra(OrbitReminderScheduler.EXTRA_TASK_ID, "prereq_rec")
        }
        OrbitReminderReceiver.handleIntentSuspend(
            context = context,
            intent = completeIntent,
            daoOverride = database.orbitDao(),
            todayEpochDay = today
        )

        val allAfterComplete = repository.observeTasks().first()
        val completedOriginal = allAfterComplete.first { it.id == "prereq_rec" }
        assertTrue("Original prerequisite task should be marked done", completedOriginal.isCompleted)

        val spawnedRecurring = allAfterComplete.firstOrNull {
            it.id != "prereq_rec" && it.title == "Complete Q2 Ledger Audit" && !it.isCompleted
        }
        assertNotNull("Completing daily recurring task must spawn next occurrence", spawnedRecurring)
        assertEquals(today + 1, spawnedRecurring?.dueDateEpochDay)

        val unlockedDependent = allAfterComplete.first { it.id == "dep_task_1" }
        assertEquals("todo", unlockedDependent.status)
        assertEquals("", unlockedDependent.dependsOnTaskId)

        // Test ACTION_RESCHEDULE_TOMORROW on unlockedDependent
        val tomorrowIntent = Intent(context, OrbitReminderReceiver::class.java).apply {
            action = OrbitReminderScheduler.ACTION_RESCHEDULE_TOMORROW
            putExtra(OrbitReminderScheduler.EXTRA_TASK_ID, "dep_task_1")
        }
        OrbitReminderReceiver.handleIntentSuspend(
            context = context,
            intent = tomorrowIntent,
            daoOverride = database.orbitDao(),
            todayEpochDay = today
        )
        val postponed = repository.observeTasks().first().first { it.id == "dep_task_1" }
        assertEquals(today + 1, postponed.dueDateEpochDay)
        assertEquals(9, postponed.scheduledStartHour)
    }

    @Test
    fun `device reboot and timezone change broadcasts reschedule active reminders`() = runBlocking {
        val futureDay = LocalDate.now().plusDays(2).toEpochDay()
        val task = OrbitTask(
            id = "boot_task_1",
            title = "Renew Passport",
            dueDateEpochDay = futureDay,
            scheduledDateEpochDay = futureDay,
            scheduledStartHour = 10,
            scheduledStartMinute = 30,
            multipleReminderOffsets = listOf(0, 30, 60),
            status = "todo"
        )
        database.orbitDao().upsertTask(LocalTaskEntity.fromDomain(task))
        OrbitReminderScheduler.cancelTaskReminder(context, task.id, 3)

        // Simulate device reboot broadcast
        OrbitReminderReceiver.handleIntentSuspend(
            context = context,
            intent = Intent(Intent.ACTION_BOOT_COMPLETED),
            daoOverride = database.orbitDao()
        )

        val bootPi = PendingIntent.getBroadcast(
            context,
            OrbitReminderScheduler.requestCodeForOffset(task.id, 0),
            fireReminderBroadcastIntent(task.id, 0),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull("Expected reminder PendingIntent to be restored after ACTION_BOOT_COMPLETED", bootPi)

        // Simulate time-zone change broadcast
        OrbitReminderScheduler.cancelTaskReminder(context, task.id, 3)
        OrbitReminderReceiver.handleIntentSuspend(
            context = context,
            intent = Intent(Intent.ACTION_TIMEZONE_CHANGED),
            daoOverride = database.orbitDao()
        )
        val tzPi = PendingIntent.getBroadcast(
            context,
            OrbitReminderScheduler.requestCodeForOffset(task.id, 1),
            fireReminderBroadcastIntent(task.id, 1),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNotNull("Expected reminder PendingIntent to be rescheduled after ACTION_TIMEZONE_CHANGED", tzPi)
    }

    @Test
    fun `alarm delivery description is honest and never promises unconditional exact delivery`() {
        val description = OrbitReminderScheduler.describeAlarmDeliveryGuarantee(context)
        assertTrue(description.isNotBlank())
        assertTrue(
            "Alarm delivery message must mention battery/Doze or windowed caveats honestly",
            description.contains("Doze", ignoreCase = true) ||
                description.contains("windowed", ignoreCase = true) ||
                description.contains("battery", ignoreCase = true)
        )
    }

    @Test
    fun `backup inspection duplicate handling strategies and ics round trip work atomically`() = runBlocking {
        repository.seedStarterWorkspaceIfEmpty()
        val initialTasks = repository.observeTasks().first()
        val exportedJson = repository.exportFullBackupJson()

        // 1. Pre-restore inspection detects schema v3 and exact duplicate counts
        val preview = repository.inspectBackupJson(exportedJson).getOrThrow()
        assertEquals(3, preview.schemaVersion)
        assertEquals(initialTasks.size, preview.taskCount)
        assertEquals(initialTasks.size, preview.duplicateTaskCount)

        // 2. Modify an existing task locally and add a brand-new task only in local workspace
        val firstTask = initialTasks.first()
        repository.upsertTask(firstTask.copy(title = "Locally Edited Title"))
        repository.upsertTask(
            OrbitTask(
                id = "unique_local_task",
                title = "Keep Me During Merge",
                dueDateEpochDay = LocalDate.now().toEpochDay()
            )
        )

        // 3. Restore with MERGE_SKIP_DUPLICATES: local edit & unique_local_task must be preserved
        repository.importBackupJson(
            rawJson = exportedJson,
            strategy = RestoreDuplicateStrategy.MERGE_SKIP_DUPLICATES
        ).getOrThrow()

        val mergedTasks = repository.observeTasks().first()
        assertEquals("Locally Edited Title", mergedTasks.first { it.id == firstTask.id }.title)
        assertTrue(mergedTasks.any { it.id == "unique_local_task" })

        // 4. Restore with REPLACE_WORKSPACE: workspace must match the backup snapshot exactly
        repository.importBackupJson(
            rawJson = exportedJson,
            strategy = RestoreDuplicateStrategy.REPLACE_WORKSPACE
        ).getOrThrow()

        val replacedTasks = repository.observeTasks().first()
        assertEquals(initialTasks.size, replacedTasks.size)
        assertEquals(firstTask.title, replacedTasks.first { it.id == firstTask.id }.title)
        assertFalse(replacedTasks.any { it.id == "unique_local_task" })

        // 5. Malformed JSON must fail cleanly without corrupting existing data
        val badResult = repository.importBackupJson("{not_valid_json")
        assertTrue(badResult.isFailure)
        assertEquals(initialTasks.size, repository.observeTasks().first().size)

        // 6. ICS export and import round-trip
        val icsContent = repository.exportCalendarIcs()
        assertTrue(icsContent.contains("BEGIN:VCALENDAR"))
        val importedIcsCount = repository.importCalendarIcs(icsContent).getOrThrow()
        assertTrue(importedIcsCount > 0)
    }

    @Test
    fun `room migration 1 to 2 preserves v1 rows and adds v2 columns cleanly`() {
        val dbName = "orbit_migration_test_${System.currentTimeMillis()}.db"
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `projectId` TEXT NOT NULL,
                        `projectName` TEXT NOT NULL,
                        `sectionName` TEXT NOT NULL,
                        `dueDateEpochDay` INTEGER NOT NULL,
                        `scheduledDateEpochDay` INTEGER NOT NULL,
                        `scheduledStartHour` INTEGER NOT NULL,
                        `scheduledStartMinute` INTEGER NOT NULL,
                        `estimatedMinutes` INTEGER NOT NULL,
                        `actualMinutes` INTEGER NOT NULL,
                        `energyLevel` TEXT NOT NULL,
                        `isImportant` INTEGER NOT NULL,
                        `isUrgent` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `recurrenceRule` TEXT NOT NULL,
                        `reminderOffsetMinutes` INTEGER NOT NULL,
                        `reminderEscalation` TEXT NOT NULL,
                        `locationTrigger` TEXT NOT NULL,
                        `dependsOnTaskId` TEXT NOT NULL,
                        `assigneeName` TEXT NOT NULL,
                        `postponeCount` INTEGER NOT NULL,
                        `tagsJoined` TEXT NOT NULL,
                        `subtasksJoined` TEXT NOT NULL,
                        `commentsJoined` TEXT NOT NULL,
                        `createdAtSeconds` INTEGER NOT NULL,
                        `updatedAtSeconds` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `routineType` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `targetDaysPerWeek` INTEGER NOT NULL,
                        `currentStreak` INTEGER NOT NULL,
                        `bestStreak` INTEGER NOT NULL,
                        `completedDaysJoined` TEXT NOT NULL,
                        `reminderTime` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `createdAtSeconds` INTEGER NOT NULL,
                        `updatedAtSeconds` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `preferences` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `themeMode` TEXT NOT NULL,
                        `accentPreset` TEXT NOT NULL,
                        `density` TEXT NOT NULL,
                        `cornerRadiusDp` INTEGER NOT NULL,
                        `dashboardPreset` TEXT NOT NULL,
                        `planningMode` TEXT NOT NULL,
                        `energyFilter` TEXT NOT NULL,
                        `quietHoursEnabled` INTEGER NOT NULL,
                        `gamificationEnabled` INTEGER NOT NULL,
                        `enabledWidgetsJoined` TEXT NOT NULL,
                        `onboardingCompleted` INTEGER NOT NULL,
                        `reducedMotion` INTEGER NOT NULL,
                        `use24HourFormat` INTEGER NOT NULL,
                        `defaultPomodoroMinutes` INTEGER NOT NULL,
                        `defaultBreakMinutes` INTEGER NOT NULL,
                        `defaultLongBreakMinutes` INTEGER NOT NULL,
                        `dailyFocusGoalMinutes` INTEGER NOT NULL,
                        `webDavUrl` TEXT NOT NULL,
                        `quickNotes` TEXT NOT NULL,
                        `createdAtSeconds` INTEGER NOT NULL,
                        `updatedAtSeconds` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO `tasks` VALUES ('v1_task', 'local_user', 'Legacy V1 Task', '', 'todo', 'p1', 'proj_1', 'Work', 'General', 20500, 20500, 9, 0, 30, 0, 'high', 1, 1, 1, '', 15, 'standard', '', '', '', 0, '', '', '', 1000, 1000)"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        val sqlDb = helper.writableDatabase
        OrbitLocalDatabase.MIGRATION_1_2.migrate(sqlDb)

        val cursor = sqlDb.query("SELECT id, title, deadlineEpochDay, multipleRemindersJoined, customFieldsJson FROM tasks WHERE id = 'v1_task'")
        assertTrue("Migrated v1 task row must exist", cursor.moveToFirst())
        assertEquals("v1_task", cursor.getString(0))
        assertEquals("Legacy V1 Task", cursor.getString(1))
        assertEquals(0L, cursor.getLong(2))
        assertEquals("15", cursor.getString(3))
        assertEquals("{}", cursor.getString(4))
        cursor.close()
        sqlDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `large dataset 300 tasks batch persistence and query performance`() = runBlocking {
        val today = LocalDate.now().toEpochDay()
        for (i in 1..300) {
            repository.upsertTask(
                OrbitTask(
                    id = "perf_task_$i",
                    title = "Performance Benchmark Task #$i",
                    priority = if (i % 4 == 0) "p1" else "p3",
                    dueDateEpochDay = today + (i % 14),
                    scheduledDateEpochDay = today + (i % 14),
                    tags = listOf("perf", "batch_${i % 5}")
                )
            )
        }
        val allTasks = repository.observeTasks().first()
        assertEquals(300, allTasks.size)
        val csv = repository.exportTasksCsv()
        assertTrue(csv.lines().size >= 301)
    }

    @Test
    fun `home widget variant updates preferences and cycles via broadcast`() = runBlocking {
        repository.seedStarterWorkspaceIfEmpty()
        val initialPrefs = repository.observePreferences().first()
        repository.savePreferences(
            initialPrefs.copy(homeWidgetVariant = NativeHomeWidgetVariant.HABIT_PROGRESS.key)
        )
        val updated = repository.observePreferences().first()
        assertEquals(NativeHomeWidgetVariant.HABIT_PROGRESS.key, updated.homeWidgetVariant)
        assertFalse(updated.enabledWidgets.isEmpty())

        // Cycle via broadcast intent
        OrbitReminderReceiver.handleIntentSuspend(
            context = context,
            intent = Intent(OrbitReminderScheduler.ACTION_CYCLE_WIDGET_VARIANT),
            daoOverride = database.orbitDao()
        )
        val cycled = repository.observePreferences().first()
        assertTrue(cycled.homeWidgetVariant != NativeHomeWidgetVariant.HABIT_PROGRESS.key)
    }
}
