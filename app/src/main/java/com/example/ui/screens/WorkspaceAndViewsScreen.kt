package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbitProject
import com.example.data.model.OrbitTask
import com.example.data.model.SmartTaskParser
import com.example.ui.theme.OrbitAmber
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitIndigo
import com.example.ui.theme.OrbitRose
import com.example.ui.viewmodel.BuiltInTaskTemplates
import com.example.ui.viewmodel.TaskTemplateBlueprint
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceAndViewsScreen(
    tasks: List<OrbitTask>,
    projects: List<OrbitProject>,
    initialFilter: String = "All",
    initialLayoutMode: String = "List",
    canUndoDelete: Boolean,
    onQuickAddTask: (String, String?, String?) -> Unit,
    onToggleTask: (OrbitTask) -> Unit,
    onTogglePinTask: (OrbitTask) -> Unit = {},
    onArchiveCompleted: () -> Unit = {},
    onSelectTask: (OrbitTask) -> Unit,
    onDuplicateTask: (OrbitTask) -> Unit,
    onDeleteTask: (OrbitTask) -> Unit,
    onRestoreTask: (OrbitTask) -> Unit,
    onUndoDelete: () -> Unit,
    onBulkComplete: (Set<String>) -> Unit,
    onBulkTomorrow: (Set<String>) -> Unit,
    onApplyTemplate: (TaskTemplateBlueprint) -> Unit,
    onSnoozeTask: (OrbitTask, Long, Int) -> Unit
) {
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    var selectedSmartFilter by androidx.compose.runtime.saveable.rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var selectedLayoutMode by androidx.compose.runtime.saveable.rememberSaveable(initialLayoutMode) { mutableStateOf(initialLayoutMode) } // List, Kanban, Matrix, Templates
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var captureText by remember { mutableStateOf("") }
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedTaskIds = remember { mutableStateListOf<String>() }


    val smartFilters = listOf(
        "All", "Inbox", "Today", "Tomorrow", "This Week", "Upcoming",
        "Anytime", "Someday", "Overdue", "Favorites", "Pinned", "Completed", "Trash"
    )

    val filteredTasks = remember(tasks, selectedSmartFilter, selectedProjectId, searchQuery, todayEpoch) {
        tasks.filter { task ->
            val matchesProject = selectedProjectId == null || task.projectId == selectedProjectId
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                when {
                    q.startsWith("#") -> task.tags.any { it.contains(q.removePrefix("#"), true) }
                    q.startsWith("@") -> task.projectName.contains(q.removePrefix("@"), true)
                    q in listOf("p1", "p2", "p3", "p4") -> task.priority.equals(q, true)
                    q == "is:overdue" -> !task.isCompleted && task.dueDateEpochDay in 1 until todayEpoch
                    q == "is:done" -> task.isCompleted
                    q == "is:pinned" -> task.isPinned
                    else -> task.title.contains(q, true) ||
                        task.description.contains(q, true) ||
                        task.projectName.contains(q, true) ||
                        task.tags.any { it.contains(q, true) }
                }
            }
            val matchesFilter = when (selectedSmartFilter) {
                "Trash" -> task.isTrashed
                "Inbox" -> !task.isTrashed && task.status == "inbox" && !task.isCompleted
                "Today" -> !task.isTrashed && !task.isCompleted && (task.dueDateEpochDay == todayEpoch || task.scheduledDateEpochDay == todayEpoch)
                "Tomorrow" -> !task.isTrashed && !task.isCompleted && (task.dueDateEpochDay == todayEpoch + 1 || task.scheduledDateEpochDay == todayEpoch + 1)
                "This Week" -> !task.isTrashed && !task.isCompleted && task.dueDateEpochDay in todayEpoch..(todayEpoch + 7)
                "Upcoming" -> !task.isTrashed && !task.isCompleted && task.dueDateEpochDay > todayEpoch
                "Anytime" -> !task.isTrashed && !task.isCompleted && task.dueDateEpochDay == 0L
                "Someday" -> !task.isTrashed && task.status == "archived"
                "Overdue" -> !task.isTrashed && !task.isCompleted && task.dueDateEpochDay in 1 until todayEpoch
                "Favorites" -> !task.isTrashed && task.isFavorite && !task.isCompleted
                "Pinned" -> !task.isTrashed && task.isPinned && !task.isCompleted
                "Completed" -> !task.isTrashed && task.isCompleted
                else -> !task.isTrashed && task.status != "archived"
            }
            matchesProject && matchesSearch && matchesFilter
        }.sortedWith(
            compareByDescending<OrbitTask> { it.isPinned }
                .thenBy { it.isCompleted }
                .thenBy { it.priority }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("workspace_views_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // Header + View Switcher
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$selectedSmartFilter Tasks",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${filteredTasks.size} items · Your task notebook",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selectedSmartFilter == "Completed" && filteredTasks.isNotEmpty()) {
                    FilterChip(
                        selected = false,
                        onClick = onArchiveCompleted,
                        label = { Text("Archive Done") },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                FilterChip(
                    selected = multiSelectMode,
                    onClick = {
                        multiSelectMode = !multiSelectMode
                        if (!multiSelectMode) selectedTaskIds.clear()
                    },
                    label = { Text(if (multiSelectMode) "Bulk (${selectedTaskIds.size})" else "Select") },
                    leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                listOf(
                    "List" to Icons.AutoMirrored.Filled.ViewList,
                    "Kanban" to Icons.Default.ViewKanban,
                    "Matrix" to Icons.Default.GridView,
                    "Templates" to Icons.Default.Checklist
                ).forEach { (mode, icon) ->
                    FilterChip(
                        selected = selectedLayoutMode == mode,
                        onClick = { selectedLayoutMode = mode },
                        label = { Text(mode) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Bulk Action Bar
        AnimatedVisibility(visible = multiSelectMode && selectedTaskIds.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedTaskIds.size} selected", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            onBulkComplete(selectedTaskIds.toSet())
                            selectedTaskIds.clear()
                            multiSelectMode = false
                        }) {
                            Text("Complete All")
                        }
                        TextButton(onClick = {
                            onBulkTomorrow(selectedTaskIds.toSet())
                            selectedTaskIds.clear()
                            multiSelectMode = false
                        }) {
                            Text("Move +1d")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar with Operators (#tag, @project, p1..p4, is:overdue, is:pinned)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter by title, #tag, @project, p1, is:overdue, or is:pinned...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tasks_search_filter_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Smart Filter Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            smartFilters.forEach { filter ->
                FilterChip(
                    selected = selectedSmartFilter == filter,
                    onClick = { selectedSmartFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        // Project Filter Strip
        if (projects.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedProjectId == null,
                    onClick = { selectedProjectId = null },
                    label = { Text("All Projects") }
                )
                projects.forEach { proj ->
                    FilterChip(
                        selected = selectedProjectId == proj.id,
                        onClick = { selectedProjectId = proj.id },
                        label = { Text(proj.name) }
                    )
                }
            }
        }

        // Undo Banner
        AnimatedVisibility(visible = canUndoDelete) {
            Surface(
                color = OrbitAmber.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Task moved to Trash", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onUndoDelete) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo")
                    }
                }
            }
        }

        // Deterministic Quick-Add 2.0 Bar with Live NLP Preview & Token Injector Chips
        val liveParsed = remember(captureText) {
            if (captureText.isNotBlank()) SmartTaskParser.parse(captureText) else null
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = captureText,
                        onValueChange = { captureText = it },
                        placeholder = {
                            Text("e.g. \"Workout every Monday Wednesday Friday at 7am p2 #fitness\"")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inbox_quick_add_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (captureText.isNotBlank()) {
                                val status = if (selectedSmartFilter == "Inbox") "inbox" else "todo"
                                onQuickAddTask(captureText, selectedProjectId, status)
                                captureText = ""
                            }
                        },
                        enabled = captureText.isNotBlank(),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                // Quick-inject token chips for 1-tap editing before saving
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("tomorrow", "at 5pm", "p1", "every weekday", "for 30 minutes", "#focus").forEach { token ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                captureText = if (captureText.isBlank()) token else "${captureText.trim()} $token"
                            }
                        ) {
                            Text(
                                text = "+ $token",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                if (liveParsed != null && liveParsed.summaryChips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        liveParsed.summaryChips.forEach { chip ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = chip,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main View Content: List vs Kanban vs Matrix vs Templates
        when (selectedLayoutMode) {
            "Kanban" -> KanbanBoardView(
                tasks = filteredTasks,
                onSelectTask = onSelectTask
            )
            "Matrix" -> MatrixFullView(
                tasks = filteredTasks.filter { !it.isCompleted },
                onSelectTask = onSelectTask
            )
            "Templates" -> BuiltInTemplatesView(
                onApplyTemplate = onApplyTemplate
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredTasks.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🏮 Quiet desk — zero tasks in $selectedSmartFilter",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Enjoy the calm evening, or type a natural-language note above to capture your next study block.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredTasks, key = { it.id.ifBlank { it.title } }) { task ->
                            val isSelected = selectedTaskIds.contains(task.id)
                            DetailedTaskCard(
                                task = task,
                                multiSelectMode = multiSelectMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) selectedTaskIds.remove(task.id)
                                    else selectedTaskIds.add(task.id)
                                },
                                onToggle = { onToggleTask(task) },
                                onTogglePin = { onTogglePinTask(task) },
                                onClick = { onSelectTask(task) },
                                onDuplicate = { onDuplicateTask(task) },
                                onDelete = { onDeleteTask(task) },
                                onRestore = { onRestoreTask(task) },
                                onSnoozeTomorrow = { onSnoozeTask(task, 1L, -1) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuiltInTemplatesView(
    onApplyTemplate: (TaskTemplateBlueprint) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(BuiltInTaskTemplates, key = { it.id }) { tpl ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${tpl.category} • ${tpl.estimatedMinutes}m • ${tpl.steps.size} checklist steps", style = MaterialTheme.typography.labelSmall, color = OrbitCyan)
                        }
                        Button(onClick = { onApplyTemplate(tpl) }) {
                            Text("Use Template")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    tpl.steps.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixFullView(
    tasks: List<OrbitTask>,
    onSelectTask: (OrbitTask) -> Unit
) {
    val q1 = tasks.filter { it.isImportant && it.isUrgent }
    val q2 = tasks.filter { it.isImportant && !it.isUrgent }
    val q3 = tasks.filter { !it.isImportant && it.isUrgent }
    val q4 = tasks.filter { !it.isImportant && !it.isUrgent }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            Triple("Q1 • Urgent & Important (Do First)", q1, OrbitAmber),
            Triple("Q2 • Important, Not Urgent (Schedule)", q2, OrbitIndigo),
            Triple("Q3 • Urgent, Less Important (Delegate/Batch)", q3, OrbitCyan),
            Triple("Q4 • Neither Urgent Nor Important (Someday)", q4, OrbitEmerald)
        ).forEach { (title, list, color) ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("$title (${list.size})", style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (list.isEmpty()) {
                            Text("No tasks in this quadrant", style = MaterialTheme.typography.bodySmall)
                        } else {
                            list.forEach { t ->
                                Text(
                                    text = "• [${t.priority.uppercase()}] ${t.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectTask(t) }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanBoardView(
    tasks: List<OrbitTask>,
    onSelectTask: (OrbitTask) -> Unit
) {
    val columns = listOf(
        "inbox" to "Inbox Capture",
        "todo" to "To Do",
        "in_progress" to "In Progress",
        "done" to "Completed"
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        columns.forEach { (statusKey, label) ->
            val colTasks = tasks.filter { it.status == statusKey }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .width(275.dp)
                    .fillMaxSize()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${colTasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colTasks, key = { it.id.ifBlank { it.title } }) { task ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectTask(task) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        PriorityBadge(priority = task.priority)
                                        Text(
                                            text = "${task.estimatedMinutes}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.projectName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedTaskCard(
    task: OrbitTask,
    multiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onToggle: () -> Unit,
    onTogglePin: () -> Unit,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onSnoozeTomorrow: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (multiSelectMode) onToggleSelect() else onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (multiSelectMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                }
                Column(modifier = Modifier.weight(1f)) {
                    CompactTaskRow(task = task, onToggle = onToggle, onClick = onClick)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.dependsOnTaskId.isNotBlank()) {
                        Surface(
                            color = OrbitRose.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔒 Blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrbitRose,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.isPinned) {
                        Surface(
                            color = OrbitAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📌 Pinned",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrbitAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    task.tags.take(3).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrbitCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.locationTrigger.isNotBlank()) {
                        Surface(
                            color = OrbitIndigo.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📍 ${task.locationTrigger}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrbitIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isTrashed) {
                        TextButton(onClick = onRestore) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore")
                        }
                    } else {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin Task",
                                tint = if (task.isPinned) OrbitAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        TextButton(onClick = onSnoozeTomorrow) {
                            Icon(Icons.Default.Schedule, contentDescription = "Postpone", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1d", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate Task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailAndCreationModal(
    initialTask: OrbitTask?,
    projects: List<OrbitProject>,
    allTasks: List<OrbitTask>,
    onDismiss: () -> Unit,
    onSaveTask: (OrbitTask) -> Unit,
    onToggleSubtask: (OrbitTask, Int) -> Unit,
    onSnoozeReminder: (OrbitTask, Long, Int) -> Unit,
    onTriggerNotificationNow: (OrbitTask) -> Unit,
    onStartFocusSession: (OrbitTask) -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val base = initialTask ?: OrbitTask(
        dueDateEpochDay = today,
        scheduledDateEpochDay = today,
        startDateEpochDay = today,
        projectId = projects.firstOrNull()?.id ?: "proj_personal",
        projectName = projects.firstOrNull()?.name ?: "Personal"
    )

    var title by remember(base) { mutableStateOf(base.title) }
    var description by remember(base) { mutableStateOf(base.description) }
    var priority by remember(base) { mutableStateOf(base.priority) }
    var status by remember(base) { mutableStateOf(base.status) }
    var projectId by remember(base) { mutableStateOf(base.projectId) }
    var projectName by remember(base) { mutableStateOf(base.projectName) }
    var energyLevel by remember(base) { mutableStateOf(base.energyLevel) }
    var estimatedMinutes by remember(base) { mutableStateOf(base.estimatedMinutes.toString()) }
    var recurrenceRule by remember(base) { mutableStateOf(base.recurrenceRule) }
    var reminderEscalation by remember(base) { mutableStateOf(base.reminderEscalation) }
    var reminderCategory by remember(base) { mutableStateOf(base.assigneeName) }
    var locationTrigger by remember(base) { mutableStateOf(base.locationTrigger) }
    var isFavorite by remember(base) { mutableStateOf(base.isFavorite) }
    var isPinned by remember(base) { mutableStateOf(base.isPinned) }
    var isImportant by remember(base) { mutableStateOf(base.isImportant) }
    var isUrgent by remember(base) { mutableStateOf(base.isUrgent) }
    var dueDateEpochDay by remember(base) { mutableLongStateOf(base.dueDateEpochDay) }
    var deadlineEpochDay by remember(base) { mutableLongStateOf(base.deadlineEpochDay) }
    var dependsOnTaskId by remember(base) { mutableStateOf(base.dependsOnTaskId) }
    var newSubtaskText by remember { mutableStateOf("") }
    var newCommentText by remember { mutableStateOf("") }
    var newCustomFieldText by remember { mutableStateOf("") }

    val reminderOffsets = remember(base.multipleReminderOffsets) {
        mutableStateListOf<Int>().apply { addAll(base.multipleReminderOffsets.ifEmpty { listOf(0, 15) }) }
    }
    val subtasksList = remember(base.subtasks) {
        mutableStateListOf<String>().apply { addAll(base.subtasks) }
    }
    val commentsList = remember(base.comments) {
        mutableStateListOf<String>().apply { addAll(base.comments) }
    }
    val customFieldsList = remember(base.customFields) {
        mutableStateListOf<String>().apply {
            addAll(base.customFields.entries.map { "${it.key}: ${it.value}" })
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTask == null) "New Yorulune Task" else "Task Details & Inspector",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) OrbitAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) OrbitAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (initialTask != null) {
                        Button(onClick = {
                            onStartFocusSession(initialTask)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Focus")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title or Deterministic Command") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_modal_title_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / Markdown Description / Attachments") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Schedule Date vs Hard Deadline Selector
            Text("Schedule Date & Hard Deadline", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    today to "Today",
                    today + 1 to "Tomorrow",
                    today + 7 to "Next Week",
                    0L to "Anytime"
                ).forEach { (ep, label) ->
                    FilterChip(
                        selected = dueDateEpochDay == ep,
                        onClick = { dueDateEpochDay = ep },
                        label = { Text("Do: $label") }
                    )
                }
                FilterChip(
                    selected = deadlineEpochDay > 0L,
                    onClick = {
                        deadlineEpochDay = if (deadlineEpochDay > 0L) 0L else (dueDateEpochDay.takeIf { it > 0 } ?: today) + 2
                    },
                    label = {
                        Text(
                            if (deadlineEpochDay > 0L) "Deadline: ${SmartTaskParser.formatEpochDay(deadlineEpochDay, today)}"
                            else "+ Set Hard Deadline"
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Checklist Presets (Deterministic, Zero AI)
            OutlinedButton(
                onClick = {
                    val presetSteps = listOf(
                        "0|Define scope & clear acceptance criteria",
                        "0|Complete first 25m focused execution block",
                        "0|Review quality & mark complete"
                    )
                    subtasksList.addAll(presetSteps)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Checklist, contentDescription = null, tint = OrbitIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Insert Standard 3-Step Execution Checklist")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Priority & Status
            Text("Priority & Eisenhower Flags", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("p1" to "P1 Critical", "p2" to "P2 High", "p3" to "P3 Normal", "p4" to "P4 Low").forEach { (key, label) ->
                    FilterChip(
                        selected = priority == key,
                        onClick = { priority = key },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = isImportant,
                    onClick = { isImportant = !isImportant },
                    label = { Text("Important") }
                )
                FilterChip(
                    selected = isUrgent,
                    onClick = { isUrgent = !isUrgent },
                    label = { Text("Urgent") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminder Category (Birthday, Anniversary, Bill, Medication)
            Text("Special Reminder Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("" to "Standard", "Birthday" to "🎂 Birthday", "Anniversary" to "💍 Anniversary", "Bill" to "💳 Bill", "Medication" to "💊 Medication").forEach { (k, label) ->
                    FilterChip(
                        selected = reminderCategory == k,
                        onClick = { reminderCategory = k },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Project & Energy
            Text("Project Space & Energy Level", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                projects.forEach { proj ->
                    FilterChip(
                        selected = projectId == proj.id,
                        onClick = {
                            projectId = proj.id
                            projectName = proj.name
                        },
                        label = { Text(proj.name) }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low" to "🔋 Low Energy", "medium" to "⚡ Medium", "high" to "🚀 High Deep Work").forEach { (k, label) ->
                    FilterChip(
                        selected = energyLevel == k,
                        onClick = { energyLevel = k },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Duration & Location
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = estimatedMinutes,
                    onValueChange = { estimatedMinutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Est. Minutes") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = locationTrigger,
                    onValueChange = { locationTrigger = it },
                    label = { Text("Location Trigger (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Recurrence Rule", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "" to "One-Time",
                    "DAILY" to "Daily",
                    "WEEKDAYS" to "Weekdays",
                    "DAYS_MON_WED_FRI" to "Mon • Wed • Fri",
                    "DAYS_TUE_THU_SAT" to "Tue • Thu • Sat",
                    "WEEKLY" to "Weekly",
                    "MONTHLY" to "Monthly",
                    "MONTHLY_1ST_MON" to "1st Mon of Month",
                    "YEARLY" to "Yearly",
                    "AFTER_COMPLETION" to "Repeat After Completion"
                ).forEach { (rule, label) ->
                    FilterChip(
                        selected = recurrenceRule == rule,
                        onClick = { recurrenceRule = rule },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Prerequisite Dependency (Auto-Unlocks on Completion)", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = dependsOnTaskId.isBlank(),
                    onClick = { dependsOnTaskId = "" },
                    label = { Text("No Blocker") }
                )
                allTasks.filter { !it.isCompleted && !it.isTrashed && it.id != base.id }.take(5).forEach { candidate ->
                    FilterChip(
                        selected = dependsOnTaskId == candidate.id,
                        onClick = {
                            dependsOnTaskId = if (dependsOnTaskId == candidate.id) "" else candidate.id
                        },
                        label = { Text("🔒 ${candidate.title.take(24)}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reminder Engine Controls + Multiple Offsets
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OrbitCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Local Device Reminder Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        if (initialTask != null) {
                            TextButton(onClick = { onTriggerNotificationNow(initialTask) }) {
                                Text("Test Alert Now")
                            }
                        }
                    }
                    Text("Notification Behavior:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("standard" to "Standard", "persistent" to "Persistent Pin", "critical" to "Critical Alert").forEach { (k, label) ->
                            FilterChip(
                                selected = reminderEscalation == k,
                                onClick = { reminderEscalation = k },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text("Multiple Reminder Offsets (Before Due Time):", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            0 to "At Due Time",
                            5 to "5m Before",
                            15 to "15m Before",
                            30 to "30m Before",
                            60 to "1h Before",
                            120 to "2h Before",
                            1440 to "1 Day Before",
                            2880 to "2 Days Before"
                        ).forEach { (offset, label) ->
                            val selected = reminderOffsets.contains(offset)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected && reminderOffsets.size > 1) reminderOffsets.remove(offset)
                                    else if (!selected) reminderOffsets.add(offset)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (initialTask != null) {
                        Text("Quick Snooze:", style = MaterialTheme.typography.labelSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15, 30, 60).forEach { mins ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onSnoozeReminder(initialTask, 0L, mins) },
                                    label = { Text("${mins}m") }
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = { onSnoozeReminder(initialTask, 1L, 15) },
                                label = { Text("Tomorrow 9 AM") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Unlimited Subtasks & Nested Checklist
            Text("Subtasks & Nested Checklist (${subtasksList.size})", style = MaterialTheme.typography.labelLarge)
            subtasksList.forEachIndexed { idx, raw ->
                val parts = raw.split("|", limit = 2)
                val isDone = parts.size == 2 && parts[0] == "1"
                val subTitle = if (parts.size == 2) parts[1] else raw
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { checked ->
                            subtasksList[idx] = "${if (checked) "1" else "0"}|$subTitle"
                            if (initialTask != null) {
                                onToggleSubtask(initialTask, idx)
                            }
                        }
                    )
                    Text(subTitle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add unlimited subtask step...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            subtasksList.add("0|${newSubtaskText.trim()}")
                            newSubtaskText = ""
                        }
                    }
                ) {
                    Text("+ Step")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Fields (Notion-style key:value metadata)
            Text("Custom Metadata Fields (${customFieldsList.size})", style = MaterialTheme.typography.labelLarge)
            customFieldsList.forEach { cf ->
                Text("🏷 $cf", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCustomFieldText,
                    onValueChange = { newCustomFieldText = it },
                    placeholder = { Text("e.g. Client: Acme Corp or URL: docs.local") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (newCustomFieldText.isNotBlank()) {
                            customFieldsList.add(newCustomFieldText.trim())
                            newCustomFieldText = ""
                        }
                    }
                ) {
                    Text("+ Field")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task History & Notes
            Text("Task Activity History & Notes (${commentsList.size + base.historyLog.size})", style = MaterialTheme.typography.labelLarge)
            base.historyLog.forEach { entry ->
                Text(
                    text = "⏱ $entry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            commentsList.forEach { comment ->
                Text(
                    text = "📝 $comment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Add note or timestamped log...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            commentsList.add(newCommentText.trim())
                            newCommentText = ""
                        }
                    }
                ) {
                    Text("Add Log")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveTask(
                                base.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    priority = priority,
                                    status = if (dependsOnTaskId.isNotBlank() && status != "done") "blocked" else status,
                                    projectId = projectId,
                                    projectName = projectName,
                                    energyLevel = energyLevel,
                                    estimatedMinutes = (estimatedMinutes.toIntOrNull() ?: 25).coerceIn(1, 1440),
                                    recurrenceRule = recurrenceRule,
                                    reminderEscalation = reminderEscalation,
                                    multipleReminderOffsets = reminderOffsets.toList(),
                                    assigneeName = reminderCategory,
                                    locationTrigger = locationTrigger.trim(),
                                    isFavorite = isFavorite,
                                    isPinned = isPinned,
                                    isImportant = isImportant,
                                    isUrgent = isUrgent,
                                    dueDateEpochDay = dueDateEpochDay,
                                    scheduledDateEpochDay = dueDateEpochDay,
                                    deadlineEpochDay = deadlineEpochDay,
                                    dependsOnTaskId = dependsOnTaskId,
                                    subtasks = subtasksList.toList(),
                                    comments = commentsList.toList(),
                                    customFields = customFieldsList.associate { raw ->
                                        val parts = raw.split(":", limit = 2)
                                        if (parts.size == 2) parts[0].trim() to parts[1].trim()
                                        else raw.trim() to "true"
                                    }
                                )
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("save_task_modal_button")
                ) {
                    Text("Save Task")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
