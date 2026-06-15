package com.chun.carlife.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.chun.carlife.MainActivity
import com.chun.carlife.R
import com.chun.carlife.ui.ACTION_ADD_REFUEL

abstract class RefuelShortcutWidget(@LayoutRes private val layoutId: Int) : AppWidgetProvider() {
    final override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_REFUEL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val views = RemoteViews(context.packageName, layoutId).apply {
            setOnClickPendingIntent(R.id.widget_refuel_shortcut_root, pendingIntent)
        }
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }
}

class RefuelShortcutWidget1x1 : RefuelShortcutWidget(R.layout.widget_refuel_shortcut_1x1)

class RefuelShortcutWidget2x1 : RefuelShortcutWidget(R.layout.widget_refuel_shortcut_2x1)
