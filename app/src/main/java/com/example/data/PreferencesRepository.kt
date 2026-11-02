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

        val SWITCH1_VISIBLE = booleanPreferencesKey("switch1_visible")
        val SWITCH2_VISIBLE = booleanPreferencesKey("switch2_visible")
        val SWITCH3_VISIBLE = booleanPreferencesKey("switch3_visible")
        val SWITCH4_VISIBLE = booleanPreferencesKey("switch4_visible")

        val SCENES_JSON = stringPreferencesKey("scenes_json")
        val SAVED_BOARDS = stringPreferencesKey("saved_boards")

        val FIREBASE_API_KEY = stringPreferencesKey("firebase_api_key")
        val FIREBASE_PROJECT_ID = stringPreferencesKey("firebase_project_id")
        val FIREBASE_DATABASE_URL = stringPreferencesKey("firebase_database_url")

        val HIDE_RECENT_PREVIEW = booleanPreferencesKey("hide_recent_preview")
        val REQUIRE_EXTERNAL_UNLOCK = booleanPreferencesKey("require_external_unlock")
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
        val hideRecentPreview = prefs[Keys.HIDE_RECENT_PREVIEW] ?: false
        val requireExternalUnlock = prefs[Keys.REQUIRE_EXTERNAL_UNLOCK] ?: false

        AppPreferences(
            themeMode = theme,
            dynamicColor = dynamicColor,
            hapticFeedback = haptic,
            biometricLockEnabled = biometric,
            boardId = boardId,
            hideRecentPreview = hideRecentPreview,
            requireExternalUnlock = requireExternalUnlock
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

    val switchVisibilityFlow: Flow<Map<Int, Boolean>> = context.dataStore.data.map { prefs ->
        mapOf(
            1 to (prefs[Keys.SWITCH1_VISIBLE] ?: true),
            2 to (prefs[Keys.SWITCH2_VISIBLE] ?: true),
            3 to (prefs[Keys.SWITCH3_VISIBLE] ?: true),
            4 to (prefs[Keys.SWITCH4_VISIBLE] ?: true)
        )
    }

    val savedBoardsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val currentBoard = prefs[Keys.BOARD_ID]?.ifBlank { "switchboard1" } ?: "switchboard1"
        val json = prefs[Keys.SAVED_BOARDS]
        if (json.isNullOrBlank()) {
            listOf(currentBoard)
        } else {
            val parsed = deserializeSavedBoards(json)
            if (parsed.isEmpty()) listOf(currentBoard) else parsed
        }
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

    suspend fun updateHideRecentPreview(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_RECENT_PREVIEW] = enabled }
    }

    suspend fun updateRequireExternalUnlock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REQUIRE_EXTERNAL_UNLOCK] = enabled }
    }

    suspend fun updateBoardId(boardId: String) {
        val sanitized = boardId.trim().ifEmpty { "switchboard1" }
        if (!com.example.util.BackupUtil.isValidBoardId(sanitized)) return
        context.dataStore.edit { it[Keys.BOARD_ID] = sanitized }
    }

    suspend fun updateSwitchVisibility(id: Int, visible: Boolean) {
        context.dataStore.edit { prefs ->
            when (id) {
                1 -> prefs[Keys.SWITCH1_VISIBLE] = visible
                2 -> prefs[Keys.SWITCH2_VISIBLE] = visible
                3 -> prefs[Keys.SWITCH3_VISIBLE] = visible
                4 -> prefs[Keys.SWITCH4_VISIBLE] = visible
            }
        }
    }

    suspend fun addSavedBoard(boardId: String) {
        val clean = boardId.trim()
        if (!com.example.util.BackupUtil.isValidBoardId(clean)) return
        context.dataStore.edit { prefs ->
            val currentBoard = prefs[Keys.BOARD_ID]?.ifBlank { "switchboard1" } ?: "switchboard1"
            val existing = deserializeSavedBoards(prefs[Keys.SAVED_BOARDS] ?: "").ifEmpty { listOf(currentBoard) }
            if (!existing.contains(clean)) {
                val updated = existing + clean
                prefs[Keys.SAVED_BOARDS] = serializeSavedBoards(updated)
            }
        }
    }

    suspend fun removeSavedBoard(boardId: String) {
        context.dataStore.edit { prefs ->
            val currentBoard = prefs[Keys.BOARD_ID]?.ifBlank { "switchboard1" } ?: "switchboard1"
            val existing = deserializeSavedBoards(prefs[Keys.SAVED_BOARDS] ?: "").ifEmpty { listOf(currentBoard) }
            if (existing.size <= 1) return@edit
            val updated = existing.filter { it != boardId }
            if (updated.isNotEmpty()) {
                prefs[Keys.SAVED_BOARDS] = serializeSavedBoards(updated)
                if (currentBoard == boardId) {
                    prefs[Keys.BOARD_ID] = updated.first()
                }
            }
        }
    }

    suspend fun applyBackup(payload: com.example.util.BackupPayload) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = payload.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = payload.dynamicColor
            prefs[Keys.HAPTIC_FEEDBACK] = payload.haptics
            prefs[Keys.BOARD_ID] = payload.boardId

            prefs[Keys.SWITCH1_NAME] = payload.switchNames[1] ?: "Switch 1"
            prefs[Keys.SWITCH2_NAME] = payload.switchNames[2] ?: "Switch 2"
            prefs[Keys.SWITCH3_NAME] = payload.switchNames[3] ?: "Switch 3"
            prefs[Keys.SWITCH4_NAME] = payload.switchNames[4] ?: "Switch 4"

            prefs[Keys.SWITCH1_ICON] = payload.switchIcons[1] ?: "bulb"
            prefs[Keys.SWITCH2_ICON] = payload.switchIcons[2] ?: "fan"
            prefs[Keys.SWITCH3_ICON] = payload.switchIcons[3] ?: "plug"
            prefs[Keys.SWITCH4_ICON] = payload.switchIcons[4] ?: "tv"

            prefs[Keys.SWITCH1_VISIBLE] = payload.switchVisibility[1] ?: true
            prefs[Keys.SWITCH2_VISIBLE] = payload.switchVisibility[2] ?: true
            prefs[Keys.SWITCH3_VISIBLE] = payload.switchVisibility[3] ?: true
            prefs[Keys.SWITCH4_VISIBLE] = payload.switchVisibility[4] ?: true

            prefs[Keys.SCENES_JSON] = serializeScenes(payload.scenes)

            val existingBoards = deserializeSavedBoards(prefs[Keys.SAVED_BOARDS] ?: "")
            if (existingBoards.isNotEmpty() && !existingBoards.contains(payload.boardId)) {
                prefs[Keys.SAVED_BOARDS] = serializeSavedBoards(existingBoards + payload.boardId)
            }
        }
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
        val cleanKey = apiKey.trim()
        val cleanProjectId = projectId.trim()
        val cleanDbUrl = databaseUrl.trim()
        com.example.SwitchboardApp.saveBootConfig(context, cleanKey, cleanProjectId, cleanDbUrl)
        context.dataStore.edit { prefs ->
            prefs[Keys.FIREBASE_API_KEY] = cleanKey
            prefs[Keys.FIREBASE_PROJECT_ID] = cleanProjectId
            prefs[Keys.FIREBASE_DATABASE_URL] = cleanDbUrl
        }
    }

    private fun serializeSavedBoards(boards: List<String>): String {
        val array = JSONArray()
        for (board in boards) {
            if (com.example.util.BackupUtil.isValidBoardId(board)) {
                array.put(board)
            }
        }
        return array.toString()
    }

    private fun deserializeSavedBoards(jsonString: String): List<String> {
        if (jsonString.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i, "").trim()
                if (com.example.util.BackupUtil.isValidBoardId(item) && !list.contains(item)) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
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
