package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbitTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DailyTaskScreen(
    showAll: Boolean,
    tasks: List<OrbitTask>,
    onSelect: (OrbitTask) -> Unit,
    onToggle: (OrbitTask) -> Unit,
    onFocus: () -> Unit
) {
    val date = LocalDate.now()
    val today = date.toEpochDay()
    val active = tasks.filter { !it.isTrashed && it.status != "archived" }
    fun nextDate(task: OrbitTask): Long =
        listOf(task.scheduledDateEpochDay, task.dueDateEpochDay, task.deadlineEpochDay)
            .filter { it > 0 }.minOrNull() ?: 0L
    val current = if (showAll) active else active.filter { task ->
        val whenDue = nextDate(task)
        whenDue == today || !task.isCompleted && whenDue in 0 until today
    }
    val upcoming = if (showAll) emptyList() else active.filter { !it.isCompleted && nextDate(it) > today }
        .sortedBy(::nextDate).take(5)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                if (!showAll) {
                    Text(date.format(formatter), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (showAll) "Tasks" else "Today", style = MaterialTheme.typography.displaySmall)
            }
        }
        if (current.isEmpty()) {
            item { Text(if (showAll) "No tasks yet." else "Nothing scheduled today.",
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(current.sortedBy { it.isCompleted }, key = { it.id }) { task ->
            DailyTaskRow(task, nextDate(task).takeIf { it > 0 }?.let { LocalDate.ofEpochDay(it).format(formatter) }, onSelect, onToggle)
        }
        if (upcoming.isNotEmpty()) {
            item { Text("Upcoming", style = MaterialTheme.typography.titleLarge) }
            items(upcoming, key = { it.id }) { task ->
                DailyTaskRow(task, LocalDate.ofEpochDay(nextDate(task)).format(formatter), onSelect, onToggle)
            }
        }
        if (!showAll) item { TextButton(onClick = onFocus) { Text("Focus timer") } }
    }
}

@Composable
private fun DailyTaskRow(
    task: OrbitTask,
    date: String?,
    onSelect: (OrbitTask) -> Unit,
    onToggle: (OrbitTask) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle(task) },
            modifier = Modifier.semantics { contentDescription = "Complete ${task.title}" }
        )
        Column(
            Modifier.weight(1f).heightIn(min = 48.dp).clickable { onSelect(task) },
            verticalArrangement = Arrangement.Center
        ) {
            Text(task.title, style = MaterialTheme.typography.titleMedium)
            if (date != null) Text(date, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyFocusScreen(
    running: Boolean,
    seconds: Int,
    total: Int,
    taskTitle: String?,
    onDuration: (Int) -> Unit,
    onToggle: () -> Unit,
    onFinish: () -> Unit
) {
    val started = running || seconds != total
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Focus", style = MaterialTheme.typography.displaySmall) }
        if (!taskTitle.isNullOrBlank()) item { Text(taskTitle, style = MaterialTheme.typography.titleMedium) }
        item {
            Text(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60),
                style = MaterialTheme.typography.displayLarge)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45).forEach { minutes ->
                    FilterChip(
                        selected = total == minutes * 60,
                        enabled = !started,
                        onClick = { onDuration(minutes) },
                        label = { Text("$minutes min") }
                    )
                }
            }
        }
        item {
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(if (running) "Pause" else if (started) "Resume" else "Start")
            }
            if (started) TextButton(onClick = onFinish) { Text("Finish session") }
        }
    }
}
