package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class Timestamp(
    val seconds: Long = System.currentTimeMillis() / 1000L,
    val nanoseconds: Int = 0
) {
    companion object {
        fun now(): Timestamp = Timestamp(System.currentTimeMillis() / 1000L, 0)
    }
}

enum class RestoreDuplicateStrategy(val key: String, val label: String, val description: String) {
    REPLACE_WORKSPACE(
        "replace",
        "Replace Workspace (Atomic)",
        "Replaces local workspace tables atomically inside a single SQLite transaction"
    ),
    MERGE_SKIP_DUPLICATES(
        "merge_skip",
        "Merge & Skip Duplicates",
        "Preserves all existing local items and imports only non-duplicate IDs from the backup"
    )
}

data class BackupPreviewSummary(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val taskCount: Int,
    val duplicateTaskCount: Int,
    val projectCount: Int,
    val habitCount: Int,
    val focusSessionCount: Int,
    val automationCount: Int,
    val hasPreferences: Boolean,
    val rawJson: String
)

data class OrbitTask(
    val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val description: String = "",
    val status: String = "todo", // inbox, todo, in_progress, done, archived, trash
    val priority: String = "p3", // p1, p2, p3, p4
    val projectId: String = "proj_personal",
    val projectName: String = "Personal",
    val sectionName: String = "General",
    val tags: List<String> = emptyList(),
    val startDateEpochDay: Long = 0L, // Start date separate from due date
    val dueDateEpochDay: Long = 0L,
    val deadlineEpochDay: Long = 0L, // Hard deadline separate from scheduled time
    val scheduledDateEpochDay: Long = 0L,
    val scheduledStartHour: Int = -1,
    val scheduledStartMinute: Int = 0,
    val estimatedMinutes: Int = 25,
    val actualMinutes: Int = 0,
    val energyLevel: String = "medium", // low, medium, high
    val recurrenceRule: String = "", // "", DAILY, WEEKDAYS, WEEKENDS, WEEKLY, MONTHLY, YEARLY, EVERY_3_DAYS, AFTER_COMPLETION, DAYS_MON_WED_FRI, DAYS_TUE_THU_SAT
    val reminderOffsetMinutes: Int = 15, // Primary reminder offset (-1 if none, 0 at time, 15, 30, 60, 1440)
    val multipleReminderOffsets: List<Int> = listOf(15), // Multiple reminders per task
    val reminderEscalation: String = "standard", // standard, persistent, critical
    val locationTrigger: String = "",
    val postponeCount: Int = 0,
    val isFavorite: Boolean = false,
    val isImportant: Boolean = false,
    val isUrgent: Boolean = false,
    val isPinned: Boolean = false,
    val subtasks: List<String> = emptyList(), // Format: "0|Step title" or "1|Step title" or "0|1|Nested step"
    val dependsOnTaskId: String = "",
    val assigneeName: String = "", // Used for Reminder Category (Birthday, Bill, Medication, Note, etc.) or Assignee
    val comments: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap(),
    val historyLog: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isCompleted: Boolean
        get() = status == "done"

    val isTrashed: Boolean
        get() = status == "trash"

    val isArchived: Boolean
        get() = status == "archived"

    fun parsedSubtasks(): List<SubtaskItem> = subtasks.mapIndexed { index, raw ->
        val parts = raw.split("|")
        when {
            parts.size >= 3 -> SubtaskItem(
                index = index,
                isDone = parts[0] == "1",
                indentLevel = parts[1].toIntOrNull()?.coerceIn(0, 3) ?: 0,
                title = parts.drop(2).joinToString("|")
            )
            parts.size == 2 -> SubtaskItem(
                index = index,
                isDone = parts[0] == "1",
                indentLevel = 0,
                title = parts[1]
            )
            else -> SubtaskItem(index = index, isDone = false, indentLevel = 0, title = raw)
        }
    }
}

data class SubtaskItem(
    val index: Int,
    val isDone: Boolean,
    val title: String,
    val indentLevel: Int = 0
) {
    fun encode(): String = if (indentLevel > 0) {
        "${if (isDone) "1" else "0"}|$indentLevel|$title"
    } else {
        "${if (isDone) "1" else "0"}|$title"
    }
}

