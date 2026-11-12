package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AutomationRule
import com.example.data.model.DashboardPresetConfig
import com.example.data.model.FocusSessionRecord
import com.example.data.model.NativeHomeWidgetVariant
import com.example.data.model.OrbitHabit
import com.example.data.model.OrbitProject
import com.example.data.model.OrbitScreenDestination
import com.example.data.model.OrbitTask
import com.example.data.model.OrbitWidgetType
import com.example.data.model.SmartTaskParser
import com.example.data.model.UserPreferencesDoc
import com.example.ui.theme.OrbitAccentPalettes
import com.example.ui.theme.OrbitAmber
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitIndigo
import com.example.ui.theme.OrbitRose
import com.example.widget.OrbitHomeWidgetProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

import com.example.data.notification.OrbitReminderScheduler

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreAndEcosystemScreen(
    initialSubTab: String = "Habits & Routines",
    tasks: List<OrbitTask>,
    projects: List<OrbitProject>,
    habits: List<OrbitHabit>,
    focusSessions: List<FocusSessionRecord>,
    automations: List<AutomationRule>,
    preferences: UserPreferencesDoc,
    onToggleHabit: (OrbitHabit) -> Unit,
    onUseHabitFreeze: (OrbitHabit) -> Unit = {},
    onCreateHabit: (String, String, String, Int, String) -> Unit,
    onCreateProject: (String, String, String, String, Boolean, List<String>) -> Unit,
    onQuickAddSpecialReminder: (String, String) -> Unit,
    onTriggerNotificationNow: (OrbitTask) -> Unit,
    onToggleWidget: (String) -> Unit,
    onToggleFavoriteWidget: (String) -> Unit = {},
    onSetWidgetSize: (String, String) -> Unit = { _, _ -> },
    onSetHomeWidgetVariant: (String) -> Unit = {},
    onMoveWidget: (String, Boolean) -> Unit,
    onApplyPreset: (DashboardPresetConfig) -> Unit,
    onUpdatePreferences: ((UserPreferencesDoc) -> UserPreferencesDoc) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportIcs: () -> Unit,
    onImportJson: (String) -> Unit,
    onPreviewImportJson: (String) -> Unit = onImportJson,
    onImportIcs: (String) -> Unit,
    onSaveFileSaf: (String) -> Unit = {},
    onOpenFileSaf: () -> Unit = {},
    onResetSeed: () -> Unit = {},
    onRestoreTaskFromTrash: (OrbitTask) -> Unit,
    onEmptyTrash: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var activeSubTab by androidx.compose.runtime.saveable.rememberSaveable(initialSubTab) { mutableStateOf(initialSubTab) }

    val subTabs = listOf(
        "Habits & Routines",
        "Projects & Shopping",
        "Reminder Center",
        "Statistics",
        "Widget Gallery",
        "Themes & Settings",
        "Backup & Restore"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("more_ecosystem_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("Yorulune 2.0 Studio & Modules", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "100% Free • Offline-First • Zero Locked Features",
            style = MaterialTheme.typography.bodySmall,
            color = OrbitEmerald
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTabs.forEach { tab ->
                FilterChip(
                    selected = activeSubTab == tab,
                    onClick = { activeSubTab = tab },
                    label = { Text(tab) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (activeSubTab) {
            "Habits & Routines" -> HabitsAndRoutinesSection(
                habits = habits,
                gamificationEnabled = preferences.gamificationEnabled,
                streakStyle = preferences.streakStyle,
                onToggleHabit = onToggleHabit,
                onUseHabitFreeze = onUseHabitFreeze,
                onCreateHabit = onCreateHabit,
                onToggleGamification = { enabled ->
                    onUpdatePreferences { it.copy(gamificationEnabled = enabled) }
                }
            )
            "Projects & Shopping" -> SharedSpacesAndGrocerySection(
                projects = projects,
                tasks = tasks,
                onCreateProject = onCreateProject
            )
            "Reminder Center" -> ReminderCenterSection(
                tasks = tasks.filter { !it.isTrashed },
                quietHoursEnabled = preferences.quietHoursEnabled,
                onToggleQuietHours = { q -> onUpdatePreferences { it.copy(quietHoursEnabled = q) } },
                onQuickAddSpecialReminder = onQuickAddSpecialReminder,
                onTriggerNotificationNow = onTriggerNotificationNow,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
            "Statistics" -> StatisticsAndReviewSection(
                tasks = tasks.filter { !it.isTrashed },
                habits = habits,
                focusSessions = focusSessions
            )
            "Widget Gallery" -> WidgetGalleryAndEditorSection(
                enabledWidgets = preferences.enabledWidgets,
                favoriteWidgets = preferences.favoriteWidgets,
                widgetSizes = preferences.widgetSizes,
                homeWidgetVariant = preferences.homeWidgetVariant,
                onToggleWidget = onToggleWidget,
                onToggleFavoriteWidget = onToggleFavoriteWidget,
                onSetWidgetSize = onSetWidgetSize,
                onSetHomeWidgetVariant = onSetHomeWidgetVariant,
                onMoveWidget = onMoveWidget,
                onApplyPreset = onApplyPreset
            )
            "Themes & Settings" -> ThemeStudioAndSettingsSection(
                preferences = preferences,
                onUpdatePreferences = onUpdatePreferences,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
            "Backup & Restore" -> BackupRestoreAndTrashSection(
                trashedTasks = tasks.filter { it.isTrashed },
                webDavUrl = preferences.webDavUrl,
                onUpdateWebDav = { url -> onUpdatePreferences { it.copy(webDavUrl = url) } },
                onExportJson = onExportJson,
                onExportCsv = onExportCsv,
                onExportIcs = onExportIcs,
                onImportJson = onImportJson,
                onPreviewImportJson = onPreviewImportJson,
                onImportIcs = onImportIcs,
                onSaveFileSaf = onSaveFileSaf,
                onOpenFileSaf = onOpenFileSaf,
                onResetSeed = onResetSeed,
                onRestoreTask = onRestoreTaskFromTrash,
                onEmptyTrash = onEmptyTrash
            )
        }
    }
}

@Composable
private fun ReminderCenterSection(
    tasks: List<OrbitTask>,
    quietHoursEnabled: Boolean,
    onToggleQuietHours: (Boolean) -> Unit,
    onQuickAddSpecialReminder: (String, String) -> Unit,
    onTriggerNotificationNow: (OrbitTask) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    var reminderTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Birthday") }

    val missedReminders = remember(tasks, todayEpoch) {
        tasks.filter { !it.isCompleted && it.dueDateEpochDay in 1 until todayEpoch }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OrbitCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Special Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Birthdays, Anniversaries, Bill Payments, Medications & Event Countdowns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Birthday" to "🎂 Birthday", "Anniversary" to "💍 Anniversary", "Bill" to "💳 Bill", "Medication" to "💊 Medication").forEach { (k, label) ->
                            FilterChip(
                                selected = selectedCategory == k,
                                onClick = { selectedCategory = k },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = reminderTitle,
                            onValueChange = { reminderTitle = it },
                            placeholder = { Text("e.g. Pay electricity bill tomorrow at 10 AM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (reminderTitle.isNotBlank()) {
                                onQuickAddSpecialReminder(reminderTitle, selectedCategory)
                                reminderTitle = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        item {
            val context = LocalContext.current
            val deliveryGuaranteeText = remember(context) {
                OrbitReminderScheduler.describeAlarmDeliveryGuarantee(context)
            }
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
                            Text("Quiet Hours, Alarm Delivery & Permission Guard", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Critical reminders bypass Quiet Hours when enabled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = quietHoursEnabled, onCheckedChange = onToggleQuietHours)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = deliveryGuaranteeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Check / Grant Android Notification Permission")
                    }
                }
            }
        }

        if (missedReminders.isNotEmpty()) {
            item {
                Text("Missed Reminder Recovery (${missedReminders.size})", style = MaterialTheme.typography.titleSmall, color = OrbitRose, fontWeight = FontWeight.Bold)
            }
            items(missedReminders, key = { "missed_${it.id}" }) { t ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OrbitRose.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Missed on ${SmartTaskParser.formatEpochDay(t.dueDateEpochDay)}", style = MaterialTheme.typography.labelSmall, color = OrbitRose)
                        }
                        TextButton(onClick = { onTriggerNotificationNow(t) }) {
                            Text("Trigger Alert")
                        }
                    }
                }
            }
        }

        item {
            Text("All Scheduled Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(tasks.filter { !it.isCompleted }.take(10), key = { "rem_${it.id}" }) { t ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (t.assigneeName.isNotBlank()) "[${t.assigneeName}] ${t.title}" else t.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Due: ${SmartTaskParser.formatEpochDay(t.dueDateEpochDay)} • ${t.reminderEscalation.uppercase()} • ${t.recurrenceRule.ifBlank { "One-Time" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onTriggerNotificationNow(t) }) {
                        Text("Test Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsAndReviewSection(
    tasks: List<OrbitTask>,
    habits: List<OrbitHabit>,
    focusSessions: List<FocusSessionRecord>
) {
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    var selectedHeatmapDay by remember { mutableStateOf(todayEpoch) }
    val completed = tasks.count { it.isCompleted }
    val open = tasks.count { !it.isCompleted }
    val total = (completed + open).coerceAtLeast(1)
    val totalEst = tasks.sumOf { it.estimatedMinutes }
    val totalActual = tasks.sumOf { it.actualMinutes } + focusSessions.sumOf { it.durationMinutes }

    val last7Days = remember(todayEpoch) { (6 downTo 0).map { todayEpoch - it } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = OrbitCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Productivity Statistics & Weekly Review 3.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Overall Completion Rate: ${(completed * 100) / total}% ($completed of $total tasks)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / total.toFloat() },
                        color = OrbitEmerald,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Time Accuracy: ${totalEst}m estimated vs. ${totalActual}m actual logged", style = MaterialTheme.typography.bodySmall, color = OrbitCyan)
                }
            }
        }

        // Interactive 7-Day Velocity & Focus Bar Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("7-Day Task & Habit Velocity Chart", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Tap any day bar to inspect daily activity below",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val dayScores = last7Days.map { d ->
                        val tDone = tasks.count { it.isCompleted && (it.dueDateEpochDay == d || it.scheduledDateEpochDay == d) }
                        val hDone = habits.count { it.isCompletedOn(d) }
                        val fDone = focusSessions.count { it.completedEpochDay == d }
                        d to (tDone + hDone + fDone)
                    }
                    val maxScore = (dayScores.maxOfOrNull { it.second } ?: 1).coerceAtLeast(4)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayScores.forEach { (epochDay, score) ->
                            val isSelected = selectedHeatmapDay == epochDay
                            val fraction = (score.toFloat() / maxScore.toFloat()).coerceIn(0.12f, 1f)
                            val dowLabel = LocalDate.ofEpochDay(epochDay)
                                .format(DateTimeFormatter.ofPattern("EEE", Locale.US))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedHeatmapDay = epochDay }
                            ) {
                                Text(
                                    text = score.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((68 * fraction).dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dowLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Priority & Energy Level Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Priority & Energy Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    listOf(
                        Triple("P1 Critical", tasks.count { it.priority == "p1" }, OrbitRose),
                        Triple("P2 High", tasks.count { it.priority == "p2" }, OrbitAmber),
                        Triple("P3 Normal", tasks.count { it.priority == "p3" }, OrbitCyan),
                        Triple("P4 Low", tasks.count { it.priority == "p4" }, OrbitEmerald)
                    ).forEach { (label, count, barColor) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(78.dp))
                            LinearProgressIndicator(
                                progress = { (count.toFloat() / total.toFloat()).coerceIn(0.03f, 1f) },
                                color = barColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(CircleShape)
                            )
                            Text("$count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interactive 28-Day Completion & Habit Heatmap", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    val days = (27 downTo 0).map { todayEpoch - it }.chunked(7)
                    days.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            week.forEach { d ->
                                val activityScore = habits.count { it.isCompletedOn(d) } +
                                    focusSessions.count { it.completedEpochDay == d }
                                val cellColor = when {
                                    activityScore >= 3 -> OrbitEmerald
                                    activityScore == 2 -> OrbitCyan
                                    activityScore == 1 -> OrbitIndigo
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val isSelected = selectedHeatmapDay == d
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                            else Modifier
                                        )
                                        .clickable { selectedHeatmapDay = d },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = LocalDate.ofEpochDay(d).dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val selHabits = habits.filter { it.isCompletedOn(selectedHeatmapDay) }
                    val selFocusMins = focusSessions.filter { it.completedEpochDay == selectedHeatmapDay }.sumOf { it.durationMinutes }
                    Text(
                        text = "${SmartTaskParser.formatEpochDay(selectedHeatmapDay, todayEpoch)}: ${selHabits.size} habits completed • ${selFocusMins}m focus logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetGalleryAndEditorSection(
    enabledWidgets: List<String>,
    favoriteWidgets: List<String>,
    widgetSizes: Map<String, String>,
    homeWidgetVariant: String,
    onToggleWidget: (String) -> Unit,
    onToggleFavoriteWidget: (String) -> Unit,
    onSetWidgetSize: (String, String) -> Unit,
    onSetHomeWidgetVariant: (String) -> Unit,
    onMoveWidget: (String, Boolean) -> Unit,
    onApplyPreset: (DashboardPresetConfig) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember {
        listOf("All", "Favorites", "Tasks", "Calendar", "Analytics", "Habits", "Focus", "Planning", "Organization", "Life", "System")
    }
    val filteredWidgets = remember(searchQuery, selectedCategory, favoriteWidgets) {
        OrbitWidgetType.entries.filter { w ->
            val matchesQuery = searchQuery.isBlank() ||
                w.title.contains(searchQuery, ignoreCase = true) ||
                w.subtitle.contains(searchQuery, ignoreCase = true)
            val matchesCat = when (selectedCategory) {
                "All" -> true
                "Favorites" -> favoriteWidgets.contains(w.key)
                else -> w.category.equals(selectedCategory, ignoreCase = true)
            }
            matchesQuery && matchesCat
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = OrbitCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "10 Native Android Home-Screen Widgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Select the active native OS widget variant below or tap 'Mode ↻' directly on your Android home-screen widget to cycle all 10 modes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NativeHomeWidgetVariant.entries.forEach { variant ->
                            FilterChip(
                                selected = homeWidgetVariant == variant.key,
                                onClick = { onSetHomeWidgetVariant(variant.key) },
                                label = { Text("${variant.number}. ${variant.title}") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active Mode: ${NativeHomeWidgetVariant.fromKey(homeWidgetVariant).description}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { OrbitHomeWidgetProvider.requestPinToHomeScreen(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Pin Native OS Widget to Android Home Screen")
                    }
                }
            }
        }

        item {
            Text(
                text = "Widget Gallery 2.0 • All 30 Dashboard Widgets (${enabledWidgets.size} Active)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 30 widgets by name or capability...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardPresetConfig.entries.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onApplyPreset(preset) },
                        label = { Text("Preset: ${preset.label}") }
                    )
                }
            }
        }

        items(filteredWidgets, key = { it.key }) { widget ->
            val isEnabled = enabledWidgets.contains(widget.key)
            val isFav = favoriteWidgets.contains(widget.key)
            val currentSize = widgetSizes[widget.key] ?: widget.defaultSize
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${widget.number} ${widget.title} ($currentSize)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${widget.category} • ${widget.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onToggleFavoriteWidget(widget.key) }) {
                            Text(if (isFav) "★" else "☆")
                        }
                        if (isEnabled) {
                            IconButton(onClick = { onMoveWidget(widget.key, true) }) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMoveWidget(widget.key, false) }) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                            }
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { onToggleWidget(widget.key) }
                        )
                    }
                    if (isEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Size:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf("Compact", "Medium", "Large", "Full").forEach { sz ->
                                FilterChip(
                                    selected = currentSize == sz,
                                    onClick = { onSetWidgetSize(widget.key, sz) },
                                    label = { Text(sz, style = MaterialTheme.typography.labelSmall) }
                                )
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
private fun HabitsAndRoutinesSection(
    habits: List<OrbitHabit>,
    gamificationEnabled: Boolean,
    streakStyle: String,
    onToggleHabit: (OrbitHabit) -> Unit,
    onUseHabitFreeze: (OrbitHabit) -> Unit,
    onCreateHabit: (String, String, String, Int, String) -> Unit,
    onToggleGamification: (Boolean) -> Unit
) {
    val todayEpoch = remember { LocalDate.now().toEpochDay() }
    var newHabitTitle by remember { mutableStateOf("") }
    var selectedRoutine by remember { mutableStateOf("morning") }
    var selectedTargetDays by remember { mutableStateOf(7) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
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
                            Text("Habits, Routines & Streak Freezes 2.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (gamificationEnabled) "Streaks ($streakStyle style), freezes & heatmaps active" else "Gentle non-gamified mode active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = gamificationEnabled, onCheckedChange = onToggleGamification)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("morning" to "☀️ Morning", "evening" to "🌙 Evening", "anytime" to "⚡ Anytime").forEach { (k, label) ->
                            FilterChip(
                                selected = selectedRoutine == k,
                                onClick = { selectedRoutine = k },
                                label = { Text(label) }
                            )
                        }
                        listOf(7 to "Daily (7/wk)", 5 to "Weekdays (5/wk)", 3 to "3x/wk").forEach { (days, label) ->
                            FilterChip(
                                selected = selectedTargetDays == days,
                                onClick = { selectedTargetDays = days },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newHabitTitle,
                            onValueChange = { newHabitTitle = it },
                            placeholder = { Text("Add habit or routine ritual...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newHabitTitle.isNotBlank()) {
                                    onCreateHabit(newHabitTitle, selectedRoutine, "health", selectedTargetDays, "07:30")
                                    newHabitTitle = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Habit")
                        }
                    }
                }
            }
        }

        items(habits, key = { it.id.ifBlank { it.title } }) { habit ->
            val doneToday = habit.isCompletedOn(todayEpoch)
            val weekDone = habit.completionsInLast7Days(todayEpoch)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleHabit(habit) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (doneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (doneToday) OrbitEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(habit.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${habit.frequencyLabel} • ${habit.reminderTime} • $weekDone/${habit.targetDaysPerWeek} this wk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (gamificationEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔥 ${habit.currentStreak}d (Best ${habit.bestStreak}d)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OrbitAmber
                                )
                                TextButton(onClick = { onUseHabitFreeze(habit) }) {
                                    Text("❄️ ${habit.streakFreezesRemaining}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // 14-Day Completion Heatmap Strip
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (13 downTo 0).forEach { offset ->
                            val day = todayEpoch - offset
                            val active = habit.isCompletedOn(day)
                            val frozen = habit.frozenEpochDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            active -> OrbitEmerald
                                            frozen -> OrbitCyan
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedSpacesAndGrocerySection(
    projects: List<OrbitProject>,
    tasks: List<OrbitTask>,
    onCreateProject: (String, String, String, String, Boolean, List<String>) -> Unit
) {
    var newSpaceName by remember { mutableStateOf("") }
    var newMembersText by remember { mutableStateOf("You, Family") }
    var isSharedSpace by remember { mutableStateOf(true) }

    val groceryItems = remember {
        mutableStateListOf(
            "Produce" to "Organic Hass Avocados & Baby Spinach",
            "Produce" to "Wild Blueberries & Lemons",
            "Dairy & Plant" to "Oat Barista Milk (2 cartons)",
            "Pantry & Coffee" to "Whole-Bean Ethiopian Espresso",
            "Household" to "Dishwasher Tabs & Sparkling Water"
        )
    }
    var newGroceryItem by remember { mutableStateOf("") }
    var selectedAisle by remember { mutableStateOf("Produce") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = OrbitCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Unlimited Project or Folder", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSpaceName,
                        onValueChange = { newSpaceName = it },
                        label = { Text("Project / Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newMembersText,
                        onValueChange = { newMembersText = it },
                        label = { Text("Optional Collaborators (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = isSharedSpace, onCheckedChange = { isSharedSpace = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shared Household List", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                if (newSpaceName.isNotBlank()) {
                                    val members = newMembersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    onCreateProject(newSpaceName, "Custom project space", "#06B6D4", "work", isSharedSpace, members)
                                    newSpaceName = ""
                                }
                            }
                        ) {
                            Text("Create Project")
                        }
                    }
                }
            }
        }

        items(projects, key = { it.id.ifBlank { it.name } }) { proj ->
            val count = tasks.count { !it.isTrashed && it.projectId == proj.id }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(proj.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$count tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrbitCyan
                        )
                    }
                    if (proj.description.isNotBlank()) {
                        Text(
                            text = proj.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Categorized Shopping Lists
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = OrbitEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shopping Lists with Category Grouping", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Produce", "Dairy & Plant", "Pantry & Coffee", "Household").forEach { aisle ->
                            FilterChip(
                                selected = selectedAisle == aisle,
                                onClick = { selectedAisle = aisle },
                                label = { Text(aisle) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newGroceryItem,
                            onValueChange = { newGroceryItem = it },
                            placeholder = { Text("Add item to $selectedAisle...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newGroceryItem.isNotBlank()) {
                                groceryItems.add(selectedAisle to newGroceryItem.trim())
                                newGroceryItem = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    groceryItems.groupBy({ it.first }, { it.second }).forEach { (aisle, items) ->
                        Text(aisle.uppercase(), style = MaterialTheme.typography.labelSmall, color = OrbitEmerald)
                        items.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStudioAndSettingsSection(
    preferences: UserPreferencesDoc,
    onUpdatePreferences: ((UserPreferencesDoc) -> UserPreferencesDoc) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yorulune Japanese Lofi & Pastel Theme Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Appearance Mode (Washi Paper / Light / Rainy-Night Dark / True OLED / System)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "paper" to "📜 Washi Paper",
                            "light" to "☀️ Sunlit Pastel Light",
                            "dark" to "🏮 Rainy Lofi Night",
                            "oled" to "🌑 True OLED Black",
                            "dynamic" to "🎨 Material You",
                            "system" to "⚙️ System Auto"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onUpdatePreferences { it.copy(themeMode = mode) } },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Japanese Lofi Core Palettes (Midnight Study, Kyoto Café & Lantern Glow)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrbitAccentPalettes.filter { it.isJapaneseLofi }.forEach { palette ->
                            FilterChip(
                                selected = preferences.accentPreset == palette.key,
                                onClick = { onUpdatePreferences { it.copy(accentPreset = palette.key) } },
                                label = { Text(palette.label) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)),
                                                CircleShape
                                            )
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pastel & Editorial Collection (Terracotta, Lavender, Sky Blue, Mint, Peach, Rose & More)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrbitAccentPalettes.filter { !it.isJapaneseLofi }.forEach { palette ->
                            FilterChip(
                                selected = preferences.accentPreset == palette.key,
                                onClick = { onUpdatePreferences { it.copy(accentPreset = palette.key) } },
                                label = { Text(palette.label) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                Brush.linearGradient(listOf(palette.gradientStart, palette.gradientEnd)),
                                                CircleShape
                                            )
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    var accentInput by remember(preferences.accentPreset) {
                        mutableStateOf(preferences.accentPreset.takeIf { it.startsWith("#") }.orEmpty())
                    }
                    OutlinedTextField(
                        value = accentInput, onValueChange = { accentInput = it.take(7) },
                        label = { Text("Custom accent · #RRGGBB") }, singleLine = true,
                        supportingText = { Text("Your selected palette returns when you choose a swatch.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onUpdatePreferences { it.copy(accentPreset = accentInput) } },
                        enabled = Regex("#[0-9a-fA-F]{6}").matches(accentInput)) { Text("Use custom accent") }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Interface Density", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("airy" to "Airy", "comfortable" to "Comfortable", "compact" to "Compact").forEach { (d, label) ->
                            FilterChip(
                                selected = preferences.density == d,
                                onClick = { onUpdatePreferences { it.copy(density = d) } },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Habit Streak Indicator Style", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("flame" to "🔥 Flame", "number" to "# Number", "bar" to "▬ Progress Bar").forEach { (st, label) ->
                            FilterChip(
                                selected = preferences.streakStyle == st,
                                onClick = { onUpdatePreferences { it.copy(streakStyle = st) } },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Card Corner Radius: ${preferences.cornerRadiusDp}dp", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.cornerRadiusDp.toFloat(),
                        onValueChange = { v -> onUpdatePreferences { it.copy(cornerRadiusDp = v.toInt()) } },
                        valueRange = 8f..32f
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Accessibility, Formats & Custom Timers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reduced-Motion Accessibility Mode", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = preferences.reducedMotion,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(reducedMotion = v) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use 24-Hour Clock Format (14:00 vs 2 PM)", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = preferences.use24HourFormat,
                            onCheckedChange = { v -> onUpdatePreferences { it.copy(use24HourFormat = v) } }
                        )
                    }
                    Text("Pomodoro Work Interval: ${preferences.defaultPomodoroMinutes}m", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.defaultPomodoroMinutes.toFloat(),
                        onValueChange = { v -> onUpdatePreferences { it.copy(defaultPomodoroMinutes = v.toInt()) } },
                        valueRange = 15f..90f
                    )
                    OutlinedButton(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Verify System Notification Permission")
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreAndTrashSection(
    trashedTasks: List<OrbitTask>,
    webDavUrl: String,
    onUpdateWebDav: (String) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportIcs: () -> Unit,
    onImportJson: (String) -> Unit,
    onPreviewImportJson: (String) -> Unit,
    onImportIcs: (String) -> Unit,
    onSaveFileSaf: (String) -> Unit,
    onOpenFileSaf: () -> Unit,
    onResetSeed: () -> Unit,
    onRestoreTask: (OrbitTask) -> Unit,
    onEmptyTrash: () -> Unit
) {
    var importPayload by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    if (confirmReset || confirmEmptyTrash) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmReset = false; confirmEmptyTrash = false },
            title = { Text(if (confirmReset) "Replace this workspace?" else "Permanently empty Trash?") },
            text = { Text(if (confirmReset) "This replaces your tasks, projects, habits and focus history with example data. Export a JSON backup first." else "Trashed tasks will be permanently deleted. Export a JSON backup first if you need to keep them.") },
            confirmButton = { TextButton(onClick = {
                if (confirmReset) onResetSeed() else onEmptyTrash()
                confirmReset = false; confirmEmptyTrash = false
            }) { Text(if (confirmReset) "Replace workspace" else "Delete permanently") } },
            dismissButton = { TextButton(onClick = { confirmReset = false; confirmEmptyTrash = false }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Transactional Backup Vault (JSON v3, CSV & .ICS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "JSON exports are unencrypted. Choose a destination in the Android document picker; a provider you choose may store your file in the cloud. You can also preview and copy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Choose where to save:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveFileSaf("json") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saf_save_json_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save .JSON")
                        }
                        OutlinedButton(
                            onClick = { onSaveFileSaf("csv") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saf_save_csv_button")
                        ) {
                            Text("Save .CSV")
                        }
                        OutlinedButton(
                            onClick = { onSaveFileSaf("ics") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saf_save_ics_button")
                        ) {
                            Text("Save .ICS")
                        }
                    }
                    Text("Inspect & Copy to Clipboard:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f)) {
                            Text("Preview JSON")
                        }
                        OutlinedButton(onClick = onExportCsv, modifier = Modifier.weight(1f)) {
                            Text("Preview CSV")
                        }
                        OutlinedButton(onClick = onExportIcs, modifier = Modifier.weight(1f)) {
                            Text("Preview .ICS")
                        }
                    }
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Workspace to Default Starter Seed")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Import & Pre-Restore Inspection (JSON v1–v3 or .ICS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onOpenFileSaf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saf_open_file_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Backup (.JSON) or Calendar (.ICS) File from Storage")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importPayload,
                        onValueChange = { importPayload = it },
                        placeholder = { Text("Or paste JSON backup or BEGIN:VCALENDAR (.ICS) content here...") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_import_payload_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onPreviewImportJson(importPayload)
                                importPayload = ""
                            },
                            enabled = importPayload.isNotBlank(),
                            modifier = Modifier.testTag("preview_restore_json_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview & Restore JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                onImportIcs(importPayload)
                                importPayload = ""
                            },
                            enabled = importPayload.isNotBlank()
                        ) {
                            Text("Import .ICS")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Keep a copy you control", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Automatic Android cloud backup and device transfer are disabled. Exports are unencrypted. The Android document picker lets you choose a destination, including a cloud provider managed by you. Yorulune does not connect to a sync server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                }
            }
        }

        item {
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
                        Text("Trash & Recovery (${trashedTasks.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (trashedTasks.isNotEmpty()) {
                            TextButton(onClick = { confirmEmptyTrash = true }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = OrbitRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Empty Trash", color = OrbitRose)
                            }
                        }
                    }
                    if (trashedTasks.isEmpty()) {
                        Text("Trash is empty.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        trashedTasks.forEach { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                                TextButton(onClick = { onRestoreTask(t) }) {
                                    Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore")
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
fun UniversalCommandCenterDialog(
    tasks: List<OrbitTask>,
    projects: List<OrbitProject>,
    onDismiss: () -> Unit,
    onSelectTask: (OrbitTask) -> Unit,
    onNavigateScreen: (String) -> Unit,
    onToggleOledMode: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matchingScreens = remember(query) {
        if (query.isBlank()) OrbitScreenDestination.entries
        else OrbitScreenDestination.entries.filter {
            it.title.contains(query, true) || it.subtitle.contains(query, true)
        }
    }
    val matchingTasks = remember(tasks, query) {
        if (query.isBlank()) tasks.filter { !it.isTrashed }.take(4)
        else tasks.filter {
            !it.isTrashed && (
                it.title.contains(query, ignoreCase = true) ||
                    it.projectName.contains(query, ignoreCase = true) ||
                    it.tags.any { t -> t.contains(query, ignoreCase = true) }
                )
        }.take(6)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = OrbitCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Command Palette (⌘K) • All 22 Screens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Jump to any of the 22 screens or search tasks...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("command_center_search_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            onToggleOledMode()
                            onDismiss()
                        },
                        label = { Text("🌑 Toggle OLED Black") }
                    )
                    matchingScreens.forEach { dest ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onNavigateScreen(dest.route)
                                onDismiss()
                            },
                            label = { Text(dest.title) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("MATCHING TASKS (${matchingTasks.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier.height(210.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(matchingTasks, key = { it.id.ifBlank { it.title } }) { task ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTask(task)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text(task.projectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                PriorityBadge(task.priority)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}
