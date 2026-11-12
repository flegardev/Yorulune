package com.example.ui.viewmodel

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.OrbitLocalRepository
import com.example.data.model.AutomationRule
import com.example.data.model.BackupPreviewSummary
import com.example.data.model.DashboardPresetConfig
import com.example.data.model.FocusSessionRecord
import com.example.data.model.OrbitHabit
import com.example.data.model.OrbitProject
import com.example.data.model.OrbitTask
import com.example.data.model.RestoreDuplicateStrategy
import com.example.data.model.SmartTaskParser
import com.example.data.model.UserPreferencesDoc
import com.example.data.notification.AmbientSoundEngine
import com.example.data.notification.OrbitReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class UiDataState<out T> {
    data object Loading : UiDataState<Nothing>()
    data class Success<T>(val data: T) : UiDataState<T>()
    data class Error(val message: String) : UiDataState<Nothing>()
}

data class TaskTemplateBlueprint(
    val id: String,
    val title: String,
    val category: String,
    val priority: String,
    val estimatedMinutes: Int,
    val steps: List<String>
)

val BuiltInTaskTemplates = listOf(
    TaskTemplateBlueprint(
        id = "tmpl_weekly_review",
        title = "Weekly GTD Review & Horizon Reset",
        category = "Productivity",
        priority = "p1",
        estimatedMinutes = 45,
        steps = listOf(
            "Clear physical desk & digital Inbox to zero",
            "Review past 7 days calendar & completed tasks",
            "Audit upcoming 14 days of deadlines & birthdays",
            "Review waiting-for & blocked dependent tasks",
            "Choose Top 3 Weekly Outcomes for next week"
        )
    ),
    TaskTemplateBlueprint(
        id = "tmpl_packing_list",
        title = "Travel & Trip Packing Master Checklist",
        category = "Travel",
        priority = "p2",
        estimatedMinutes = 30,
        steps = listOf(
            "Passport, ID, boarding pass & offline tickets",
            "Phone charger, power bank, universal adapter & cables",
            "Medications, toiletries & travel-size essentials",
            "Weather-appropriate clothing & comfortable walking shoes",
            "Lock windows, unplug appliances & set plants reminder"
        )
    ),
    TaskTemplateBlueprint(
        id = "tmpl_project_launch",
        title = "Release & Project Launch Readiness",
        category = "Work",
        priority = "p1",
        estimatedMinutes = 60,
        steps = listOf(
            "Run full regression suite & verify zero crash defects",
            "Verify accessibility contrast & reduced-motion support",
            "Export local backup snapshot & changelog notes",
            "Publish release package & update project milestone"
        )
    ),
    TaskTemplateBlueprint(
        id = "tmpl_morning_reset",
        title = "Morning Home & Kitchen Reset",
        category = "Home",
        priority = "p3",
        estimatedMinutes = 20,
        steps = listOf(
            "Make bed & open blinds for natural sunlight",
            "Unload dishwasher & wipe kitchen counters",
            "Start coffee/tea & review Today's Top 3 Priorities"
        )
    ),
    TaskTemplateBlueprint(
        id = "tmpl_monthly_finance",
        title = "Monthly Budget, Bills & Savings Audit",
        category = "Finance",
        priority = "p2",
        estimatedMinutes = 35,
        steps = listOf(
            "Verify rent/mortgage & utility bills paid",
            "Review credit card statements for unused subscriptions",
            "Transfer monthly savings allocation",
            "Log receipts into tax folder"
        )
    )
)

