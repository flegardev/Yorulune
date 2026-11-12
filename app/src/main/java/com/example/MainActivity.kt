package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.OrbitLocalDatabase
import com.example.data.local.OrbitLocalRepository
import com.example.data.model.OrbitTask
import com.example.data.model.RestoreDuplicateStrategy
import com.example.data.notification.OrbitReminderScheduler
import com.example.ui.screens.FirstRunWalkthroughDialog
import com.example.ui.screens.FocusCockpitScreen
import com.example.ui.screens.MissionControlScreen
import com.example.ui.screens.MoreAndEcosystemScreen
import com.example.ui.screens.SmartCalendarScreen
import com.example.ui.screens.TaskDetailAndCreationModal
import com.example.ui.screens.UniversalCommandCenterDialog
import com.example.ui.screens.WorkspaceAndViewsScreen
import com.example.ui.theme.OrbitTheme
import com.example.ui.viewmodel.OrbitViewModel
import com.example.ui.viewmodel.UiDataState
import androidx.compose.material3.OutlinedButton
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OrbitReminderScheduler.ensureChannels(this)
        enableEdgeToEdge()
        setContent {
            OrbitRootApp()
        }
    }
}

@Composable
fun OrbitRootApp() {
    val orbitViewModel: OrbitViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dao = OrbitLocalDatabase.getInstance(app).orbitDao()
                OrbitViewModel(OrbitLocalRepository(dao, app), com.example.data.FocusRecoveryStore(app))
            }
        }
    )
    OrbitLocalShell(viewModel = orbitViewModel)
}

