package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AutomationRule
import com.example.data.model.BackupPreviewSummary
import com.example.data.model.FocusSessionRecord
import com.example.data.model.OrbitHabit
import com.example.data.model.OrbitProject
import com.example.data.model.OrbitTask
import com.example.data.model.RestoreDuplicateStrategy
import com.example.data.model.Timestamp
import com.example.data.model.UserPreferencesDoc
import com.example.data.notification.OrbitReminderScheduler
import com.example.widget.OrbitHomeWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["dueDateEpochDay"]),
        Index(value = ["scheduledDateEpochDay"]),
        Index(value = ["projectId"])
    ]
)
data class LocalTaskEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val title: String = "",
    val description: String = "",
    val status: String = "todo",
    val priority: String = "p3",
    val projectId: String = "proj_personal",
    val projectName: String = "Personal",
    val sectionName: String = "General",
    val tagsJoined: String = "",
    val startDateEpochDay: Long = 0L,
    val dueDateEpochDay: Long = 0L,
    val deadlineEpochDay: Long = 0L,
    val scheduledDateEpochDay: Long = 0L,
    val scheduledStartHour: Int = -1,
    val scheduledStartMinute: Int = 0,
    val estimatedMinutes: Int = 25,
    val actualMinutes: Int = 0,
    val energyLevel: String = "medium",
    val recurrenceRule: String = "",
    val reminderOffsetMinutes: Int = 15,
    val multipleRemindersJoined: String = "15",
    val reminderEscalation: String = "standard",
    val locationTrigger: String = "",
    val postponeCount: Int = 0,
    val isFavorite: Boolean = false,
    val isImportant: Boolean = false,
    val isUrgent: Boolean = false,
    val isPinned: Boolean = false,
    val subtasksJoined: String = "",
    val dependsOnTaskId: String = "",
    val assigneeName: String = "",
    val commentsJoined: String = "",
    val attachmentsJoined: String = "",
    val customFieldsJson: String = "{}",
    val historyLogJoined: String = "",
    val createdAtSeconds: Long = System.currentTimeMillis() / 1000L,
    val updatedAtSeconds: Long = System.currentTimeMillis() / 1000L
) {
    fun toDomain(): OrbitTask {
        val customMap = mutableMapOf<String, String>()
        if (customFieldsJson.isNotBlank() && customFieldsJson.startsWith("{")) {
            runCatching {
                val obj = JSONObject(customFieldsJson)
                obj.keys().forEach { k -> customMap[k] = obj.optString(k, "") }
            }
        }
        val parsedOffsets = if (multipleRemindersJoined.isBlank()) {
            if (reminderOffsetMinutes >= 0) listOf(reminderOffsetMinutes) else emptyList()
        } else {
            multipleRemindersJoined.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        return OrbitTask(
            id = id,
            userId = userId,
            title = title,
            description = description,
            status = status,
            priority = priority,
            projectId = projectId,
            projectName = projectName,
            sectionName = sectionName,
            tags = if (tagsJoined.isBlank()) emptyList() else tagsJoined.split("\u001F"),
            startDateEpochDay = startDateEpochDay,
            dueDateEpochDay = dueDateEpochDay,
            deadlineEpochDay = deadlineEpochDay,
            scheduledDateEpochDay = scheduledDateEpochDay,
            scheduledStartHour = scheduledStartHour,
            scheduledStartMinute = scheduledStartMinute,
            estimatedMinutes = estimatedMinutes,
            actualMinutes = actualMinutes,
            energyLevel = energyLevel,
            recurrenceRule = recurrenceRule,
            reminderOffsetMinutes = reminderOffsetMinutes,
            multipleReminderOffsets = parsedOffsets,
            reminderEscalation = reminderEscalation,
            locationTrigger = locationTrigger,
            postponeCount = postponeCount,
            isFavorite = isFavorite,
            isImportant = isImportant,
            isUrgent = isUrgent,
            isPinned = isPinned,
            subtasks = if (subtasksJoined.isBlank()) emptyList() else subtasksJoined.split("\u001F"),
            dependsOnTaskId = dependsOnTaskId,
            assigneeName = assigneeName,
            comments = if (commentsJoined.isBlank()) emptyList() else commentsJoined.split("\u001F"),
            attachments = if (attachmentsJoined.isBlank()) emptyList() else attachmentsJoined.split("\u001F"),
            customFields = customMap,
            historyLog = if (historyLogJoined.isBlank()) emptyList() else historyLogJoined.split("\u001F"),
            createdAt = Timestamp(createdAtSeconds, 0),
            updatedAt = Timestamp(updatedAtSeconds, 0)
        )
    }

    companion object {
        fun fromDomain(task: OrbitTask): LocalTaskEntity {
            val nowSec = System.currentTimeMillis() / 1000L
            val customJson = JSONObject().apply {
                task.customFields.forEach { (k, v) -> put(k, v) }
            }.toString()
            return LocalTaskEntity(
                id = task.id.ifBlank { "task_${UUID.randomUUID().toString().replace("-", "").take(16)}" },
                userId = task.userId.ifBlank { "local_user" },
                title = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                projectId = task.projectId,
                projectName = task.projectName,
                sectionName = task.sectionName,
                tagsJoined = task.tags.joinToString("\u001F"),
                startDateEpochDay = task.startDateEpochDay,
                dueDateEpochDay = task.dueDateEpochDay,
                deadlineEpochDay = task.deadlineEpochDay,
                scheduledDateEpochDay = task.scheduledDateEpochDay,
                scheduledStartHour = task.scheduledStartHour,
                scheduledStartMinute = task.scheduledStartMinute,
                estimatedMinutes = task.estimatedMinutes,
                actualMinutes = task.actualMinutes,
                energyLevel = task.energyLevel,
                recurrenceRule = task.recurrenceRule,
                reminderOffsetMinutes = task.reminderOffsetMinutes,
                multipleRemindersJoined = task.multipleReminderOffsets.joinToString(","),
                reminderEscalation = task.reminderEscalation,
                locationTrigger = task.locationTrigger,
                postponeCount = task.postponeCount,
                isFavorite = task.isFavorite,
                isImportant = task.isImportant,
                isUrgent = task.isUrgent,
                isPinned = task.isPinned,
                subtasksJoined = task.subtasks.joinToString("\u001F"),
                dependsOnTaskId = task.dependsOnTaskId,
                assigneeName = task.assigneeName,
                commentsJoined = task.comments.joinToString("\u001F"),
                attachmentsJoined = task.attachments.joinToString("\u001F"),
                customFieldsJson = customJson,
                historyLogJoined = task.historyLog.joinToString("\u001F"),
                createdAtSeconds = task.createdAt?.seconds ?: nowSec,
                updatedAtSeconds = nowSec
            )
        }
    }
}

@Entity(tableName = "projects")
data class LocalProjectEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val name: String = "",
    val description: String = "",
    val colorHex: String = "#C86D51",
    val iconName: String = "folder",
    val category: String = "work",
    val isShared: Boolean = false,
    val membersJoined: String = "",
    val createdAtSeconds: Long = System.currentTimeMillis() / 1000L
) {
    fun toDomain(): OrbitProject = OrbitProject(
        id = id,
        userId = userId,
        name = name,
        description = description,
        colorHex = colorHex,
        iconName = iconName,
        category = category,
        isShared = isShared,
        members = if (membersJoined.isBlank()) emptyList() else membersJoined.split("\u001F"),
        createdAt = Timestamp(createdAtSeconds, 0),
        updatedAt = Timestamp(createdAtSeconds, 0)
    )

    companion object {
        fun fromDomain(p: OrbitProject): LocalProjectEntity = LocalProjectEntity(
            id = p.id.ifBlank { "proj_${UUID.randomUUID().toString().replace("-", "").take(12)}" },
            userId = p.userId.ifBlank { "local_user" },
            name = p.name,
            description = p.description,
            colorHex = p.colorHex,
            iconName = p.iconName,
            category = p.category,
            isShared = p.isShared,
            membersJoined = p.members.joinToString("\u001F")
        )
    }
}