class OrbitViewModel(
    private val repository: OrbitLocalRepository,
    private val focusRecovery: com.example.data.FocusRecoveryStore? = null
) : ViewModel() {

    private val soundEngine = AmbientSoundEngine()

    // Reactive Local SQLite Streams
    val tasksState: StateFlow<UiDataState<List<OrbitTask>>> = repository
        .observeTasks()
        .map<List<OrbitTask>, UiDataState<List<OrbitTask>>> { UiDataState.Success(it) }
        .catch { emit(UiDataState.Error(it.localizedMessage ?: "Failed to load tasks")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiDataState.Loading
        )

    val projectsState: StateFlow<List<OrbitProject>> = repository
        .observeProjects()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val habitsState: StateFlow<List<OrbitHabit>> = repository
        .observeHabits()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val focusSessionsState: StateFlow<List<FocusSessionRecord>> = repository
        .observeFocusSessions()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val automationsState: StateFlow<List<AutomationRule>> = repository
        .observeAutomations()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val preferencesState: StateFlow<UserPreferencesDoc> = repository
        .observePreferences()
        .catch { emit(UserPreferencesDoc()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserPreferencesDoc()
        )

    // UI Feedback & Undo Stack
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var lastDeletedTask: OrbitTask? = null
    private val _canUndoDelete = MutableStateFlow(false)
    val canUndoDelete: StateFlow<Boolean> = _canUndoDelete.asStateFlow()

    // Exported Data Preview Modal & Pre-Restore Inspection State
    private val _exportedContentPreview = MutableStateFlow<Pair<String, String>?>(null)
    val exportedContentPreview: StateFlow<Pair<String, String>?> = _exportedContentPreview.asStateFlow()

    private val _backupPreviewState = MutableStateFlow<BackupPreviewSummary?>(null)
    val backupPreviewState: StateFlow<BackupPreviewSummary?> = _backupPreviewState.asStateFlow()

    // Celebration state
    private val _celebrationPulse = MutableStateFlow(false)
    val celebrationPulse: StateFlow<Boolean> = _celebrationPulse.asStateFlow()

    // Focus Timer State (Monotonic Clock Anchored)
    private val recoveredFocus = focusRecovery?.restore()
    private val recoveredPayload = recoveredFocus?.payload
    private val timerWrites = kotlinx.coroutines.channels.Channel<Pair<org.json.JSONObject, Boolean>>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _timerMode = MutableStateFlow(recoveredPayload?.optString("mode")?.takeIf { it in listOf("pomodoro","deep_work","short_break","long_break","stopwatch") } ?: "pomodoro") // pomodoro, deep_work, short_break, long_break, stopwatch
    val timerMode: StateFlow<String> = _timerMode.asStateFlow()

    private val _timerSecondsRemaining = MutableStateFlow(recoveredPayload?.let { p ->
        val base = p.optInt("seconds",1500)
        val delta = recoveredFocus?.secondsWhileAway?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
        if (_timerMode.value == "stopwatch") (base.toLong()+delta).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        else (base.toLong()-delta).coerceAtLeast(0).toInt()
    } ?: 1500)
    val timerSecondsRemaining: StateFlow<Int> = _timerSecondsRemaining.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(recoveredPayload?.optInt("total",1500)?.coerceAtLeast(0) ?: 1500)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _activeFocusTask = MutableStateFlow<OrbitTask?>(null)
    val activeFocusTask: StateFlow<OrbitTask?> = _activeFocusTask.asStateFlow()

    private val _ambientSound = MutableStateFlow(recoveredPayload?.optString("sound", "Silent") ?: "Silent")
    val ambientSound: StateFlow<String> = _ambientSound.asStateFlow()

    private val _ambientVolume = MutableStateFlow(0.7f)
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()

    private val _deepFocusZenMode = MutableStateFlow(false)
    val deepFocusZenMode: StateFlow<Boolean> = _deepFocusZenMode.asStateFlow()

    private var timerJob: Job? = null
    private var timerBaseAnchorRealtimeMs: Long = 0L
    private var timerInitialSecondsAtStart: Int = 25 * 60

    init {
        viewModelScope.launch {
            for ((payload, active) in timerWrites) {
                val saved = kotlinx.coroutines.withContext(Dispatchers.IO) { focusRecovery?.save(payload, active) ?: true }
                if (!saved) _snackbarMessage.value = "Could not save timer recovery. Keep the app open until storage is available."
            }
        }
        val taskId = recoveredPayload?.optString("task").orEmpty()
        if (taskId.isNotBlank()) viewModelScope.launch {
            _activeFocusTask.value = repository.observeTasks().first().find { it.id == taskId }
            persistTimer()
        }
        else persistTimer()
        if (recoveredPayload?.optBoolean("active") == true) _snackbarMessage.value = "Recovered your focus session, paused. Resume when you are ready."
        viewModelScope.launch {
            repository.seedStarterWorkspaceIfEmpty()
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissExportPreview() {
        _exportedContentPreview.value = null
    }

    fun dismissBackupPreview() {
        _backupPreviewState.value = null
    }

    fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun resetSeedData() {
        viewModelScope.launch {
            repository.resetToStarterSeed(preservePreferences = true)
            _snackbarMessage.value = "Workspace reset to fresh starter seed data"
        }
    }

    // --- DETERMINISTIC TASK ACTIONS ---

    fun quickAddNaturalTask(rawInput: String, targetProjectId: String? = null, statusOverride: String? = null) {
        if (rawInput.isBlank()) return
        val today = LocalDate.now().toEpochDay()
        val parsed = SmartTaskParser.parse(rawInput, today)
        val projects = projectsState.value
        val matchedProject = when {
            targetProjectId != null -> projects.firstOrNull { it.id == targetProjectId }
            parsed.projectHint.isNotBlank() -> projects.firstOrNull {
                it.name.contains(parsed.projectHint, ignoreCase = true) ||
                    it.category.contains(parsed.projectHint, ignoreCase = true)
            }
            else -> null
        } ?: projects.firstOrNull() ?: OrbitProject(id = "proj_personal", name = "Life & Wellness")

        val newTask = OrbitTask(
            title = parsed.cleanTitle,
            description = "",
            status = statusOverride ?: "todo",
            priority = parsed.priority,
            projectId = matchedProject.id,
            projectName = matchedProject.name,
            sectionName = if (parsed.reminderCategory.isNotBlank()) parsed.reminderCategory else "General",
            tags = parsed.tags,
            startDateEpochDay = parsed.startDateEpochDay,
            dueDateEpochDay = parsed.dueDateEpochDay,
            deadlineEpochDay = parsed.deadlineEpochDay,
            scheduledDateEpochDay = parsed.scheduledDateEpochDay,
            scheduledStartHour = parsed.scheduledStartHour,
            scheduledStartMinute = parsed.scheduledStartMinute,
            estimatedMinutes = parsed.estimatedMinutes,
            energyLevel = parsed.energyLevel,
            recurrenceRule = parsed.recurrenceRule,
            locationTrigger = parsed.locationTrigger,
            assigneeName = parsed.reminderCategory,
            isImportant = parsed.priority in listOf("p1", "p2"),
            isUrgent = parsed.priority == "p1" || parsed.dueDateEpochDay == today,
            historyLog = listOf("Captured via Quick Add 2.0")
        )

        viewModelScope.launch {
            val res = repository.upsertTask(newTask)
            if (res.isSuccess) {
                _snackbarMessage.value = "Captured: \"${parsed.cleanTitle}\""
            } else {
                _snackbarMessage.value = "Failed to save task: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun saveFullTask(task: OrbitTask) {
        viewModelScope.launch {
            val isUpdate = task.id.isNotBlank()
            val updatedHistory = (task.historyLog + if (isUpdate) "Updated task details" else "Created task").takeLast(20)
            val res = repository.upsertTask(task.copy(historyLog = updatedHistory))
            if (res.isSuccess) {
                _snackbarMessage.value = if (isUpdate) "Task saved" else "Task created"
            } else {
                _snackbarMessage.value = "Could not save task: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun toggleTaskPin(task: OrbitTask) {
        viewModelScope.launch {
            val pinned = !task.isPinned
            repository.upsertTask(task.copy(isPinned = pinned))
            _snackbarMessage.value = if (pinned) "Pinned \"${task.title}\" to top" else "Unpinned \"${task.title}\""
        }
    }

    fun duplicateTask(task: OrbitTask) {
        viewModelScope.launch {
            repository.upsertTask(
                task.copy(
                    id = "",
                    title = "${task.title} (Copy)",
                    status = "todo",
                    historyLog = listOf("Duplicated from ${task.title}"),
                    createdAt = null,
                    updatedAt = null
                )
            )
            _snackbarMessage.value = "Duplicated \"${task.title}\""
        }
    }

    fun applyTaskTemplate(template: TaskTemplateBlueprint) {
        val today = LocalDate.now().toEpochDay()
        val encodedSteps = template.steps.map { "0|$it" }
        viewModelScope.launch {
            repository.upsertTask(
                OrbitTask(
                    title = template.title,
                    description = "Created from ${template.category} template (${template.steps.size} steps)",
                    status = "todo",
                    priority = template.priority,
                    dueDateEpochDay = today,
                    scheduledDateEpochDay = today,
                    estimatedMinutes = template.estimatedMinutes,
                    subtasks = encodedSteps,
                    isImportant = template.priority in listOf("p1", "p2"),
                    isUrgent = template.priority == "p1",
                    historyLog = listOf("Created from ${template.title} template")
                )
            )
            _snackbarMessage.value = "Created \"${template.title}\" from template!"
        }
    }

    fun toggleTaskCompletion(task: OrbitTask) {
        viewModelScope.launch {
            val nowDone = !task.isCompleted
            val newStatus = if (nowDone) "done" else "todo"
            val updatedHistory = (task.historyLog + if (nowDone) "Marked completed" else "Reopened task").takeLast(20)
            val updated = task.copy(status = newStatus, historyLog = updatedHistory)
            repository.upsertTask(updated)

            if (nowDone) {
                _celebrationPulse.value = true
                // Deterministic recurring task calculation
                if (task.recurrenceRule.isNotBlank()) {
                    val today = LocalDate.now().toEpochDay()
                    val baseDay = if (task.recurrenceRule == "AFTER_COMPLETION") {
                        today
                    } else if (task.dueDateEpochDay > 0) {
                        task.dueDateEpochDay
                    } else {
                        today
                    }
                    val nextEpochDay = SmartTaskParser.calculateNextRecurrenceDay(baseDay, task.recurrenceRule)
                    repository.upsertTask(
                        task.copy(
                            id = "",
                            status = "todo",
                            dueDateEpochDay = nextEpochDay,
                            scheduledDateEpochDay = nextEpochDay,
                            subtasks = task.parsedSubtasks().map { it.copy(isDone = false).encode() },
                            historyLog = listOf("Auto-generated from recurring rule ${task.recurrenceRule}"),
                            createdAt = null,
                            updatedAt = null
                        )
                    )
                    _snackbarMessage.value = "Completed! Next occurrence scheduled for ${SmartTaskParser.formatEpochDay(nextEpochDay)}."
                } else {
                    val currentList = (tasksState.value as? UiDataState.Success)?.data.orEmpty()
                    val dependent = currentList.firstOrNull { it.dependsOnTaskId == task.id }
                    if (dependent != null) {
                        repository.upsertTask(
                            dependent.copy(
                                dependsOnTaskId = "",
                                status = "todo",
                                scheduledDateEpochDay = LocalDate.now().toEpochDay(),
                                historyLog = (dependent.historyLog + "Unlocked after completing ${task.title}").takeLast(20)
                            )
                        )
                        _snackbarMessage.value = "Completed! Unlocked dependent task: ${dependent.title}"
                    } else {
                        _snackbarMessage.value = "Task completed"
                    }
                }
                delay(1400L)
                _celebrationPulse.value = false
            }
        }
    }

    fun toggleSubtask(task: OrbitTask, subtaskIndex: Int) {
        if (task.id.isBlank()) return
        val parsed = task.parsedSubtasks().toMutableList()
        if (subtaskIndex !in parsed.indices) return
        val current = parsed[subtaskIndex]
        parsed[subtaskIndex] = current.copy(isDone = !current.isDone)
        val encoded = parsed.map { it.encode() }
        viewModelScope.launch {
            repository.upsertTask(task.copy(subtasks = encoded))
        }
    }

    fun snoozeOrPostponeTask(task: OrbitTask, daysToAdd: Long, minutesOffset: Int = -1) {
        if (task.id.isBlank()) return
        val today = LocalDate.now().toEpochDay()
        val baseDay = if (task.dueDateEpochDay > today) task.dueDateEpochDay else today
        val newDue = baseDay + daysToAdd
        viewModelScope.launch {
            repository.upsertTask(
                task.copy(
                    dueDateEpochDay = newDue,
                    scheduledDateEpochDay = newDue,
                    postponeCount = (task.postponeCount + 1).coerceAtMost(999),
                    reminderOffsetMinutes = if (minutesOffset >= 0) minutesOffset else task.reminderOffsetMinutes,
                    historyLog = (task.historyLog + "Snoozed/Rescheduled").takeLast(20)
                )
            )
            _snackbarMessage.value = if (daysToAdd > 0) {
                "Rescheduled to ${SmartTaskParser.formatEpochDay(newDue)}"
            } else {
                "Reminder snoozed for ${minutesOffset}m"
            }
        }
    }

    fun deleteTaskWithUndo(task: OrbitTask) {
        if (task.id.isBlank()) return
        lastDeletedTask = task
        _canUndoDelete.value = true
        viewModelScope.launch {
            if (task.status == "trash") {
                repository.deleteTaskPermanently(task.id)
                _snackbarMessage.value = "Permanently deleted \"${task.title}\""
            } else {
                repository.upsertTask(task.copy(status = "trash"))
                _snackbarMessage.value = "Moved \"${task.title}\" to Trash"
            }
        }
    }

    fun restoreFromTrash(task: OrbitTask) {
        viewModelScope.launch {
            repository.upsertTask(task.copy(status = "todo"))
            _snackbarMessage.value = "Restored \"${task.title}\" from Trash"
        }
    }

    fun emptyTrashBin() {
        viewModelScope.launch {
            repository.emptyTrash()
            _snackbarMessage.value = "Trash emptied"
        }
    }

    fun undoLastDelete() {
        val toRestore = lastDeletedTask ?: return
        lastDeletedTask = null
        _canUndoDelete.value = false
        viewModelScope.launch {
            repository.upsertTask(toRestore.copy(status = "todo"))
            _snackbarMessage.value = "Restored \"${toRestore.title}\""
        }
    }

    fun bulkCompleteTasks(taskIds: Set<String>) {
        val all = (tasksState.value as? UiDataState.Success)?.data.orEmpty()
        viewModelScope.launch {
            all.filter { it.id in taskIds }.forEach { t ->
                repository.upsertTask(t.copy(status = "done"))
            }
            _snackbarMessage.value = "Completed ${taskIds.size} tasks"
        }
    }

    fun bulkRescheduleTomorrow(taskIds: Set<String>) {
        val tomorrow = LocalDate.now().toEpochDay() + 1
        val all = (tasksState.value as? UiDataState.Success)?.data.orEmpty()
        viewModelScope.launch {
            all.filter { it.id in taskIds }.forEach { t ->
                repository.upsertTask(t.copy(dueDateEpochDay = tomorrow, scheduledDateEpochDay = tomorrow))
            }
            _snackbarMessage.value = "Moved ${taskIds.size} tasks to Tomorrow"
        }
    }

    fun archiveCompletedTasks() {
        val all = (tasksState.value as? UiDataState.Success)?.data.orEmpty()
        val doneList = all.filter { it.status == "done" }
        if (doneList.isEmpty()) {
            _snackbarMessage.value = "No completed tasks to archive"
            return
        }
        viewModelScope.launch {
            doneList.forEach { t ->
                repository.upsertTask(t.copy(status = "archived"))
            }
            _snackbarMessage.value = "Archived ${doneList.size} completed tasks"
        }
    }

    /**
     * 100% Deterministic Free-Slot Time Blocker (Zero AI).
     * Assigns unscheduled tasks for a given day into open workday hours sorted by priority.
     */
    fun autoAssignFreeTimeSlotsDeterministically(dayEpoch: Long = LocalDate.now().toEpochDay()) {
        val all = (tasksState.value as? UiDataState.Success)?.data.orEmpty()
        val prefs = preferencesState.value
        val dayTasks = all.filter {
            !it.isCompleted && !it.isTrashed &&
                (it.scheduledDateEpochDay == dayEpoch || it.dueDateEpochDay == dayEpoch)
        }
        val occupiedHours = dayTasks.filter { it.scheduledStartHour >= 0 }.map { it.scheduledStartHour }.toMutableSet()
        val unscheduled = dayTasks.filter { it.scheduledStartHour < 0 }.sortedBy { it.priority }

        if (unscheduled.isEmpty()) {
            _snackbarMessage.value = "All tasks for this day already have time blocks!"
            return
        }

        viewModelScope.launch {
            var nextCandidateHour = prefs.workdayStartHour.coerceIn(6, 12)
            val maxHour = prefs.workdayEndHour.coerceIn(15, 22)
            var scheduledCount = 0
            for (task in unscheduled) {
                while (occupiedHours.contains(nextCandidateHour) && nextCandidateHour < maxHour) {
                    nextCandidateHour++
                }
                if (nextCandidateHour <= maxHour) {
                    occupiedHours.add(nextCandidateHour)
                    repository.upsertTask(
                        task.copy(
                            scheduledDateEpochDay = dayEpoch,
                            scheduledStartHour = nextCandidateHour,
                            scheduledStartMinute = 0
                        )
                    )
                    scheduledCount++
                    nextCandidateHour++
                }
            }
            _snackbarMessage.value = "Time-blocked $scheduledCount tasks into open calendar slots!"
        }
    }

    fun triggerTaskReminderNow(context: Context, task: OrbitTask) {
        val status = OrbitReminderScheduler.triggerImmediateReminder(
            context = context,
            task = task,
            quietHoursActive = preferencesState.value.quietHoursEnabled
        )
        _snackbarMessage.value = status
    }

    // --- PROJECTS & HABITS ---

    fun createNewProject(name: String, description: String, colorHex: String, category: String, isShared: Boolean, members: List<String>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val res = repository.upsertProject(
                OrbitProject(
                    name = name.trim(),
                    description = description.trim(),
                    colorHex = colorHex,
                    iconName = if (isShared) "group" else "folder",
                    category = category,
                    isShared = isShared,
                    members = members.take(10)
                )
            )
            if (res.isSuccess) {
                _snackbarMessage.value = "Created project \"${name.trim()}\""
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _snackbarMessage.value = "Deleted project"
        }
    }

    fun createNewHabit(
        title: String,
        routineType: String,
        category: String,
        targetDays: Int,
        reminderTime: String,
        frequencyLabel: String = "Daily"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.upsertHabit(
                OrbitHabit(
                    title = title.trim(),
                    description = "Consistent daily ritual",
                    routineType = routineType,
                    category = category,
                    frequencyLabel = frequencyLabel,
                    targetDaysPerWeek = targetDays.coerceIn(1, 7),
                    reminderTime = reminderTime
                )
            )
            _snackbarMessage.value = "Added habit \"${title.trim()}\""
        }
    }

    fun toggleHabitToday(habit: OrbitHabit, targetEpochDay: Long = LocalDate.now().toEpochDay()) {
        if (habit.id.isBlank()) return
        val currentDays = habit.completedEpochDays.toMutableList()
        val alreadyDone = currentDays.contains(targetEpochDay)
        val newStreak: Int
        val newBest: Int

        if (alreadyDone) {
            currentDays.remove(targetEpochDay)
            newStreak = (habit.currentStreak - 1).coerceAtLeast(0)
            newBest = habit.bestStreak
        } else {
            if (currentDays.size >= 180) currentDays.removeAt(0)
            currentDays.add(targetEpochDay)
            newStreak = habit.currentStreak + 1
            newBest = maxOf(habit.bestStreak, newStreak)
            _celebrationPulse.value = true
        }

        viewModelScope.launch {
            repository.upsertHabit(
                habit.copy(
                    completedEpochDays = currentDays,
                    currentStreak = newStreak,
                    bestStreak = newBest
                )
            )
            if (!alreadyDone) {
                delay(1000L)
                _celebrationPulse.value = false
            }
        }
    }

    fun useHabitStreakFreeze(habit: OrbitHabit) {
        if (habit.streakFreezesRemaining <= 0) {
            _snackbarMessage.value = "No streak freezes remaining for \"${habit.title}\""
            return
        }
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            repository.upsertHabit(
                habit.copy(
                    streakFreezesRemaining = habit.streakFreezesRemaining - 1,
                    frozenEpochDays = (habit.frozenEpochDays + today).distinct()
                )
            )
            _snackbarMessage.value = "Applied Streak Freeze for \"${habit.title}\" (${habit.streakFreezesRemaining - 1} left)"
        }
    }

    // --- FOCUS TIMER & AMBIENT AUDIO (MONOTONIC CLOCK ANCHORED) ---

    fun selectFocusMode(mode: String, customMinutes: Int? = null) {
        pauseTimer()
        _timerMode.value = mode
        val prefs = preferencesState.value
        val mins = customMinutes ?: when (mode) {
            "pomodoro" -> prefs.defaultPomodoroMinutes
            "short_break" -> prefs.defaultBreakMinutes
            "long_break" -> prefs.defaultLongBreakMinutes
            "deep_work" -> 50
            "stopwatch" -> 0
            else -> 25
        }
        _timerTotalSeconds.value = if (mode == "stopwatch") 0 else mins * 60
        _timerSecondsRemaining.value = _timerTotalSeconds.value
        persistTimer()
    }

    private fun persistTimer() {
        timerWrites.trySend(org.json.JSONObject().put("mode",_timerMode.value)
            .put("seconds",_timerSecondsRemaining.value).put("total",_timerTotalSeconds.value)
            .put("active",_timerRunning.value).put("sound",_ambientSound.value)
            .put("task",_activeFocusTask.value?.id.orEmpty()) to _timerRunning.value)
    }

    fun resetTimer() {
        selectFocusMode(_timerMode.value)
        _snackbarMessage.value = "Timer reset"
    }

    fun setFocusTask(task: OrbitTask?) {
        _activeFocusTask.value = task
        persistTimer()
    }

    fun setAmbientSoundscape(sound: String) {
        _ambientSound.value = sound
        persistTimer()
        if (_timerRunning.value) {
            soundEngine.start(sound, viewModelScope, _ambientVolume.value)
        }
    }

    fun setAmbientVolume(volume: Float) {
        val clamped = volume.coerceIn(0.05f, 1.0f)
        _ambientVolume.value = clamped
        soundEngine.setVolume(clamped)
        updatePreferences { it.copy(ambientVolume = clamped) }
    }

    fun toggleDeepFocusZenMode() {
        _deepFocusZenMode.value = !_deepFocusZenMode.value
    }

    fun startOrPauseTimer() {
        if (_timerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _timerRunning.value = true
        timerBaseAnchorRealtimeMs = SystemClock.elapsedRealtime()
        timerInitialSecondsAtStart = _timerSecondsRemaining.value
        persistTimer()
        soundEngine.start(_ambientSound.value, viewModelScope, _ambientVolume.value)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _timerRunning.value) {
                delay(250L) // Poll monotonic clock 4x/sec for drift-free accuracy
                val elapsedSec = ((SystemClock.elapsedRealtime() - timerBaseAnchorRealtimeMs) / 1000L).toInt()
                if (_timerMode.value == "stopwatch") {
                    val currentTotal = timerInitialSecondsAtStart + elapsedSec
                    _timerSecondsRemaining.value = currentTotal
                    _timerTotalSeconds.value = currentTotal
                } else {
                    val remaining = (timerInitialSecondsAtStart - elapsedSec).coerceAtLeast(0)
                    _timerSecondsRemaining.value = remaining
                    if (remaining <= 0) {
                        completeFocusSession()
                        break
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        if (_timerRunning.value) {
            val elapsed = ((SystemClock.elapsedRealtime() - timerBaseAnchorRealtimeMs) / 1000L).coerceAtLeast(0)
            _timerSecondsRemaining.value = if (_timerMode.value == "stopwatch")
                (timerInitialSecondsAtStart.toLong() + elapsed).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            else (timerInitialSecondsAtStart.toLong() - elapsed).coerceAtLeast(0).toInt()
        }
        _timerRunning.value = false
        persistTimer()
        timerJob?.cancel()
        soundEngine.stop()
    }

    fun completeFocusSession() {
        pauseTimer()
        val elapsedMinutes = com.example.data.model.focusMinutesToRecord(
            _timerMode.value, _timerTotalSeconds.value, _timerSecondsRemaining.value
        )

        val focusTask = _activeFocusTask.value
        viewModelScope.launch {
            repository.recordFocusSession(
                FocusSessionRecord(
                    taskId = focusTask?.id.orEmpty(),
                    taskTitle = focusTask?.title ?: "Deep Focus Session",
                    mode = _timerMode.value,
                    durationMinutes = elapsedMinutes,
                    ambientSound = _ambientSound.value,
                    completedEpochDay = LocalDate.now().toEpochDay()
                )
            )
            if (focusTask != null && focusTask.id.isNotBlank()) {
                repository.upsertTask(
                    focusTask.copy(actualMinutes = (focusTask.actualMinutes + elapsedMinutes).coerceAtMost(99999))
                )
            }
            _snackbarMessage.value = "Logged ${elapsedMinutes}m focus session!"
            selectFocusMode(_timerMode.value)
        }
    }

    // --- THEME STUDIO, WIDGETS & PREFERENCES ---

    fun updatePreferences(transform: (UserPreferencesDoc) -> UserPreferencesDoc) {
        val updated = transform(preferencesState.value)
        viewModelScope.launch {
            repository.savePreferences(updated)
        }
    }

    fun applyDashboardPreset(preset: DashboardPresetConfig) {
        updatePreferences {
            it.copy(
                dashboardPreset = preset.key,
                enabledWidgets = preset.widgets
            )
        }
        _snackbarMessage.value = "Applied ${preset.label} layout (${preset.widgets.size} widgets)"
    }

    fun toggleWidgetVisibility(widgetKey: String) {
        val current = preferencesState.value.enabledWidgets.toMutableList()
        if (current.contains(widgetKey)) {
            if (current.size > 1) current.remove(widgetKey)
        } else {
            current.add(widgetKey)
        }
        updatePreferences { it.copy(enabledWidgets = current) }
    }

    fun toggleFavoriteWidget(widgetKey: String) {
        val current = preferencesState.value.favoriteWidgets.toMutableList()
        if (current.contains(widgetKey)) {
            current.remove(widgetKey)
        } else {
            current.add(widgetKey)
        }
        updatePreferences { it.copy(favoriteWidgets = current) }
    }

    fun setWidgetSize(widgetKey: String, size: String) {
        val current = preferencesState.value.widgetSizes.toMutableMap()
        current[widgetKey] = size
        updatePreferences { it.copy(widgetSizes = current) }
    }

    fun setHomeWidgetVariant(variantKey: String) {
        updatePreferences { it.copy(homeWidgetVariant = variantKey) }
        _snackbarMessage.value = "Active Home-Screen Widget updated"
    }

    fun moveWidgetOrder(widgetKey: String, moveUp: Boolean) {
        val current = preferencesState.value.enabledWidgets.toMutableList()
        val idx = current.indexOf(widgetKey)
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in current.indices) return
        val item = current.removeAt(idx)
        current.add(targetIdx, item)
        updatePreferences { it.copy(enabledWidgets = current) }
    }

    fun toggleAutomationRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.saveAutomation(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun createCustomAutomation(title: String, trigger: String, condition: String, action: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveAutomation(
                AutomationRule(
                    title = title.trim(),
                    triggerType = trigger.trim(),
                    conditionText = condition.trim(),
                    actionType = action.trim(),
                    isEnabled = true
                )
            )
            _snackbarMessage.value = "Workflow rule saved"
        }
    }

    // --- LOCAL BACKUP, EXPORT & IMPORT (JSON, CSV, ICS + ANDROID DOCUMENT PICKER) ---

    fun prepareSafExport(
        format: String,
        onReady: (suggestedFilename: String, mimeType: String, content: String) -> Unit
    ) {
        val dateTag = LocalDate.now().toString()
        when (format.lowercase()) {
            "csv" -> onReady("yorulune_tasks_$dateTag.csv", "text/csv", "")
            "ics" -> onReady("yorulune_calendar_$dateTag.ics", "text/calendar", "")
            else -> onReady("yorulune_vault_backup_v3_$dateTag.json", "application/json", "")
        }
    }

    /** Provider I/O and serialization stay off the UI thread. Regeneration from
     * Room avoids retaining an export buffer in a recreatable Activity. */
    fun saveExportToDocument(context: Context, uri: Uri, format: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val payload = when (format) {
                    "csv" -> repository.exportTasksCsv()
                    "ics" -> repository.exportCalendarIcs()
                    else -> repository.exportFullBackupJson()
                }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error("Unable to open selected destination")
            }.onSuccess { postMessage("Saved to your chosen document provider") }
                .onFailure { postMessage("Export failed: ${it.localizedMessage ?: "I/O error"}") }
        }
    }

    fun inspectDocument(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    com.example.data.readDocumentText(it)
                } ?: error("Unable to open selected document")
            }.onSuccess { text ->
                val trimmed = text.trim()
                when {
                    trimmed.isEmpty() -> postMessage("Selected file is empty")
                    trimmed.contains("BEGIN:VCALENDAR", true) || trimmed.contains("BEGIN:VEVENT", true) -> importCalendarIcs(trimmed)
                    else -> inspectBackupBeforeRestore(trimmed)
                }
            }.onFailure { postMessage("Import failed: ${it.localizedMessage ?: "I/O error"}") }
        }
    }

    fun exportBackupJson() {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.exportFullBackupJson()
            _exportedContentPreview.value = "JSON Full Vault Backup (v3)" to json
            _snackbarMessage.value = "Generated ${json.length} bytes of local JSON backup"
        }
    }

    fun exportTasksCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val csv = repository.exportTasksCsv()
            _exportedContentPreview.value = "CSV Tasks Export" to csv
            _snackbarMessage.value = "Generated CSV spreadsheet export (${csv.lines().size - 1} rows)"
        }
    }

    fun exportCalendarIcs() {
        viewModelScope.launch(Dispatchers.IO) {
            val ics = repository.exportCalendarIcs()
            _exportedContentPreview.value = "iCalendar (.ICS) Export" to ics
            _snackbarMessage.value = "Generated standard RFC-5545 .ICS calendar file"
        }
    }

    fun inspectBackupBeforeRestore(rawJson: String) {
        if (rawJson.isBlank()) {
            _snackbarMessage.value = "Backup content is empty"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.inspectBackupJson(rawJson)
                .onSuccess { summary ->
                    _backupPreviewState.value = summary
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Invalid backup file: ${err.localizedMessage}"
                }
        }
    }

    fun confirmBackupRestore(strategy: RestoreDuplicateStrategy) {
        val preview = _backupPreviewState.value ?: return
        _backupPreviewState.value = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.importBackupJson(preview.rawJson, strategy)
                .onSuccess { count ->
                    _snackbarMessage.value = "Restored $count records (${strategy.label})!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Restore rolled back safely: ${err.localizedMessage}"
                }
        }
    }

    fun importBackupJson(
        rawJson: String,
        strategy: RestoreDuplicateStrategy = RestoreDuplicateStrategy.REPLACE_WORKSPACE
    ) {
        if (rawJson.isBlank()) return
        viewModelScope.launch {
            val res = repository.importBackupJson(rawJson, strategy)
            res.onSuccess { count ->
                _snackbarMessage.value = "Successfully restored $count records from JSON backup!"
            }.onFailure { err ->
                _snackbarMessage.value = "Import error: ${err.localizedMessage}"
            }
        }
    }

    fun importCalendarIcs(rawIcs: String) {
        if (rawIcs.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.importCalendarIcs(rawIcs)
            res.onSuccess { count ->
                _snackbarMessage.value = "Imported $count calendar events from .ICS!"
            }.onFailure { err ->
                _snackbarMessage.value = "ICS Import error: ${err.localizedMessage}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stop()
    }
}
