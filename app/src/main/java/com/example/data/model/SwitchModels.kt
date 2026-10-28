package com.example.data.model

import com.example.ui.theme.ThemeMode

data class SwitchRelay(
    val id: Int, // 1, 2, 3, 4
    val relayKey: String, // "relay1", "relay2", "relay3", "relay4"
    val name: String,
    val iconKey: String,
    val isOn: Boolean,
    val isPending: Boolean = false
)

data class SceneItem(
    val id: String,
    val name: String,
    // true = turn ON, false = turn OFF, null = leave unchanged
    val relay1: Boolean? = null,
    val relay2: Boolean? = null,
    val relay3: Boolean? = null,
    val relay4: Boolean? = null
) {
    fun summary(): String {
        val actions = mutableListOf<String>()
        if (relay1 != null) actions.add("S1: ${if (relay1) "ON" else "OFF"}")
        if (relay2 != null) actions.add("S2: ${if (relay2) "ON" else "OFF"}")
        if (relay3 != null) actions.add("S3: ${if (relay3) "ON" else "OFF"}")
        if (relay4 != null) actions.add("S4: ${if (relay4) "ON" else "OFF"}")
        return if (actions.isEmpty()) "No changes" else actions.joinToString(", ")
    }
}

data class BoardStatus(
    val hasLastSeen: Boolean,
    val isBoardOnline: Boolean,
    val statusText: String
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val hapticFeedback: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val boardId: String = "switchboard1"
)

data class FirebaseProjectConfig(
    val apiKey: String = "",
    val projectId: String = "",
    val databaseUrl: String = ""
) {
    val isCustomConfigured: Boolean
        get() = apiKey.isNotBlank() && projectId.isNotBlank() && !apiKey.contains("Dummy", ignoreCase = true)
}
