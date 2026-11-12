package com.example.qa

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout

/** Emulator-only debug host. It never allocates IDs in the user's launcher host. */
class WidgetPreviewActivity : Activity() {
    private lateinit var host: AppWidgetHost
    private var widgetId = -1
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        check(Build.MODEL.contains("sdk", true)) { "Disposable emulator required" }
        val width = intent.getIntExtra("width", 280)
        val height = intent.getIntExtra("height", 240)
        require(width in 110..600 && height in 110..600)
        val providerName = intent.getStringExtra("provider") ?: "com.example.widget.OrbitHomeWidgetProvider"
        require(providerName in setOf("com.example.widget.OrbitHomeWidgetProvider"))
        val manager = AppWidgetManager.getInstance(this)
        host = AppWidgetHost(this, 611111)
        host.deleteHost() // This isolated QA host only; recover an interrupted run.
        widgetId = host.allocateAppWidgetId()
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
        }
        val provider = ComponentName(this, Class.forName(providerName))
        check(manager.bindAppWidgetIdIfAllowed(widgetId, provider, options)) { "Grant this emulator debug package widget binding for QA" }
        host.startListening()
        val view = host.createView(this, widgetId, manager.getAppWidgetInfo(widgetId))
        view.updateAppWidgetSize(options, width, height, width, height)
        val density = resources.displayMetrics.density
        val root = FrameLayout(this).apply { setBackgroundColor(0xFF39343D.toInt()) }
        root.addView(view, FrameLayout.LayoutParams((width*density).toInt(), (height*density).toInt(), Gravity.CENTER))
        setContentView(root)
        // AppWidgetProvider receives the real system update, not a mock RemoteViews.
        sendBroadcast(android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            component = provider
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        })
    }
    override fun onDestroy() {
        if (::host.isInitialized) {
            if (widgetId >= 0) host.deleteAppWidgetId(widgetId)
            host.stopListening()
        }
        super.onDestroy()
    }
}
