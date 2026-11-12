package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.FocusRecoveryStore
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class FocusRecoveryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var now = 100_000L
    private var boot = 4
    private fun store() = FocusRecoveryStore(context, { now }, { boot })
    @Before fun cleanFixture() { context.getSharedPreferences("kissaten_focus_recovery",0).edit().clear().commit() }
    @Test fun runningSessionSurvivesNewStoreInstanceWithMonotonicElapsedTime() {
        store().save(JSONObject().put("notes","a saved thought").put("remaining",1500),true)
        now += 12_500
        val restored = requireNotNull(store().restore())
        assertEquals(12L, restored.secondsWhileAway)
        assertEquals("a saved thought",restored.payload.getString("notes"))
    }
    @Test fun pausedSessionDoesNotAccumulateBackgroundTime() {
        store().save(JSONObject().put("elapsed",20),false); now += 100_000
        assertEquals(0L,requireNotNull(store().restore()).secondsWhileAway)
    }
    @Test fun rebootDoesNotCompareDifferentMonotonicEpochs() {
        store().save(JSONObject().put("elapsed",20),true);boot++;now=250_000
        assertEquals(0L,requireNotNull(store().restore()).secondsWhileAway)
    }
    @Test fun corruptSnapshotDoesNotCrashStartup() {
        context.getSharedPreferences("kissaten_focus_recovery",0).edit().putString("snapshot","broken").commit()
        assertNull(store().restore())
    }
}
