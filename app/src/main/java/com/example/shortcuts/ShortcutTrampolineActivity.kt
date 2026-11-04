package com.example.shortcuts

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.util.BiometricHelper
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

class ShortcutTrampolineActivity : FragmentActivity() {

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

        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, getString(R.string.no_internet_command_not_sent), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val prefs = PreferencesRepository(applicationContext)
            val appPrefs = prefs.preferencesFlow.first()
            val visibility = prefs.switchVisibilityFlow.first()
            val updates = mutableMapOf<String, Boolean>()
            for (id in 1..4) {
                if (visibility[id] != false) {
                    updates["relay$id"] = targetOn
                }
            }

            if (updates.isEmpty()) {
                Toast.makeText(
                    this@ShortcutTrampolineActivity,
                    getString(R.string.all_switches_hidden_toast),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            if (appPrefs.biometricLockEnabled && appPrefs.requireExternalUnlock) {
                BiometricHelper.showBiometricPrompt(
                    activity = this@ShortcutTrampolineActivity,
                    title = getString(R.string.biometric_prompt_title),
                    subtitle = getString(R.string.biometric_prompt_subtitle),
                    onSuccess = {
                        CoroutineScope(Dispatchers.Main).launch {
                            executeShortcutAction(appPrefs.boardId, targetOn, updates)
                        }
                    },
                    onError = {
                        finish()
                    }
                )
            } else {
                executeShortcutAction(appPrefs.boardId, targetOn, updates)
            }
        }
    }

    private suspend fun executeShortcutAction(
        boardId: String,
        targetOn: Boolean,
        updates: Map<String, Boolean>
    ) {
        if (updates.isEmpty()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.all_switches_hidden_toast),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        try {
            withTimeout(8000L) {
                FirebaseDatabase.getInstance()
                    .getReference(boardId)
                    .updateChildren(updates as Map<String, Any>)
                    .await()
            }

            val msg = if (targetOn) "All switches turned ON" else "All switches turned OFF"
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        } catch (_: TimeoutCancellationException) {
            Toast.makeText(
                applicationContext,
                getString(R.string.command_timed_out),
                Toast.LENGTH_SHORT
            ).show()
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
