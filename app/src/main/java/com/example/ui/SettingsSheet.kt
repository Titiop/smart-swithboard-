package com.example.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import com.example.ui.theme.ThemeMode
import com.example.util.BackupPayload
import com.example.util.BackupUtil
import com.example.util.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: AppPreferences,
    userEmail: String?,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onBoardIdChange: (String) -> Unit,
    firebaseConfig: com.example.data.model.FirebaseProjectConfig = com.example.data.model.FirebaseProjectConfig(),
    onSaveFirebaseConfig: (String, String, String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit,
    switches: List<SwitchRelay> = emptyList(),
    switchVisibility: Map<Int, Boolean> = mapOf(1 to true, 2 to true, 3 to true, 4 to true),
    onSwitchVisibilityChange: (Int, Boolean) -> Unit = { _, _ -> },
    savedBoards: List<String> = emptyList(),
    onAddSavedBoard: (String) -> Unit = {},
    onRemoveSavedBoard: (String) -> Unit = {},
    isDemoMode: Boolean = false,
    onFetchBoardInfo: suspend (String) -> BoardDeviceInfo? = { null },
    onExportBackup: () -> String = { "" },
    onRestoreBackup: (BackupPayload) -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onOpenSchedules: () -> Unit = {},
    onHideRecentPreviewChange: (Boolean) -> Unit = {},
    onRequireExternalUnlockChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val extraColors = LocalSwitchboardColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showBoardInfoDialog by remember { mutableStateOf(false) }
    var showSavedBoardsDialog by remember { mutableStateOf(false) }
    var pendingBackupToRestore by remember { mutableStateOf<BackupPayload?>(null) }
    var boardIdInput by remember { mutableStateOf(preferences.boardId) }
    var boardIdError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(preferences.boardId) {
        boardIdInput = preferences.boardId
        boardIdError = false
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = onExportBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onShowMessage(context.getString(R.string.backup_exported_success))
            } catch (_: Exception) {
                onShowMessage(context.getString(R.string.invalid_backup_file))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                val parsed = if (content != null) BackupUtil.parseAndValidateBackup(content) else null
                if (parsed == null) {
                    onShowMessage(context.getString(R.string.invalid_backup_file))
                } else {
                    pendingBackupToRestore = parsed
                }
            } catch (_: Exception) {
                onShowMessage(context.getString(R.string.invalid_backup_file))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.textPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Account section
            userEmail?.let { email ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(extraColors.offCard)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signed in as",
                            style = MaterialTheme.typography.labelSmall,
                            color = extraColors.textSecondary
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = extraColors.textPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(AppIcons.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Theme section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = extraColors.brandAccent
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark",
                    ThemeMode.AMOLED to "AMOLED"
                ).forEach { (mode, label) ->
                    val isSelected = preferences.themeMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onThemeChange(mode) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extraColors.brandAccent,
                            selectedLabelColor = Color(0xFF0E1013)
                        )
                    )
                }
            }

            // Dynamic color on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Material You Dynamic Colors",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extraColors.textPrimary
                        )
                        Text(
                            text = "Default is OFF to preserve brand green",
                            style = MaterialTheme.typography.labelSmall,
                            color = extraColors.textSecondary
                        )
                    }
                    Switch(
                        checked = preferences.dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0E1013),
                            checkedTrackColor = extraColors.brandAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Switches visibility section
            Text(
                text = stringResource(R.string.settings_switches_section),
                style = MaterialTheme.typography.labelLarge,
                color = extraColors.brandAccent
            )
            Spacer(modifier = Modifier.height(8.dp))

            (1..4).forEach { id ->
                val swName = switches.find { it.id == id }?.name ?: "Switch $id"
                val isVisible = switchVisibility[id] ?: true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = swName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { checked -> onSwitchVisibilityChange(id, checked) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0E1013),
                            checkedTrackColor = extraColors.brandAccent
                        ),
                        modifier = Modifier.testTag("settings_switch_visible_$id")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSchedules()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_schedules_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedules_title),
                    color = extraColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactions section
            Text(
                text = "Interactions & Security",
                style = MaterialTheme.typography.labelLarge,
                color = extraColors.brandAccent
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Haptic Feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Haptic Feedback",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textPrimary
                    )
                    Text(
                        text = "Vibrate lightly when toggling switches",
                        style = MaterialTheme.typography.labelSmall,
                        color = extraColors.textSecondary
                    )
                }
                Switch(
                    checked = preferences.hapticFeedback,
                    onCheckedChange = onHapticChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0E1013),
                        checkedTrackColor = extraColors.brandAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Biometric App Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric App Lock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textPrimary
                    )
                    Text(
                        text = "Lock app on open and after 30s in background",
                        style = MaterialTheme.typography.labelSmall,
                        color = extraColors.textSecondary
                    )
                }
                Switch(
                    checked = preferences.biometricLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !BiometricHelper.isBiometricAvailable(context)) {
                            // Biometric not available on device
                        } else {
                            onBiometricChange(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0E1013),
                        checkedTrackColor = extraColors.brandAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Require unlock for widget, tiles and shortcuts (effective when biometric lock is ON)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.require_external_unlock_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (preferences.biometricLockEnabled) extraColors.textPrimary else extraColors.textSecondary
                    )
                    Text(
                        text = stringResource(R.string.require_external_unlock_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = extraColors.textSecondary
                    )
                }
                Switch(
                    checked = preferences.requireExternalUnlock,
                    onCheckedChange = onRequireExternalUnlockChange,
                    enabled = preferences.biometricLockEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0E1013),
                        checkedTrackColor = extraColors.brandAccent
                    ),
                    modifier = Modifier.testTag("require_external_unlock_toggle")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hide app preview in recent apps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.hide_recent_preview_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.hide_recent_preview_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = extraColors.textSecondary
                    )
                }
                Switch(
                    checked = preferences.hideRecentPreview,
                    onCheckedChange = onHideRecentPreviewChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0E1013),
                        checkedTrackColor = extraColors.brandAccent
                    ),
                    modifier = Modifier.testTag("hide_recent_preview_toggle")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Backup and restore section
            Text(
                text = stringResource(R.string.backup_and_restore_section),
                style = MaterialTheme.typography.labelLarge,
                color = extraColors.brandAccent
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("smart_switchboard_backup.json") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup_export_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.backup_export), color = extraColors.textPrimary)
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("backup_import_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.backup_import), color = extraColors.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Advanced section
            Text(
                text = "Advanced Hardware Config",
                style = MaterialTheme.typography.labelLarge,
                color = extraColors.brandAccent
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = boardIdInput,
                onValueChange = {
                    boardIdInput = it
                    val trimmed = it.trim()
                    if (BackupUtil.isValidBoardId(trimmed)) {
                        boardIdError = false
                        onBoardIdChange(trimmed)
                    } else {
                        boardIdError = trimmed.isNotEmpty()
                    }
                },
                isError = boardIdError,
                label = { Text("Board ID (Firebase Path)") },
                supportingText = {
                    if (boardIdError) {
                        Text(stringResource(R.string.saved_boards_invalid_id), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Firebase Realtime Database root node (default: switchboard1)")
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("board_id_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = extraColors.brandAccent,
                    unfocusedBorderColor = extraColors.cardBorder,
                    focusedTextColor = extraColors.textPrimary,
                    unfocusedTextColor = extraColors.textPrimary,
                    focusedLabelColor = extraColors.brandAccent,
                    unfocusedLabelColor = extraColors.textSecondary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showSavedBoardsDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("saved_boards_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.saved_boards_title), color = extraColors.textPrimary)
                }

                OutlinedButton(
                    onClick = { showBoardInfoDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("board_info_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.board_info_title), color = extraColors.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showConfigDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(AppIcons.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = extraColors.brandAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Firebase Web API Key & Project Setup", color = extraColors.textPrimary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(extraColors.offCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Smart Switchboard v1.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lightweight, real-time native controller for 4-relay ESP8266 IoT switchboards with live Firebase sync and zero background battery drain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extraColors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to sign out of Smart Switchboard?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onDismiss()
                        onLogout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfigDialog) {
        FirebaseConfigDialog(
            currentConfig = firebaseConfig,
            onDismiss = { showConfigDialog = false },
            onSave = onSaveFirebaseConfig
        )
    }

    if (showBoardInfoDialog) {
        BoardInfoDialog(
            boardId = preferences.boardId,
            isDemoMode = isDemoMode,
            onFetchBoardInfo = onFetchBoardInfo,
            onDismiss = { showBoardInfoDialog = false }
        )
    }

    if (showSavedBoardsDialog) {
        SavedBoardsDialog(
            savedBoards = savedBoards,
            currentBoardId = preferences.boardId,
            onSelectBoard = { selectedId ->
                boardIdInput = selectedId
                onBoardIdChange(selectedId)
            },
            onAddBoard = onAddSavedBoard,
            onRemoveBoard = onRemoveSavedBoard,
            onDismiss = { showSavedBoardsDialog = false }
        )
    }

    pendingBackupToRestore?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingBackupToRestore = null },
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestoreBackup(payload)
                        pendingBackupToRestore = null
                        onShowMessage(context.getString(R.string.backup_restored_success))
                    }
                ) {
                    Text("Restore", color = extraColors.brandAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackupToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
