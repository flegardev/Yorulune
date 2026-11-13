package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.kissatenTone
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FocusSessionRecord
import com.example.data.model.OrbitTask
import com.example.data.model.SmartTaskParser
import com.example.ui.theme.OrbitAmber
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitIndigo
import com.example.ui.theme.OrbitRose
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SmartCalendarScreen(
    tasks: List<OrbitTask>,
    use24Hour: Boolean = false,
    workdayStartHour: Int = 8,
    workdayEndHour: Int = 18,
    onAutoFillFreeSlots: (Long) -> Unit,
    onExportIcs: () -> Unit,
    onSelectTask: (OrbitTask) -> Unit,
    onAssignTaskToHour: (OrbitTask, Long, Int) -> Unit
) {
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    var selectedDayEpoch by androidx.compose.runtime.saveable.rememberSaveable { mutableLongStateOf(todayEpoch) }
    // Views: Timeline, Daily, Weekly, Monthly, Yearly, Agenda
    var calendarSubView by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Timeline") }
    var showScheduledVsDue by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Scheduled") }

    val weekDays = remember(todayEpoch) { (-1..12).map { todayEpoch + it } }

    val dayTasks = remember(tasks, selectedDayEpoch, showScheduledVsDue) {
        tasks.filter { task ->
            !task.isTrashed && !task.isCompleted && if (showScheduledVsDue == "Scheduled") {
                task.scheduledDateEpochDay == selectedDayEpoch || (task.scheduledDateEpochDay == 0L && task.dueDateEpochDay == selectedDayEpoch)
            } else {
                task.dueDateEpochDay == selectedDayEpoch
            }
        }
    }

    val unscheduledDayTasks = remember(dayTasks) {
        dayTasks.filter { it.scheduledStartHour < 0 }
    }
    val totalMinutes = remember(dayTasks) { dayTasks.sumOf { it.estimatedMinutes } }
    val workdayCapacityMinutes = remember(workdayStartHour, workdayEndHour) {
        ((workdayEndHour - workdayStartHour).coerceAtLeast(4)) * 60
    }
    val capacityRatio = (totalMinutes.toFloat() / workdayCapacityMinutes.toFloat()).coerceIn(0f, 1f)

    val conflictingHours = remember(dayTasks) {
        dayTasks.filter { it.scheduledStartHour >= 0 }
            .groupBy { it.scheduledStartHour }
            .filterValues { it.size > 1 }
            .keys
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("smart_calendar_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Calendar & Time Blocking", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Day, Week, Month, Year, Agenda, Timeline & ICS Export",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onExportIcs) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(".ICS")
                }
                Button(onClick = { onAutoFillFreeSlots(selectedDayEpoch) }) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fill Slots")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 14-Day Horizontal Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekDays.forEach { dayEpoch ->
                val date = LocalDate.ofEpochDay(dayEpoch)
                val isSelected = dayEpoch == selectedDayEpoch
                val dayTaskCount = tasks.count {
                    !it.isTrashed && !it.isCompleted && (it.scheduledDateEpochDay == dayEpoch || it.dueDateEpochDay == dayEpoch)
                }

                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(64.dp)
                        .clickable { selectedDayEpoch = dayEpoch }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEE")).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$dayTaskCount tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else kissatenTone(OrbitCyan, MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6 Calendar Views Strip (Timeline, Daily, Weekly, Monthly, Yearly, Agenda)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Timeline", "Daily", "Weekly", "Monthly", "Yearly", "Agenda").forEach { mode ->
                FilterChip(
                    selected = calendarSubView == mode,
                    onClick = { calendarSubView = mode },
                    label = { Text(mode) }
                )
            }
            listOf("Scheduled", "Due Date").forEach { type ->
                FilterChip(
                    selected = showScheduledVsDue == type,
                    onClick = { showScheduledVsDue = type },
                    label = { Text(type) }
                )
            }
        }

        // Daily Capacity Bar & Conflict Warning
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${SmartTaskParser.formatEpochDay(selectedDayEpoch, todayEpoch)} Capacity: ${totalMinutes}m / ${workdayCapacityMinutes}m",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${((1f - capacityRatio) * workdayCapacityMinutes).toInt()}m free focus capacity",
                        style = MaterialTheme.typography.labelSmall,
                        color = kissatenTone(OrbitEmerald, MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { capacityRatio },
                    color = if (totalMinutes > workdayCapacityMinutes) OrbitRose else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                if (conflictingHours.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = OrbitAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Overlap at ${conflictingHours.joinToString { SmartTaskParser.formatHourMinute(it, 0, use24Hour) }} — tap Fill Slots to spread evenly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = kissatenTone(OrbitAmber, MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }

        when (calendarSubView) {
            "Agenda", "Daily" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayTasks, key = { it.id.ifBlank { it.title } }) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTask(task) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Time: ${SmartTaskParser.formatHourMinute(task.scheduledStartHour, task.scheduledStartMinute, use24Hour)} • ${task.estimatedMinutes}m • ${task.projectName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PriorityBadge(task.priority)
                            }
                        }
                    }
                }
            }
            "Weekly" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((0..6).toList(), key = { "wk_$it" }) { offset ->
                        val day = selectedDayEpoch + offset
                        val list = tasks.filter { !it.isTrashed && (it.dueDateEpochDay == day || it.scheduledDateEpochDay == day) }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDayEpoch = day }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(SmartTaskParser.formatEpochDay(day, todayEpoch), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${list.size} tasks • ${list.sumOf { it.estimatedMinutes }}m", style = MaterialTheme.typography.labelSmall, color = kissatenTone(OrbitCyan, MaterialTheme.colorScheme.surfaceVariant))
                                }
                                list.take(3).forEach { t ->
                                    Text("• ${t.title}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            "Monthly" -> {
                val month = remember(selectedDayEpoch) { YearMonth.from(LocalDate.ofEpochDay(selectedDayEpoch)) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val weeks = (1..month.lengthOfMonth()).chunked(7)
                    items(weeks.size) { wkIdx ->
                        val week = weeks[wkIdx]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            week.forEach { d ->
                                val ep = month.atDay(d).toEpochDay()
                                val cnt = tasks.count { !it.isTrashed && (it.dueDateEpochDay == ep || it.scheduledDateEpochDay == ep) }
                                Surface(
                                    color = if (ep == selectedDayEpoch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable {
                                            selectedDayEpoch = ep
                                            calendarSubView = "Timeline"
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(d.toString(), style = MaterialTheme.typography.labelMedium)
                                        if (cnt > 0) {
                                            Text("${cnt}•", style = MaterialTheme.typography.labelSmall, color = kissatenTone(OrbitCyan, MaterialTheme.colorScheme.surfaceVariant))
                                        }
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(modifier = Modifier.size(44.dp)) }
                        }
                    }
                }
            }
            "Yearly" -> {
                val year = LocalDate.ofEpochDay(selectedDayEpoch).year
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Month.entries) { m ->
                        val ym = YearMonth.of(year, m)
                        val startEp = ym.atDay(1).toEpochDay()
                        val endEp = ym.atEndOfMonth().toEpochDay()
                        val mCount = tasks.count { !it.isTrashed && it.dueDateEpochDay in startEp..endEp }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDayEpoch = startEp
                                    calendarSubView = "Monthly"
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${m.name.lowercase().replaceFirstChar { it.uppercase() }} $year", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("$mCount scheduled tasks", style = MaterialTheme.typography.labelMedium, color = kissatenTone(OrbitCyan, MaterialTheme.colorScheme.surfaceVariant))
                            }
                        }
                    }
                }
            }
            else -> {
                // Timeline Hourly View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unscheduledDayTasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Unscheduled Tasks (${unscheduledDayTasks.size}) — Tap any free hour below to assign manually:",
                                style = MaterialTheme.typography.labelLarge,
                                color = kissatenTone(OrbitCyan, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    items((7..21).toList(), key = { "hour_$it" }) { hour ->
                        val tasksInHour = dayTasks.filter { it.scheduledStartHour == hour }
                        val isWorkHour = hour in workdayStartHour..workdayEndHour
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = SmartTaskParser.formatHourMinute(hour, 0, use24Hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isWorkHour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isWorkHour) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier
                                    .width(72.dp)
                                    .padding(top = 10.dp)
                            )
                            if (tasksInHour.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isWorkHour) 0.45f else 0.25f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            unscheduledDayTasks.firstOrNull()?.let { firstUnscheduled ->
                                                onAssignTaskToHour(firstUnscheduled, selectedDayEpoch, hour)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isWorkHour) "Focus Window" else "After-Hours Window",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (unscheduledDayTasks.isNotEmpty()) {
                                            Text(
                                                text = "+ Block \"${unscheduledDayTasks.first().title.take(20)}\"",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = kissatenTone(OrbitCyan, MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tasksInHour.forEach { task ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectTask(task) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = task.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${task.estimatedMinutes}m block • ${task.projectName} • ⚡ ${task.energyLevel}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                PriorityBadge(task.priority)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusCockpitScreen(
    openTasks: List<OrbitTask>,
    focusSessions: List<FocusSessionRecord>,
    timerRunning: Boolean,
    timerMode: String,
    timerSecondsRemaining: Int,
    timerTotalSeconds: Int,
    activeFocusTask: OrbitTask?,
    ambientSound: String,
    ambientVolume: Float = 0.45f,
    deepFocusZenMode: Boolean,
    dailyFocusGoalMinutes: Int = 120,
    onSelectMode: (String, Int?) -> Unit,
    onResetTimer: () -> Unit,
    onSelectTask: (OrbitTask?) -> Unit,
    onSelectSound: (String) -> Unit,
    onChangeAmbientVolume: (Float) -> Unit = {},
    onToggleZenMode: () -> Unit,
    onStartOrPause: () -> Unit,
    onCompleteSession: () -> Unit
) {
    val minutes = timerSecondsRemaining / 60
    val seconds = timerSecondsRemaining % 60
    val formattedClock = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val progress = if (timerMode == "stopwatch") {
        ((timerSecondsRemaining % 1500).toFloat() / 1500f)
    } else if (timerTotalSeconds > 0) {
        (timerSecondsRemaining.toFloat() / timerTotalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val totalFocusMinutes = remember(focusSessions) { focusSessions.sumOf { it.durationMinutes } }
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("focus_cockpit_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (deepFocusZenMode) "Quiet midnight study" else "Midnight Study & Focus Timer",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Drift-Free Monotonic Timer • Lofi Café & Night Train Soundscapes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleZenMode) {
                    Icon(
                        imageVector = if (deepFocusZenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Full-Screen Focus Mode"
                    )
                }
            }
        }

        // Mode Switcher Pills
        if (!deepFocusZenMode) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("pomodoro", "Pomodoro", null),
                        Triple("short_break", "Short Break", null),
                        Triple("long_break", "Long Break", null),
                        Triple("deep_work", "Deep Work 50m", 50),
                        Triple("stopwatch", "Stopwatch", 0)
                    ).forEach { (key, label, mins) ->
                        FilterChip(
                            selected = timerMode == key,
                            onClick = { onSelectMode(key, mins) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Circular Orbit Timer Dial
        item {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = primaryColor.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = if (timerRunning) OrbitCyan else primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timerMode.uppercase().replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedClock,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeFocusTask?.title?.take(26) ?: "Unlinked Focus Session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // Play / Pause / Reset / Complete Controls
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartOrPause,
                    modifier = Modifier
                        .height(54.dp)
                        .width(150.dp)
                        .testTag("focus_start_pause_button")
                ) {
                    Icon(
                        imageVector = if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (timerRunning) "Pause" else "Start")
                }

                OutlinedButton(
                    onClick = onResetTimer,
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }

                OutlinedButton(
                    onClick = onCompleteSession,
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log")
                }
            }
        }

        // Ambient Soundscape Selector & Volume Mixer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lofi Study & Ambient Audio Synthesizer (8 Offline Soundscapes)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Silent",
                            "Rainy Tokyo Café",
                            "Night Train Ride",
                            "Kyoto Bamboo Wind",
                            "Cosmic Rain",
                            "Warm Vinyl Static",
                            "Forest Breeze",
                            "Deep Space Brown",
                            "40Hz Gamma Flow"
                        ).forEach { sound ->
                            FilterChip(
                                selected = ambientSound == sound,
                                onClick = { onSelectSound(sound) },
                                label = { Text(sound) }
                            )
                        }
                    }
                    if (ambientSound != "Silent") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Volume ${(ambientVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = ambientVolume,
                                onValueChange = onChangeAmbientVolume,
                                valueRange = 0.05f..1.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Active Task Selector & Focus History (hidden in Zen mode)
        if (!deepFocusZenMode) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Link Active Task to Timer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        openTasks.take(4).forEach { task ->
                            val isSelected = activeFocusTask?.id == task.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onSelectTask(task) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${task.actualMinutes}m / ${task.estimatedMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OrbitCyan
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Focus Statistics & Session History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Goal: ${totalFocusMinutes}m / ${dailyFocusGoalMinutes}m", style = MaterialTheme.typography.labelMedium, color = OrbitEmerald)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (focusSessions.isEmpty()) {
                            Text(
                                text = "Complete your first Pomodoro or Stopwatch block to build focus history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            focusSessions.take(6).forEach { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${session.taskTitle} (${session.mode})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "+${session.durationMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OrbitEmerald
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
