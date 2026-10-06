package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.data.LifeOsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DashboardWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_WATER = "com.example.widget.DASHBOARD_ADD_WATER"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
            val currentMl = prefs.getInt("water_logged_ml", 1250)
            val nextMl = (currentMl + 250).coerceAtMost(3000)
            prefs.edit().putInt("water_logged_ml", nextMl).apply()
            WidgetUtils.updateAllWidgets(context)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
        val waterLogged = prefs.getInt("water_logged_ml", 1250)
        val maxTarget = 3000

        val views = RemoteViews(context.packageName, R.layout.widget_dashboard)

        // Set static/local hydration data
        views.setTextViewText(R.id.widget_water_lbl, "$waterLogged / $maxTarget ml")
        val progress = ((waterLogged.toFloat() / maxTarget.toFloat()) * 100).toInt().coerceIn(0, 100)
        views.setProgressBar(R.id.widget_water_progress, 100, progress, false)

        // Setup Add Water button PendingIntent
        val addIntent = Intent(context, DashboardWidgetProvider::class.java).apply {
            action = ACTION_ADD_WATER
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 3, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_add_water, addPendingIntent)

        // Setup Launcher PendingIntent for widget tap
        val appIntent = Intent(context, com.example.MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_dashboard_root, appPendingIntent)

        // Asynchronously query database for the active focal task
        CoroutineScope(Dispatchers.IO).launch {
            val db = LifeOsDatabase.getDatabase(context)
            val allTasks = db.dao().getAllTasks().firstOrNull() ?: emptyList()
            val incompleteTask = allTasks.filter { !it.isCompleted }.minByOrNull { it.id }

            if (incompleteTask != null) {
                val blockTimeString = if (!incompleteTask.blockTime.isNullOrEmpty()) {
                    "[Time: ${incompleteTask.blockTime}] "
                } else {
                    ""
                }
                views.setTextViewText(R.id.widget_task_title, incompleteTask.title)
                views.setTextViewText(R.id.widget_task_subtitle, "${blockTimeString}Tap to focus & complete in App")
            } else {
                views.setTextViewText(R.id.widget_task_title, "All tasks completed! ✨")
                views.setTextViewText(R.id.widget_task_subtitle, "Create new tasks on the agenda board.")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
