package com.example.tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.data.FirebaseRepository
import com.example.data.PreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

abstract class BaseSwitchTileService(private val relayId: Int) : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isRelayOn: Boolean = false
    private var switchName: String = "Switch $relayId"
    private var boardId: String = "switchboard1"

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
            if (auth?.currentUser == null) {
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    label = "Switch $relayId"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = "Tap to login"
                    }
                    updateTile()
                }
                return@launch
            }

            try {
                val prefs = PreferencesRepository(applicationContext)
                val names = prefs.switchNamesFlow.first()
                switchName = names[relayId] ?: "Switch $relayId"
                boardId = prefs.preferencesFlow.first().boardId

                // Read value with a single get() when tile becomes visible
                val snapshot = FirebaseDatabase.getInstance()
                    .getReference(boardId)
                    .child("relay$relayId")
                    .get()
                    .await()

                isRelayOn = snapshot.getValue(Boolean::class.java) ?: false

                qsTile?.apply {
                    state = if (isRelayOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = switchName
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = if (isRelayOn) "ON" else "OFF"
                    }
                    updateTile()
                }
            } catch (_: Exception) {
                // If offline or error, maintain current state
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
        if (auth?.currentUser == null) {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
            return
        }

        // Toggle state
        val target = !isRelayOn
        isRelayOn = target

        // Update tile UI immediately
        qsTile?.apply {
            state = if (target) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = switchName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (target) "ON" else "OFF"
            }
            updateTile()
        }

        // Write to Firebase
        serviceScope.launch {
            try {
                FirebaseDatabase.getInstance()
                    .getReference(boardId)
                    .child("relay$relayId")
                    .setValue(target)
                    .await()
            } catch (_: Exception) {
                // Rollback if failed
                isRelayOn = !target
                qsTile?.apply {
                    state = if (isRelayOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = if (isRelayOn) "ON" else "OFF"
                    }
                    updateTile()
                }
            }
        }
    }
}

class Switch1TileService : BaseSwitchTileService(1)
class Switch2TileService : BaseSwitchTileService(2)
class Switch3TileService : BaseSwitchTileService(3)
class Switch4TileService : BaseSwitchTileService(4)