@Entity(tableName = "habits")
data class LocalHabitEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val title: String = "",
    val description: String = "",
    val routineType: String = "morning",
    val category: String = "health",
    val frequencyLabel: String = "Daily",
    val targetDaysPerWeek: Int = 7,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakFreezesRemaining: Int = 2,
    val completedDaysJoined: String = "",
    val frozenDaysJoined: String = "",
    val reminderTime: String = "07:00",
    val iconName: String = "flame",
    val createdAtSeconds: Long = System.currentTimeMillis() / 1000L
) {
    fun toDomain(): OrbitHabit = OrbitHabit(
        id = id,
        userId = userId,
        title = title,
        description = description,
        routineType = routineType,
        category = category,
        frequencyLabel = frequencyLabel,
        targetDaysPerWeek = targetDaysPerWeek,
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        streakFreezesRemaining = streakFreezesRemaining,
        completedEpochDays = if (completedDaysJoined.isBlank()) emptyList()
        else completedDaysJoined.split(",").mapNotNull { it.toLongOrNull() },
        frozenEpochDays = if (frozenDaysJoined.isBlank()) emptyList()
        else frozenDaysJoined.split(",").mapNotNull { it.toLongOrNull() },
        reminderTime = reminderTime,
        iconName = iconName,
        createdAt = Timestamp(createdAtSeconds, 0),
        updatedAt = Timestamp(createdAtSeconds, 0)
    )

    companion object {
        fun fromDomain(h: OrbitHabit): LocalHabitEntity = LocalHabitEntity(
            id = h.id.ifBlank { "habit_${UUID.randomUUID().toString().replace("-", "").take(12)}" },
            userId = h.userId.ifBlank { "local_user" },
            title = h.title,
            description = h.description,
            routineType = h.routineType,
            category = h.category,
            frequencyLabel = h.frequencyLabel,
            targetDaysPerWeek = h.targetDaysPerWeek,
            currentStreak = h.currentStreak,
            bestStreak = h.bestStreak,
            streakFreezesRemaining = h.streakFreezesRemaining,
            completedDaysJoined = h.completedEpochDays.distinct().sorted().joinToString(","),
            frozenDaysJoined = h.frozenEpochDays.distinct().sorted().joinToString(","),
            reminderTime = h.reminderTime,
            iconName = h.iconName
        )
    }
}

@Entity(tableName = "focus_sessions")
data class LocalFocusSessionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val taskId: String = "",
    val taskTitle: String = "",
    val mode: String = "pomodoro",
    val durationMinutes: Int = 25,
    val ambientSound: String = "Cosmic Rain",
    val completedEpochDay: Long = 0L,
    val createdAtSeconds: Long = System.currentTimeMillis() / 1000L
) {
    fun toDomain(): FocusSessionRecord = FocusSessionRecord(
        id = id,
        userId = userId,
        taskId = taskId,
        taskTitle = taskTitle,
        mode = mode,
        durationMinutes = durationMinutes,
        ambientSound = ambientSound,
        completedEpochDay = completedEpochDay,
        createdAt = Timestamp(createdAtSeconds, 0),
        updatedAt = Timestamp(createdAtSeconds, 0)
    )

    companion object {
        fun fromDomain(s: FocusSessionRecord): LocalFocusSessionEntity = LocalFocusSessionEntity(
            id = s.id.ifBlank { "focus_${UUID.randomUUID().toString().replace("-", "").take(12)}" },
            userId = s.userId.ifBlank { "local_user" },
            taskId = s.taskId,
            taskTitle = s.taskTitle,
            mode = s.mode,
            durationMinutes = s.durationMinutes,
            ambientSound = s.ambientSound,
            completedEpochDay = s.completedEpochDay
        )
    }
}

@Entity(tableName = "automations")
data class LocalAutomationEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val title: String = "",
    val triggerType: String = "",
    val conditionText: String = "",
    val actionType: String = "",
    val isEnabled: Boolean = true,
    val createdAtSeconds: Long = System.currentTimeMillis() / 1000L
) {
    fun toDomain(): AutomationRule = AutomationRule(
        id = id,
        userId = userId,
        title = title,
        triggerType = triggerType,
        conditionText = conditionText,
        actionType = actionType,
        isEnabled = isEnabled,
        createdAt = Timestamp(createdAtSeconds, 0),
        updatedAt = Timestamp(createdAtSeconds, 0)
    )

    companion object {
        fun fromDomain(r: AutomationRule): LocalAutomationEntity = LocalAutomationEntity(
            id = r.id.ifBlank { "auto_${UUID.randomUUID().toString().replace("-", "").take(12)}" },
            userId = r.userId.ifBlank { "local_user" },
            title = r.title,
            triggerType = r.triggerType,
            conditionText = r.conditionText,
            actionType = r.actionType,
            isEnabled = r.isEnabled
        )
    }
}

