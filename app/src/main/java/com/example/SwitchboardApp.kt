package com.example

import android.app.Application
import android.content.Context
import com.example.data.PreferencesRepository
import com.example.data.model.FirebaseProjectConfig
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
            val bootPrefs = getSharedPreferences(BOOT_PREFS_NAME, Context.MODE_PRIVATE)
            val isMigrated = bootPrefs.getBoolean(KEY_MIGRATED, false)

            val customConfig: FirebaseProjectConfig? = if (isMigrated) {
                FirebaseProjectConfig(
                    apiKey = bootPrefs.getString(KEY_API_KEY, "") ?: "",
                    projectId = bootPrefs.getString(KEY_PROJECT_ID, "") ?: "",
                    databaseUrl = bootPrefs.getString(KEY_DATABASE_URL, "") ?: ""
                )
            } else {
                val migratedConfig = runCatching {
                    runBlocking { PreferencesRepository(this@SwitchboardApp).firebaseConfigFlow.first() }
                }.getOrNull()

                bootPrefs.edit()
                    .putString(KEY_API_KEY, migratedConfig?.apiKey?.trim() ?: "")
                    .putString(KEY_PROJECT_ID, migratedConfig?.projectId?.trim() ?: "")
                    .putString(KEY_DATABASE_URL, migratedConfig?.databaseUrl?.trim() ?: "")
                    .putBoolean(KEY_MIGRATED, true)
                    .apply()

                migratedConfig
            }

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
        const val BOOT_PREFS_NAME = "firebase_boot"
        const val KEY_API_KEY = "apiKey"
        const val KEY_PROJECT_ID = "projectId"
        const val KEY_DATABASE_URL = "databaseUrl"
        const val KEY_MIGRATED = "migrated"

        fun saveBootConfig(
            context: Context,
            apiKey: String,
            projectId: String,
            databaseUrl: String
        ) {
            try {
                context.getSharedPreferences(BOOT_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_API_KEY, apiKey.trim())
                    .putString(KEY_PROJECT_ID, projectId.trim())
                    .putString(KEY_DATABASE_URL, databaseUrl.trim())
                    .putBoolean(KEY_MIGRATED, true)
                    .apply()
            } catch (_: Exception) {}
        }

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

                saveBootConfig(context, cleanKey, cleanProjectId, cleanDbUrl)

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
