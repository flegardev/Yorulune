package com.example

import com.example.data.model.NativeHomeWidgetVariant
import com.example.data.model.OrbitHabit
import com.example.data.model.OrbitWidgetType
import com.example.data.model.SmartTaskParser
import com.example.data.notification.OrbitReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ExampleUnitTest {

    @Test
    fun `parse natural language tomorrow time priority and tag`() {
        val today = LocalDate.of(2026, 4, 19) // Sunday
        val parsed = SmartTaskParser.parse(
            rawInput = "Finish homework tomorrow at 5pm p1 #school",
            todayEpochDay = today.toEpochDay()
        )
        assertEquals("Finish homework", parsed.cleanTitle)
        assertEquals(today.plusDays(1).toEpochDay(), parsed.dueDateEpochDay)
        assertEquals(17, parsed.scheduledStartHour)
        assertEquals(0, parsed.scheduledStartMinute)
        assertEquals("p1", parsed.priority)
        assertTrue(parsed.tags.contains("school"))
    }

    @Test
    fun `parse natural language monthly and first monday recurrence`() {
        val today = LocalDate.of(2026, 4, 19)
        val monthly = SmartTaskParser.parse(
            rawInput = "Pay electricity bill every month",
            todayEpochDay = today.toEpochDay()
        )
        assertEquals("Pay electricity bill", monthly.cleanTitle)
        assertEquals("MONTHLY", monthly.recurrenceRule)

        val firstMonday = SmartTaskParser.parse(
            rawInput = "Quarterly portfolio check first monday of every month at 9am",
            todayEpochDay = today.toEpochDay()
        )
        assertEquals("MONTHLY_1ST_MON", firstMonday.recurrenceRule)
        assertEquals(9, firstMonday.scheduledStartHour)
    }

    @Test
    fun `parse natural language multi-weekday recurrence`() {
        val today = LocalDate.of(2026, 4, 19)
        val parsed = SmartTaskParser.parse(
            rawInput = "Workout every Monday Wednesday Friday",
            todayEpochDay = today.toEpochDay()
        )
        assertEquals("Workout", parsed.cleanTitle)
        assertEquals("DAYS_MON_WED_FRI", parsed.recurrenceRule)
    }

    @Test
    fun `parse natural language duration and evening time`() {
        val today = LocalDate.of(2026, 4, 19)
        val parsed = SmartTaskParser.parse(
            rawInput = "Read for 30 minutes every evening",
            todayEpochDay = today.toEpochDay()
        )
        assertEquals("Read", parsed.cleanTitle)
        assertEquals(30, parsed.estimatedMinutes)
        assertEquals("DAILY", parsed.recurrenceRule)
        assertEquals(19, parsed.scheduledStartHour)
        assertEquals(0, parsed.scheduledStartMinute)
    }

    @Test
    fun `parse natural language explicit month and day`() {
        val today = LocalDate.of(2026, 4, 19)
        val parsed = SmartTaskParser.parse(
            rawInput = "Submit tax documents on April 30",
            todayEpochDay = today.toEpochDay()
        )
        assertEquals("Submit tax documents", parsed.cleanTitle)
        assertEquals(LocalDate.of(2026, 4, 30).toEpochDay(), parsed.dueDateEpochDay)
    }

    @Test
    fun `calculate next recurrence day across rules including MONTHLY_1ST_MON and AFTER_COMPLETION`() {
        val sunday = LocalDate.of(2026, 4, 19).toEpochDay()
        val monday = LocalDate.of(2026, 4, 20).toEpochDay()
        val wednesday = LocalDate.of(2026, 4, 22).toEpochDay()
        val friday = LocalDate.of(2026, 4, 24).toEpochDay()
        val nextMonday = LocalDate.of(2026, 4, 27).toEpochDay()
        val mayFirstMonday = LocalDate.of(2026, 5, 4).toEpochDay()

        assertEquals(monday, SmartTaskParser.calculateNextRecurrenceDay(sunday, "DAILY"))
        assertEquals(nextMonday, SmartTaskParser.calculateNextRecurrenceDay(friday, "WEEKDAYS"))
        assertEquals(wednesday, SmartTaskParser.calculateNextRecurrenceDay(monday, "DAYS_MON_WED_FRI"))
        assertEquals(friday, SmartTaskParser.calculateNextRecurrenceDay(wednesday, "DAYS_MON_WED_FRI"))
        assertEquals(mayFirstMonday, SmartTaskParser.calculateNextRecurrenceDay(sunday, "MONTHLY_1ST_MON"))
        assertEquals(tuesdayEpoch() + 1, SmartTaskParser.calculateNextRecurrenceDay(tuesdayEpoch(), "AFTER_COMPLETION"))
    }

    private fun tuesdayEpoch(): Long = LocalDate.of(2026, 4, 21).toEpochDay()

    @Test
    fun `computeTriggerMillis handles daylight saving transitions and time zone changes accurately`() {
        val nyZone = ZoneId.of("America/New_York")
        val utcZone = ZoneId.of("UTC")
        val tokyoZone = ZoneId.of("Asia/Tokyo")

        // US DST Spring-Forward day: March 8, 2026 (clocks jump 02:00 -> 03:00 EST/EDT)
        val springForwardDay = LocalDate.of(2026, 3, 8).toEpochDay()
        val beforeJump = OrbitReminderScheduler.computeTriggerMillis(
            targetEpochDay = springForwardDay,
            hour = 1,
            minute = 30,
            offsetMinutes = 0,
            zoneId = nyZone
        )
        val afterJump = OrbitReminderScheduler.computeTriggerMillis(
            targetEpochDay = springForwardDay,
            hour = 3,
            minute = 30,
            offsetMinutes = 0,
            zoneId = nyZone
        )
        // Because 2:00-3:00 AM is skipped during spring-forward, 1:30 AM EST to 3:30 AM EDT is 1 real hour (3,600,000 ms), not 2 hours!
        assertEquals(3_600_000L, afterJump - beforeJump)

        // Verify time-zone change shifts absolute epoch millis while preserving local wall-clock hour
        val regularDay = LocalDate.of(2026, 4, 20).toEpochDay()
        val triggerUtc = OrbitReminderScheduler.computeTriggerMillis(regularDay, 9, 0, 15, utcZone)
        val triggerTokyo = OrbitReminderScheduler.computeTriggerMillis(regularDay, 9, 0, 15, tokyoZone)
        assertNotEquals(triggerUtc, triggerTokyo)
        assertEquals(8, Instant.ofEpochMilli(triggerUtc).atZone(utcZone).hour)
        assertEquals(45, Instant.ofEpochMilli(triggerUtc).atZone(utcZone).minute)
        assertEquals(8, Instant.ofEpochMilli(triggerTokyo).atZone(tokyoZone).hour)
        assertEquals(45, Instant.ofEpochMilli(triggerTokyo).atZone(tokyoZone).minute)
    }

    @Test
    fun `habit scheduled for date and 7 day completion ratio`() {
        val sunday = LocalDate.of(2026, 4, 19) // Sunday
        val monday = LocalDate.of(2026, 4, 20) // Monday
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)

        val weekendHabit = OrbitHabit(
            id = "h_weekend",
            title = "Call a friend",
            targetDaysPerWeek = 2,
            frequencyLabel = "Weekends",
            completedEpochDays = listOf(sunday.toEpochDay())
        )
        assertTrue(weekendHabit.isScheduledForDate(sunday))
        assertFalse(weekendHabit.isScheduledForDate(monday))
        assertEquals(1, weekendHabit.completionsInLast7Days(sunday.toEpochDay()))
    }

    @Test
    fun `verify all 30 dashboard widgets and 10 native home widget variants exist`() {
        assertEquals(30, OrbitWidgetType.entries.size)
        assertEquals(10, NativeHomeWidgetVariant.entries.size)
    }

    @Test
    fun `verify Japanese Lofi and 3_0 pastel color themes resolve with gradient and soft light tokens`() {
        val requiredJapaneseLofiThemes = listOf(
            "sakura_blush", "matcha_mist", "kyoto_sky", "lantern_amber", "indigo_night",
            "rainy_tokyo", "plum_ink", "bamboo_green", "soft_cream", "moonlight_blue"
        )
        val requiredPastelAndEditorialThemes = listOf(
            "terracotta", "lavender", "sky", "mint", "peach", "rose", "ocean", "midnight", "sage", "clay", "orbit_indigo"
        )
        val paletteKeys = com.example.ui.theme.OrbitAccentPalettes.map { it.key }.toSet()
        (requiredJapaneseLofiThemes + requiredPastelAndEditorialThemes).forEach { key ->
            assertTrue("Expected theme '$key' in OrbitAccentPalettes", paletteKeys.contains(key))
            val resolved = com.example.ui.theme.getAccentPalette(key)
            assertEquals(key, resolved.key)
            assertNotEquals(resolved.gradientStart, resolved.gradientEnd)
        }
        requiredJapaneseLofiThemes.forEach { key ->
            assertTrue(com.example.ui.theme.getAccentPalette(key).isJapaneseLofi)
        }
    }
}
