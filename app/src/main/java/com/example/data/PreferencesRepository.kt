package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.SceneItem
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "switchboard_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val BOARD_ID = stringPreferencesKey("board_id")

        val SWITCH1_NAME = stringPreferencesKey("switch1_name")
        val SWITCH2_NAME = stringPreferencesKey("switch2_name")
        val SWITCH3_NAME = stringPreferencesKey("switch3_name")
        val SWITCH4_NAME = stringPreferencesKey("switch4_name")

        val SWITCH1_ICON = stringPreferencesKey("switch1_icon")
        val SWITCH2_ICON = stringPreferencesKey("switch2_icon")
        val SWITCH3_ICON = stringPreferencesKey("switch3_icon")
        val SWITCH4_ICON = stringPreferencesKey("switch4_icon")

        val SCENES_JSON = stringPreferencesKey("scenes_json")

        val FIREBASE_API_KEY = stringPreferencesKey("firebase_api_key")
        val FIREBASE_PROJECT_ID = stringPreferencesKey("firebase_project_id")
        val FIREBASE_DATABASE_URL = stringPreferencesKey("firebase_database_url")
    }

    val firebaseConfigFlow: Flow<com.example.data.model.FirebaseProjectConfig> = context.dataStore.data.map { prefs ->
        com.example.data.model.FirebaseProjectConfig(
            apiKey = prefs[Keys.FIREBASE_API_KEY] ?: "",
            projectId = prefs[Keys.FIREBASE_PROJECT_ID] ?: "",
            databaseUrl = prefs[Keys.FIREBASE_DATABASE_URL] ?: ""
        )
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        val dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false
        val haptic = prefs[Keys.HAPTIC_FEEDBACK] ?: true
        val biometric = prefs[Keys.BIOMETRIC_LOCK] ?: false
        val boardId = prefs[Keys.BOARD_ID]?.ifBlank { "switchboard1" } ?: "switchboard1"

        AppPreferences(
            themeMode = theme,
            dynamicColor = dynamicColor,
            hapticFeedback = haptic,
            biometricLockEnabled = biometric,
            boardId = boardId
        )
    }

    val switchNamesFlow: Flow<Map<Int, String>> = context.dataStore.data.map { prefs ->
        mapOf(
            1 to (prefs[Keys.SWITCH1_NAME] ?: "Switch 1"),
            2 to (prefs[Keys.SWITCH2_NAME] ?: "Switch 2"),
            3 to (prefs[Keys.SWITCH3_NAME] ?: "Switch 3"),
            4 to (prefs[Keys.SWITCH4_NAME] ?: "Switch 4")
        )
    }

    val switchIconsFlow: Flow<Map<Int, String>> = context.dataStore.data.map { prefs ->
        mapOf(
            1 to (prefs[Keys.SWITCH1_ICON] ?: "bulb"),
            2 to (prefs[Keys.SWITCH2_ICON] ?: "fan"),
            3 to (prefs[Keys.SWITCH3_ICON] ?: "plug"),
            4 to (prefs[Keys.SWITCH4_ICON] ?: "tv")
        )
    }

    val scenesFlow: Flow<List<SceneItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.SCENES_JSON]
        if (json.isNullOrBlank()) {
            defaultScenes()
        } else {
            deserializeScenes(json)
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun updateBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun updateBoardId(boardId: String) {
        val sanitized = boardId.trim().ifEmpty { "switchboard1" }
        context.dataStore.edit { it[Keys.BOARD_ID] = sanitized }
    }

    suspend fun updateSwitchMeta(id: Int, name: String, iconKey: String) {
        context.dataStore.edit { prefs ->
            when (id) {
                1 -> {
                    prefs[Keys.SWITCH1_NAME] = name.trim().ifEmpty { "Switch 1" }
                    prefs[Keys.SWITCH1_ICON] = iconKey
                }
                2 -> {
                    prefs[Keys.SWITCH2_NAME] = name.trim().ifEmpty { "Switch 2" }
                    prefs[Keys.SWITCH2_ICON] = iconKey
                }
                3 -> {
                    prefs[Keys.SWITCH3_NAME] = name.trim().ifEmpty { "Switch 3" }
                    prefs[Keys.SWITCH3_ICON] = iconKey
                }
                4 -> {
                    prefs[Keys.SWITCH4_NAME] = name.trim().ifEmpty { "Switch 4" }
                    prefs[Keys.SWITCH4_ICON] = iconKey
                }
            }
        }
    }

    suspend fun saveScenes(scenes: List<SceneItem>) {
        val json = serializeScenes(scenes)
        context.dataStore.edit { it[Keys.SCENES_JSON] = json }
    }

    suspend fun saveFirebaseConfig(apiKey: String, projectId: String, databaseUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIREBASE_API_KEY] = apiKey.trim()
            prefs[Keys.FIREBASE_PROJECT_ID] = projectId.trim()
            prefs[Keys.FIREBASE_DATABASE_URL] = databaseUrl.trim()
        }
    }

    private fun defaultScenes(): List<SceneItem> {
        return listOf(
            SceneItem(
                id = "scene_good_night",
                name = "Good Night",
                relay1 = false,
                relay2 = false,
                relay3 = false,
                relay4 = false
            ),
            SceneItem(
                id = "scene_away",
                name = "Away",
                relay1 = false,
                relay2 = false,
                relay3 = false,
                relay4 = false
            )
        )
    }

    private fun serializeScenes(scenes: List<SceneItem>): String {
        val jsonArray = JSONArray()
        for (scene in scenes) {
            val obj = JSONObject().apply {
                put("id", scene.id)
                put("name", scene.name)
                scene.relay1?.let { put("relay1", it) }
                scene.relay2?.let { put("relay2", it) }
                scene.relay3?.let { put("relay3", it) }
                scene.relay4?.let { put("relay4", it) }
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun deserializeScenes(jsonString: String): List<SceneItem> {
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<SceneItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Scene ${i + 1}")
                val r1 = if (obj.has("relay1")) obj.getBoolean("relay1") else null
                val r2 = if (obj.has("relay2")) obj.getBoolean("relay2") else null
                val r3 = if (obj.has("relay3")) obj.getBoolean("relay3") else null
                val r4 = if (obj.has("relay4")) obj.getBoolean("relay4") else null
                list.add(SceneItem(id = id, name = name, relay1 = r1, relay2 = r2, relay3 = r3, relay4 = r4))
            }
            if (list.isEmpty()) defaultScenes() else list
        } catch (e: Exception) {
            defaultScenes()
        }
    }
}
