package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DashboardPresetConfig
import com.example.data.model.FocusSessionRecord
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
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PriorityP1Red
import com.example.ui.theme.PriorityP2Amber
import com.example.ui.theme.PriorityP3Blue
import com.example.ui.theme.PriorityP4Slate
import com.example.ui.theme.getAccentPalette
import com.example.widget.OrbitHomeWidgetProvider
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CuratedWisdomQuotes = listOf(
    "\"Simplicity is about subtracting the obvious and adding the meaningful.\" — John Maeda",
    "\"Focus is a matter of deciding what things you're not going to do.\" — John Carmack",
    "\"Your mind is for having ideas, not holding them.\" — David Allen",
    "\"Small daily improvements over time lead to stunning results.\" — Robin Sharma",
    "\"Action expresses priorities.\" — Mahatma Gandhi"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MissionControlScreen(
    tasks: List<OrbitTask>,
    projects: List<OrbitProject>,
    habits: List<OrbitHabit>,
    focusSessions: List<FocusSessionRecord>,
    preferences: UserPreferencesDoc,
    celebrationPulse: Boolean,
    onQuickAddTask: (String) -> Unit,
    onToggleTask: (OrbitTask) -> Unit,
    onSelectTask: (OrbitTask) -> Unit,
    onStartFocusForTask: (OrbitTask) -> Unit,
    onToggleHabit: (OrbitHabit) -> Unit,
    onRescheduleOverdueToToday: (OrbitTask) -> Unit,
    onApplyPreset: (DashboardPresetConfig) -> Unit,
    onToggleWidget: (String) -> Unit,
    onMoveWidget: (String, Boolean) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSelectEnergyFilter: (String) -> Unit,
    onUpdatePreferences: ((UserPreferencesDoc) -> UserPreferencesDoc) -> Unit = {},
    onResetSeed: () -> Unit = {},
    onOpenCommandCenter: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val todayDate = remember { LocalDate.now() }
    val todayEpoch = remember { todayDate.toEpochDay() }
    val dayOfWeekUpper = remember {
        todayDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.US)).uppercase(Locale.US)
    }
    val monthName = remember {
        todayDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.US))
    }
    val dayOfMonthNumber = remember {
        todayDate.dayOfMonth.toString()
    }

    var showWidgetStudio by remember { mutableStateOf(false) }
    var showTweaksPanel by remember { mutableStateOf(false) }
    var inlineCaptureText by remember { mutableStateOf("") }
    var quoteIndex by remember { mutableIntStateOf(0) }

    val activeTasks = remember(tasks) { tasks.filter { !it.isTrashed && it.status != "archived" } }
    val openTasks = remember(activeTasks) { activeTasks.filter { !it.isCompleted } }
    val completedTasks = remember(activeTasks) { activeTasks.filter { it.isCompleted } }
    val overdueTasks = remember(openTasks, todayEpoch) {
        openTasks.filter { it.dueDateEpochDay in 1 until todayEpoch }
    }
    val todayTasks = remember(openTasks, todayEpoch) {
        openTasks.filter { it.dueDateEpochDay == todayEpoch || it.scheduledDateEpochDay == todayEpoch || it.dueDateEpochDay == 0L }
    }
    val upcomingTasks = remember(openTasks, todayEpoch) {
        openTasks.filter { it.dueDateEpochDay > todayEpoch }.sortedBy { it.dueDateEpochDay }
    }
    val topPriorities = remember(openTasks) {
        openTasks.sortedWith(compareByDescending<OrbitTask> { it.isPinned }.thenBy { it.priority }.thenByDescending { it.isImportant }).take(4)
    }
    val totalFocusMinutes = remember(focusSessions) {
        focusSessions.sumOf { it.durationMinutes }
    }

    val todayHabits = remember(habits, todayDate) {
        val scheduled = habits.filter { it.isScheduledForDate(todayDate) }
        scheduled.ifEmpty { habits.take(4) }
    }
    val notTodayHabits = remember(habits, todayHabits) {
        val todayIds = todayHabits.map { it.id }.toSet()
        habits.filter { it.id !in todayIds }
    }
    val todayHabitsDone = remember(todayHabits, todayEpoch) {
        todayHabits.count { it.isCompletedOn(todayEpoch) }
    }
    val todayHabitsTotal = todayHabits.size.coerceAtLeast(1)
    val todayHabitsPct = (todayHabitsDone * 100) / todayHabitsTotal

    val itemSpacing = when (preferences.density) {
        "compact" -> 10.dp
        "airy", "spacious" -> 18.dp
        else -> 14.dp
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mission_control_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        item(key = "kissaten_welcome") { com.example.ui.theme.KissatenWelcome("Your quiet corner of the universe.", 2) }
        // 1. Editorial Header + Tweaks & Preset Bar (Inspired by Reference Design)
        item(key = "hero_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) dayOfWeekUpper else "$dayOfWeekUpper · YOUR STUDY DESK",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 1.6.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontFamily = PlayfairDisplayFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                ) {
                                    append("$monthName ")
                                }
                                withStyle(
                                    SpanStyle(
                                        fontFamily = PlayfairDisplayFontFamily,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(dayOfMonthNumber)
                                }
                            },
                            fontSize = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) 28.sp else 44.sp,
                            lineHeight = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) 34.sp else 48.sp
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val nextThemeMode = when (preferences.themeMode) {
                            "paper" -> "light"
                            "light" -> "dark"
                            "dark" -> "oled"
                            "oled" -> "system"
                            else -> "paper"
                        }
                        val themeModeBadge = when (preferences.themeMode) {
                            "light" -> "☀️ Light"
                            "dark" -> "🏮 Lofi Night"
                            "oled" -> "🌑 OLED"
                            "system" -> "⚙️ Auto"
                            "dynamic" -> "🎨 You"
                            else -> "📜 Washi"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clickable {
                                    onUpdatePreferences { it.copy(themeMode = nextThemeMode) }
                                }
                                .testTag("quick_theme_mode_toggle")
                        ) {
                            Text(
                                text = themeModeBadge,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            color = if (showTweaksPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clickable { showTweaksPanel = !showTweaksPanel }
                                .testTag("toggle_tweaks_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Tweaks Studio",
                                    tint = if (showTweaksPanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tweaks",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (showTweaksPanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier
                                .clickable { onOpenCommandCenter() }
                                .testTag("open_command_center_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Command Palette",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⌘K",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = { showWidgetStudio = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .size(38.dp)
                                .testTag("customize_widgets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DashboardCustomize,
                                contentDescription = "Customize 30 Widgets",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subtle Lantern-Glow Daily Summary Banner ("1 of 4 today  ---  25%")
                val activeAccent = remember(preferences.accentPreset) {
                    getAccentPalette(preferences.accentPreset)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    activeAccent.gradientStart.copy(alpha = 0.16f),
                                    activeAccent.gradientEnd.copy(alpha = 0.07f)
                                )
                            )
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$todayHabitsDone of ${todayHabits.size} today • ${activeAccent.label} • ${totalFocusMinutes}m deep focus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)) {
                                        append("$todayHabitsPct")
                                    }
                                    withStyle(SpanStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                        append(" %")
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (todayHabitsPct / 100f).coerceIn(0.04f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                // Expandable Tweaks Card (Japanese Lofi + Pastel & Editorial Themes)
                AnimatedVisibility(visible = showTweaksPanel) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .testTag("tweaks_studio_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tweaks & Japanese Lofi Theme Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { showTweaksPanel = false }) {
                                    Text("✕")
                                }
                            }

                            Text("JAPANESE LOFI & PASTEL PALETTES (100% FREE)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OrbitAccentPalettes.forEach { palette ->
                                    val selected = preferences.accentPreset == palette.key
                                    Surface(
                                        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.clickable {
                                            onUpdatePreferences { it.copy(accentPreset = palette.key) }
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(palette.primary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = palette.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Text("APPEARANCE MODE (WASHI / LIGHT / RAINY-NIGHT DARK / OLED / AUTO)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "paper" to "📜 Washi Paper",
                                    "light" to "☀️ Sunlit Light",
                                    "dark" to "🏮 Rainy Lofi Night",
                                    "oled" to "🌑 True OLED",
                                    "system" to "⚙️ System Auto",
                                    "dynamic" to "🎨 Dynamic"
                                ).forEach { (key, label) ->
                                    val selected = preferences.themeMode == key
                                    Surface(
                                        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.clickable {
                                            onUpdatePreferences { it.copy(themeMode = key) }
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DENSITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("airy", "compact").forEach { d ->
                                            val selected = preferences.density == d
                                            Surface(
                                                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.clickable {
                                                    onUpdatePreferences { it.copy(density = d) }
                                                }
                                            ) {
                                                Text(
                                                    text = d,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("STREAK STYLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("flame", "number", "bar").forEach { st ->
                                            val selected = preferences.streakStyle == st
                                            Surface(
                                                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.clickable {
                                                    onUpdatePreferences { it.copy(streakStyle = st) }
                                                }
                                            ) {
                                                Text(
                                                    text = st,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DATA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .clickable { onResetSeed() }
                                            .testTag("tweaks_reset_seed_button")
                                    ) {
                                        Text(
                                            text = "Reset seed",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("LAYOUT PRESET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        DashboardPresetConfig.entries.take(4).forEach { preset ->
                                            val selected = preferences.dashboardPreset == preset.key
                                            Surface(
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.clickable { onApplyPreset(preset) }
                                            ) {
                                                Text(
                                                    text = preset.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // TODAY Rituals & Habits Section (Matches Reference Design)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${todayHabits.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                ) {
                    Column {
                        todayHabits.forEachIndexed { idx, habit ->
                            val isDone = habit.isCompletedOn(todayEpoch)
                            val weekCount = habit.completionsInLast7Days(todayEpoch)
                            val target = habit.targetDaysPerWeek.coerceAtLeast(1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleHabit(habit) }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = if (preferences.density == "compact") 11.dp else 15.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isDone) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = if (habit.reminderTime.isNotBlank()) "${habit.frequencyLabel} · ${habit.reminderTime}" else habit.frequencyLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Streak Indicator (flame / number / bar)
                                when (preferences.streakStyle) {
                                    "number" -> {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "${habit.currentStreak}d",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    "bar" -> {
                                        LinearProgressIndicator(
                                            progress = { (habit.currentStreak / 30f).coerceIn(0.1f, 1f) },
                                            modifier = Modifier
                                                .width(42.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                    else -> {
                                        Surface(
                                            color = if (habit.currentStreak > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            else Color.Transparent,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (habit.currentStreak > 0) "🔥" else "💧",
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${habit.currentStreak}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (habit.currentStreak > 0) MaterialTheme.colorScheme.onSurface
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // 7/7 Circular Weekly Ring
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val ringProgress = (weekCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                                    val ringColor = MaterialTheme.colorScheme.primary
                                    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = trackColor,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = ringColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f * ringProgress,
                                            useCenter = false,
                                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        text = "$weekCount/$target",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (idx < todayHabits.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }

                // NOT TODAY Rituals Section (Matches Reference Design)
                if (notTodayHabits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOT TODAY",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 1.4.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${notTodayHabits.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    ) {
                        Column {
                            notTodayHabits.forEachIndexed { idx, habit ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleHabit(habit) }
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habit.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = habit.frequencyLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                                if (idx < notTodayHabits.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delightful Completion Banner
        item(key = "celebration_banner") {
            AnimatedVisibility(visible = celebrationPulse && !preferences.reducedMotion) {
                Surface(
                    color = OrbitEmerald.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OrbitEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Momentum Increased! Task saved to local vault ✨",
                            style = MaterialTheme.typography.labelLarge,
                            color = OrbitEmerald
                        )
                    }
                }
            }
        }

        // Dynamic Customizable Widgets from all 30 OrbitWidgetType entries
        items(
            items = preferences.enabledWidgets,
            key = { "widget_$it" }
        ) { widgetKey ->
            val widgetType = OrbitWidgetType.fromKey(widgetKey)
            when (widgetType) {
                OrbitWidgetType.QUICK_ADD -> {
                    QuickCaptureWidgetCard(
                        inlineCaptureText = inlineCaptureText,
                        onTextChange = { inlineCaptureText = it },
                        onSubmit = {
                            if (inlineCaptureText.isNotBlank()) {
                                onQuickAddTask(inlineCaptureText)
                                inlineCaptureText = ""
                            }
                        }
                    )
                }
                OrbitWidgetType.PROGRESS_RING -> {
                    DailyOrbitRingWidget(
                        completedCount = completedTasks.size,
                        totalCount = (completedTasks.size + openTasks.size).coerceAtLeast(1),
                        focusMinutes = totalFocusMinutes,
                        focusGoalMinutes = preferences.dailyFocusGoalMinutes.coerceAtLeast(30),
                        habitsDoneToday = habits.count { it.isCompletedOn(todayEpoch) },
                        habitsTotal = habits.size.coerceAtLeast(1),
                        plannedMinutesToday = todayTasks.sumOf { it.estimatedMinutes }
                    )
                }
                OrbitWidgetType.PRIORITY -> {
                    TopPrioritiesWidget(
                        priorities = topPriorities,
                        onToggleTask = onToggleTask,
                        onSelectTask = onSelectTask,
                        onStartFocus = onStartFocusForTask
                    )
                }
                OrbitWidgetType.TODAY -> {
                    TodayChecklistWidget(
                        title = "Today's Tasks (${todayTasks.size})",
                        tasks = todayTasks.take(6),
                        onToggleTask = onToggleTask,
                        onSelectTask = onSelectTask,
                        onViewAll = { onNavigateToScreen(OrbitScreenDestination.TODAY.route) }
                    )
                }
                OrbitWidgetType.UPCOMING -> {
                    TodayChecklistWidget(
                        title = "Upcoming Tasks (${upcomingTasks.size})",
                        tasks = upcomingTasks.take(5),
                        onToggleTask = onToggleTask,
                        onSelectTask = onSelectTask,
                        onViewAll = { onNavigateToScreen(OrbitScreenDestination.UPCOMING.route) }
                    )
                }
                OrbitWidgetType.OVERDUE -> {
                    OverdueTasksWidget(
                        overdueTasks = overdueTasks,
                        onRescheduleToday = onRescheduleOverdueToToday,
                        onSelectTask = onSelectTask
                    )
                }
                OrbitWidgetType.CALENDAR -> {
                    TimelineScheduleWidget(
                        tasks = todayTasks.filter { it.scheduledStartHour >= 0 }.sortedBy { it.scheduledStartHour },
                        totalEstimatedMinutes = todayTasks.sumOf { it.estimatedMinutes },
                        use24Hour = preferences.use24HourFormat,
                        onOpenCalendar = { onNavigateToScreen(OrbitScreenDestination.CALENDAR.route) },
                        onSelectTask = onSelectTask
                    )
                }
                OrbitWidgetType.MINI_CALENDAR -> {
                    MiniMonthCalendarWidget(
                        tasks = openTasks,
                        todayEpoch = todayEpoch,
                        onOpenFullCalendar = { onNavigateToScreen(OrbitScreenDestination.CALENDAR.route) }
                    )
                }
                OrbitWidgetType.WEEKLY_PROGRESS -> {
                    WeeklyProgressChartWidget(
                        completedTasks = completedTasks,
                        focusSessions = focusSessions,
                        todayEpoch = todayEpoch,
                        onOpenStats = { onNavigateToScreen(OrbitScreenDestination.STATISTICS.route) }
                    )
                }
                OrbitWidgetType.MONTHLY_STATS -> {
                    MonthlyStatisticsWidget(
                        allTasks = activeTasks,
                        habits = habits,
                        focusSessions = focusSessions,
                        onOpenStats = { onNavigateToScreen(OrbitScreenDestination.STATISTICS.route) }
                    )
                }
                OrbitWidgetType.PROJECT -> {
                    ActiveProjectsWidget(
                        projects = projects,
                        tasks = activeTasks,
                        onOpenProjects = { onNavigateToScreen(OrbitScreenDestination.PROJECT_MANAGER.route) }
                    )
                }
                OrbitWidgetType.HABIT -> {
                    HabitStreaksWidget(
                        title = "Daily Habit Tracker",
                        habits = habits,
                        todayEpoch = todayEpoch,
                        onToggleHabit = onToggleHabit,
                        onOpenHabits = { onNavigateToScreen(OrbitScreenDestination.HABIT_TRACKER.route) }
                    )
                }
                OrbitWidgetType.STREAK -> {
                    StreakLeaderboardWidget(
                        habits = habits,
                        onOpenHabits = { onNavigateToScreen(OrbitScreenDestination.HABIT_TRACKER.route) }
                    )
                }
                OrbitWidgetType.FOCUS -> {
                    FocusLaunchpadWidget(
                        modeLabel = "Pomodoro Timer (${preferences.defaultPomodoroMinutes}m)",
                        totalFocusMinutes = totalFocusMinutes,
                        sessionsCount = focusSessions.size,
                        onOpenFocus = { onNavigateToScreen(OrbitScreenDestination.FOCUS_TIMER.route) }
                    )
                }
                OrbitWidgetType.STOPWATCH -> {
                    FocusLaunchpadWidget(
                        modeLabel = "Focus Stopwatch & Deep Work",
                        totalFocusMinutes = totalFocusMinutes,
                        sessionsCount = focusSessions.size,
                        onOpenFocus = { onNavigateToScreen(OrbitScreenDestination.FOCUS_TIMER.route) }
                    )
                }
                OrbitWidgetType.COUNTDOWN, OrbitWidgetType.DEADLINES -> {
                    DeadlineCountdownsWidget(
                        title = if (widgetType == OrbitWidgetType.DEADLINES) "Important P1/P2 Deadlines" else "Milestone Countdowns",
                        tasks = openTasks.filter { it.dueDateEpochDay >= todayEpoch }.take(5),
                        todayEpoch = todayEpoch,
                        onSelectTask = onSelectTask
                    )
                }
                OrbitWidgetType.NOTES -> {
                    QuickNotesScratchpadWidget(
                        notes = preferences.quickNotes,
                        onSaveNotes = onUpdateNotes
                    )
                }
                OrbitWidgetType.SHOPPING_LIST -> {
                    ShoppingListDashboardWidget(
                        tasks = openTasks.filter {
                            it.projectName.contains("Shopping", true) ||
                                it.sectionName.contains("Grocer", true) ||
                                it.tags.contains("shopping")
                        },
                        onToggleTask = onToggleTask,
                        onOpenShoppingScreen = { onNavigateToScreen(OrbitScreenDestination.SHOPPING_LISTS.route) }
                    )
                }
                OrbitWidgetType.WEEK_PLANNER -> {
                    WeekPlannerHorizonWidget(
                        openTasks = openTasks,
                        todayEpoch = todayEpoch,
                        onSelectTask = onSelectTask
                    )
                }
                OrbitWidgetType.RECENTLY_COMPLETED -> {
                    TodayChecklistWidget(
                        title = "Recently Completed (${completedTasks.size})",
                        tasks = completedTasks.take(4),
                        onToggleTask = onToggleTask,
                        onSelectTask = onSelectTask,
                        onViewAll = { onNavigateToScreen(OrbitScreenDestination.ALL_TASKS.route) }
                    )
                }
                OrbitWidgetType.TASKS_BY_CATEGORY -> {
                    TasksByCategoryWidget(
                        tasks = activeTasks,
                        habits = habits
                    )
                }
                OrbitWidgetType.TIME_TRACKING -> {
                    TimeTrackingComparisonWidget(
                        tasks = activeTasks.take(5)
                    )
                }
                OrbitWidgetType.GOAL -> {
                    DailyGoalsWidget(
                        topPriorities = topPriorities.take(3),
                        focusMinutes = totalFocusMinutes,
                        focusGoal = preferences.dailyFocusGoalMinutes,
                        onToggleTask = onToggleTask
                    )
                }
                OrbitWidgetType.CUSTOM_FILTER -> {
                    EnergyMatchmakerWidget(
                        selectedEnergy = preferences.energyFilter,
                        openTasks = openTasks,
                        onSelectEnergy = onSelectEnergyFilter,
                        onSelectTask = onSelectTask,
                        onToggleTask = onToggleTask
                    )
                }
                OrbitWidgetType.EISENHOWER -> {
                    EisenhowerMatrixWidget(
                        openTasks = openTasks,
                        onSelectTask = onSelectTask
                    )
                }
                OrbitWidgetType.BIRTHDAYS -> {
                    SpecialRemindersWidget(
                        tasks = openTasks.filter {
                            it.assigneeName in listOf("Birthday", "Anniversary", "Bill", "Medication") ||
                                it.tags.any { tag -> tag in listOf("birthday", "bills", "medication") }
                        },
                        todayEpoch = todayEpoch,
                        onSelectTask = onSelectTask,
                        onOpenReminders = { onNavigateToScreen(OrbitScreenDestination.REMINDER_CENTER.route) }
                    )
                }
                OrbitWidgetType.ROUTINES -> {
                    RoutineChecklistWidget(
                        habits = habits,
                        todayEpoch = todayEpoch,
                        onToggleHabit = onToggleHabit,
                        onOpenRoutines = { onNavigateToScreen(OrbitScreenDestination.ROUTINES.route) }
                    )
                }
                OrbitWidgetType.QUOTES -> {
                    MotivationalQuotesWidget(
                        quote = CuratedWisdomQuotes[quoteIndex % CuratedWisdomQuotes.size],
                        onNextQuote = { quoteIndex = (quoteIndex + 1) % CuratedWisdomQuotes.size }
                    )
                }
                OrbitWidgetType.SHORTCUTS -> {
                    PersonalizedShortcutsWidget(
                        onNavigateToScreen = onNavigateToScreen,
                        onPinNativeHomeWidget = {
                            OrbitHomeWidgetProvider.requestPinToHomeScreen(context)
                        }
                    )
                }
            }
        }

        // Widget Gallery & Editor Footer
        item(key = "widget_gallery_footer") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToScreen(OrbitScreenDestination.WIDGET_GALLERY.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("30-Widget Gallery")
                }
                Button(
                    onClick = { showWidgetStudio = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Layout")
                }
            }
        }
    }

    if (showWidgetStudio) {
        ModalBottomSheet(
            onDismissRequest = { showWidgetStudio = false }
        ) {
            WidgetCustomizationSheet(
                enabledWidgets = preferences.enabledWidgets,
                onToggleWidget = onToggleWidget,
                onMoveWidget = onMoveWidget,
                onApplyPreset = onApplyPreset
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCaptureWidgetCard(
    inlineCaptureText: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val liveParsed = remember(inlineCaptureText) {
        if (inlineCaptureText.isNotBlank()) SmartTaskParser.parse(inlineCaptureText) else null
    }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inlineCaptureText,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            "Quick capture: \"Pay rent tomorrow at 3 PM priority high #bills\"",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_quick_add_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmit,
                    enabled = inlineCaptureText.isNotBlank(),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("dashboard_quick_add_submit")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
            if (liveParsed != null && liveParsed.summaryChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    liveParsed.summaryChips.forEach { chip ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyOrbitRingWidget(
    completedCount: Int,
    totalCount: Int,
    focusMinutes: Int,
    focusGoalMinutes: Int,
    habitsDoneToday: Int,
    habitsTotal: Int,
    plannedMinutesToday: Int
) {
    val taskRatio = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    val focusRatio = (focusMinutes.toFloat() / focusGoalMinutes.toFloat()).coerceIn(0f, 1f)
    val habitRatio = (habitsDoneToday.toFloat() / habitsTotal.toFloat()).coerceIn(0f, 1f)
    val isOverloaded = plannedMinutesToday > 360

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(104.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 9.dp.toPx()
                        val gap = 12.dp.toPx()

                        drawArc(
                            color = OrbitIndigo.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = OrbitIndigo,
                            startAngle = -90f,
                            sweepAngle = 360f * taskRatio,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        val midInset = gap
                        drawArc(
                            color = OrbitCyan.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(midInset, midInset),
                            size = androidx.compose.ui.geometry.Size(size.width - midInset * 2, size.height - midInset * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = OrbitCyan,
                            startAngle = -90f,
                            sweepAngle = 360f * focusRatio,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(midInset, midInset),
                            size = androidx.compose.ui.geometry.Size(size.width - midInset * 2, size.height - midInset * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        val inInset = gap * 2
                        drawArc(
                            color = OrbitEmerald.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inInset, inInset),
                            size = androidx.compose.ui.geometry.Size(size.width - inInset * 2, size.height - inInset * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = OrbitEmerald,
                            startAngle = -90f,
                            sweepAngle = 360f * habitRatio,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inInset, inInset),
                            size = androidx.compose.ui.geometry.Size(size.width - inInset * 2, size.height - inInset * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(taskRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Daily Progress Ring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    MetricLegendRow(color = OrbitIndigo, label = "Tasks Completed", value = "$completedCount / $totalCount")
                    MetricLegendRow(color = OrbitCyan, label = "Focus Logged", value = "${focusMinutes}m / ${focusGoalMinutes}m")
                    MetricLegendRow(color = OrbitEmerald, label = "Daily Habits", value = "$habitsDoneToday / $habitsTotal")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = if (isOverloaded) OrbitAmber.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isOverloaded) {
                            "⚠️ Workload Alert: ${plannedMinutesToday / 60}h ${plannedMinutesToday % 60}m planned today"
                        } else {
                            "✓ Workload Balance: ${plannedMinutesToday / 60}h ${plannedMinutesToday % 60}m scheduled today"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverloaded) OrbitAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricLegendRow(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopPrioritiesWidget(
    priorities: List<OrbitTask>,
    onToggleTask: (OrbitTask) -> Unit,
    onSelectTask: (OrbitTask) -> Unit,
    onStartFocus: (OrbitTask) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = PriorityP1Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top Priority Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                priorities.firstOrNull()?.let { first ->
                    TextButton(onClick = { onStartFocus(first) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Focus #1")
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (priorities.isEmpty()) {
                Text(
                    text = "No open priority tasks. Capture your top outcomes above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                priorities.forEachIndexed { idx, task ->
                    CompactTaskRow(
                        task = task,
                        rankBadge = "#${idx + 1}",
                        onToggle = { onToggleTask(task) },
                        onClick = { onSelectTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverdueTasksWidget(
    overdueTasks: List<OrbitTask>,
    onRescheduleToday: (OrbitTask) -> Unit,
    onSelectTask: (OrbitTask) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (overdueTasks.isNotEmpty()) OrbitRose.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (overdueTasks.isNotEmpty()) OrbitRose else OrbitEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (overdueTasks.isEmpty()) "Zero Overdue Tasks 🎉" else "Overdue Tasks (${overdueTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (overdueTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                overdueTasks.take(4).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTask(task) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("Due ${SmartTaskParser.formatEpochDay(task.dueDateEpochDay)}", style = MaterialTheme.typography.labelSmall, color = OrbitRose)
                        }
                        TextButton(onClick = { onRescheduleToday(task) }) {
                            Text("Move to Today")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayChecklistWidget(
    title: String,
    tasks: List<OrbitTask>,
    onToggleTask: (OrbitTask) -> Unit,
    onSelectTask: (OrbitTask) -> Unit,
    onViewAll: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onViewAll) {
                    Text("Open View")
                }
            }
            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks in this view right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tasks.forEach { task ->
                    CompactTaskRow(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onClick = { onSelectTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMonthCalendarWidget(
    tasks: List<OrbitTask>,
    todayEpoch: Long,
    onOpenFullCalendar: () -> Unit
) {
    val today = remember(todayEpoch) { LocalDate.ofEpochDay(todayEpoch) }
    val yearMonth = remember(today) { YearMonth.from(today) }
    val daysInMonth = yearMonth.lengthOfMonth()
    val taskDays = remember(tasks) {
        tasks.flatMap { listOf(it.dueDateEpochDay, it.scheduledDateEpochDay) }.filter { it > 0 }.toSet()
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrbitCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onOpenFullCalendar) { Text("Full Calendar") }
            }
            Spacer(modifier = Modifier.height(6.dp))
            val rows = (1..daysInMonth).chunked(7)
            rows.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { dayNum ->
                        val epoch = yearMonth.atDay(dayNum).toEpochDay()
                        val isToday = epoch == todayEpoch
                        val hasTasks = taskDays.contains(epoch)
                        Surface(
                            color = when {
                                isToday -> MaterialTheme.colorScheme.primary
                                hasTasks -> OrbitCyan.copy(alpha = 0.18f)
                                else -> Color.Transparent
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyProgressChartWidget(
    completedTasks: List<OrbitTask>,
    focusSessions: List<FocusSessionRecord>,
    todayEpoch: Long,
    onOpenStats: () -> Unit
) {
    val days = remember(todayEpoch) { (6 downTo 0).map { todayEpoch - it } }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Progress Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenStats) { Text("Analytics") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { dayEpoch ->
                    val date = LocalDate.ofEpochDay(dayEpoch)
                    val count = completedTasks.count { it.dueDateEpochDay == dayEpoch || it.scheduledDateEpochDay == dayEpoch } +
                        focusSessions.count { it.completedEpochDay == dayEpoch }
                    val barHeight = (16 + count * 14).coerceAtMost(72).dp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dayEpoch == todayEpoch) OrbitCyan else OrbitIndigo)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EE")).take(2),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyStatisticsWidget(
    allTasks: List<OrbitTask>,
    habits: List<OrbitHabit>,
    focusSessions: List<FocusSessionRecord>,
    onOpenStats: () -> Unit
) {
    val completed = allTasks.count { it.isCompleted }
    val total = allTasks.size.coerceAtLeast(1)
    val focusHours = focusSessions.sumOf { it.durationMinutes } / 60f
    val bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Monthly Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenStats) { Text("Full Report") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill("Completion", "${(completed * 100) / total}%", OrbitEmerald)
                StatPill("Focus Time", String.format("%.1fh", focusHours), OrbitCyan)
                StatPill("Best Streak", "${bestStreak}d", OrbitAmber)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HabitStreaksWidget(
    title: String,
    habits: List<OrbitHabit>,
    todayEpoch: Long,
    onToggleHabit: (OrbitHabit) -> Unit,
    onOpenHabits: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = OrbitAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenHabits) { Text("Habits") }
            }
            habits.forEach { habit ->
                val doneToday = habit.isCompletedOn(todayEpoch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHabit(habit) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (doneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (doneToday) OrbitEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(habit.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${habit.routineType.uppercase()} • 🔥 ${habit.currentStreak}d streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakLeaderboardWidget(
    habits: List<OrbitHabit>,
    onOpenHabits: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Habit Streaks & Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenHabits) { Text("Details") }
            }
            habits.sortedByDescending { it.currentStreak }.forEach { h ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(h.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("🔥 ${h.currentStreak}d (Best ${h.bestStreak}d)", style = MaterialTheme.typography.labelSmall, color = OrbitAmber)
                }
            }
        }
    }
}

@Composable
private fun QuickNotesScratchpadWidget(
    notes: String,
    onSaveNotes: (String) -> Unit
) {
    var draft by remember(notes) { mutableStateOf(notes) }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = OrbitCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Notes & Scratchpad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (draft != notes) {
                    TextButton(onClick = { onSaveNotes(draft) }) {
                        Text("Save Note")
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    onSaveNotes(it)
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShoppingListDashboardWidget(
    tasks: List<OrbitTask>,
    onToggleTask: (OrbitTask) -> Unit,
    onOpenShoppingScreen: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = OrbitEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shopping List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenShoppingScreen) { Text("Aisles") }
            }
            if (tasks.isEmpty()) {
                Text("Shopping list is clear.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tasks.take(3).forEach { t ->
                    CompactTaskRow(task = t, onToggle = { onToggleTask(t) }, onClick = onOpenShoppingScreen)
                }
            }
        }
    }
}

@Composable
private fun WeekPlannerHorizonWidget(
    openTasks: List<OrbitTask>,
    todayEpoch: Long,
    onSelectTask: (OrbitTask) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Weekly Planner (Next 7 Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            (0..6).forEach { offset ->
                val day = todayEpoch + offset
                val dayTasks = openTasks.filter { it.dueDateEpochDay == day || it.scheduledDateEpochDay == day }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = SmartTaskParser.formatEpochDay(day, todayEpoch),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (offset == 0) OrbitCyan else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(90.dp)
                    )
                    Text(
                        text = if (dayTasks.isEmpty()) "Free day" else dayTasks.joinToString { it.title },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { dayTasks.firstOrNull()?.let(onSelectTask) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${dayTasks.sumOf { it.estimatedMinutes }}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitIndigo
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeTrackingComparisonWidget(tasks: List<OrbitTask>) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time Tracking (Estimated vs. Actual)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            tasks.forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(t.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(
                        text = "Est ${t.estimatedMinutes}m • Actual ${t.actualMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyGoalsWidget(
    topPriorities: List<OrbitTask>,
    focusMinutes: Int,
    focusGoal: Int,
    onToggleTask: (OrbitTask) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Goals & Focus Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Focus Target: ${focusMinutes}m / ${focusGoal}m",
                style = MaterialTheme.typography.bodySmall,
                color = OrbitCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            topPriorities.forEach { t ->
                CompactTaskRow(task = t, onToggle = { onToggleTask(t) }, onClick = {})
            }
        }
    }
}

@Composable
private fun SpecialRemindersWidget(
    tasks: List<OrbitTask>,
    todayEpoch: Long,
    onSelectTask: (OrbitTask) -> Unit,
    onOpenReminders: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cake, contentDescription = null, tint = OrbitAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Birthdays, Bills & Medications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenReminders) { Text("Reminder Center") }
            }
            if (tasks.isEmpty()) {
                Text("No upcoming special reminders.", style = MaterialTheme.typography.bodySmall)
            } else {
                tasks.take(4).forEach { t ->
                    val days = (t.dueDateEpochDay - todayEpoch).coerceAtLeast(0)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTask(t) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${t.assigneeName.ifBlank { "Alert" }}: ${t.title}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(if (days == 0L) "Today" else "In ${days}d", style = MaterialTheme.typography.labelSmall, color = OrbitAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineChecklistWidget(
    habits: List<OrbitHabit>,
    todayEpoch: Long,
    onToggleHabit: (OrbitHabit) -> Unit,
    onOpenRoutines: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Morning & Evening Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenRoutines) { Text("All Routines") }
            }
            habits.take(3).forEach { h ->
                val done = h.isCompletedOn(todayEpoch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHabit(h) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) OrbitEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${h.routineType.uppercase()}: ${h.title}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MotivationalQuotesWidget(
    quote: String,
    onNextQuote: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = OrbitCyan)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = quote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextQuote) {
                Icon(Icons.Default.Refresh, contentDescription = "Next Quote", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalizedShortcutsWidget(
    onNavigateToScreen: (String) -> Unit,
    onPinNativeHomeWidget: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All 22 Screens • Quick Jump", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onPinNativeHomeWidget) {
                    Text("+ Pin Android Widget")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OrbitScreenDestination.entries.forEach { dest ->
                    FilterChip(
                        selected = false,
                        onClick = { onNavigateToScreen(dest.route) },
                        label = { Text(dest.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyMatchmakerWidget(
    selectedEnergy: String,
    openTasks: List<OrbitTask>,
    onSelectEnergy: (String) -> Unit,
    onSelectTask: (OrbitTask) -> Unit,
    onToggleTask: (OrbitTask) -> Unit
) {
    val filtered = remember(openTasks, selectedEnergy) {
        if (selectedEnergy == "all") openTasks.take(4)
        else openTasks.filter { it.energyLevel.equals(selectedEnergy, ignoreCase = true) }.take(4)
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = OrbitAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom Filter & Energy Matchmaker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "low" to "🔋 Low", "medium" to "⚡ Medium", "high" to "🚀 High").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedEnergy == key,
                        onClick = { onSelectEnergy(key) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            filtered.forEach { task ->
                CompactTaskRow(task = task, onToggle = { onToggleTask(task) }, onClick = { onSelectTask(task) })
            }
        }
    }
}

@Composable
private fun EisenhowerMatrixWidget(
    openTasks: List<OrbitTask>,
    onSelectTask: (OrbitTask) -> Unit
) {
    val doFirst = openTasks.filter { it.isImportant && it.isUrgent }
    val schedule = openTasks.filter { it.isImportant && !it.isUrgent }
    val delegate = openTasks.filter { !it.isImportant && it.isUrgent }
    val eliminate = openTasks.filter { !it.isImportant && !it.isUrgent }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Eisenhower 4-Quadrant Decision Matrix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuadrantCell(
                    title = "Q1 • Do First",
                    color = PriorityP1Red,
                    tasks = doFirst,
                    onSelectTask = onSelectTask,
                    modifier = Modifier.weight(1f)
                )
                QuadrantCell(
                    title = "Q2 • Schedule",
                    color = OrbitIndigo,
                    tasks = schedule,
                    onSelectTask = onSelectTask,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuadrantCell(
                    title = "Q3 • Delegate",
                    color = PriorityP2Amber,
                    tasks = delegate,
                    onSelectTask = onSelectTask,
                    modifier = Modifier.weight(1f)
                )
                QuadrantCell(
                    title = "Q4 • Someday",
                    color = PriorityP4Slate,
                    tasks = eliminate,
                    onSelectTask = onSelectTask,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuadrantCell(
    title: String,
    color: Color,
    tasks: List<OrbitTask>,
    onSelectTask: (OrbitTask) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                Text(text = "${tasks.size}", style = MaterialTheme.typography.labelSmall, color = color)
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (tasks.isEmpty()) {
                Text(text = "Empty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tasks.take(2).forEach { t ->
                    Text(
                        text = "• ${t.title}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTask(t) }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineScheduleWidget(
    tasks: List<OrbitTask>,
    totalEstimatedMinutes: Int,
    use24Hour: Boolean,
    onOpenCalendar: () -> Unit,
    onSelectTask: (OrbitTask) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = OrbitCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Time-Blocked Schedule (${totalEstimatedMinutes}m)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenCalendar) { Text("Timeline") }
            }
            if (tasks.isEmpty()) {
                Text(
                    text = "No time-blocked tasks yet. Open Calendar to assign hours manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tasks.take(4).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTask(task) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SmartTaskParser.formatHourMinute(task.scheduledStartHour, task.scheduledStartMinute, use24Hour),
                            style = MaterialTheme.typography.labelMedium,
                            color = OrbitCyan,
                            modifier = Modifier.width(76.dp)
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.estimatedMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusLaunchpadWidget(
    modeLabel: String,
    totalFocusMinutes: Int,
    sessionsCount: Int,
    onOpenFocus: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(modeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "$sessionsCount sessions logged • ${totalFocusMinutes}m total focus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onOpenFocus) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Timer")
            }
        }
    }
}

@Composable
private fun ActiveProjectsWidget(
    projects: List<OrbitProject>,
    tasks: List<OrbitTask>,
    onOpenProjects: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Project Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenProjects) { Text("Manage") }
            }
            projects.forEach { proj ->
                val projTasks = tasks.filter { it.projectId == proj.id }
                val done = projTasks.count { it.isCompleted }
                val ratio = if (projTasks.isNotEmpty()) done.toFloat() / projTasks.size else 0f
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${proj.name} ${if (proj.isShared) "👥" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$done/${projTasks.size} tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadlineCountdownsWidget(
    title: String,
    tasks: List<OrbitTask>,
    todayEpoch: Long,
    onSelectTask: (OrbitTask) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            tasks.forEach { task ->
                val daysLeft = (task.dueDateEpochDay - todayEpoch).coerceAtLeast(0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTask(task) }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(task.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                    Surface(
                        color = if (daysLeft == 0L) OrbitRose.copy(alpha = 0.18f) else OrbitCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (daysLeft == 0L) "Due Today" else "In ${daysLeft}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (daysLeft == 0L) OrbitRose else OrbitCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksByCategoryWidget(
    tasks: List<OrbitTask>,
    habits: List<OrbitHabit>
) {
    val workCount = tasks.count { it.projectName.contains("Product", true) || it.tags.contains("deepwork") }.coerceAtLeast(1)
    val healthCount = (tasks.count { it.tags.contains("health") } + habits.size).coerceAtLeast(1)
    val householdCount = tasks.count { it.projectName.contains("Household", true) || it.tags.contains("shopping") }.coerceAtLeast(1)
    val total = (workCount + healthCount + householdCount).toFloat()

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = OrbitEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tasks by Category & Life Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            MetricLegendRow(OrbitIndigo, "Work & Engineering", "${((workCount / total) * 100).toInt()}% ($workCount)")
            Spacer(modifier = Modifier.height(4.dp))
            MetricLegendRow(OrbitEmerald, "Health, Fitness & Habits", "${((healthCount / total) * 100).toInt()}% ($healthCount)")
            Spacer(modifier = Modifier.height(4.dp))
            MetricLegendRow(OrbitAmber, "Household, Bills & Shopping", "${((householdCount / total) * 100).toInt()}% ($householdCount)")
        }
    }
}

@Composable
fun CompactTaskRow(
    task: OrbitTask,
    rankBadge: String? = null,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Complete task",
                tint = if (task.isCompleted) OrbitEmerald else priorityColor(task.priority)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rankBadge != null) {
                    Text(
                        text = "$rankBadge ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val metaParts = buildList {
                add(task.projectName)
                if (task.dueDateEpochDay > 0) add(SmartTaskParser.formatEpochDay(task.dueDateEpochDay))
                if (task.scheduledStartHour >= 0) add(SmartTaskParser.formatHourMinute(task.scheduledStartHour, task.scheduledStartMinute))
                add("${task.estimatedMinutes}m")
                if (task.subtasks.isNotEmpty()) {
                    val doneSub = task.parsedSubtasks().count { it.isDone }
                    add("☑ $doneSub/${task.subtasks.size}")
                }
                if (task.recurrenceRule.isNotBlank()) add("↻ ${task.recurrenceRule}")
            }
            Text(
                text = metaParts.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        PriorityBadge(priority = task.priority)
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = priorityColor(priority)
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun priorityColor(priority: String): Color = when (priority.lowercase()) {
    "p1" -> PriorityP1Red
    "p2" -> PriorityP2Amber
    "p3" -> PriorityP3Blue
    else -> PriorityP4Slate
}

@Composable
fun WidgetCustomizationSheet(
    enabledWidgets: List<String>,
    onToggleWidget: (String) -> Unit,
    onMoveWidget: (String, Boolean) -> Unit,
    onApplyPreset: (DashboardPresetConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "30-Widget Gallery & Layout Editor",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "All 30 widgets are 100% free and unlocked. Toggle, reorder, or apply layout presets.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

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
                    label = { Text(preset.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.height(420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(OrbitWidgetType.entries, key = { it.key }) { widget ->
                val isEnabled = enabledWidgets.contains(widget.key)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${widget.number} ${widget.title} (${widget.defaultSize})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${widget.category} • ${widget.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                }
            }
        }
    }
}
