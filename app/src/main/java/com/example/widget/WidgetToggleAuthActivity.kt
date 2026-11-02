package com.example.widget

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.util.BiometricHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetToggleAuthActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val relayId = intent?.getIntExtra(SwitchboardWidgetProvider.EXTRA_RELAY_ID, -1) ?: -1
        if (relayId !in 1..4) {
            finish()
            return
        }

        val appContext = applicationContext

        BiometricHelper.showBiometricPrompt(
            activity = this,
            title = getString(R.string.biometric_prompt_title),
            subtitle = getString(R.string.biometric_prompt_subtitle),
            onSuccess = {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val prefs = PreferencesRepository(appContext)
                        val boardId = prefs.preferencesFlow.first().boardId
                        WidgetToggleReceiver.performRelayToggle(appContext, boardId, relayId)
                    } finally {
                        finish()
                    }
                }
            },
            onError = {
                finish()
            }
        )
    }
}
