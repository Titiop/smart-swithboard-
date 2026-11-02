package com.example.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppPreferences
import com.example.data.model.BoardStatus
import com.example.data.model.RelayTimerInfo
import com.example.data.model.ScheduleItem
import com.example.data.model.SceneItem
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import com.example.ui.theme.ThemeMode
import com.example.util.BackupPayload
import com.example.util.HapticUtil
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    switches: List<SwitchRelay>,
    scenes: List<SceneItem>,
    appPreferences: AppPreferences,
    isConnected: Boolean,
    boardStatus: BoardStatus,
    userEmail: String?,
    snackbarHostState: SnackbarHostState,
    onToggleSwitch: (SwitchRelay) -> Unit,
    onSetAllSwitches: (Boolean) -> Unit,
    onApplyScene: (SceneItem) -> Unit,
    onUpdateSwitchMeta: (id: Int, name: String, iconKey: String) -> Unit,
    onSaveScene: (SceneItem) -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (fromIndex: Int, toIndex: Int) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onBoardIdChange: (String) -> Unit,
    firebaseConfig: com.example.data.model.FirebaseProjectConfig = com.example.data.model.FirebaseProjectConfig(),
    onSaveFirebaseConfig: (String, String, String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit,
    switchVisibility: Map<Int, Boolean> = mapOf(1 to true, 2 to true, 3 to true, 4 to true),
    onUpdateSwitchVisibility: (Int, Boolean) -> Unit = { _, _ -> },
    savedBoards: List<String> = emptyList(),
    onAddSavedBoard: (String) -> Unit = {},
    onRemoveSavedBoard: (String) -> Unit = {},
    isDemoMode: Boolean = false,
    onFetchBoardInfo: suspend (String) -> BoardDeviceInfo? = { null },
    onExportBackup: () -> String = { "" },
    onRestoreBackup: (BackupPayload) -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    schedules: List<ScheduleItem> = emptyList(),
    timers: Map<Int, RelayTimerInfo> = emptyMap(),
    serverTimeOffsetMs: Long = 0L,
    onSchedulesSheetOpenChange: (Boolean) -> Unit = {},
    onSaveSchedule: (ScheduleItem, Boolean) -> Unit = { _, _ -> },
    onToggleScheduleEnabled: (ScheduleItem, Boolean) -> Unit = { _, _ -> },
    onDeleteSchedule: (String) -> Unit = {},
    onStartRelayTimer: (relayId: Int, action: Boolean, minutes: Int) -> Unit = { _, _, _ -> },
    onCancelRelayTimer: (relayId: Int) -> Unit = {},
    onFetchAutoOffMinutes: suspend (relayId: Int) -> Int = { 0 },
    onSaveAutoOffMinutes: (relayId: Int, minutes: Int) -> Unit = { _, _ -> },
    onHideRecentPreviewChange: (Boolean) -> Unit = {},
    onRequireExternalUnlockChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val extraColors = LocalSwitchboardColors.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var editingSwitch by remember { mutableStateOf<SwitchRelay?>(null) }
    var showScenesSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSchedulesSheet by remember { mutableStateOf(false) }

    // Update once per minute while visible to compute remaining timer labels
    var currentTickMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timers, serverTimeOffsetMs) {
        while (true) {
            currentTickMs = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    val estimatedServerNowMs = currentTickMs + serverTimeOffsetMs

    fun computeTimerLabel(relayId: Int): String? {
        val timer = timers[relayId] ?: return null
        val remainingMs = timer.atMs - estimatedServerNowMs
        if (remainingMs <= 0L) return null
        val remainingMinutes = ((remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
        val actionWord = if (timer.action) "ON" else "OFF"
        return "$actionWord in $remainingMinutes min"
    }

    val visibleSwitches = switches.filter { switchVisibility[it.id] != false }
    val activeCount = visibleSwitches.count { it.isOn }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Switchboard",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = extraColors.textPrimary
                        )

                        // Connection Status Chip (green/red dot)
                        ConnectionChip(isConnected = isConnected)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Summary line under title: "x of N ON" (animated number change)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = activeCount,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) +
                                        slideInVertically(animationSpec = tween(220)) { height -> height / 2 })
                                    .togetherWith(fadeOut(animationSpec = tween(150)))
                            },
                            label = "activeCountAnimation"
                        ) { count ->
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (count > 0) extraColors.brandAccent else extraColors.textSecondary
                            )
                        }
                        Text(
                            text = " of ${visibleSwitches.size} ON",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = extraColors.textSecondary
                        )
                    }

                    // Optional Board Status (from lastSeen)
                    if (boardStatus.hasLastSeen) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = boardStatus.statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (boardStatus.isBoardOnline) extraColors.brandAccent else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Settings icon button
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = AppIcons.Settings,
                        contentDescription = "Settings",
                        tint = extraColors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Saved Boards Quick Switch Chip Row (shown only when more than one board is saved)
            if (savedBoards.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    savedBoards.forEach { boardId ->
                        val isSelected = boardId == appPreferences.boardId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    HapticUtil.performLightHaptic(context, appPreferences.hapticFeedback)
                                    onBoardIdChange(boardId)
                                }
                            },
                            label = {
                                Text(
                                    text = boardId,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extraColors.brandAccent,
                                selectedLabelColor = Color(0xFF0E1013),
                                containerColor = extraColors.offCard,
                                labelColor = extraColors.textSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("saved_board_chip_$boardId")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Offline Banner when not connected to Firebase
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No connection - controls disabled",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 2x2 Grid of Switch Cards (or 4 columns in landscape), or empty state if all hidden
            if (visibleSwitches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.all_switches_hidden_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val columns = if (isLandscape) 4 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(visibleSwitches, key = { it.id }) { relay ->
                        SwitchCard(
                            relay = relay,
                            isEnabled = isConnected,
                            onToggle = {
                                HapticUtil.performLightHaptic(context, appPreferences.hapticFeedback)
                                onToggleSwitch(relay)
                            },
                            onLongPress = {
                                editingSwitch = relay
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            timerLabel = computeTimerLabel(relay.id)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Action Chips Row below grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All ON Chip
                AssistChip(
                    onClick = {
                        HapticUtil.performLightHaptic(context, appPreferences.hapticFeedback)
                        onSetAllSwitches(true)
                    },
                    enabled = isConnected && visibleSwitches.isNotEmpty(),
                    label = { Text("All ON", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Power,
                            contentDescription = null,
                            tint = extraColors.brandAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = extraColors.offCard,
                        labelColor = extraColors.textPrimary
                    ),
                    border = BorderStroke(1.dp, extraColors.cardBorder),
                    modifier = Modifier.testTag("all_on_chip")
                )

                // All OFF Chip
                AssistChip(
                    onClick = {
                        HapticUtil.performLightHaptic(context, appPreferences.hapticFeedback)
                        onSetAllSwitches(false)
                    },
                    enabled = isConnected && visibleSwitches.isNotEmpty(),
                    label = { Text("All OFF", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Power,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = extraColors.offCard,
                        labelColor = extraColors.textPrimary
                    ),
                    border = BorderStroke(1.dp, extraColors.cardBorder),
                    modifier = Modifier.testTag("all_off_chip")
                )

                // Saved Scenes Chips
                scenes.forEach { scene ->
                    AssistChip(
                        onClick = {
                            HapticUtil.performLightHaptic(context, appPreferences.hapticFeedback)
                            onApplyScene(scene)
                        },
                        enabled = isConnected,
                        label = { Text(scene.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Scenes,
                                contentDescription = null,
                                tint = extraColors.brandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = extraColors.offCard,
                            labelColor = extraColors.textPrimary
                        ),
                        border = BorderStroke(1.dp, extraColors.cardBorder)
                    )
                }

                // Manage Scenes Button
                AssistChip(
                    onClick = { showScenesSheet = true },
                    label = { Text("Scenes") },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = null,
                            tint = extraColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = extraColors.offCard.copy(alpha = 0.6f),
                        labelColor = extraColors.textSecondary
                    ),
                    border = BorderStroke(1.dp, extraColors.cardBorder),
                    modifier = Modifier.testTag("manage_scenes_chip")
                )
            }
        }
    }

    // Edit Switch Bottom Sheet
    editingSwitch?.let { relay ->
        EditSwitchSheet(
            relay = relay,
            onDismiss = { editingSwitch = null },
            onSave = { name, iconKey ->
                onUpdateSwitchMeta(relay.id, name, iconKey)
                editingSwitch = null
            },
            initialVisible = switchVisibility[relay.id] ?: true,
            onSaveVisibility = { visible ->
                onUpdateSwitchVisibility(relay.id, visible)
            },
            isConnected = isConnected,
            activeTimerLabel = computeTimerLabel(relay.id),
            onStartTimer = { action, mins ->
                onStartRelayTimer(relay.id, action, mins)
                currentTickMs = System.currentTimeMillis()
            },
            onCancelTimer = {
                onCancelRelayTimer(relay.id)
                currentTickMs = System.currentTimeMillis()
            },
            onFetchAutoOffMinutes = {
                onFetchAutoOffMinutes(relay.id)
            },
            onSaveAutoOffMinutes = { mins ->
                onSaveAutoOffMinutes(relay.id, mins)
            }
        )
    }

    // Scenes Management Bottom Sheet
    if (showScenesSheet) {
        ScenesSheet(
            scenes = scenes,
            switches = switches,
            isEnabled = isConnected,
            onDismiss = { showScenesSheet = false },
            onApplyScene = onApplyScene,
            onSaveScene = onSaveScene,
            onDeleteScene = onDeleteScene,
            onMoveScene = onMoveScene
        )
    }

    // Schedules Bottom Sheet (attaches Firebase listener only while open)
    if (showSchedulesSheet) {
        SchedulesSheet(
            schedules = schedules,
            switches = switches,
            isConnected = isConnected,
            onDismiss = {
                showSchedulesSheet = false
                onSchedulesSheetOpenChange(false)
            },
            onSaveSchedule = onSaveSchedule,
            onToggleScheduleEnabled = onToggleScheduleEnabled,
            onDeleteSchedule = onDeleteSchedule
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsSheet(
            preferences = appPreferences,
            userEmail = userEmail,
            onDismiss = { showSettingsSheet = false },
            onThemeChange = onThemeChange,
            onDynamicColorChange = onDynamicColorChange,
            onHapticChange = onHapticChange,
            onBiometricChange = onBiometricChange,
            onBoardIdChange = onBoardIdChange,
            firebaseConfig = firebaseConfig,
            onSaveFirebaseConfig = onSaveFirebaseConfig,
            onLogout = onLogout,
            switches = switches,
            switchVisibility = switchVisibility,
            onSwitchVisibilityChange = onUpdateSwitchVisibility,
            savedBoards = savedBoards,
            onAddSavedBoard = onAddSavedBoard,
            onRemoveSavedBoard = onRemoveSavedBoard,
            isDemoMode = isDemoMode,
            onFetchBoardInfo = onFetchBoardInfo,
            onExportBackup = onExportBackup,
            onRestoreBackup = onRestoreBackup,
            onShowMessage = onShowMessage,
            onOpenSchedules = {
                showSchedulesSheet = true
                onSchedulesSheetOpenChange(true)
            },
            onHideRecentPreviewChange = onHideRecentPreviewChange,
            onRequireExternalUnlockChange = onRequireExternalUnlockChange
        )
    }
}

@Composable
fun ConnectionChip(isConnected: Boolean) {
    val dotColor = if (isConnected) Color(0xFF3DDC84) else Color(0xFFFF6B6B)
    val text = if (isConnected) "Online" else "Offline"
    val extraColors = LocalSwitchboardColors.current

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(extraColors.offCard)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = extraColors.textSecondary
        )
    }
}