data class OrbitProject(
    val id: String = "",
    val userId: String = "local_user",
    val name: String = "",
    val description: String = "",
    val colorHex: String = "#C86D51",
    val iconName: String = "folder",
    val category: String = "work", // work, personal, health, education, finance, shopping, routines
    val isShared: Boolean = false,
    val members: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class OrbitHabit(
    val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val description: String = "",
    val routineType: String = "morning", // morning, evening, anytime, custom
    val category: String = "health",
    val frequencyLabel: String = "Daily", // Daily, Weekdays, Weekends, Tue · Thu · Sat, Mon · Wed · Fri, Weekly, Monthly
    val targetDaysPerWeek: Int = 7,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakFreezesRemaining: Int = 2,
    val completedEpochDays: List<Long> = emptyList(),
    val frozenEpochDays: List<Long> = emptyList(),
    val reminderTime: String = "07:00",
    val iconName: String = "flame",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun isCompletedOn(epochDay: Long): Boolean = completedEpochDays.contains(epochDay)

    fun completionsInLast7Days(todayEpochDay: Long): Int =
        (0L..6L).count { offset -> completedEpochDays.contains(todayEpochDay - offset) }

    fun isScheduledForDate(date: LocalDate): Boolean {
        val dow = date.dayOfWeek
        val norm = frequencyLabel.lowercase(Locale.US)
        return when {
            norm.contains("weekday") -> dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
            norm.contains("weekend") -> dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
            norm.contains("tue") && norm.contains("thu") ->
                dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY || dow == DayOfWeek.SATURDAY
            norm.contains("mon") && norm.contains("wed") ->
                dow == DayOfWeek.MONDAY || dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.FRIDAY
            else -> true
        }
    }
}

data class FocusSessionRecord(
    val id: String = "",
    val userId: String = "local_user",
    val taskId: String = "",
    val taskTitle: String = "",
    val mode: String = "pomodoro", // pomodoro, deep_work, stopwatch, short_break, long_break
    val durationMinutes: Int = 25,
    val ambientSound: String = "Cosmic Rain",
    val completedEpochDay: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AutomationRule(
    val id: String = "",
    val userId: String = "local_user",
    val title: String = "",
    val triggerType: String = "When a task is completed",
    val conditionText: String = "Task has dependent tasks",
    val actionType: String = "Unlock dependent task & notify",
    val isEnabled: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class UserPreferencesDoc(
    val id: String = "default",
    val userId: String = "local_user",
    val themeMode: String = "paper", // paper, light, dark, oled, system, dynamic
    val accentPreset: String = "terracotta", // terracotta, sage, sky, lavender, clay, orbit_indigo, cyber_cyan, emerald_zen, sunset_amber, rose_quartz, royal_violet
    val density: String = "airy", // airy, comfortable, compact
    val streakStyle: String = "flame", // flame, number, bar
    val cornerRadiusDp: Int = 22,
    val dashboardPreset: String = "mission_control",
    val planningMode: String = "manual", // manual, time_blocked, zen
    val energyFilter: String = "all", // all, low, medium, high
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val gamificationEnabled: Boolean = true,
    val enabledWidgets: List<String> = defaultWidgetOrder(),
    val favoriteWidgets: List<String> = listOf("today", "habit", "progress_ring", "focus", "mini_calendar"),
    val widgetSizes: Map<String, String> = emptyMap(), // widgetKey -> Compact, Medium, Large, Full
    val homeWidgetVariant: String = "today_checklist", // 10 native home widget variants
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
    val quickNotes: String = "• Capture ideas in 2 seconds\n• Review weekly milestones every Friday\n• 100% Free & Offline-First",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun defaultWidgetOrder(): List<String> = listOf(
            "progress_ring",
            "quick_add",
            "today",
            "habit",
            "streak",
            "priority",
            "upcoming",
            "overdue",
            "mini_calendar",
            "focus",
            "eisenhower",
            "project",
            "shopping_list",
            "birthdays",
            "routines",
            "notes",
            "weekly_progress",
            "quotes",
            "shortcuts"
        )
    }
}

/**
 * 10 Native Android Home-Screen Widget Variants required by Part 3.
 */
enum class NativeHomeWidgetVariant(
    val key: String,
    val number: Int,
    val title: String,
    val description: String
) {
    TODAY_CHECKLIST("today_checklist", 1, "Today's Checklist", "Interactive today tasks & 1-tap complete"),
    QUICK_CAPTURE("quick_capture", 2, "Quick Task Capture", "Fast 2-second task entry & inbox counter"),
    MINI_CALENDAR("mini_calendar", 3, "Mini Calendar", "Current month overview & today's time blocks"),
    UPCOMING_REMINDERS("upcoming_reminders", 4, "Upcoming Reminders", "Next scheduled alarms, bills & birthdays"),
    HABIT_PROGRESS("habit_progress", 5, "Habit Progress", "Daily rituals, flame streaks & 7/7 rings"),
    PROGRESS_RING("progress_ring", 6, "Daily Progress Ring", "Task completion %, focus minutes & habits"),
    IMPORTANT_TASKS("important_tasks", 7, "Important Tasks", "Top P1 & P2 high-impact priorities"),
    DEADLINE_COUNTDOWN("deadline_countdown", 8, "Deadline Countdown", "Days remaining until key milestones"),
    FOCUS_STATUS("focus_status", 9, "Focus Session Status", "Pomodoro quota & deep work minutes logged"),
    PROJECT_PROGRESS("project_progress", 10, "Project Progress", "Active project completion bars & health");

    companion object {
        fun fromKey(key: String): NativeHomeWidgetVariant =
            entries.firstOrNull { it.key == key } ?: TODAY_CHECKLIST
    }
}

/**
 * All 22 Screens in Orbit.
 */
enum class OrbitScreenDestination(
    val route: String,
    val title: String,
    val subtitle: String,
    val group: String
) {
    DASHBOARD("dashboard", "Main Dashboard", "30 customizable widgets & editorial daily view", "Core"),
    INBOX("inbox", "Inbox", "Quick captures awaiting organization", "Tasks"),
    TODAY("today", "Today", "Tasks & reminders scheduled for today", "Tasks"),
    TOMORROW("tomorrow", "Tomorrow", "Next-day preparation & time blocks", "Tasks"),
    UPCOMING("upcoming", "Upcoming", "7-day & future horizon tasks", "Tasks"),
    ALL_TASKS("all_tasks", "All Tasks", "Complete filterable task database", "Tasks"),
    CALENDAR("calendar", "Calendar 2.0", "Day, Week, Month, Year, Agenda & Timeline", "Planning"),
    TASK_DETAILS("task_details", "Task Templates & Inspector", "Templates, dependencies & checklists", "Tasks"),
    PROJECT_MANAGER("projects", "Project Manager", "Folders, sections & color-coded spaces", "Organization"),
    KANBAN_BOARD("kanban", "Kanban Board", "Status columns & workflow board", "Organization"),
    HABIT_TRACKER("habits", "Habits & Routines 2.0", "Heatmaps, streak freezes & Today/Not Today", "Habits"),
    FOCUS_TIMER("focus", "Focus Cockpit 2.0", "Accurate Pomodoro, stopwatch & ambient mixer", "Focus"),
    STATISTICS("statistics", "Advanced Statistics", "Charts, completion heatmaps & weekly review", "Insights"),
    WIDGET_GALLERY("widget_gallery", "Widget Gallery 2.0", "All 30 dashboard widgets + 10 OS widgets", "Customization"),
    WIDGET_EDITOR("widget_editor", "Widget Editor", "Reorder, resize & style active widgets", "Customization"),
    THEMES_STUDIO("themes_studio", "Theme Studio & Tweaks", "Paper, Dark, OLED, Dynamic, accents & density", "Customization"),
    REMINDER_CENTER("reminder_center", "Reminder Engine 2.0", "AlarmManager, snooze, quiet hours & boot sync", "Planning"),
    SEARCH("search", "Universal Search", "Search tasks, habits, projects, notes & tags", "Tasks"),
    SHOPPING_LISTS("shopping_lists", "Shopping Lists 2.0", "Auto-grouped aisles, quantities & text export", "Life"),
    ROUTINES("routines", "Routines", "Morning, evening & recurring step checklists", "Habits"),
    SETTINGS("settings", "Settings", "Formats, shortcuts, accessibility & timers", "System"),
    BACKUP_RESTORE("backup_restore", "Backup Vault & Restore", "Transactional JSON, CSV, ICS & seed reset", "System");

    companion object {
        fun fromRoute(route: String): OrbitScreenDestination =
            entries.firstOrNull { it.route == route } ?: DASHBOARD
    }
}

/**
 * All 30 Customizable Dashboard Widgets.
 */
enum class OrbitWidgetType(
    val key: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val category: String,
    val defaultSize: String = "Medium" // Compact, Medium, Large, Full
) {
    TODAY("today", 1, "Today's Tasks", "Interactive checklist for today", "Tasks", "Large"),
    UPCOMING("upcoming", 2, "Upcoming Tasks", "Next 7 days of scheduled items", "Tasks", "Medium"),
    OVERDUE("overdue", 3, "Overdue Tasks", "Past-due items with 1-tap reschedule", "Tasks", "Compact"),
    CALENDAR("calendar", 4, "Calendar", "Hourly time-blocking & events", "Calendar", "Large"),
    MINI_CALENDAR("mini_calendar", 5, "Mini Calendar", "Interactive monthly date grid", "Calendar", "Medium"),
    PROGRESS_RING("progress_ring", 6, "Daily Progress Ring", "Concentric task, focus & habit rings", "Analytics", "Large"),
    WEEKLY_PROGRESS("weekly_progress", 7, "Weekly Progress", "7-day bar chart of completions", "Analytics", "Medium"),
    MONTHLY_STATS("monthly_stats", 8, "Monthly Statistics", "30-day completion rate & velocity", "Analytics", "Medium"),
    QUICK_ADD("quick_add", 9, "Quick Add Task", "Deterministic natural-language capture", "Tasks", "Compact"),
    PRIORITY("priority", 10, "Priority Tasks", "Top P1 & P2 critical outcomes", "Tasks", "Medium"),
    PROJECT("project", 11, "Project Progress", "Completion bars for active projects", "Organization", "Medium"),
    HABIT("habit", 12, "Habit Tracker", "One-tap daily habit check-ins", "Habits", "Medium"),
    STREAK("streak", 13, "Habit Streaks", "Current & best streak leaderboard", "Habits", "Compact"),
    FOCUS("focus", 14, "Pomodoro Timer", "Quick-launch 25m focus session", "Focus", "Medium"),
    STOPWATCH("stopwatch", 15, "Focus Stopwatch", "Open-ended time tracker", "Focus", "Compact"),
    COUNTDOWN("countdown", 16, "Countdown Timer", "Days remaining until key milestones", "Planning", "Medium"),
    DEADLINES("deadlines", 17, "Important Deadlines", "Strict P1/P2 due dates", "Planning", "Medium"),
    NOTES("notes", 18, "Notes", "Instant scratchpad & pinned memos", "Life", "Medium"),
    SHOPPING_LIST("shopping_list", 19, "Shopping List", "Categorized grocery & errand items", "Life", "Medium"),
    WEEK_PLANNER("week_planner", 20, "Weekly Planner", "Day-by-day workload balance", "Planning", "Large"),
    RECENTLY_COMPLETED("recently_completed", 21, "Recently Completed", "Completed log with 1-tap undo", "Tasks", "Compact"),
    TASKS_BY_CATEGORY("tasks_by_category", 22, "Tasks by Category", "Distribution across Work, Health & Home", "Organization", "Medium"),
    TIME_TRACKING("time_tracking", 23, "Time Tracking", "Estimated vs. actual minutes logged", "Focus", "Medium"),
    GOAL("goal", 24, "Daily Goals", "Top 3 daily targets & focus quota", "Planning", "Medium"),
    CUSTOM_FILTER("custom_filter", 25, "Custom Filter Widgets", "Saved smart filter (#deepwork / High Energy)", "Organization", "Medium"),
    EISENHOWER("eisenhower", 26, "Eisenhower Matrix", "Urgent vs. Important 4-quadrant grid", "Organization", "Large"),
    BIRTHDAYS("birthdays", 27, "Upcoming Birthdays", "Birthdays, anniversaries & bills", "Life", "Compact"),
    ROUTINES("routines", 28, "Routine Checklist", "Morning & evening ritual steps", "Habits", "Medium"),
    QUOTES("quotes", 29, "Motivational Quotes", "Curated timeless wisdom", "Life", "Compact"),
    SHORTCUTS("shortcuts", 30, "Personalized Shortcuts", "1-tap jump to any of the 22 screens", "System", "Medium");

    companion object {
        fun fromKey(key: String): OrbitWidgetType =
            entries.firstOrNull { it.key == key } ?: TODAY
    }
}

enum class DashboardPresetConfig(
    val key: String,
    val label: String,
    val tagline: String,
    val widgets: List<String>
) {
    MISSION_CONTROL(
        "mission_control",
        "Mission Control",
        "Balanced command center with core productivity widgets",
        UserPreferencesDoc.defaultWidgetOrder()
    ),
    MINIMAL(
        "minimal",
        "Minimal Zen",
        "Calm, distraction-free editorial essentials",
        listOf("progress_ring", "quick_add", "priority", "today", "habit", "focus", "quotes")
    ),
    STUDENT(
        "student",
        "Student Studio",
        "Deadlines, study Pomodoros, mini calendar & course tracking",
        listOf("progress_ring", "quick_add", "deadlines", "countdown", "today", "mini_calendar", "focus", "habit", "notes")
    ),
    PROFESSIONAL(
        "professional",
        "Executive Pro",
        "Priority outcomes, Eisenhower matrix, calendar & time tracking",
        listOf("progress_ring", "quick_add", "priority", "eisenhower", "calendar", "today", "project", "time_tracking", "weekly_progress")
    ),
    CREATOR(
        "creator",
        "Creator Studio",
        "Projects, deep work timer, scratchpad notes & routines",
        listOf("progress_ring", "quick_add", "project", "focus", "stopwatch", "notes", "today", "routines", "streak")
    ),
    ADHD_FRIENDLY(
        "adhd_friendly",
        "ADHD-Friendly",
        "Low overwhelm: Top 3 priorities, overdue rescue & 25m timer",
        listOf("quick_add", "priority", "focus", "overdue", "today", "habit", "streak", "quotes")
    ),
    POWER_USER(
        "power_user",
        "All 30 Ultimate",
        "Every single one of the 30 customizable widgets unlocked & active",
        OrbitWidgetType.entries.map { it.key }
    );

    companion object {
        fun fromKey(key: String): DashboardPresetConfig =
            entries.firstOrNull { it.key == key } ?: MISSION_CONTROL
    }
}

data class ParsedTaskPreview(
    val cleanTitle: String,
    val priority: String,
    val startDateEpochDay: Long = 0L,
    val dueDateEpochDay: Long,
    val deadlineEpochDay: Long = 0L,
    val scheduledDateEpochDay: Long,
    val scheduledStartHour: Int,
    val scheduledStartMinute: Int,
    val estimatedMinutes: Int,
    val energyLevel: String,
    val recurrenceRule: String,
    val locationTrigger: String,
    val tags: List<String>,
    val projectHint: String,
    val reminderCategory: String,
    val summaryChips: List<String>
)

/**
 * 100% Deterministic, Rule-Based Natural-Language Parser (Zero AI).
 * Supports expressions such as:
 * - "Finish homework tomorrow at 5pm p1 #school"
 * - "Pay electricity bill every month"
 * - "Workout every Monday Wednesday Friday"
 * - "Read for 30 minutes every evening"
 */
object SmartTaskParser {
    fun parse(rawInput: String, todayEpochDay: Long = LocalDate.now().toEpochDay()): ParsedTaskPreview {
        var working = rawInput.trim()
        val chips = mutableListOf<String>()

        var priority = "p3"
        var targetEpochDay = todayEpochDay
        var startDateEpochDay = 0L
        var deadlineEpochDay = 0L
        var startHour = -1
        var startMinute = 0
        var estimatedMinutes = 25
        var recurrence = ""
        var locationTrigger = ""
        var projectHint = ""
        var reminderCategory = ""
        val tags = mutableListOf<String>()

        // 1. Extract #tags
        val tagRegex = Regex("""#([A-Za-z0-9_-]+)""")
        tagRegex.findAll(working).forEach { match ->
            val tag = match.groupValues[1].lowercase(Locale.US)
            if (tag.isNotBlank() && !tags.contains(tag)) {
                tags.add(tag)
                chips.add("#$tag")
            }
        }
        working = working.replace(tagRegex, "").trim()

        // 2. Extract @project or /project hints
        val projRegex = Regex("""(?:@|/)([A-Za-z0-9_-]+)""")
        projRegex.find(working)?.let { match ->
            projectHint = match.groupValues[1]
            chips.add("📁 $projectHint")
            working = working.replaceRange(match.range, "").trim()
        }

        // 3. Detect Special Reminder Categories (Birthday, Anniversary, Bill, Medication, Shopping)
        when {
            Regex("""\b(birthday|bday)\b""", RegexOption.IGNORE_CASE).containsMatchIn(working) -> {
                reminderCategory = "Birthday"
                recurrence = "YEARLY"
                if (!tags.contains("birthday")) tags.add("birthday")
                chips.add("🎂 Birthday (Yearly)")
            }
            Regex("""\b(anniversary)\b""", RegexOption.IGNORE_CASE).containsMatchIn(working) -> {
                reminderCategory = "Anniversary"
                recurrence = "YEARLY"
                if (!tags.contains("anniversary")) tags.add("anniversary")
                chips.add("💍 Anniversary (Yearly)")
            }
            Regex("""\b(bill|rent|invoice|mortgage|subscription|electricity|water|internet)\b""", RegexOption.IGNORE_CASE).containsMatchIn(working) -> {
                reminderCategory = "Bill"
                if (recurrence.isBlank()) recurrence = "MONTHLY"
                if (!tags.contains("bills")) tags.add("bills")
                chips.add("💳 Bill Reminder")
            }
            Regex("""\b(medication|medicine|pill|vitamin|dose|supplement)\b""", RegexOption.IGNORE_CASE).containsMatchIn(working) -> {
                reminderCategory = "Medication"
                if (recurrence.isBlank()) recurrence = "DAILY"
                if (!tags.contains("health")) tags.add("health")
                chips.add("💊 Medication")
            }
        }

        // 4. Priority tokens (p1..p4, !1..!4, !!!, urgent, high priority, low priority)
        val priorityRegex = Regex(
            """\b(p1|p2|p3|p4|!1|!2|!3|!4|urgent|critical|high\s+priority|priority\s+high|medium\s+priority|low\s+priority|priority\s+low)\b|(!{1,3})(?=\s|$)""",
            RegexOption.IGNORE_CASE
        )
        priorityRegex.find(working)?.let { match ->
            val token = match.value.lowercase(Locale.US)
            priority = when {
                token in listOf("p1", "!1", "!!!", "urgent", "critical") -> "p1"
                token in listOf("p2", "!2", "!!", "high priority", "priority high") -> "p2"
                token in listOf("p4", "!4", "low priority", "priority low") -> "p4"
                else -> "p3"
            }
            chips.add("Priority ${priority.uppercase(Locale.US)}")
            working = working.replaceRange(match.range, "").trim()
        }

        // 5. Multi-weekday & advanced recurrence ("every Monday Wednesday Friday", "every Tue Thu Sat", "every evening", etc.)
        val multiDayMap = mapOf(
            "mon" to "MON", "monday" to "MON",
            "tue" to "TUE", "tues" to "TUE", "tuesday" to "TUE",
            "wed" to "WED", "wednesday" to "WED",
            "thu" to "THU", "thur" to "THU", "thurs" to "THU", "thursday" to "THU",
            "fri" to "FRI", "friday" to "FRI",
            "sat" to "SAT", "saturday" to "SAT",
            "sun" to "SUN", "sunday" to "SUN"
        )
        val multiWeekdayRegex = Regex(
            """\bevery\s+((?:(?:mon(?:day)?|tue(?:s|sday)?|wed(?:nesday)?|thu(?:r|rs|rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)(?:\s*,\s*|\s+and\s+|\s+)){2,}(?:mon(?:day)?|tue(?:s|sday)?|wed(?:nesday)?|thu(?:r|rs|rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?))\b""",
            RegexOption.IGNORE_CASE
        )
        val everyXRegex = Regex("""\bevery\s+(\d+)\s+(days?|weeks?|months?)\b""", RegexOption.IGNORE_CASE)
        val afterCompletionRegex = Regex("""\bafter\s+completion\b""", RegexOption.IGNORE_CASE)
        val everyTimeOfDayRegex = Regex("""\bevery\s+(morning|afternoon|evening|night)\b""", RegexOption.IGNORE_CASE)
        val stdRecurRegex = Regex(
            """\b((?:(?:on\s+the\s+|every\s+)?(?:first|1st)\s+monday(?:\s+of\s+(?:every|each|the)\s+month)?)|every\s+day|daily|every\s+weekday|weekdays|every\s+weekend|weekends|every\s+week|weekly|every\s+month|monthly|every\s+year|yearly|annually)\b""",
            RegexOption.IGNORE_CASE
        )

        when {
            multiWeekdayRegex.containsMatchIn(working) -> {
                val m = multiWeekdayRegex.find(working)!!
                val rawDays = m.groupValues[1].lowercase(Locale.US)
                    .split(Regex("""[\s,]+"""))
                    .filter { it != "and" && it.isNotBlank() }
                    .mapNotNull { multiDayMap[it] }
                    .distinct()
                if (rawDays.isNotEmpty()) {
                    recurrence = "DAYS_${rawDays.joinToString("_")}"
                    chips.add("↻ ${rawDays.joinToString("/")}")
                }
                working = working.replaceRange(m.range, "").trim()
            }
            everyTimeOfDayRegex.containsMatchIn(working) -> {
                val m = everyTimeOfDayRegex.find(working)!!
                val period = m.groupValues[1].lowercase(Locale.US)
                recurrence = "DAILY"
                if (startHour == -1) {
                    startHour = when (period) {
                        "morning" -> 8
                        "afternoon" -> 14
                        "evening" -> 19
                        else -> 21
                    }
                    startMinute = 0
                    chips.add("🕒 ${formatHourMinute(startHour, startMinute)}")
                }
                chips.add("↻ Daily ($period)")
                working = working.replaceRange(m.range, "").trim()
            }
            afterCompletionRegex.containsMatchIn(working) -> {
                val m = afterCompletionRegex.find(working)!!
                recurrence = "AFTER_COMPLETION"
                chips.add("↻ After Completion")
                working = working.replaceRange(m.range, "").trim()
            }
            everyXRegex.containsMatchIn(working) -> {
                val m = everyXRegex.find(working)!!
                val count = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 2
                val unit = m.groupValues[2].uppercase(Locale.US)
                recurrence = "EVERY_${count}_$unit"
                chips.add("↻ Every $count ${m.groupValues[2].lowercase(Locale.US)}")
                working = working.replaceRange(m.range, "").trim()
            }
            stdRecurRegex.containsMatchIn(working) -> {
                val m = stdRecurRegex.find(working)!!
                val token = m.value.lowercase(Locale.US)
                recurrence = when {
                    token.contains("first monday") || token.contains("1st monday") -> "MONTHLY_1ST_MON"
                    token.contains("weekday") -> "WEEKDAYS"
                    token.contains("weekend") -> "WEEKENDS"
                    token.contains("week") -> "WEEKLY"
                    token.contains("month") -> "MONTHLY"
                    token.contains("year") || token.contains("annual") -> "YEARLY"
                    else -> "DAILY"
                }
                chips.add("↻ ${recurrence.lowercase(Locale.US).replace('_', ' ').replaceFirstChar { it.uppercase() }}")
                working = working.replaceRange(m.range, "").trim()
            }
        }

        // 6. Relative & explicit calendar dates ("on April 30", "in 3 days", "in 2 weeks", "today", "tomorrow", "next monday", etc.)
        val monthMap = mapOf(
            "jan" to 1, "january" to 1,
            "feb" to 2, "february" to 2,
            "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4,
            "may" to 5,
            "jun" to 6, "june" to 6,
            "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8,
            "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11,
            "dec" to 12, "december" to 12
        )
        val monthDayRegex = Regex(
            """\b(?:on\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?\b""",
            RegexOption.IGNORE_CASE
        )
        val inDaysRegex = Regex("""\bin\s+(\d+)\s+(days?|weeks?)\b""", RegexOption.IGNORE_CASE)
        val weekdayRegex = Regex("""\b(?:next\s+|on\s+|every\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""", RegexOption.IGNORE_CASE)
        val dateTokenRegex = Regex("""\b(today|tonight|tomorrow|tmrw|next\s+week|this\s+weekend)\b""", RegexOption.IGNORE_CASE)

        when {
            monthDayRegex.containsMatchIn(working) -> {
                val m = monthDayRegex.find(working)!!
                val mNum = monthMap[m.groupValues[1].lowercase(Locale.US)] ?: 1
                val dNum = m.groupValues[2].toIntOrNull()?.coerceIn(1, 31) ?: 1
                val baseDate = LocalDate.ofEpochDay(todayEpochDay)
                val parsedDate = runCatching {
                    val candidate = LocalDate.of(baseDate.year, mNum, dNum.coerceAtMost(LocalDate.of(baseDate.year, mNum, 1).lengthOfMonth()))
                    if (candidate.isBefore(baseDate)) candidate.plusYears(1) else candidate
                }.getOrDefault(baseDate)
                targetEpochDay = parsedDate.toEpochDay()
                chips.add("📅 ${formatEpochDay(targetEpochDay, todayEpochDay)}")
                working = working.replaceRange(m.range, "").trim()
            }
            inDaysRegex.containsMatchIn(working) -> {
                val m = inDaysRegex.find(working)!!
                val num = m.groupValues[1].toLongOrNull() ?: 1L
                val isWeeks = m.groupValues[2].lowercase(Locale.US).startsWith("week")
                targetEpochDay = todayEpochDay + if (isWeeks) num * 7L else num
                chips.add("📅 ${formatEpochDay(targetEpochDay, todayEpochDay)}")
                working = working.replaceRange(m.range, "").trim()
            }
            dateTokenRegex.containsMatchIn(working) -> {
                val m = dateTokenRegex.find(working)!!
                val token = m.value.lowercase(Locale.US)
                targetEpochDay = when {
                    token == "tomorrow" || token == "tmrw" -> todayEpochDay + 1
                    token == "next week" -> todayEpochDay + 7
                    token == "this weekend" -> {
                        val base = LocalDate.ofEpochDay(todayEpochDay)
                        base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).toEpochDay()
                    }
                    else -> todayEpochDay
                }
                if (token == "tonight" && startHour == -1) {
                    startHour = 20
                    startMinute = 0
                }
                chips.add("📅 ${formatEpochDay(targetEpochDay, todayEpochDay)}")
                working = working.replaceRange(m.range, "").trim()
            }
            weekdayRegex.containsMatchIn(working) -> {
                val m = weekdayRegex.find(working)!!
                val dayName = m.groupValues[1].uppercase(Locale.US)
                val isEvery = m.value.lowercase(Locale.US).startsWith("every ")
                val dow = runCatching { DayOfWeek.valueOf(dayName) }.getOrDefault(DayOfWeek.MONDAY)
                val nextDate = LocalDate.ofEpochDay(todayEpochDay).with(TemporalAdjusters.nextOrSame(dow))
                targetEpochDay = nextDate.toEpochDay()
                if (isEvery && recurrence.isBlank()) {
                    recurrence = "WEEKLY"
                    chips.add("↻ Weekly (${dayName.take(3)})")
                }
                chips.add("📅 ${dayName.lowercase(Locale.US).replaceFirstChar { it.uppercase() }}")
                working = working.replaceRange(m.range, "").trim()
            }
        }

        // 7. Time tokens ("at 3pm", "at 14:30", "by 5:00 pm")
        val timeRegex = Regex("""\b(?:at|by)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
        timeRegex.find(working)?.let { match ->
            val rawHour = match.groupValues[1].toIntOrNull() ?: 9
            val rawMin = match.groupValues[2].toIntOrNull() ?: 0
            val ampm = match.groupValues[3].lowercase(Locale.US)
            startHour = when {
                ampm == "pm" && rawHour < 12 -> rawHour + 12
                ampm == "am" && rawHour == 12 -> 0
                else -> rawHour.coerceIn(0, 23)
            }
            startMinute = rawMin.coerceIn(0, 59)
            chips.add("🕒 ${formatHourMinute(startHour, startMinute)}")
            working = working.replaceRange(match.range, "").trim()
        }

        // 8. Duration tokens ("for 45m", "for 2h", "30 mins", "for 30 minutes")
        val durationRegex = Regex("""\b(?:for\s+)?(\d+)\s*(m|min|mins|minutes|h|hr|hrs|hours)\b""", RegexOption.IGNORE_CASE)
        durationRegex.find(working)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: 25
            val unit = match.groupValues[2].lowercase(Locale.US)
            estimatedMinutes = if (unit.startsWith("h")) {
                (amount * 60).coerceIn(5, 480)
            } else {
                amount.coerceIn(5, 480)
            }
            chips.add("⏱ ${estimatedMinutes}m")
            working = working.replaceRange(match.range, "").trim()
        }

        // 9. Location context ("at Home", "at Office", "at Gym", "at Supermarket")
        val locRegex = Regex("""\b(?:at|near)\s+(home|office|work|gym|store|supermarket|market|campus|library|pharmacy)\b""", RegexOption.IGNORE_CASE)
        locRegex.find(working)?.let { match ->
            val place = match.groupValues[1].replaceFirstChar { it.uppercase() }
            locationTrigger = "Arriving: $place"
            chips.add("📍 $place")
            working = working.replaceRange(match.range, "").trim()
        }

        // Clean up leading "Remind me to"
        working = working.replace(Regex("""^remind\s+me\s+to\s+""", RegexOption.IGNORE_CASE), "")
        val cleanTitle = working.replace(Regex("""\s+"""), " ").trim().ifEmpty { rawInput.trim() }

        val energy = when {
            estimatedMinutes >= 45 || priority == "p1" -> "high"
            estimatedMinutes <= 15 -> "low"
            else -> "medium"
        }

        return ParsedTaskPreview(
            cleanTitle = cleanTitle,
            priority = priority,
            startDateEpochDay = startDateEpochDay,
            dueDateEpochDay = targetEpochDay,
            deadlineEpochDay = deadlineEpochDay,
            scheduledDateEpochDay = targetEpochDay,
            scheduledStartHour = startHour,
            scheduledStartMinute = startMinute,
            estimatedMinutes = estimatedMinutes,
            energyLevel = energy,
            recurrenceRule = recurrence,
            locationTrigger = locationTrigger,
            tags = tags,
            projectHint = projectHint,
            reminderCategory = reminderCategory,
            summaryChips = chips
        )
    }

    /**
     * Deterministic recurrence calculator that handles time-zone-safe LocalDate math
     * for DAILY, WEEKDAYS, WEEKENDS, WEEKLY, MONTHLY, YEARLY, AFTER_COMPLETION,
     * EVERY_X_DAYS/WEEKS/MONTHS, and multi-weekday sets (e.g., DAYS_MON_WED_FRI).
     */
    fun calculateNextRecurrenceDay(baseEpochDay: Long, rule: String): Long {
        val baseDate = LocalDate.ofEpochDay(baseEpochDay.coerceAtLeast(1L))
        return when {
            rule == "DAILY" || rule == "AFTER_COMPLETION" -> baseEpochDay + 1
            rule == "WEEKDAYS" -> {
                var next = baseDate.plusDays(1)
                while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                    next = next.plusDays(1)
                }
                next.toEpochDay()
            }
            rule == "WEEKENDS" -> {
                var next = baseDate.plusDays(1)
                while (next.dayOfWeek != DayOfWeek.SATURDAY && next.dayOfWeek != DayOfWeek.SUNDAY) {
                    next = next.plusDays(1)
                }
                next.toEpochDay()
            }
            rule == "WEEKLY" -> baseEpochDay + 7
            rule == "MONTHLY_1ST_MON" -> {
                val nextMonth = baseDate.plusMonths(1).withDayOfMonth(1)
                nextMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)).toEpochDay()
            }
            rule == "MONTHLY" -> baseDate.plusMonths(1).toEpochDay()
            rule == "YEARLY" -> baseDate.plusYears(1).toEpochDay()
            rule.startsWith("DAYS_") -> {
                val dayTokens = rule.removePrefix("DAYS_").split("_").toSet()
                val allowedDows = dayTokens.mapNotNull { token ->
                    when (token) {
                        "MON" -> DayOfWeek.MONDAY
                        "TUE" -> DayOfWeek.TUESDAY
                        "WED" -> DayOfWeek.WEDNESDAY
                        "THU" -> DayOfWeek.THURSDAY
                        "FRI" -> DayOfWeek.FRIDAY
                        "SAT" -> DayOfWeek.SATURDAY
                        "SUN" -> DayOfWeek.SUNDAY
                        else -> null
                    }
                }.toSet()
                if (allowedDows.isEmpty()) {
                    baseEpochDay + 1
                } else {
                    var next = baseDate.plusDays(1)
                    var guard = 0
                    while (!allowedDows.contains(next.dayOfWeek) && guard < 14) {
                        next = next.plusDays(1)
                        guard++
                    }
                    next.toEpochDay()
                }
            }
            rule.startsWith("EVERY_") -> {
                val parts = rule.split("_")
                val count = parts.getOrNull(1)?.toLongOrNull() ?: 3L
                val unit = parts.getOrNull(2).orEmpty()
                when {
                    unit.startsWith("WEEK") -> baseEpochDay + (count * 7L)
                    unit.startsWith("MONTH") -> baseDate.plusMonths(count).toEpochDay()
                    else -> baseEpochDay + count
                }
            }
            else -> baseEpochDay + 1
        }
    }

    fun formatHourMinute(hour: Int, minute: Int, use24Hour: Boolean = false): String {
        if (hour < 0) return "Unscheduled"
        if (use24Hour) {
            return String.format(Locale.US, "%02d:%02d", hour, minute)
        }
        val ampm = if (hour >= 12) "PM" else "AM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return if (minute == 0) "$h12 $ampm" else String.format(Locale.US, "%d:%02d %s", h12, minute, ampm)
    }

    fun formatEpochDay(epochDay: Long, todayEpochDay: Long = LocalDate.now().toEpochDay()): String {
        if (epochDay <= 0L) return "No Date"
        return when (epochDay) {
            todayEpochDay -> "Today"
            todayEpochDay + 1 -> "Tomorrow"
            todayEpochDay - 1 -> "Yesterday"
            else -> LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        }
    }
}
