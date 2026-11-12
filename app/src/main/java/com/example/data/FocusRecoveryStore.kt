package com.example.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import org.json.JSONObject

data class RecoveredFocus(val payload: JSONObject, val secondsWhileAway: Long)

/** Private recovery only. Reopen paused; never play audio or log a session implicitly. */
class FocusRecoveryStore(
    context: Context,
    private val uptime: () -> Long = { SystemClock.elapsedRealtime() },
    private val boot: () -> Int = { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
) {
    private val prefs = context.getSharedPreferences("kissaten_focus_recovery", Context.MODE_PRIVATE)
    fun save(payload: JSONObject, active: Boolean): Boolean {
        // Call on Dispatchers.IO. A completed write is durable before it returns.
        return prefs.edit().putString("snapshot", JSONObject().put("payload", payload)
            .put("active", active).put("uptime", uptime()).put("boot", boot()).toString()).commit()
    }
    fun restore(): RecoveredFocus? = runCatching {
        val raw = prefs.getString("snapshot", null) ?: return null
        val saved = JSONObject(raw)
        val savedBoot = saved.optInt("boot", -1)
        val delta = if (saved.optBoolean("active") && savedBoot >= 0 && savedBoot == boot())
            (uptime() - saved.getLong("uptime")).coerceAtLeast(0) / 1000L else 0L
        RecoveredFocus(saved.getJSONObject("payload"), delta)
    }.getOrNull()
}
