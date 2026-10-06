package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import java.util.Locale

class HealthWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_STEPS = "com.example.widget.ACTION_ADD_STEPS"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_STEPS) {
            val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
            val currentSteps = prefs.getInt("wearable_steps", 0)
            val currentCals = prefs.getInt("wearable_calories", 0)
            
            // Increment by 1000 steps and 50 kcal as a quick widget simulation reward!
            val nextSteps = currentSteps + 1000
            val nextCals = currentCals + 50
            
            prefs.edit().apply {
                putInt("wearable_steps", nextSteps)
                putInt("wearable_calories", nextCals)
                apply()
            }
            WidgetUtils.updateAllWidgets(context)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("life_os_prefs", Context.MODE_PRIVATE)
        val steps = prefs.getInt("wearable_steps", 0)
        val calories = prefs.getInt("wearable_calories", 0)
        val sleep = prefs.getFloat("wearable_sleep_hours", 0.0f)
        val bpm = prefs.getInt("wearable_heart_rate", 60)

        val views = RemoteViews(context.packageName, R.layout.widget_health)

        views.setTextViewText(R.id.widget_steps_value, String.format(Locale.US, "%,d", steps))
        views.setTextViewText(R.id.widget_calories_value, "$calories kcal")
        views.setTextViewText(R.id.widget_sleep_value, String.format(Locale.US, "%.1f h", sleep))
        views.setTextViewText(R.id.widget_bpm_value, "$bpm bpm")

        // Progress bar for steps (daily goal: 10000 steps)
        val stepProgress = ((steps.toFloat() / 10000f) * 100).toInt().coerceIn(0, 100)
        views.setProgressBar(R.id.widget_steps_progress, 100, stepProgress, false)

        // Setup PendingIntent for "Log Steps" button
        val stepsIntent = Intent(context, HealthWidgetProvider::class.java).apply {
            action = ACTION_ADD_STEPS
        }
        val stepsPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId * 10 + 4, stepsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_add_steps, stepsPendingIntent)

        // Launch App on Widget click
        val appIntent = Intent(context, com.example.MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_health_root, appPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
