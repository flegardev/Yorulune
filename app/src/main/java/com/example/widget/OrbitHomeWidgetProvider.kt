package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.OrbitLocalDatabase
import com.example.data.model.OrbitTask
import com.example.data.notification.OrbitReminderReceiver
import com.example.data.notification.OrbitReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class OrbitHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        update(context, manager, ids) { pending?.finish() }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

    companion object {
        fun requestUpdate(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, OrbitHomeWidgetProvider::class.java))
            if (ids.isNotEmpty()) update(app, manager, ids)
        }

        fun requestPinToHomeScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < 26) return false
            val manager = AppWidgetManager.getInstance(context)
            return manager.isRequestPinAppWidgetSupported &&
                manager.requestPinAppWidget(ComponentName(context, OrbitHomeWidgetProvider::class.java), null, null)
        }

        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray, finished: () -> Unit = {}) {
            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = OrbitLocalDatabase.getInstance(app).orbitDao()
                    val today = LocalDate.now().toEpochDay()
                    val tasks = dao.getAllTasksOnce().asSequence().map { it.toDomain() }
                        .filter { !it.isCompleted && !it.isTrashed && it.status != "archived" }
                        .filter { task ->
                            val date = listOf(task.scheduledDateEpochDay, task.dueDateEpochDay, task.deadlineEpochDay)
                                .filter { it > 0 }.minOrNull() ?: today
                            date <= today
                        }
                        .sortedBy { it.priority }.take(3).toList()
                    val theme = dao.getPreferencesOnce()?.themeMode ?: "system"
                    for (id in ids) {
                        val options = manager.getAppWidgetOptions(id)
                        fun render(size: SizeF) = content(app, size, tasks, theme)
                        val sizes = if (Build.VERSION.SDK_INT >= 31) {
                            BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                        } else null
                        val views = if (Build.VERSION.SDK_INT >= 31 && !sizes.isNullOrEmpty()) {
                            RemoteViews(sizes.distinct().take(16).associateWith(::render))
                        } else render(SizeF(
                            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 122).toFloat(),
                            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 58).toFloat()
                        ))
                        manager.updateAppWidget(id, views)
                    }
                } catch (error: Exception) {
                    android.util.Log.w("YoruluneWidget", "Could not refresh widget", error)
                } finally {
                    finished()
                }
            }
        }

        private fun content(context: Context, size: SizeF, tasks: List<OrbitTask>, theme: String): RemoteViews {
            val config = context.resources.configuration
            val isOled = theme == "oled"
            val isDark = isOled || theme == "dark" ||
                (theme in listOf("system", "dynamic") && config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            val ink = if (isDark) 0xFFF5EBF4.toInt() else 0xFF2A2130.toInt()
            val actionInk = if (isDark) 0xFFE6B8DF.toInt() else 0xFF713969.toInt()
            val enoughHeight = size.height >= if (config.fontScale > 1.3f) 132f else 108f
            val narrow = size.width < 110f
            val first = tasks.firstOrNull()
            return RemoteViews(context.packageName, R.layout.orbit_appwidget).apply {
                setInt(android.R.id.background, "setBackgroundResource", when {
                    isOled -> R.drawable.adaptive_widget_background_oled
                    isDark -> R.drawable.adaptive_widget_background_dark
                    else -> R.drawable.adaptive_widget_background
                })
                val padding = ((if (narrow || !enoughHeight) 4 else 10) * context.resources.displayMetrics.density).toInt()
                setViewPadding(android.R.id.background, padding, padding, padding, padding)
                for (id in listOf(R.id.widget_headline, R.id.widget_detail, R.id.widget_more)) setTextColor(id, ink)
                for (id in listOf(R.id.widget_add, R.id.widget_aux, R.id.widget_done)) setTextColor(id, actionInk)
                setTextViewText(R.id.widget_headline, when {
                    narrow -> if (first == null) "Today" else first.title
                    enoughHeight -> "Today"
                    else -> first?.title ?: "Today"
                })
                setTextViewTextSize(R.id.widget_headline, TypedValue.COMPLEX_UNIT_SP, if (narrow) 13f else 16f)
                setTextViewText(R.id.widget_add, if (size.width >= 182f) "+ Task" else "+")
                setTextViewText(R.id.widget_aux, "Focus")
                setTextViewText(R.id.widget_detail, first?.title ?: "No tasks today")
                setTextViewText(R.id.widget_more, tasks.drop(1).joinToString("\n") { it.title })
                setViewVisibility(R.id.widget_add, if (narrow) View.GONE else View.VISIBLE)
                setViewVisibility(R.id.widget_aux, if (size.width >= 240f) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.widget_detail_row, if (enoughHeight && !narrow) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.widget_done, if (first == null) View.GONE else View.VISIBLE)
                setViewVisibility(R.id.widget_more, if (size.height >= 172f && !narrow && tasks.size > 1) View.VISIBLE else View.GONE)
                setContentDescription(R.id.widget_add, "Add task")
                setContentDescription(R.id.widget_aux, "Open focus timer")
                setOnClickPendingIntent(android.R.id.background, launch(context, "today"))
                setOnClickPendingIntent(R.id.widget_add, launch(context, "add"))
                setOnClickPendingIntent(R.id.widget_aux, launch(context, "focus"))
                if (first != null) {
                    setContentDescription(R.id.widget_done, "Complete " + first.title)
                    val intent = Intent(context, OrbitReminderReceiver::class.java).apply {
                        action = OrbitReminderScheduler.ACTION_COMPLETE_TASK
                        data = android.net.Uri.Builder().scheme("yorulune").authority("widget-complete")
                            .appendPath(first.id).build()
                        putExtra(OrbitReminderScheduler.EXTRA_TASK_ID, first.id)
                    }
                    setOnClickPendingIntent(R.id.widget_done, PendingIntent.getBroadcast(context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                }
            }
        }

        private fun launch(context: Context, shortcut: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data = android.net.Uri.Builder().scheme("yorulune").authority("widget").appendPath(shortcut).build()
                putExtra(MainActivity.WIDGET_ACTION, shortcut)
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
