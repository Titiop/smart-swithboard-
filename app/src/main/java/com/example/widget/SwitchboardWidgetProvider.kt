package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SwitchboardWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_RELAY = "com.smartswitchboard.action.TOGGLE_RELAY"
        const val EXTRA_RELAY_ID = "extra_relay_id"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SwitchboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, SwitchboardWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_RELAY) {
            val relayId = intent.getIntExtra(EXTRA_RELAY_ID, 1)
            handleRelayToggle(context, relayId)
        }
    }

    private fun handleRelayToggle(context: Context, relayId: Int) {
        val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
        if (auth?.currentUser == null) {
            Toast.makeText(context, "Please open Smart Switchboard and sign in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val prefs = PreferencesRepository(context)
                val boardId = prefs.preferencesFlow.first().boardId
                val ref = FirebaseDatabase.getInstance().getReference(boardId).child("relay$relayId")

                val currentSnap = ref.get().await()
                val currentVal = currentSnap.getValue(Boolean::class.java) ?: false
                val targetVal = !currentVal

                ref.setValue(targetVal).await()

                // Refresh widgets immediately
                updateAllWidgets(context)
            } catch (e: Exception) {
                Toast.makeText(context, "Widget toggle failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWidgetView(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_switchboard)

        // Tapping header opens the main app
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val prefs = PreferencesRepository(context)
                val names = prefs.switchNamesFlow.first()
                val boardId = prefs.preferencesFlow.first().boardId

                val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
                if (auth?.currentUser == null) {
                    views.setTextViewText(R.id.widget_status, "Sign in required")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                views.setTextViewText(R.id.widget_status, "Live")

                val snapshot = FirebaseDatabase.getInstance().getReference(boardId).get().await()
                val r1 = snapshot.child("relay1").getValue(Boolean::class.java) ?: false
                val r2 = snapshot.child("relay2").getValue(Boolean::class.java) ?: false
                val r3 = snapshot.child("relay3").getValue(Boolean::class.java) ?: false
                val r4 = snapshot.child("relay4").getValue(Boolean::class.java) ?: false

                bindRelayButton(context, views, 1, names[1] ?: "Switch 1", r1, R.id.btn_relay1, R.id.txt_relay1_name, R.id.txt_relay1_state)
                bindRelayButton(context, views, 2, names[2] ?: "Switch 2", r2, R.id.btn_relay2, R.id.txt_relay2_name, R.id.txt_relay2_state)
                bindRelayButton(context, views, 3, names[3] ?: "Switch 3", r3, R.id.btn_relay3, R.id.txt_relay3_name, R.id.txt_relay3_state)
                bindRelayButton(context, views, 4, names[4] ?: "Switch 4", r4, R.id.btn_relay4, R.id.txt_relay4_name, R.id.txt_relay4_state)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
                // If offline, still bind buttons with fallback
                bindRelayButton(context, views, 1, "Switch 1", false, R.id.btn_relay1, R.id.txt_relay1_name, R.id.txt_relay1_state)
                bindRelayButton(context, views, 2, "Switch 2", false, R.id.btn_relay2, R.id.txt_relay2_name, R.id.txt_relay2_state)
                bindRelayButton(context, views, 3, "Switch 3", false, R.id.btn_relay3, R.id.txt_relay3_name, R.id.txt_relay3_state)
                bindRelayButton(context, views, 4, "Switch 4", false, R.id.btn_relay4, R.id.txt_relay4_name, R.id.txt_relay4_state)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun bindRelayButton(
        context: Context,
        views: RemoteViews,
        relayId: Int,
        name: String,
        isOn: Boolean,
        btnLayoutId: Int,
        nameViewId: Int,
        stateViewId: Int
    ) {
        views.setTextViewText(nameViewId, name)
        views.setTextViewText(stateViewId, if (isOn) "ON" else "OFF")
        views.setTextColor(stateViewId, if (isOn) Color.parseColor("#3DDC84") else Color.parseColor("#8B93A3"))
        views.setInt(
            btnLayoutId,
            "setBackgroundResource",
            if (isOn) R.drawable.widget_btn_on else R.drawable.widget_btn_off
        )

        val toggleIntent = Intent(context, SwitchboardWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_RELAY
            putExtra(EXTRA_RELAY_ID, relayId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            relayId,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(btnLayoutId, pendingIntent)
    }
}
