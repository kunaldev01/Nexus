package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUtils {
    fun updateAllWidgets(context: Context) {
        // Broadcast update to Hydration widget
        val hydrationIntent = Intent(context, HydrationWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val hydrationIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, HydrationWidgetProvider::class.java)
        )
        if (hydrationIds.isNotEmpty()) {
            hydrationIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, hydrationIds)
            context.sendBroadcast(hydrationIntent)
        }

        // Broadcast update to Dashboard widget
        val dashboardIntent = Intent(context, DashboardWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val dashboardIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, DashboardWidgetProvider::class.java)
        )
        if (dashboardIds.isNotEmpty()) {
            dashboardIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, dashboardIds)
            context.sendBroadcast(dashboardIntent)
        }

        // Broadcast update to Health widget
        val healthIntent = Intent(context, HealthWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val healthIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, HealthWidgetProvider::class.java)
        )
        if (healthIds.isNotEmpty()) {
            healthIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, healthIds)
            context.sendBroadcast(healthIntent)
        }
    }
}