private data class OrbitNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun OrbitLocalShell(viewModel: OrbitViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val tasksUiState by viewModel.tasksState.collectAsStateWithLifecycle()
    val projects by viewModel.projectsState.collectAsStateWithLifecycle()
    val habits by viewModel.habitsState.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessionsState.collectAsStateWithLifecycle()
    val automations by viewModel.automationsState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val canUndoDelete by viewModel.canUndoDelete.collectAsStateWithLifecycle()
    val celebrationPulse by viewModel.celebrationPulse.collectAsStateWithLifecycle()

    val timerRunning by viewModel.timerRunning.collectAsStateWithLifecycle()
    val timerMode by viewModel.timerMode.collectAsStateWithLifecycle()
    val timerSecondsRemaining by viewModel.timerSecondsRemaining.collectAsStateWithLifecycle()
    val timerTotalSeconds by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val activeFocusTask by viewModel.activeFocusTask.collectAsStateWithLifecycle()
    val ambientSound by viewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume by viewModel.ambientVolume.collectAsStateWithLifecycle()
    val deepFocusZenMode by viewModel.deepFocusZenMode.collectAsStateWithLifecycle()
    val exportedContentPreview by viewModel.exportedContentPreview.collectAsStateWithLifecycle()
    val backupPreviewState by viewModel.backupPreviewState.collectAsStateWithLifecycle()

    val safCreateJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.saveExportToDocument(context.applicationContext, uri, "json")
    }

    val safCreateCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.saveExportToDocument(context.applicationContext, uri, "csv")
    }

    val safCreateIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        if (uri != null) viewModel.saveExportToDocument(context.applicationContext, uri, "ics")
    }

    val safOpenDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.inspectDocument(context.applicationContext, uri)
    }

    val tasks = (tasksUiState as? UiDataState.Success)?.data.orEmpty()

    var currentTab by rememberSaveable { mutableStateOf("home") }
    var activeSmartFilter by rememberSaveable { mutableStateOf("All") }
    var activeLayoutMode by rememberSaveable { mutableStateOf("List") }
    var activeMoreSubTab by rememberSaveable { mutableStateOf("Habits & Routines") }
    var editingTask by remember { mutableStateOf<OrbitTask?>(null) }
    var showTaskModal by remember { mutableStateOf(false) }
    var showCommandCenter by remember { mutableStateOf(false) }
    var showWalkthrough by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.postMessage(
            if (granted) "System notifications enabled for Yorulune Reminders!"
            else "Using in-app reminder banners (system permission denied)"
        )
    }

    // BackHandler for secondary tabs per navigation guidelines
    BackHandler(enabled = currentTab != "home") {
        currentTab = "home"
    }

    val handleNavigateDestination: (String) -> Unit = { route ->
        when (route) {
            "dashboard", "home" -> currentTab = "home"
            "inbox", "today", "tomorrow", "upcoming", "all_tasks", "completed", "overdue",
            "favorites", "trash", "templates", "task_details", "kanban", "eisenhower", "search" -> {
                when (route) {
                    "inbox" -> {
                        activeSmartFilter = "Inbox"
                        activeLayoutMode = "List"
                    }
                    "today" -> {
                        activeSmartFilter = "Today"
                        activeLayoutMode = "List"
                    }
                    "tomorrow" -> {
                        activeSmartFilter = "Tomorrow"
                        activeLayoutMode = "List"
                    }
                    "upcoming" -> {
                        activeSmartFilter = "Upcoming"
                        activeLayoutMode = "List"
                    }
                    "all_tasks", "search" -> {
                        activeSmartFilter = "All"
                        activeLayoutMode = "List"
                    }
                    "completed" -> {
                        activeSmartFilter = "Completed"
                        activeLayoutMode = "List"
                    }
                    "overdue" -> {
                        activeSmartFilter = "Overdue"
                        activeLayoutMode = "List"
                    }
                    "favorites" -> {
                        activeSmartFilter = "Favorites"
                        activeLayoutMode = "List"
                    }
                    "trash" -> {
                        activeSmartFilter = "Trash"
                        activeLayoutMode = "List"
                    }
                    "templates", "task_details" -> {
                        activeSmartFilter = "All"
                        activeLayoutMode = "Templates"
                    }
                    "kanban" -> {
                        activeSmartFilter = "All"
                        activeLayoutMode = "Kanban"
                    }
                    "eisenhower" -> {
                        activeSmartFilter = "All"
                        activeLayoutMode = "Matrix"
                    }
                }
                currentTab = "inbox"
            }
            "calendar", "weekly_planner" -> currentTab = "calendar"
            "focus" -> currentTab = "focus"
            "habits", "routines", "projects", "shopping", "shopping_lists",
            "reminders", "reminder_center", "statistics", "widgets", "widget_gallery",
            "widget_editor", "themes", "themes_studio", "settings", "backup", "backup_restore", "more" -> {
                activeMoreSubTab = when (route) {
                    "habits", "routines" -> "Habits & Routines"
                    "projects", "shopping", "shopping_lists" -> "Projects & Shopping"
                    "reminders", "reminder_center" -> "Reminder Center"
                    "statistics" -> "Statistics"
                    "widgets", "widget_gallery", "widget_editor" -> "Widget Gallery"
                    "themes", "themes_studio", "settings" -> "Themes & Settings"
                    "backup", "backup_restore" -> "Backup & Restore"
                    else -> "Habits & Routines"
                }
                currentTab = "more"
            }
            else -> currentTab = "home"
        }
    }

    // Primary 5-tab navigation: Home | Tasks | Calendar | Habits (& Studio) | Focus
    val navItems = remember {
        listOf(
            OrbitNavItem("home", "Home", Icons.Default.Dashboard),
            OrbitNavItem("inbox", "Tasks", Icons.Default.Inbox),
            OrbitNavItem("calendar", "Calendar", Icons.Default.CalendarMonth),
            OrbitNavItem("more", "Habits", Icons.Default.Loop),
            OrbitNavItem("focus", "Focus", Icons.Default.Timer)
        )
    }

    OrbitTheme(
        themeMode = preferences.themeMode,
        accentPreset = preferences.accentPreset,
        cornerRadiusDp = preferences.cornerRadiusDp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isWideScreen && !(currentTab == "focus" && deepFocusZenMode)) {
                        NavigationBar {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentTab == item.route,
                                    onClick = { currentTab = item.route },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("nav_tab_${item.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!(currentTab == "focus" && deepFocusZenMode)) {
                        FloatingActionButton(
                            onClick = {
                                editingTask = null
                                showTaskModal = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("global_quick_add_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Quick Add Task")
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen && !(currentTab == "focus" && deepFocusZenMode)) {
                        NavigationRail {
                            navItems.forEach { item ->
                                NavigationRailItem(
                                    selected = currentTab == item.route,
                                    onClick = { currentTab = item.route },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                            NavigationRailItem(
                                selected = currentTab == "more" && activeMoreSubTab == "Widget Gallery",
                                onClick = {
                                    activeMoreSubTab = "Widget Gallery"
                                    currentTab = "more"
                                },
                                icon = { Icon(Icons.Default.AutoAwesomeMosaic, contentDescription = "Widgets") },
                                label = { Text("Widgets") }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            "home" -> MissionControlScreen(
                                tasks = tasks,
                                projects = projects,
                                habits = habits,
                                focusSessions = focusSessions,
                                preferences = preferences,
                                celebrationPulse = celebrationPulse,
                                onQuickAddTask = { raw -> viewModel.quickAddNaturalTask(raw) },
                                onToggleTask = { t -> viewModel.toggleTaskCompletion(t) },
                                onSelectTask = { t ->
                                    editingTask = t
                                    showTaskModal = true
                                },
                                onStartFocusForTask = { t ->
                                    viewModel.setFocusTask(t)
                                    currentTab = "focus"
                                },
                                onToggleHabit = { h -> viewModel.toggleHabitToday(h) },
                                onRescheduleOverdueToToday = { t ->
                                    val today = LocalDate.now().toEpochDay()
                                    viewModel.saveFullTask(
                                        t.copy(
                                            dueDateEpochDay = today,
                                            scheduledDateEpochDay = today
                                        )
                                    )
                                },
                                onApplyPreset = { p -> viewModel.applyDashboardPreset(p) },
                                onToggleWidget = { w -> viewModel.toggleWidgetVisibility(w) },
                                onMoveWidget = { w, up -> viewModel.moveWidgetOrder(w, up) },
                                onUpdateNotes = { notes ->
                                    viewModel.updatePreferences { it.copy(quickNotes = notes) }
                                },
                                onSelectEnergyFilter = { e ->
                                    viewModel.updatePreferences { it.copy(energyFilter = e) }
                                },
                                onUpdatePreferences = { transform -> viewModel.updatePreferences(transform) },
                                onResetSeed = { viewModel.resetSeedData() },
                                onOpenCommandCenter = { showCommandCenter = true },
                                onNavigateToScreen = handleNavigateDestination
                            )
                            "inbox" -> WorkspaceAndViewsScreen(
                                tasks = tasks,
                                projects = projects,
                                initialFilter = activeSmartFilter,
                                initialLayoutMode = activeLayoutMode,
                                canUndoDelete = canUndoDelete,
                                onQuickAddTask = { raw, projId, status ->
                                    viewModel.quickAddNaturalTask(raw, projId, status)
                                },
                                onToggleTask = { t -> viewModel.toggleTaskCompletion(t) },
                                onTogglePinTask = { t -> viewModel.toggleTaskPin(t) },
                                onArchiveCompleted = { viewModel.archiveCompletedTasks() },
                                onSelectTask = { t ->
                                    editingTask = t
                                    showTaskModal = true
                                },
                                onDuplicateTask = { t -> viewModel.duplicateTask(t) },
                                onDeleteTask = { t -> viewModel.deleteTaskWithUndo(t) },
                                onRestoreTask = { t -> viewModel.restoreFromTrash(t) },
                                onUndoDelete = { viewModel.undoLastDelete() },
                                onBulkComplete = { ids -> viewModel.bulkCompleteTasks(ids) },
                                onBulkTomorrow = { ids -> viewModel.bulkRescheduleTomorrow(ids) },
                                onApplyTemplate = { tmpl -> viewModel.applyTaskTemplate(tmpl) },
                                onSnoozeTask = { t, days, mins -> viewModel.snoozeOrPostponeTask(t, days, mins) }
                            )
                            "calendar" -> SmartCalendarScreen(
                                tasks = tasks,
                                use24Hour = preferences.use24HourFormat,
                                workdayStartHour = preferences.workdayStartHour,
                                workdayEndHour = preferences.workdayEndHour,
                                onAutoFillFreeSlots = { viewModel.autoAssignFreeTimeSlotsDeterministically() },
                                onExportIcs = { viewModel.exportCalendarIcs() },
                                onSelectTask = { t ->
                                    editingTask = t
                                    showTaskModal = true
                                },
                                onAssignTaskToHour = { task, dayEpoch, hour ->
                                    viewModel.saveFullTask(
                                        task.copy(
                                            scheduledDateEpochDay = dayEpoch,
                                            scheduledStartHour = hour,
                                            scheduledStartMinute = 0
                                        )
                                    )
                                }
                            )
                            "focus" -> FocusCockpitScreen(
                                openTasks = tasks.filter { !it.isCompleted && !it.isTrashed },
                                focusSessions = focusSessions,
                                timerRunning = timerRunning,
                                timerMode = timerMode,
                                timerSecondsRemaining = timerSecondsRemaining,
                                timerTotalSeconds = timerTotalSeconds,
                                activeFocusTask = activeFocusTask,
                                ambientSound = ambientSound,
                                ambientVolume = ambientVolume,
                                deepFocusZenMode = deepFocusZenMode,
                                dailyFocusGoalMinutes = preferences.dailyFocusGoalMinutes,
                                onSelectMode = { m, mins -> viewModel.selectFocusMode(m, mins) },
                                onSelectTask = { t -> viewModel.setFocusTask(t) },
                                onSelectSound = { s -> viewModel.setAmbientSoundscape(s) },
                                onChangeAmbientVolume = { vol -> viewModel.setAmbientVolume(vol) },
                                onToggleZenMode = { viewModel.toggleDeepFocusZenMode() },
                                onStartOrPause = { viewModel.startOrPauseTimer() },
                                onResetTimer = { viewModel.resetTimer() },
                                onCompleteSession = { viewModel.completeFocusSession() }
                            )
                            "more" -> MoreAndEcosystemScreen(
                                initialSubTab = activeMoreSubTab,
                                tasks = tasks,
                                projects = projects,
                                habits = habits,
                                focusSessions = focusSessions,
                                automations = automations,
                                preferences = preferences,
                                onToggleHabit = { h -> viewModel.toggleHabitToday(h) },
                                onUseHabitFreeze = { h -> viewModel.useHabitStreakFreeze(h) },
                                onCreateHabit = { title, routine, cat, days, rem ->
                                    viewModel.createNewHabit(title, routine, cat, days, rem)
                                },
                                onCreateProject = { name, desc, color, cat, shared, members ->
                                    viewModel.createNewProject(name, desc, color, cat, shared, members)
                                },
                                onQuickAddSpecialReminder = { title, category ->
                                    viewModel.quickAddNaturalTask("$title #$category")
                                },
                                onTriggerNotificationNow = { t ->
                                    viewModel.triggerTaskReminderNow(context, t)
                                },
                                onToggleWidget = { w -> viewModel.toggleWidgetVisibility(w) },
                                onToggleFavoriteWidget = { w -> viewModel.toggleFavoriteWidget(w) },
                                onSetWidgetSize = { w, sz -> viewModel.setWidgetSize(w, sz) },
                                onSetHomeWidgetVariant = { variant ->
                                    viewModel.setHomeWidgetVariant(variant)
                                },
                                onMoveWidget = { w, up -> viewModel.moveWidgetOrder(w, up) },
                                onApplyPreset = { p -> viewModel.applyDashboardPreset(p) },
                                onUpdatePreferences = { transform -> viewModel.updatePreferences(transform) },
                                onExportJson = { viewModel.exportBackupJson() },
                                onExportCsv = { viewModel.exportTasksCsv() },
                                onExportIcs = { viewModel.exportCalendarIcs() },
                                onImportJson = { raw -> viewModel.importBackupJson(raw) },
                                onPreviewImportJson = { raw -> viewModel.inspectBackupBeforeRestore(raw) },
                                onImportIcs = { raw -> viewModel.importCalendarIcs(raw) },
                                onSaveFileSaf = { format ->
                                    viewModel.prepareSafExport(format) { suggestedName, _, _ ->
                                        when (format.lowercase()) {
                                            "csv" -> safCreateCsvLauncher.launch(suggestedName)
                                            "ics" -> safCreateIcsLauncher.launch(suggestedName)
                                            else -> safCreateJsonLauncher.launch(suggestedName)
                                        }
                                    }
                                },
                                onOpenFileSaf = {
                                    safOpenDocumentLauncher.launch(
                                        arrayOf("application/json", "text/calendar", "text/plain", "*/*")
                                    )
                                },
                                onRestoreTaskFromTrash = { t -> viewModel.restoreFromTrash(t) },
                                onEmptyTrash = { viewModel.emptyTrashBin() },
                                onResetSeed = { viewModel.resetSeedData() },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.postMessage("Notifications already enabled on this Android version")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showTaskModal) {
                TaskDetailAndCreationModal(
                    initialTask = editingTask,
                    projects = projects,
                    allTasks = tasks,
                    onDismiss = { showTaskModal = false },
                    onSaveTask = { updated -> viewModel.saveFullTask(updated) },
                    onToggleSubtask = { t, idx -> viewModel.toggleSubtask(t, idx) },
                    onSnoozeReminder = { t, days, mins ->
                        viewModel.snoozeOrPostponeTask(t, days, mins)
                    },
                    onTriggerNotificationNow = { t ->
                        viewModel.triggerTaskReminderNow(context, t)
                    },
                    onStartFocusSession = { t ->
                        viewModel.setFocusTask(t)
                        currentTab = "focus"
                    }
                )
            }

            if (showCommandCenter) {
                UniversalCommandCenterDialog(
                    tasks = tasks,
                    projects = projects,
                    onDismiss = { showCommandCenter = false },
                    onSelectTask = { t ->
                        editingTask = t
                        showTaskModal = true
                    },
                    onNavigateScreen = handleNavigateDestination,
                    onToggleOledMode = {
                        viewModel.updatePreferences {
                            it.copy(themeMode = if (it.themeMode == "oled") "paper" else "oled")
                        }
                    }
                )
            }

            exportedContentPreview?.let { (title, content) ->
                Dialog(onDismissRequest = { viewModel.dismissExportPreview() }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "100% open format — copy to clipboard or save to any local file:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = content,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 320.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { viewModel.dismissExportPreview() }) {
                                    Text("Close")
                                }
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(content))
                                        viewModel.postMessage("Copied $title to clipboard")
                                        viewModel.dismissExportPreview()
                                    }
                                ) {
                                    Text("Copy to Clipboard")
                                }
                            }
                        }
                    }
                }
            }

            backupPreviewState?.let { preview ->
                Dialog(onDismissRequest = { viewModel.dismissBackupPreview() }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .testTag("backup_preview_dialog")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pre-Restore Backup Inspection (Schema v${preview.schemaVersion})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Tasks in Backup: ${preview.taskCount} (${preview.duplicateTaskCount} matching existing IDs)\n" +
                                    "• Projects: ${preview.projectCount} • Habits: ${preview.habitCount}\n" +
                                    "• Focus Sessions: ${preview.focusSessionCount} • Automations: ${preview.automationCount}\n" +
                                    "• Preferences Included: ${if (preview.hasPreferences) "Yes" else "No"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Choose Duplicate & Restore Strategy:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    viewModel.confirmBackupRestore(RestoreDuplicateStrategy.MERGE_SKIP_DUPLICATES)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_merge_skip_duplicates_button")
                            ) {
                                Text("Merge & Skip Duplicates (Keep Existing)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.confirmBackupRestore(RestoreDuplicateStrategy.REPLACE_WORKSPACE)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_replace_workspace_button")
                            ) {
                                Text("Replace Workspace Atomically")
                            }
                            TextButton(
                                onClick = { viewModel.dismissBackupPreview() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            if (showWalkthrough) {
                Dialog(onDismissRequest = { showWalkthrough = false }) {
                    FirstRunWalkthroughDialog(
                        currentPresetKey = preferences.dashboardPreset,
                        onApplyPresetAndTask = { preset, rawTask ->
                            viewModel.applyDashboardPreset(preset)
                            if (rawTask.isNotBlank()) {
                                viewModel.quickAddNaturalTask(rawTask)
                            }
                            viewModel.updatePreferences { it.copy(onboardingCompleted = true) }
                            showWalkthrough = false
                        },
                        onDismiss = { showWalkthrough = false }
                    )
                }
            }
        }
    }
}
