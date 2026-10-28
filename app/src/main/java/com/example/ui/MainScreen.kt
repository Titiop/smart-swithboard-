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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPreferences
import com.example.data.model.BoardStatus
import com.example.data.model.SceneItem
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import com.example.ui.theme.ThemeMode
import com.example.util.HapticUtil

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
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val extraColors = LocalSwitchboardColors.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var editingSwitch by remember { mutableStateOf<SwitchRelay?>(null) }
    var showScenesSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val activeCount = switches.count { it.isOn }

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

                    // Summary line under title: "2 of 4 ON" (animated number change)
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
                            text = " of ${switches.size.coerceAtLeast(4)} ON",
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

            // 2x2 Grid of Switch Cards (or 4 columns in landscape)
            val columns = if (isLandscape) 4 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(switches, key = { it.id }) { relay ->
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
                            .height(150.dp)
                    )
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
                    enabled = isConnected,
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
                    enabled = isConnected,
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
            onLogout = onLogout
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
