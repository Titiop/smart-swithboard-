package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.R
import com.example.data.PreferencesRepository
import com.example.util.NetworkUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class WidgetToggleReceiver : BroadcastReceiver() {

    companion object {
        suspend fun performRelayToggle(appContext: Context, boardId: String, relayId: Int) {
            try {
                withTimeout(8000L) {
                    val ref = FirebaseDatabase.getInstance().getReference(boardId).child("relay$relayId")

                    val currentSnap = ref.get().await()
                    val currentVal = currentSnap.getValue(Boolean::class.java) ?: false
                    val targetVal = !currentVal

                    ref.setValue(targetVal).await()
                }
                SwitchboardWidgetProvider.updateAllWidgets(appContext)
            } catch (_: TimeoutCancellationException) {
                Toast.makeText(appContext, appContext.getString(R.string.command_timed_out), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    appContext,
                    "Widget toggle failed: ${e.localizedMessage ?: "Error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val relayId = intent.getIntExtra(SwitchboardWidgetProvider.EXTRA_RELAY_ID, -1)
        if (relayId !in 1..4) return

        val auth = try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
        if (auth?.currentUser == null) {
            Toast.makeText(context, "Please open Smart Switchboard and sign in", Toast.LENGTH_SHORT).show()
            return
        }

        if (!NetworkUtil.isOnline(context)) {
            Toast.makeText(context, context.getString(R.string.no_internet_command_not_sent), Toast.LENGTH_SHORT).show()
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val prefs = PreferencesRepository(appContext)
                val appPrefs = prefs.preferencesFlow.first()
                if (appPrefs.biometricLockEnabled && appPrefs.requireExternalUnlock) {
                    val authIntent = Intent(appContext, WidgetToggleAuthActivity::class.java).apply {
                        putExtra(SwitchboardWidgetProvider.EXTRA_RELAY_ID, relayId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    appContext.startActivity(authIntent)
                } else {
                    performRelayToggle(appContext, appPrefs.boardId, relayId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
