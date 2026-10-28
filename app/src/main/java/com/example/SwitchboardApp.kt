package com.example

import android.app.Application
import android.content.Context
import com.example.data.PreferencesRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SwitchboardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized()
    }

    private fun ensureFirebaseInitialized() {
        try {
            val customConfig = runCatching {
                runBlocking { PreferencesRepository(this@SwitchboardApp).firebaseConfigFlow.first() }
            }.getOrNull()

            if (customConfig != null && customConfig.isCustomConfigured) {
                configureFirebase(
                    context = this,
                    apiKey = customConfig.apiKey,
                    projectId = customConfig.projectId,
                    databaseUrl = customConfig.databaseUrl.ifBlank {
                        "https://${customConfig.projectId}-default-rtdb.firebaseio.com"
                    }
                )
            } else if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:100000000001:android:3ddc843ddc843ddc")
                    .setApiKey("AIzaSyDummyKeyForSmartSwitchboard0001")
                    .setProjectId("smart-switchboard-esp")
                    .setDatabaseUrl("https://smart-switchboard-esp-default-rtdb.firebaseio.com")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (_: Exception) {}
    }

    companion object {
        fun configureFirebase(
            context: Context,
            apiKey: String,
            projectId: String,
            databaseUrl: String
        ): Boolean {
            return try {
                val cleanKey = apiKey.trim()
                val cleanProjectId = projectId.trim()
                val cleanDbUrl = databaseUrl.trim().ifBlank {
                    "https://$cleanProjectId-default-rtdb.firebaseio.com"
                }

                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setApiKey(cleanKey)
                    .setProjectId(cleanProjectId)
                    .setDatabaseUrl(cleanDbUrl)
                    .build()

                val apps = FirebaseApp.getApps(context)
                if (apps.isNotEmpty()) {
                    val app = apps.first()
                    app.delete()
                }
                FirebaseApp.initializeApp(context, options)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
