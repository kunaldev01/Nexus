package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class HydrationWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_WATER = "com.example.widget.ACTION_ADD_WATER"
        const val ACTION_RESET_WATER = "com.example.widget.ACTION_RESET_WATER"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_ADD_WATER || action == ACTION_RESET_WATER) {
            val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
            val currentMl = prefs.getInt("water_logged_ml", 1250)

            val nextMl = if (action == ACTION_ADD_WATER) {
                (currentMl + 250).coerceAtMost(3000)
            } else {
                0
            }

            prefs.edit().putInt("water_logged_ml", nextMl).apply()
            WidgetUtils.updateAllWidgets(context)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
        val waterLogged = prefs.getInt("water_logged_ml", 1250)
        val maxTarget = 3000

        val views = RemoteViews(context.packageName, R.layout.widget_hydration)

        // Set text
        views.setTextViewText(R.id.widget_water_value, "$waterLogged ml")
        views.setTextViewText(R.id.widget_water_target, "Goal: $maxTarget ml")

        // Progress bar percentage
        val progress = ((waterLogged.toFloat() / maxTarget.toFloat()) * 100).toInt().coerceIn(0, 100)
        views.setProgressBar(R.id.widget_water_progress, 100, progress, false)

        // Setup PendingIntents for interactive buttons
        val addIntent = Intent(context, HydrationWidgetProvider::class.java).apply {
            action = ACTION_ADD_WATER
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 1, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_water, addPendingIntent)

        val resetIntent = Intent(context, HydrationWidgetProvider::class.java).apply {
            action = ACTION_RESET_WATER
        }
        val resetPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 2, resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_reset_water, resetPendingIntent)

        // Launch App on Widget click
        val appIntent = Intent(context, com.example.MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hydration_root, appPendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
