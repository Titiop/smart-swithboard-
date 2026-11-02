package com.example.util

import com.example.data.model.AppPreferences
import com.example.data.model.SceneItem
import com.example.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val schemaVersion: Int = 1,
    val switchNames: Map<Int, String>,
    val switchIcons: Map<Int, String>,
    val switchVisibility: Map<Int, Boolean>,
    val scenes: List<SceneItem>,
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val haptics: Boolean,
    val boardId: String
)

object BackupUtil {

    private val BOARD_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,40}$")

    fun isValidBoardId(boardId: String): Boolean {
        return BOARD_ID_REGEX.matches(boardId)
    }

    fun exportToJson(
        preferences: AppPreferences,
        switchNames: Map<Int, String>,
        switchIcons: Map<Int, String>,
        switchVisibility: Map<Int, Boolean>,
        scenes: List<SceneItem>
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)

        val namesObj = JSONObject()
        for (id in 1..4) {
            namesObj.put(id.toString(), switchNames[id] ?: "Switch $id")
        }
        root.put("switchNames", namesObj)

        val iconsObj = JSONObject()
        for (id in 1..4) {
            val defaultIcon = when (id) {
                1 -> "bulb"
                2 -> "fan"
                3 -> "plug"
                else -> "tv"
            }
            iconsObj.put(id.toString(), switchIcons[id] ?: defaultIcon)
        }
        root.put("switchIcons", iconsObj)

        val visibilityObj = JSONObject()
        for (id in 1..4) {
            visibilityObj.put(id.toString(), switchVisibility[id] ?: true)
        }
        root.put("switchVisibility", visibilityObj)

        val scenesArray = JSONArray()
        for (scene in scenes) {
            val sceneObj = JSONObject().apply {
                put("id", scene.id)
                put("name", scene.name)
                if (scene.relay1 != null) put("relay1", scene.relay1) else put("relay1", JSONObject.NULL)
                if (scene.relay2 != null) put("relay2", scene.relay2) else put("relay2", JSONObject.NULL)
                if (scene.relay3 != null) put("relay3", scene.relay3) else put("relay3", JSONObject.NULL)
                if (scene.relay4 != null) put("relay4", scene.relay4) else put("relay4", JSONObject.NULL)
            }
            scenesArray.put(sceneObj)
        }
        root.put("scenes", scenesArray)

        root.put("themeMode", preferences.themeMode.name)
        root.put("dynamicColor", preferences.dynamicColor)
        root.put("haptics", preferences.hapticFeedback)
        root.put("boardId", preferences.boardId)

        return root.toString(2)
    }

    fun parseAndValidateBackup(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)

            if (!root.has("schemaVersion") || root.opt("schemaVersion") !is Int) return null
            val schemaVersion = root.getInt("schemaVersion")
            if (schemaVersion != 1) return null

            val namesObj = root.optJSONObject("switchNames") ?: return null
            val switchNames = mutableMapOf<Int, String>()
            for (id in 1..4) {
                val raw = namesObj.opt(id.toString()) as? String ?: return null
                if (raw.isBlank()) return null
                switchNames[id] = raw.trim()
            }

            val iconsObj = root.optJSONObject("switchIcons") ?: return null
            val switchIcons = mutableMapOf<Int, String>()
            for (id in 1..4) {
                val raw = iconsObj.opt(id.toString()) as? String ?: return null
                if (raw.isBlank()) return null
                switchIcons[id] = raw.trim()
            }

            val visibilityObj = root.optJSONObject("switchVisibility") ?: return null
            val switchVisibility = mutableMapOf<Int, Boolean>()
            for (id in 1..4) {
                val raw = visibilityObj.opt(id.toString()) as? Boolean ?: return null
                switchVisibility[id] = raw
            }

            val scenesArray = root.optJSONArray("scenes") ?: return null
            val scenes = mutableListOf<SceneItem>()
            for (i in 0 until scenesArray.length()) {
                val obj = scenesArray.optJSONObject(i) ?: return null
                val id = obj.opt("id") as? String ?: return null
                val name = obj.opt("name") as? String ?: return null
                if (id.isBlank() || name.isBlank()) return null

                val r1 = parseOptionalBoolean(obj, "relay1") ?: return null
                val r2 = parseOptionalBoolean(obj, "relay2") ?: return null
                val r3 = parseOptionalBoolean(obj, "relay3") ?: return null
                val r4 = parseOptionalBoolean(obj, "relay4") ?: return null

                scenes.add(
                    SceneItem(
                        id = id.trim(),
                        name = name.trim(),
                        relay1 = r1.value,
                        relay2 = r2.value,
                        relay3 = r3.value,
                        relay4 = r4.value
                    )
                )
            }

            val themeModeStr = root.opt("themeMode") as? String ?: return null
            val themeMode = try {
                ThemeMode.valueOf(themeModeStr)
            } catch (_: Exception) {
                return null
            }

            val dynamicColor = root.opt("dynamicColor") as? Boolean ?: return null
            val haptics = root.opt("haptics") as? Boolean ?: return null
            val boardId = (root.opt("boardId") as? String)?.trim() ?: return null
            if (!isValidBoardId(boardId)) return null

            BackupPayload(
                schemaVersion = schemaVersion,
                switchNames = switchNames,
                switchIcons = switchIcons,
                switchVisibility = switchVisibility,
                scenes = scenes,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                haptics = haptics,
                boardId = boardId
            )
        } catch (_: Exception) {
            null
        }
    }

    private data class BoxedNullableBoolean(val value: Boolean?)

    private fun parseOptionalBoolean(obj: JSONObject, key: String): BoxedNullableBoolean? {
        if (!obj.has(key) || obj.isNull(key)) {
            return BoxedNullableBoolean(null)
        }
        val raw = obj.opt(key)
        return if (raw is Boolean) {
            BoxedNullableBoolean(raw)
        } else {
            null
        }
    }
}