@Entity(tableName = "preferences")
data class LocalPreferencesEntity(
    @PrimaryKey val id: String = "default",
    val themeMode: String = "paper",
    val accentPreset: String = "terracotta",
    val density: String = "airy",
    val streakStyle: String = "flame",
    val cornerRadiusDp: Int = 22,
    val dashboardPreset: String = "mission_control",
    val planningMode: String = "manual",
    val energyFilter: String = "all",
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val gamificationEnabled: Boolean = true,
    val enabledWidgetsJoined: String = UserPreferencesDoc.defaultWidgetOrder().joinToString(","),
    val favoriteWidgetsJoined: String = "today,habit,progress_ring,focus,mini_calendar",
    val widgetSizesJson: String = "{}",
    val homeWidgetVariant: String = "today_checklist",
    val onboardingCompleted: Boolean = true,
    val reducedMotion: Boolean = false,
    val use24HourFormat: Boolean = false,
    val workdayStartHour: Int = 8,
    val workdayEndHour: Int = 19,
    val showWeekendsInCalendar: Boolean = true,
    val defaultPomodoroMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val defaultLongBreakMinutes: Int = 15,
    val dailyFocusGoalMinutes: Int = 120,
    val ambientVolume: Float = 0.7f,
    val webDavUrl: String = "",
    val quickNotes: String = "• Capture ideas in 2 seconds\n• Review weekly milestones every Friday\n• 100% Free & Offline-First"
) {
    fun toDomain(): UserPreferencesDoc {
        val sizesMap = mutableMapOf<String, String>()
        if (widgetSizesJson.isNotBlank() && widgetSizesJson.startsWith("{")) {
            runCatching {
                val obj = JSONObject(widgetSizesJson)
                obj.keys().forEach { k -> sizesMap[k] = obj.optString(k, "Medium") }
            }
        }
        return UserPreferencesDoc(
            id = id,
            userId = "local_user",
            themeMode = themeMode,
            accentPreset = accentPreset,
            density = density,
            streakStyle = streakStyle,
            cornerRadiusDp = cornerRadiusDp,
            dashboardPreset = dashboardPreset,
            planningMode = planningMode,
            energyFilter = energyFilter,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStartHour = quietHoursStartHour,
            quietHoursEndHour = quietHoursEndHour,
            gamificationEnabled = gamificationEnabled,
            enabledWidgets = if (enabledWidgetsJoined.isBlank()) UserPreferencesDoc.defaultWidgetOrder()
            else enabledWidgetsJoined.split(","),
            favoriteWidgets = if (favoriteWidgetsJoined.isBlank()) emptyList()
            else favoriteWidgetsJoined.split(","),
            widgetSizes = sizesMap,
            homeWidgetVariant = homeWidgetVariant,
            onboardingCompleted = onboardingCompleted,
            reducedMotion = reducedMotion,
            use24HourFormat = use24HourFormat,
            workdayStartHour = workdayStartHour,
            workdayEndHour = workdayEndHour,
            showWeekendsInCalendar = showWeekendsInCalendar,
            defaultPomodoroMinutes = defaultPomodoroMinutes,
            defaultBreakMinutes = defaultBreakMinutes,
            defaultLongBreakMinutes = defaultLongBreakMinutes,
            dailyFocusGoalMinutes = dailyFocusGoalMinutes,
            ambientVolume = ambientVolume,
            webDavUrl = webDavUrl,
            quickNotes = quickNotes,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
    }

    companion object {
        fun fromDomain(p: UserPreferencesDoc): LocalPreferencesEntity {
            val sizesJson = JSONObject().apply {
                p.widgetSizes.forEach { (k, v) -> put(k, v) }
            }.toString()
            return LocalPreferencesEntity(
                id = "default",
                themeMode = p.themeMode,
                accentPreset = p.accentPreset,
                density = p.density,
                streakStyle = p.streakStyle,
                cornerRadiusDp = p.cornerRadiusDp,
                dashboardPreset = p.dashboardPreset,
                planningMode = p.planningMode,
                energyFilter = p.energyFilter,
                quietHoursEnabled = p.quietHoursEnabled,
                quietHoursStartHour = p.quietHoursStartHour,
                quietHoursEndHour = p.quietHoursEndHour,
                gamificationEnabled = p.gamificationEnabled,
                enabledWidgetsJoined = p.enabledWidgets.joinToString(","),
                favoriteWidgetsJoined = p.favoriteWidgets.joinToString(","),
                widgetSizesJson = sizesJson,
                homeWidgetVariant = p.homeWidgetVariant,
                onboardingCompleted = p.onboardingCompleted,
                reducedMotion = p.reducedMotion,
                use24HourFormat = p.use24HourFormat,
                workdayStartHour = p.workdayStartHour,
                workdayEndHour = p.workdayEndHour,
                showWeekendsInCalendar = p.showWeekendsInCalendar,
                defaultPomodoroMinutes = p.defaultPomodoroMinutes,
                defaultBreakMinutes = p.defaultBreakMinutes,
                defaultLongBreakMinutes = p.defaultLongBreakMinutes,
                dailyFocusGoalMinutes = p.dailyFocusGoalMinutes,
                ambientVolume = p.ambientVolume,
                webDavUrl = p.webDavUrl,
                quickNotes = p.quickNotes
            )
        }
    }
}

@Dao
interface OrbitDao {
    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, updatedAtSeconds DESC")
    fun observeAllTasks(): Flow<List<LocalTaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, updatedAtSeconds DESC")
    suspend fun getAllTasksOnce(): List<LocalTaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): LocalTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: LocalTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<LocalTaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE status = 'trash'")
    suspend fun emptyTrash()

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    @Query("SELECT * FROM projects ORDER BY createdAtSeconds ASC")
    fun observeAllProjects(): Flow<List<LocalProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY createdAtSeconds ASC")
    suspend fun getAllProjectsOnce(): List<LocalProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: LocalProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjects(projects: List<LocalProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()

    @Query("SELECT * FROM habits ORDER BY createdAtSeconds ASC")
    fun observeAllHabits(): Flow<List<LocalHabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAtSeconds ASC")
    suspend fun getAllHabitsOnce(): List<LocalHabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): LocalHabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: LocalHabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<LocalHabitEntity>)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()

    @Query("SELECT * FROM focus_sessions ORDER BY createdAtSeconds DESC")
    fun observeAllFocusSessions(): Flow<List<LocalFocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY createdAtSeconds DESC")
    suspend fun getAllFocusSessionsOnce(): List<LocalFocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFocusSession(session: LocalFocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFocusSessions(sessions: List<LocalFocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllFocusSessions()

    @Query("SELECT * FROM automations ORDER BY createdAtSeconds ASC")
    fun observeAllAutomations(): Flow<List<LocalAutomationEntity>>

    @Query("SELECT * FROM automations ORDER BY createdAtSeconds ASC")
    suspend fun getAllAutomationsOnce(): List<LocalAutomationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAutomation(rule: LocalAutomationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAutomations(rules: List<LocalAutomationEntity>)

    @Query("DELETE FROM automations")
    suspend fun clearAllAutomations()

    @Query("SELECT * FROM preferences WHERE id = 'default' LIMIT 1")
    fun observePreferences(): Flow<LocalPreferencesEntity?>

    @Query("SELECT * FROM preferences WHERE id = 'default' LIMIT 1")
    suspend fun getPreferencesOnce(): LocalPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: LocalPreferencesEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasksIgnoreDuplicates(tasks: List<LocalTaskEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjectsIgnoreDuplicates(projects: List<LocalProjectEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHabitsIgnoreDuplicates(habits: List<LocalHabitEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFocusSessionsIgnoreDuplicates(sessions: List<LocalFocusSessionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAutomationsIgnoreDuplicates(rules: List<LocalAutomationEntity>): List<Long>

    @Transaction
    suspend fun restoreFullBackupTransaction(
        tasks: List<LocalTaskEntity>,
        projects: List<LocalProjectEntity>,
        habits: List<LocalHabitEntity>,
        sessions: List<LocalFocusSessionEntity>,
        automations: List<LocalAutomationEntity>,
        preferences: LocalPreferencesEntity?
    ) {
        clearAllTasks()
        clearAllProjects()
        clearAllHabits()
        clearAllFocusSessions()
        clearAllAutomations()
        if (projects.isNotEmpty()) upsertProjects(projects)
        if (tasks.isNotEmpty()) upsertTasks(tasks)
        if (habits.isNotEmpty()) upsertHabits(habits)
        if (sessions.isNotEmpty()) upsertFocusSessions(sessions)
        if (automations.isNotEmpty()) upsertAutomations(automations)
        if (preferences != null) upsertPreferences(preferences)
    }

    @Transaction
    suspend fun mergeBackupSkipDuplicatesTransaction(
        tasks: List<LocalTaskEntity>,
        projects: List<LocalProjectEntity>,
        habits: List<LocalHabitEntity>,
        sessions: List<LocalFocusSessionEntity>,
        automations: List<LocalAutomationEntity>,
        preferences: LocalPreferencesEntity?
    ): Int {
        var insertedCount = 0
        if (projects.isNotEmpty()) {
            insertedCount += insertProjectsIgnoreDuplicates(projects).count { it != -1L }
        }
        if (tasks.isNotEmpty()) {
            insertedCount += insertTasksIgnoreDuplicates(tasks).count { it != -1L }
        }
        if (habits.isNotEmpty()) {
            insertedCount += insertHabitsIgnoreDuplicates(habits).count { it != -1L }
        }
        if (sessions.isNotEmpty()) {
            insertedCount += insertFocusSessionsIgnoreDuplicates(sessions).count { it != -1L }
        }
        if (automations.isNotEmpty()) {
            insertedCount += insertAutomationsIgnoreDuplicates(automations).count { it != -1L }
        }
        if (preferences != null && getPreferencesOnce() == null) {
            upsertPreferences(preferences)
        }
        return insertedCount
    }

    @Transaction
    suspend fun resetAndSeedWorkspaceTransaction(
        tasks: List<LocalTaskEntity>,
        projects: List<LocalProjectEntity>,
        habits: List<LocalHabitEntity>,
        sessions: List<LocalFocusSessionEntity>,
        automations: List<LocalAutomationEntity>,
        preferences: LocalPreferencesEntity
    ) {
        clearAllTasks()
        clearAllProjects()
        clearAllHabits()
        clearAllFocusSessions()
        clearAllAutomations()
        upsertProjects(projects)
        upsertTasks(tasks)
        upsertHabits(habits)
        upsertFocusSessions(sessions)
        upsertAutomations(automations)
        upsertPreferences(preferences)
    }
}

@Database(
    entities = [
        LocalTaskEntity::class,
        LocalProjectEntity::class,
        LocalHabitEntity::class,
        LocalFocusSessionEntity::class,
        LocalAutomationEntity::class,
        LocalPreferencesEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class OrbitLocalDatabase : RoomDatabase() {
    abstract fun orbitDao(): OrbitDao

    companion object {
        /**
         * Non-destructive migration from version 1 to version 2.
         * Preserves all existing user tasks, habits, projects, focus sessions, and preferences.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Upgrade tasks table
                db.execSQL("ALTER TABLE tasks ADD COLUMN startDateEpochDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN deadlineEpochDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN multipleRemindersJoined TEXT NOT NULL DEFAULT '15'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN attachmentsJoined TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN customFieldsJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN historyLogJoined TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_status ON tasks(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dueDateEpochDay ON tasks(dueDateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_scheduledDateEpochDay ON tasks(scheduledDateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_projectId ON tasks(projectId)")

                // Upgrade habits table
                db.execSQL("ALTER TABLE habits ADD COLUMN frequencyLabel TEXT NOT NULL DEFAULT 'Daily'")
                db.execSQL("ALTER TABLE habits ADD COLUMN streakFreezesRemaining INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE habits ADD COLUMN frozenDaysJoined TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN iconName TEXT NOT NULL DEFAULT 'flame'")

                // Upgrade preferences table
                db.execSQL("ALTER TABLE preferences ADD COLUMN streakStyle TEXT NOT NULL DEFAULT 'flame'")
                db.execSQL("ALTER TABLE preferences ADD COLUMN quietHoursStartHour INTEGER NOT NULL DEFAULT 22")
                db.execSQL("ALTER TABLE preferences ADD COLUMN quietHoursEndHour INTEGER NOT NULL DEFAULT 7")
                db.execSQL("ALTER TABLE preferences ADD COLUMN favoriteWidgetsJoined TEXT NOT NULL DEFAULT 'today,habit,progress_ring,focus,mini_calendar'")
                db.execSQL("ALTER TABLE preferences ADD COLUMN widgetSizesJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE preferences ADD COLUMN homeWidgetVariant TEXT NOT NULL DEFAULT 'today_checklist'")
                db.execSQL("ALTER TABLE preferences ADD COLUMN workdayStartHour INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE preferences ADD COLUMN workdayEndHour INTEGER NOT NULL DEFAULT 19")
                db.execSQL("ALTER TABLE preferences ADD COLUMN showWeekendsInCalendar INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE preferences ADD COLUMN ambientVolume REAL NOT NULL DEFAULT 0.7")
            }
        }

        @Volatile
        private var INSTANCE: OrbitLocalDatabase? = null

        fun getInstance(context: Context): OrbitLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OrbitLocalDatabase::class.java,
                    "orbit_local_productivity.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

/**
 * 100% Free, Offline-First Repository backed by Room SQLite.
 * Zero mandatory login, zero subscriptions, non-destructive migrations,
 * transactional backup & restore, and automatic home-screen widget & alarm sync.
 */
class OrbitLocalRepository(
    private val dao: OrbitDao,
    private val appContext: Context? = null
) {
    fun observeTasks(): Flow<List<OrbitTask>> =
        dao.observeAllTasks().map { list -> list.map { it.toDomain() } }

    fun observeProjects(): Flow<List<OrbitProject>> =
        dao.observeAllProjects().map { list -> list.map { it.toDomain() } }

    fun observeHabits(): Flow<List<OrbitHabit>> =
        dao.observeAllHabits().map { list -> list.map { it.toDomain() } }

    fun observeFocusSessions(): Flow<List<FocusSessionRecord>> =
        dao.observeAllFocusSessions().map { list -> list.map { it.toDomain() } }

    fun observeAutomations(): Flow<List<AutomationRule>> =
        dao.observeAllAutomations().map { list -> list.map { it.toDomain() } }

    fun observePreferences(): Flow<UserPreferencesDoc> =
        dao.observePreferences().map { it?.toDomain() ?: UserPreferencesDoc() }

    suspend fun getAllTasksOnce(): List<OrbitTask> =
        dao.getAllTasksOnce().map { it.toDomain() }

    suspend fun getTaskById(taskId: String): OrbitTask? =
        dao.getTaskById(taskId)?.toDomain()

    suspend fun getAllHabitsOnce(): List<OrbitHabit> =
        dao.getAllHabitsOnce().map { it.toDomain() }

    suspend fun upsertTask(task: OrbitTask): Result<String> = runCatching {
        val entity = LocalTaskEntity.fromDomain(task)
        dao.upsertTask(entity)
        val savedDomain = entity.toDomain()
        appContext?.let { ctx ->
            if (!savedDomain.isCompleted && !savedDomain.isTrashed) {
                OrbitReminderScheduler.scheduleTaskReminder(ctx, savedDomain)
            } else {
                OrbitReminderScheduler.cancelTaskReminder(ctx, savedDomain.id)
            }
        }
        refreshHomeWidget()
        entity.id
    }

    suspend fun deleteTaskPermanently(taskId: String): Result<Unit> = runCatching {
        dao.deleteTaskById(taskId)
        appContext?.let { OrbitReminderScheduler.cancelTaskReminder(it, taskId) }
        refreshHomeWidget()
    }

    suspend fun emptyTrash(): Result<Unit> = runCatching {
        dao.emptyTrash()
        refreshHomeWidget()
    }

    suspend fun upsertProject(project: OrbitProject): Result<String> = runCatching {
        val entity = LocalProjectEntity.fromDomain(project)
        dao.upsertProject(entity)
        refreshHomeWidget()
        entity.id
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        dao.deleteProjectById(projectId)
        refreshHomeWidget()
    }

    suspend fun upsertHabit(habit: OrbitHabit): Result<String> = runCatching {
        val entity = LocalHabitEntity.fromDomain(habit)
        dao.upsertHabit(entity)
        refreshHomeWidget()
        entity.id
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        dao.deleteHabitById(habitId)
        refreshHomeWidget()
    }

    suspend fun recordFocusSession(session: FocusSessionRecord): Result<String> = runCatching {
        val entity = LocalFocusSessionEntity.fromDomain(session)
        dao.upsertFocusSession(entity)
        refreshHomeWidget()
        entity.id
    }

    suspend fun saveAutomation(rule: AutomationRule): Result<String> = runCatching {
        val entity = LocalAutomationEntity.fromDomain(rule)
        dao.upsertAutomation(entity)
        entity.id
    }

    suspend fun savePreferences(prefs: UserPreferencesDoc): Result<Unit> = runCatching {
        dao.upsertPreferences(LocalPreferencesEntity.fromDomain(prefs))
        refreshHomeWidget()
    }

    private fun refreshHomeWidget() {
        appContext?.let { OrbitHomeWidgetProvider.requestUpdate(it) }
    }

    suspend fun seedStarterWorkspaceIfEmpty() {
        val currentTasks = dao.getAllTasksOnce()
        val currentHabits = dao.getAllHabitsOnce()
        if (currentTasks.isNotEmpty() || currentHabits.isNotEmpty() ||
            dao.getAllProjectsOnce().isNotEmpty() || dao.getPreferencesOnce() != null) return
        resetToStarterSeed(preservePreferences = true)
    }

    /**
     * Resets or seeds the starter workspace atomically inside a Room @Transaction.
     * Includes the habits from the reference design (Morning meditation, Drink 8 glasses of water,
     * Read 30 minutes, Call a friend, Go for a run, Write in journal) plus rich tasks & projects.
     */
    suspend fun resetToStarterSeed(preservePreferences: Boolean = false) {
        val today = LocalDate.now().toEpochDay()
        val currentPrefs = if (preservePreferences) dao.getPreferencesOnce() else null

        val starterProjects = listOf(
            OrbitProject(
                id = "proj_work",
                name = "Product & Engineering",
                description = "Core roadmap, architecture & sprint deliverables",
                colorHex = "#C86D51",
                iconName = "rocket",
                category = "work",
                isShared = true,
                members = listOf("You", "Sarah Chen", "Alex Rivera")
            ),
            OrbitProject(
                id = "proj_personal",
                name = "Life & Wellness",
                description = "Health, mindfulness, reading & personal growth",
                colorHex = "#5F8D75",
                iconName = "heart",
                category = "health",
                isShared = false,
                members = listOf("You")
            ),
            OrbitProject(
                id = "proj_household",
                name = "Household & Shopping",
                description = "Groceries, bills, birthdays & home maintenance",
                colorHex = "#B07D62",
                iconName = "home",
                category = "shopping",
                isShared = true,
                members = listOf("You", "Sam", "Jamie")
            ),
            OrbitProject(
                id = "proj_learning",
                name = "Study & Mastery",
                description = "Courses, books & skill acquisition",
                colorHex = "#4A8BB2",
                iconName = "school",
                category = "education",
                isShared = false,
                members = listOf("You")
            )
        )

        val starterTasks = listOf(
            OrbitTask(
                id = "task_arch_roadmap",
                title = "Finalize Q4 System Architecture & Release Checklist",
                description = "Review performance benchmarks, offline Room SQLite migrations, and accessibility audit.",
                status = "in_progress",
                priority = "p1",
                projectId = "proj_work",
                projectName = "Product & Engineering",
                sectionName = "Architecture",
                tags = listOf("deepwork", "architecture", "release"),
                startDateEpochDay = today - 1,
                dueDateEpochDay = today,
                deadlineEpochDay = today + 1,
                scheduledDateEpochDay = today,
                scheduledStartHour = 9,
                scheduledStartMinute = 30,
                estimatedMinutes = 50,
                actualMinutes = 25,
                energyLevel = "high",
                reminderOffsetMinutes = 15,
                multipleReminderOffsets = listOf(0, 15, 60),
                reminderEscalation = "critical",
                isFavorite = true,
                isImportant = true,
                isUrgent = true,
                isPinned = true,
                subtasks = listOf(
                    "1|Verify offline-first Room SQLite persistence & Migration(1, 2)",
                    "1|Audit all 30 customizable dashboard widgets & 10 OS widgets",
                    "0|Export ICS calendar & JSON backup vault"
                ),
                comments = listOf("Completed initial database schema benchmark in 4ms."),
                historyLog = listOf("Created in Product & Engineering", "Pinned to top of Today")
            ),
            OrbitTask(
                id = "task_design_polish",
                title = "Polish Editorial Paper, Dark & True OLED Themes",
                description = "Ensure all 22 screens and 30 widgets adapt cleanly to Airy, Comfortable, and Compact density.",
                status = "todo",
                priority = "p2",
                projectId = "proj_work",
                projectName = "Product & Engineering",
                sectionName = "Design System",
                tags = listOf("design", "ui"),
                dueDateEpochDay = today,
                scheduledDateEpochDay = today,
                scheduledStartHour = 14,
                scheduledStartMinute = 0,
                estimatedMinutes = 40,
                energyLevel = "medium",
                reminderOffsetMinutes = 15,
                reminderEscalation = "persistent",
                isFavorite = true,
                isImportant = true,
                isUrgent = false,
                subtasks = listOf(
                    "1|Verify WCAG contrast ratios in Paper, Dark & OLED modes",
                    "0|Test reduced-motion accessibility toggle"
                )
            ),
            OrbitTask(
                id = "task_run_mobility",
                title = "Evening 5K Run & 15-Minute Mobility Routine",
                description = "Zone 2 outdoor run followed by hip & shoulder stretching.",
                status = "todo",
                priority = "p2",
                projectId = "proj_personal",
                projectName = "Life & Wellness",
                sectionName = "Fitness",
                tags = listOf("health", "running"),
                dueDateEpochDay = today,
                scheduledDateEpochDay = today,
                scheduledStartHour = 17,
                scheduledStartMinute = 30,
                estimatedMinutes = 45,
                energyLevel = "high",
                recurrenceRule = "WEEKDAYS",
                isImportant = true,
                isUrgent = false
            ),
            OrbitTask(
                id = "task_grocery_run",
                title = "Weekly Organic Produce, Oat Milk & Espresso Run",
                description = "Auto-grouped shopping list items for the household.",
                status = "inbox",
                priority = "p3",
                projectId = "proj_household",
                projectName = "Household & Shopping",
                sectionName = "Groceries",
                tags = listOf("shopping", "errands"),
                dueDateEpochDay = today,
                scheduledDateEpochDay = today,
                scheduledStartHour = 18,
                scheduledStartMinute = 30,
                estimatedMinutes = 25,
                energyLevel = "low",
                locationTrigger = "Arriving: Local Market",
                isUrgent = true,
                subtasks = listOf(
                    "0|Produce: Hass avocados (4x), baby spinach, blueberries",
                    "0|Dairy & Plant: Barista oat milk (2 cartons)",
                    "0|Pantry: Whole-bean Ethiopian espresso (500g)"
                )
            ),
            OrbitTask(
                id = "task_birthday_mom",
                title = "Mom's Birthday — Order Flowers & Call",
                description = "Annual birthday reminder with early 1-day notification.",
                status = "todo",
                priority = "p1",
                projectId = "proj_personal",
                projectName = "Life & Wellness",
                sectionName = "Celebrations",
                tags = listOf("birthday", "family"),
                dueDateEpochDay = today + 2,
                deadlineEpochDay = today + 2,
                scheduledDateEpochDay = today + 2,
                scheduledStartHour = 10,
                scheduledStartMinute = 0,
                estimatedMinutes = 20,
                recurrenceRule = "YEARLY",
                assigneeName = "Birthday",
                isFavorite = true,
                isImportant = true
            ),
            OrbitTask(
                id = "task_bill_rent",
                title = "Pay Monthly Rent & Fiber Internet Bill",
                description = "Recurring monthly bill payment reminder.",
                status = "todo",
                priority = "p1",
                projectId = "proj_household",
                projectName = "Household & Shopping",
                sectionName = "Bills & Finance",
                tags = listOf("bills", "finance"),
                dueDateEpochDay = today + 1,
                deadlineEpochDay = today + 1,
                scheduledDateEpochDay = today + 1,
                scheduledStartHour = 11,
                scheduledStartMinute = 0,
                estimatedMinutes = 15,
                recurrenceRule = "MONTHLY",
                assigneeName = "Bill",
                isImportant = true,
                isUrgent = true
            ),
            OrbitTask(
                id = "task_medication_vitamins",
                title = "Take Morning Vitamin D3, Omega-3 & Magnesium",
                description = "Daily health & medication reminder.",
                status = "todo",
                priority = "p2",
                projectId = "proj_personal",
                projectName = "Life & Wellness",
                sectionName = "Medication",
                tags = listOf("medication", "health"),
                dueDateEpochDay = today,
                scheduledDateEpochDay = today,
                scheduledStartHour = 8,
                scheduledStartMinute = 0,
                estimatedMinutes = 5,
                recurrenceRule = "DAILY",
                assigneeName = "Medication",
                isImportant = true
            ),
            OrbitTask(
                id = "task_overdue_taxes",
                title = "Organize Quarterly Expense Receipts & Backup Vault",
                description = "Past-due administrative task ready for 1-tap rescheduling.",
                status = "todo",
                priority = "p2",
                projectId = "proj_household",
                projectName = "Household & Shopping",
                sectionName = "Finance",
                tags = listOf("finance", "admin"),
                dueDateEpochDay = today - 1,
                scheduledDateEpochDay = today,
                scheduledStartHour = 16,
                scheduledStartMinute = 0,
                estimatedMinutes = 35,
                postponeCount = 2,
                isImportant = true,
                isUrgent = true
            )
        )

        // Habits matching the user's reference image (TODAY vs NOT TODAY with streaks and 7/7 rings)
        val starterHabits = listOf(
            OrbitHabit(
                id = "habit_meditation",
                title = "Morning meditation",
                description = "10 minutes of mindful breathing before opening screens",
                routineType = "morning",
                category = "mindfulness",
                frequencyLabel = "Daily",
                targetDaysPerWeek = 7,
                currentStreak = 48,
                bestStreak = 64,
                streakFreezesRemaining = 2,
                completedEpochDays = (0L..6L).map { today - it }, // Completed today! (1 of 4 today -> 25%)
                reminderTime = "07:00"
            ),
            OrbitHabit(
                id = "habit_hydration",
                title = "Drink 8 glasses of water",
                description = "Stay hydrated throughout the workday",
                routineType = "morning",
                category = "health",
                frequencyLabel = "Daily",
                targetDaysPerWeek = 7,
                currentStreak = 1,
                bestStreak = 21,
                streakFreezesRemaining = 2,
                completedEpochDays = listOf(today - 1, today - 2, today - 4, today - 5), // 4/7
                reminderTime = "09:00"
            ),
            OrbitHabit(
                id = "habit_reading",
                title = "Read 30 minutes",
                description = "Evening non-fiction or literature reading ritual",
                routineType = "evening",
                category = "personal",
                frequencyLabel = "Daily",
                targetDaysPerWeek = 7,
                currentStreak = 0,
                bestStreak = 18,
                streakFreezesRemaining = 2,
                completedEpochDays = listOf(today - 2, today - 3, today - 5, today - 6), // 4/7
                reminderTime = "21:30"
            ),
            OrbitHabit(
                id = "habit_call_friend",
                title = "Call a friend",
                description = "Stay connected with close friends and family",
                routineType = "anytime",
                category = "social",
                frequencyLabel = "Daily",
                targetDaysPerWeek = 2,
                currentStreak = 0,
                bestStreak = 9,
                streakFreezesRemaining = 2,
                completedEpochDays = emptyList(), // 0/2
                reminderTime = "18:00"
            ),
            OrbitHabit(
                id = "habit_run",
                title = "Go for a run",
                description = "Outdoor 5K aerobic conditioning",
                routineType = "morning",
                category = "health",
                frequencyLabel = "Tue · Thu · Sat",
                targetDaysPerWeek = 3,
                currentStreak = 6,
                bestStreak = 15,
                streakFreezesRemaining = 2,
                completedEpochDays = listOf(today - 1, today - 3, today - 5),
                reminderTime = "07:30"
            ),
            OrbitHabit(
                id = "habit_journal",
                title = "Write in journal",
                description = " Evening reflection and gratitude log",
                routineType = "evening",
                category = "mindfulness",
                frequencyLabel = "Weekdays",
                targetDaysPerWeek = 5,
                currentStreak = 12,
                bestStreak = 30,
                streakFreezesRemaining = 2,
                completedEpochDays = listOf(today - 1, today - 2, today - 3, today - 4),
                reminderTime = "21:00"
            )
        )

        val starterSessions = listOf(
            LocalFocusSessionEntity(
                id = "focus_seed_1",
                taskId = "task_arch_roadmap",
                taskTitle = "Finalize Q4 System Architecture",
                mode = "pomodoro",
                durationMinutes = 25,
                ambientSound = "Cosmic Rain",
                completedEpochDay = today
            ),
            LocalFocusSessionEntity(
                id = "focus_seed_2",
                taskId = "task_design_polish",
                taskTitle = "Polish Editorial Paper & OLED Themes",
                mode = "deep_work",
                durationMinutes = 50,
                ambientSound = "Deep Space Brown",
                completedEpochDay = today - 1
            )
        )

        val starterAutomations = listOf(
            AutomationRule(
                id = "auto_unlock_deps",
                title = "Auto-Unlock Dependent Tasks",
                triggerType = "When a task is marked Completed",
                conditionText = "Another task depends on its ID",
                actionType = "Unlock dependent task & schedule for Today",
                isEnabled = true
            ),
            AutomationRule(
                id = "auto_recurring_regen",
                title = "Deterministic Recurring Generator",
                triggerType = "When a recurring task is completed",
                conditionText = "Recurrence rule is DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY, or AFTER_COMPLETION",
                actionType = "Generate next occurrence automatically",
                isEnabled = true
            )
        )

        dao.resetAndSeedWorkspaceTransaction(
            tasks = starterTasks.map { LocalTaskEntity.fromDomain(it) },
            projects = starterProjects.map { LocalProjectEntity.fromDomain(it) },
            habits = starterHabits.map { LocalHabitEntity.fromDomain(it) },
            sessions = starterSessions,
            automations = starterAutomations.map { LocalAutomationEntity.fromDomain(it) },
            preferences = currentPrefs ?: LocalPreferencesEntity()
        )
        refreshHomeWidget()
    }

    // --- TRANSACTIONAL FULL BACKUP, EXPORT & IMPORT (JSON, CSV, ICS) ---

    suspend fun exportFullBackupJson(): String {
        val tasks = dao.getAllTasksOnce().map { it.toDomain() }
        val projects = dao.getAllProjectsOnce().map { it.toDomain() }
        val habits = dao.getAllHabitsOnce().map { it.toDomain() }
        val sessions = dao.getAllFocusSessionsOnce().map { it.toDomain() }
        val automations = dao.getAllAutomationsOnce().map { it.toDomain() }
        val prefs = dao.getPreferencesOnce()?.toDomain() ?: UserPreferencesDoc()

        return JSONObject().apply {
            put("app", "Orbit 2.0 — 100% Free Local-First Productivity OS")
            put("version", 3)
            put("exportedAtEpochDay", LocalDate.now().toEpochDay())
            put("preferences", JSONObject().apply {
                put("themeMode", prefs.themeMode)
                put("accentPreset", prefs.accentPreset)
                put("density", prefs.density)
                put("streakStyle", prefs.streakStyle)
                put("cornerRadiusDp", prefs.cornerRadiusDp)
                put("dashboardPreset", prefs.dashboardPreset)
                put("quietHoursEnabled", prefs.quietHoursEnabled)
                put("quietHoursStartHour", prefs.quietHoursStartHour)
                put("quietHoursEndHour", prefs.quietHoursEndHour)
                put("gamificationEnabled", prefs.gamificationEnabled)
                put("enabledWidgets", JSONArray(prefs.enabledWidgets))
                put("favoriteWidgets", JSONArray(prefs.favoriteWidgets))
                put("homeWidgetVariant", prefs.homeWidgetVariant)
                put("quickNotes", prefs.quickNotes)
            })
            put("tasks", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("description", t.description)
                        put("status", t.status)
                        put("priority", t.priority)
                        put("projectId", t.projectId)
                        put("projectName", t.projectName)
                        put("sectionName", t.sectionName)
                        put("startDateEpochDay", t.startDateEpochDay)
                        put("dueDateEpochDay", t.dueDateEpochDay)
                        put("deadlineEpochDay", t.deadlineEpochDay)
                        put("scheduledDateEpochDay", t.scheduledDateEpochDay)
                        put("scheduledStartHour", t.scheduledStartHour)
                        put("scheduledStartMinute", t.scheduledStartMinute)
                        put("estimatedMinutes", t.estimatedMinutes)
                        put("actualMinutes", t.actualMinutes)
                        put("energyLevel", t.energyLevel)
                        put("recurrenceRule", t.recurrenceRule)
                        put("reminderOffsetMinutes", t.reminderOffsetMinutes)
                        put("multipleReminderOffsets", JSONArray(t.multipleReminderOffsets))
                        put("reminderEscalation", t.reminderEscalation)
                        put("locationTrigger", t.locationTrigger)
                        put("dependsOnTaskId", t.dependsOnTaskId)
                        put("assigneeName", t.assigneeName)
                        put("isFavorite", t.isFavorite)
                        put("isImportant", t.isImportant)
                        put("isUrgent", t.isUrgent)
                        put("isPinned", t.isPinned)
                        put("tags", JSONArray(t.tags))
                        put("subtasks", JSONArray(t.subtasks))
                        put("comments", JSONArray(t.comments))
                        put("attachments", JSONArray(t.attachments))
                        put("historyLog", JSONArray(t.historyLog))
                    })
                }
            })
            put("projects", JSONArray().apply {
                projects.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description)
                        put("colorHex", p.colorHex)
                        put("iconName", p.iconName)
                        put("category", p.category)
                        put("isShared", p.isShared)
                        put("members", JSONArray(p.members))
                    })
                }
            })
            put("habits", JSONArray().apply {
                habits.forEach { h ->
                    put(JSONObject().apply {
                        put("id", h.id)
                        put("title", h.title)
                        put("description", h.description)
                        put("routineType", h.routineType)
                        put("category", h.category)
                        put("frequencyLabel", h.frequencyLabel)
                        put("targetDaysPerWeek", h.targetDaysPerWeek)
                        put("currentStreak", h.currentStreak)
                        put("bestStreak", h.bestStreak)
                        put("streakFreezesRemaining", h.streakFreezesRemaining)
                        put("completedEpochDays", JSONArray(h.completedEpochDays))
                        put("frozenEpochDays", JSONArray(h.frozenEpochDays))
                        put("reminderTime", h.reminderTime)
                    })
                }
            })
            put("focusSessions", JSONArray().apply {
                sessions.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("taskId", s.taskId)
                        put("taskTitle", s.taskTitle)
                        put("mode", s.mode)
                        put("durationMinutes", s.durationMinutes)
                        put("ambientSound", s.ambientSound)
                        put("completedEpochDay", s.completedEpochDay)
                    })
                }
            })
            put("automations", JSONArray().apply {
                automations.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("title", a.title)
                        put("triggerType", a.triggerType)
                        put("conditionText", a.conditionText)
                        put("actionType", a.actionType)
                        put("isEnabled", a.isEnabled)
                    })
                }
            })
        }.toString(2)
    }

    /**
     * Validates and previews a JSON backup file before restoring.
     * Detects schema version, record counts, and how many task IDs already exist locally.
     */
    suspend fun inspectBackupJson(rawJson: String): Result<BackupPreviewSummary> = runCatching {
        com.example.data.validateDocumentStructure(rawJson)
        val trimmed = rawJson.trim()
        require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
            "Backup file must be a valid JSON object."
        }
        val root = JSONObject(trimmed)
        val hasKnownSection = root.has("tasks") || root.has("projects") || root.has("habits") || root.has("preferences")
        require(hasKnownSection) {
            "JSON does not contain Orbit backup sections (tasks, projects, habits, or preferences)."
        }

        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAtEpochMillis", System.currentTimeMillis())
        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        val projectsArr = root.optJSONArray("projects") ?: JSONArray()
        val habitsArr = root.optJSONArray("habits") ?: JSONArray()
        val sessionsArr = root.optJSONArray("focusSessions") ?: JSONArray()
        val automationsArr = root.optJSONArray("automations") ?: JSONArray()
        val hasPrefs = root.optJSONObject("preferences") != null

        val existingTaskIds = dao.getAllTasksOnce().map { it.id }.toSet()
        var duplicateTasks = 0
        for (i in 0 until tasksArr.length()) {
            val obj = tasksArr.getJSONObject(i)
            val title = obj.optString("title", "Imported Task")
            val due = obj.optLong("dueDateEpochDay", LocalDate.now().toEpochDay())
            val id = obj.optString("id").ifBlank {
                "imp_${(title.lowercase() + "_" + due).hashCode().toUInt().toString(16)}"
            }
            if (existingTaskIds.contains(id)) {
                duplicateTasks++
            }
        }

        BackupPreviewSummary(
            schemaVersion = version,
            exportedAtEpochMillis = exportedAt,
            taskCount = tasksArr.length(),
            duplicateTaskCount = duplicateTasks,
            projectCount = projectsArr.length(),
            habitCount = habitsArr.length(),
            focusSessionCount = sessionsArr.length(),
            automationCount = automationsArr.length(),
            hasPreferences = hasPrefs,
            rawJson = trimmed
        )
    }

    /**
     * Restores all tables atomically inside a Room @Transaction.
     * Supports both REPLACE_WORKSPACE and MERGE_SKIP_DUPLICATES duplicate handling strategies,
     * and migrates v1/v2/v3 JSON payloads safely with automatic rollback on malformed records.
     */
    suspend fun importBackupJson(
        rawJson: String,
        strategy: RestoreDuplicateStrategy = RestoreDuplicateStrategy.REPLACE_WORKSPACE
    ): Result<Int> = runCatching {
        com.example.data.validateDocumentStructure(rawJson)
        val trimmed = rawJson.trim()
        require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
            "Backup content is not a valid JSON object"
        }
        val root = JSONObject(trimmed)
        val hasKnownSection = root.has("tasks") || root.has("projects") || root.has("habits") || root.has("preferences")
        require(hasKnownSection) {
            "Missing Orbit backup sections in JSON"
        }

        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        val projectsArr = root.optJSONArray("projects") ?: JSONArray()
        val habitsArr = root.optJSONArray("habits") ?: JSONArray()
        val sessionsArr = root.optJSONArray("focusSessions") ?: JSONArray()
        val automationsArr = root.optJSONArray("automations") ?: JSONArray()
        val prefsObj = root.optJSONObject("preferences")

        val seenTaskIds = HashSet<String>()
        val taskEntities = mutableListOf<LocalTaskEntity>()
        for (i in 0 until tasksArr.length()) {
            val obj = tasksArr.getJSONObject(i)
            val tagsList = mutableListOf<String>()
            obj.optJSONArray("tags")?.let { arr ->
                for (j in 0 until arr.length()) tagsList.add(arr.getString(j))
            }
            val subtasksList = mutableListOf<String>()
            obj.optJSONArray("subtasks")?.let { arr ->
                for (j in 0 until arr.length()) subtasksList.add(arr.getString(j))
            }
            val commentsList = mutableListOf<String>()
            obj.optJSONArray("comments")?.let { arr ->
                for (j in 0 until arr.length()) commentsList.add(arr.getString(j))
            }
            val attachmentsList = mutableListOf<String>()
            obj.optJSONArray("attachments")?.let { arr ->
                for (j in 0 until arr.length()) attachmentsList.add(arr.getString(j))
            }
            val historyList = mutableListOf<String>()
            obj.optJSONArray("historyLog")?.let { arr ->
                for (j in 0 until arr.length()) historyList.add(arr.getString(j))
            }
            val offsetsList = mutableListOf<Int>()
            obj.optJSONArray("multipleReminderOffsets")?.let { arr ->
                for (j in 0 until arr.length()) offsetsList.add(arr.optInt(j, 15))
            }
            val customFieldsMap = mutableMapOf<String, String>()
            obj.optJSONObject("customFields")?.let { cfObj ->
                cfObj.keys().forEach { k -> customFieldsMap[k] = cfObj.optString(k, "") }
            }
            val title = obj.optString("title", "Imported Task")
            val due = obj.optLong("dueDateEpochDay", LocalDate.now().toEpochDay())
            val stableId = obj.optString("id").ifBlank {
                "imp_${(title.lowercase() + "_" + due).hashCode().toUInt().toString(16)}"
            }
            if (!seenTaskIds.add(stableId)) continue

            val primaryOffset = obj.optInt("reminderOffsetMinutes", 15)
            val task = OrbitTask(
                id = stableId,
                title = title,
                description = obj.optString("description", ""),
                status = obj.optString("status", "todo"),
                priority = obj.optString("priority", "p3"),
                projectId = obj.optString("projectId", "proj_personal"),
                projectName = obj.optString("projectName", "Personal"),
                sectionName = obj.optString("sectionName", "General"),
                startDateEpochDay = obj.optLong("startDateEpochDay", 0L),
                dueDateEpochDay = due,
                deadlineEpochDay = obj.optLong("deadlineEpochDay", 0L),
                scheduledDateEpochDay = obj.optLong("scheduledDateEpochDay", due),
                scheduledStartHour = obj.optInt("scheduledStartHour", -1),
                scheduledStartMinute = obj.optInt("scheduledStartMinute", 0),
                estimatedMinutes = obj.optInt("estimatedMinutes", 25),
                actualMinutes = obj.optInt("actualMinutes", 0),
                energyLevel = obj.optString("energyLevel", "medium"),
                recurrenceRule = obj.optString("recurrenceRule", ""),
                reminderOffsetMinutes = primaryOffset,
                multipleReminderOffsets = if (offsetsList.isEmpty()) listOf(primaryOffset) else offsetsList.distinct(),
                reminderEscalation = obj.optString("reminderEscalation", "standard"),
                locationTrigger = obj.optString("locationTrigger", ""),
                postponeCount = obj.optInt("postponeCount", 0),
                dependsOnTaskId = obj.optString("dependsOnTaskId", ""),
                assigneeName = obj.optString("assigneeName", ""),
                isFavorite = obj.optBoolean("isFavorite", false),
                isImportant = obj.optBoolean("isImportant", false),
                isUrgent = obj.optBoolean("isUrgent", false),
                isPinned = obj.optBoolean("isPinned", false),
                tags = tagsList,
                subtasks = subtasksList,
                comments = commentsList,
                attachments = attachmentsList,
                customFields = customFieldsMap,
                historyLog = historyList
            )
            taskEntities.add(LocalTaskEntity.fromDomain(task))
        }

        val seenProjectIds = HashSet<String>()
        val projectEntities = mutableListOf<LocalProjectEntity>()
        for (i in 0 until projectsArr.length()) {
            val obj = projectsArr.getJSONObject(i)
            val members = mutableListOf<String>()
            obj.optJSONArray("members")?.let { arr ->
                for (j in 0 until arr.length()) members.add(arr.getString(j))
            }
            val name = obj.optString("name", "Imported Project")
            val stableId = obj.optString("id").ifBlank { "proj_${name.lowercase().hashCode().toUInt().toString(16)}" }
            if (!seenProjectIds.add(stableId)) continue
            projectEntities.add(
                LocalProjectEntity.fromDomain(
                    OrbitProject(
                        id = stableId,
                        name = name,
                        description = obj.optString("description", ""),
                        colorHex = obj.optString("colorHex", "#C86D51"),
                        iconName = obj.optString("iconName", "folder"),
                        category = obj.optString("category", "work"),
                        isShared = obj.optBoolean("isShared", false),
                        members = members
                    )
                )
            )
        }

        val seenHabitIds = HashSet<String>()
        val habitEntities = mutableListOf<LocalHabitEntity>()
        for (i in 0 until habitsArr.length()) {
            val obj = habitsArr.getJSONObject(i)
            val completedDays = mutableListOf<Long>()
            obj.optJSONArray("completedEpochDays")?.let { arr ->
                for (j in 0 until arr.length()) completedDays.add(arr.optLong(j))
            }
            val frozenDays = mutableListOf<Long>()
            obj.optJSONArray("frozenEpochDays")?.let { arr ->
                for (j in 0 until arr.length()) frozenDays.add(arr.optLong(j))
            }
            val title = obj.optString("title", "Imported Habit")
            val stableId = obj.optString("id").ifBlank { "habit_${title.lowercase().hashCode().toUInt().toString(16)}" }
            if (!seenHabitIds.add(stableId)) continue
            habitEntities.add(
                LocalHabitEntity.fromDomain(
                    OrbitHabit(
                        id = stableId,
                        title = title,
                        description = obj.optString("description", ""),
                        routineType = obj.optString("routineType", "morning"),
                        category = obj.optString("category", "health"),
                        frequencyLabel = obj.optString("frequencyLabel", "Daily"),
                        targetDaysPerWeek = obj.optInt("targetDaysPerWeek", 7),
                        currentStreak = obj.optInt("currentStreak", 0),
                        bestStreak = obj.optInt("bestStreak", 0),
                        streakFreezesRemaining = obj.optInt("streakFreezesRemaining", 2),
                        completedEpochDays = completedDays,
                        frozenEpochDays = frozenDays,
                        reminderTime = obj.optString("reminderTime", "07:00"),
                        iconName = obj.optString("iconName", "flame")
                    )
                )
            )
        }

        val seenSessionIds = HashSet<String>()
        val sessionEntities = mutableListOf<LocalFocusSessionEntity>()
        for (i in 0 until sessionsArr.length()) {
            val obj = sessionsArr.getJSONObject(i)
            val stableId = obj.optString("id").ifBlank { "focus_imp_$i" }
            if (!seenSessionIds.add(stableId)) continue
            sessionEntities.add(
                LocalFocusSessionEntity.fromDomain(
                    FocusSessionRecord(
                        id = stableId,
                        taskId = obj.optString("taskId", ""),
                        taskTitle = obj.optString("taskTitle", "Focus Session"),
                        mode = obj.optString("mode", "pomodoro"),
                        durationMinutes = obj.optInt("durationMinutes", 25),
                        ambientSound = obj.optString("ambientSound", "Cosmic Rain"),
                        completedEpochDay = obj.optLong("completedEpochDay", LocalDate.now().toEpochDay())
                    )
                )
            )
        }

        val seenAutoIds = HashSet<String>()
        val automationEntities = mutableListOf<LocalAutomationEntity>()
        for (i in 0 until automationsArr.length()) {
            val obj = automationsArr.getJSONObject(i)
            val stableId = obj.optString("id").ifBlank { "auto_imp_$i" }
            if (!seenAutoIds.add(stableId)) continue
            automationEntities.add(
                LocalAutomationEntity.fromDomain(
                    AutomationRule(
                        id = stableId,
                        title = obj.optString("title", "Rule"),
                        triggerType = obj.optString("triggerType", ""),
                        conditionText = obj.optString("conditionText", ""),
                        actionType = obj.optString("actionType", ""),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            )
        }

        val currentPrefs = dao.getPreferencesOnce()?.toDomain() ?: UserPreferencesDoc()
        val updatedPrefsEntity = if (prefsObj != null) {
            val widgetsList = mutableListOf<String>()
            prefsObj.optJSONArray("enabledWidgets")?.let { arr ->
                for (j in 0 until arr.length()) widgetsList.add(arr.getString(j))
            }
            val favWidgetsList = mutableListOf<String>()
            prefsObj.optJSONArray("favoriteWidgets")?.let { arr ->
                for (j in 0 until arr.length()) favWidgetsList.add(arr.getString(j))
            }
            val sizesMap = mutableMapOf<String, String>()
            prefsObj.optJSONObject("widgetSizes")?.let { szObj ->
                szObj.keys().forEach { k -> sizesMap[k] = szObj.optString(k, "Medium") }
            }
            LocalPreferencesEntity.fromDomain(
                currentPrefs.copy(
                    themeMode = prefsObj.optString("themeMode", currentPrefs.themeMode),
                    accentPreset = prefsObj.optString("accentPreset", currentPrefs.accentPreset),
                    density = prefsObj.optString("density", currentPrefs.density),
                    streakStyle = prefsObj.optString("streakStyle", currentPrefs.streakStyle),
                    cornerRadiusDp = prefsObj.optInt("cornerRadiusDp", currentPrefs.cornerRadiusDp),
                    dashboardPreset = prefsObj.optString("dashboardPreset", currentPrefs.dashboardPreset),
                    quietHoursEnabled = prefsObj.optBoolean("quietHoursEnabled", currentPrefs.quietHoursEnabled),
                    enabledWidgets = if (widgetsList.isEmpty()) currentPrefs.enabledWidgets else widgetsList,
                    favoriteWidgets = if (favWidgetsList.isEmpty()) currentPrefs.favoriteWidgets else favWidgetsList,
                    widgetSizes = if (sizesMap.isEmpty()) currentPrefs.widgetSizes else sizesMap,
                    homeWidgetVariant = prefsObj.optString("homeWidgetVariant", currentPrefs.homeWidgetVariant),
                    quickNotes = prefsObj.optString("quickNotes", currentPrefs.quickNotes)
                )
            )
        } else null

        val affectedCount = if (strategy == RestoreDuplicateStrategy.MERGE_SKIP_DUPLICATES) {
            dao.mergeBackupSkipDuplicatesTransaction(
                tasks = taskEntities,
                projects = projectEntities,
                habits = habitEntities,
                sessions = sessionEntities,
                automations = automationEntities,
                preferences = updatedPrefsEntity
            )
        } else {
            dao.restoreFullBackupTransaction(
                tasks = taskEntities,
                projects = projectEntities,
                habits = habitEntities,
                sessions = sessionEntities,
                automations = automationEntities,
                preferences = updatedPrefsEntity
            )
            taskEntities.size + projectEntities.size + habitEntities.size
        }

        appContext?.let { ctx ->
            dao.getAllTasksOnce().map { it.toDomain() }.forEach { task ->
                if (!task.isCompleted && !task.isTrashed) {
                    OrbitReminderScheduler.scheduleTaskReminder(ctx, task)
                } else {
                    OrbitReminderScheduler.cancelTaskReminder(ctx, task.id)
                }
            }
        }
        refreshHomeWidget()
        affectedCount
    }

    suspend fun exportTasksCsv(): String {
        val tasks = dao.getAllTasksOnce().map { it.toDomain() }
        val sb = StringBuilder()
        sb.appendLine("ID,Title,Status,Priority,Project,StartDate,DueDate,Deadline,ScheduledHour,EstMinutes,ActualMinutes,Recurrence,Category,Pinned,Tags")
        tasks.forEach { t ->
            val safeTitle = "\"" + t.title.replace("\"", "\"\"") + "\""
            val safeProj = "\"" + t.projectName.replace("\"", "\"\"") + "\""
            val startStr = if (t.startDateEpochDay > 0) LocalDate.ofEpochDay(t.startDateEpochDay).toString() else ""
            val dueStr = if (t.dueDateEpochDay > 0) LocalDate.ofEpochDay(t.dueDateEpochDay).toString() else ""
            val deadStr = if (t.deadlineEpochDay > 0) LocalDate.ofEpochDay(t.deadlineEpochDay).toString() else ""
            val tagsStr = "\"" + t.tags.joinToString(";") + "\""
            sb.appendLine("${t.id},$safeTitle,${t.status},${t.priority},$safeProj,$startStr,$dueStr,$deadStr,${t.scheduledStartHour},${t.estimatedMinutes},${t.actualMinutes},${t.recurrenceRule},${t.assigneeName},${t.isPinned},$tagsStr")
        }
        return sb.toString()
    }

    suspend fun exportCalendarIcs(): String {
        val tasks = dao.getAllTasksOnce().map { it.toDomain() }.filter { !it.isTrashed }
        val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Orbit 2.0 Productivity OS//100% Free Local Calendar//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        tasks.forEach { t ->
            val day = if (t.scheduledDateEpochDay > 0) LocalDate.ofEpochDay(t.scheduledDateEpochDay)
            else if (t.dueDateEpochDay > 0) LocalDate.ofEpochDay(t.dueDateEpochDay)
            else LocalDate.now()
            val dateStr = day.format(fmt)
            val hour = if (t.scheduledStartHour in 0..23) t.scheduledStartHour else 9
            val dtStart = String.format("%sT%02d%02d00", dateStr, hour, t.scheduledStartMinute.coerceIn(0, 59))
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:${t.id}@orbit.local")
            sb.appendLine("DTSTART:$dtStart")
            sb.appendLine("SUMMARY:${t.title.replace("\n", " ")}")
            if (t.description.isNotBlank()) {
                sb.appendLine("DESCRIPTION:${t.description.replace("\n", "\\n")}")
            }
            sb.appendLine("CATEGORIES:${t.projectName}")
            sb.appendLine("STATUS:${if (t.isCompleted) "CONFIRMED" else "TENTATIVE"}")
            sb.appendLine("END:VEVENT")
        }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    suspend fun importCalendarIcs(rawIcs: String): Result<Int> = runCatching {
        require(rawIcs.length <= com.example.data.MAX_DOCUMENT_BYTES && rawIcs.toByteArray(Charsets.UTF_8).size <= com.example.data.MAX_DOCUMENT_BYTES) { "Calendar exceeds the 16 MiB safe import limit." }
        val blocks = rawIcs.split("BEGIN:VEVENT").drop(1)
        var count = 0
        val today = LocalDate.now().toEpochDay()
        for (block in blocks) {
            val lines = block.lines()
            val uidRaw = lines.firstOrNull { it.startsWith("UID:") }?.removePrefix("UID:")?.substringBefore("@")?.trim().orEmpty()
            val summary = lines.firstOrNull { it.startsWith("SUMMARY:") }?.removePrefix("SUMMARY:")?.trim() ?: continue
            val desc = lines.firstOrNull { it.startsWith("DESCRIPTION:") }?.removePrefix("DESCRIPTION:")?.trim() ?: ""
            val category = lines.firstOrNull { it.startsWith("CATEGORIES:") }?.removePrefix("CATEGORIES:")?.trim() ?: "Calendar Import"
            val dtStart = lines.firstOrNull { it.startsWith("DTSTART") }?.substringAfter(":")?.trim() ?: ""
            val parsedEpoch = try {
                if (dtStart.length >= 8) {
                    LocalDate.parse(dtStart.take(8), DateTimeFormatter.ofPattern("yyyyMMdd")).toEpochDay()
                } else today
            } catch (_: Exception) {
                today
            }
            val parsedHour = try {
                if (dtStart.contains("T") && dtStart.substringAfter("T").length >= 2) {
                    dtStart.substringAfter("T").take(2).toInt().coerceIn(0, 23)
                } else 9
            } catch (_: Exception) {
                9
            }
            val parsedMin = try {
                if (dtStart.contains("T") && dtStart.substringAfter("T").length >= 4) {
                    dtStart.substringAfter("T").substring(2, 4).toInt().coerceIn(0, 59)
                } else 0
            } catch (_: Exception) {
                0
            }
            val stableId = uidRaw.ifBlank {
                "ics_${(summary.lowercase() + "_" + parsedEpoch + "_" + parsedHour).hashCode().toUInt().toString(16)}"
            }
            val importedTask = OrbitTask(
                id = stableId,
                title = summary,
                description = desc.replace("\\n", "\n"),
                status = "todo",
                priority = "p3",
                projectId = "proj_personal",
                projectName = category,
                dueDateEpochDay = parsedEpoch,
                scheduledDateEpochDay = parsedEpoch,
                scheduledStartHour = parsedHour,
                scheduledStartMinute = parsedMin,
                tags = listOf("ics-import")
            )
            dao.upsertTask(LocalTaskEntity.fromDomain(importedTask))
            count++
        }
        refreshHomeWidget()
        count
    }
}
