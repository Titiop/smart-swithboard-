package com.example.shortcuts

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.example.data.PreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShortcutTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }

        if (auth?.currentUser == null) {
            Toast.makeText(this, "Please open app and sign in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val targetOn = when (action) {
            "com.smartswitchboard.action.ALL_ON" -> true
            "com.smartswitchboard.action.ALL_OFF" -> false
            else -> {
                finish()
                return
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val prefs = PreferencesRepository(applicationContext)
                val boardId = prefs.preferencesFlow.first().boardId

                val updates = mapOf(
                    "relay1" to targetOn,
                    "relay2" to targetOn,
                    "relay3" to targetOn,
                    "relay4" to targetOn
                )

                FirebaseDatabase.getInstance()
                    .getReference(boardId)
                    .updateChildren(updates as Map<String, Any>)
                    .await()

                val msg = if (targetOn) "All switches turned ON" else "All switches turned OFF"
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Failed to update switches: ${e.localizedMessage ?: "Network error"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                finish()
            }
        }
    }
}
