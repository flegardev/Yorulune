package com.example

import android.os.Build
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

/** UI-only regression: no resets, seeding, imports, uninstalls or user-record deletion. */
@RunWith(AndroidJUnit4::class)
class KissatenDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun requireEmulator() {
        check(Build.MODEL.contains("sdk", ignoreCase=true)) { "Disposable Android emulator required" }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(600)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val folder=File(instrumentation.targetContext.getExternalFilesDir(null), "kissaten-qa")
        folder.mkdirs()
        val bitmap=checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(folder,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    @Test fun navigationAndThemeSurviveActivityRecreation() {
        for (label in listOf("Home","Tasks","Calendar","Habits","Focus")) {
            compose.onAllNodesWithText(label).onLast().performClick()
            compose.waitForIdle()
            if (label == "Tasks") {
                compose.onNodeWithText("All Tasks").assertIsDisplayed()
                compose.onNodeWithTag("tasks_search_filter_input").assertIsDisplayed()
            }
            capture(label.lowercase().replace(" ","-"))
        }
        compose.onAllNodesWithText("Home").onLast().performClick()
        for (index in 1..5) {
            compose.onNodeWithTag("quick_theme_mode_toggle").performScrollTo().performClick()
            capture("appearance-cycle-$index")
        }
        compose.onAllNodesWithText("Tasks").onLast().performClick()
        compose.onNodeWithText("Kanban").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("All Tasks").assertIsDisplayed()
        compose.onNodeWithTag("tasks_search_filter_input").assertIsDisplayed()
        compose.onNodeWithText("Kanban").assertIsSelected()
        capture("recreated-tasks")
        compose.onAllNodesWithText("Calendar").onLast().performClick()
        compose.onNodeWithText("Weekly").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Weekly").assertIsSelected()
        capture("recreated-calendar")
    }
}
