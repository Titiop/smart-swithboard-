package com.example.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SceneItem
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesSheet(
    scenes: List<SceneItem>,
    switches: List<SwitchRelay>,
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onApplyScene: (SceneItem) -> Unit,
    onSaveScene: (SceneItem) -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingScene by remember { mutableStateOf<SceneItem?>(null) }
    var sceneToDelete by remember { mutableStateOf<SceneItem?>(null) }

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
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scenes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.textPrimary
                )

                Button(
                    onClick = {
                        editingScene = SceneItem(
                            id = UUID.randomUUID().toString(),
                            name = "",
                            relay1 = false,
                            relay2 = false,
                            relay3 = false,
                            relay4 = false
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.brandAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("create_scene_button")
                ) {
                    Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Scene")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (scenes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scenes created yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(scenes) { index, scene ->
                        SceneListItem(
                            scene = scene,
                            isEnabled = isEnabled,
                            canMoveUp = index > 0,
                            canMoveDown = index < scenes.size - 1,
                            onApply = { onApplyScene(scene) },
                            onEdit = { editingScene = scene },
                            onDelete = { sceneToDelete = scene },
                            onMoveUp = { onMoveScene(index, index - 1) },
                            onMoveDown = { onMoveScene(index, index + 1) }
                        )
                    }
                }
            }
        }
    }

    // Edit/Create Dialog
    editingScene?.let { scene ->
        SceneEditorDialog(
            initialScene = scene,
            switches = switches,
            onDismiss = { editingScene = null },
            onConfirm = { updated ->
                onSaveScene(updated)
                editingScene = null
            }
        )
    }

    // Delete Confirmation Dialog
    sceneToDelete?.let { scene ->
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            title = { Text("Delete Scene") },
            text = { Text("Are you sure you want to delete '${scene.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteScene(scene.id)
                        sceneToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SceneListItem(
    scene: SceneItem,
    isEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val extraColors = LocalSwitchboardColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extraColors.offCard)
            .border(1.dp, extraColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scene.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = scene.summary(),
                style = MaterialTheme.typography.labelSmall,
                color = extraColors.textSecondary
            )
        }

        // Apply Button
        Button(
            onClick = onApply,
            enabled = isEnabled,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extraColors.brandAccent.copy(alpha = 0.2f),
                contentColor = extraColors.brandAccent
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Move Up
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = AppIcons.ArrowUp,
                contentDescription = "Move up",
                tint = if (canMoveUp) extraColors.textSecondary else extraColors.textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Move Down
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = AppIcons.ArrowDown,
                contentDescription = "Move down",
                tint = if (canMoveDown) extraColors.textSecondary else extraColors.textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Edit
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = AppIcons.Edit,
                contentDescription = "Edit",
                tint = extraColors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Delete
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = AppIcons.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SceneEditorDialog(
    initialScene: SceneItem,
    switches: List<SwitchRelay>,
    onDismiss: () -> Unit,
    onConfirm: (SceneItem) -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    var name by remember { mutableStateOf(initialScene.name) }
    var r1 by remember { mutableStateOf(initialScene.relay1) }
    var r2 by remember { mutableStateOf(initialScene.relay2) }
    var r3 by remember { mutableStateOf(initialScene.relay3) }
    var r4 by remember { mutableStateOf(initialScene.relay4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialScene.name.isEmpty()) "Create Scene" else "Edit Scene")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scene Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Configure Switches for this Scene:",
                    style = MaterialTheme.typography.labelMedium,
                    color = extraColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Switch 1
                val s1Name = switches.find { it.id == 1 }?.name ?: "Switch 1"
                SwitchActionRow(label = s1Name, state = r1, onStateChange = { r1 = it })

                // Switch 2
                val s2Name = switches.find { it.id == 2 }?.name ?: "Switch 2"
                SwitchActionRow(label = s2Name, state = r2, onStateChange = { r2 = it })

                // Switch 3
                val s3Name = switches.find { it.id == 3 }?.name ?: "Switch 3"
                SwitchActionRow(label = s3Name, state = r3, onStateChange = { r3 = it })

                // Switch 4
                val s4Name = switches.find { it.id == 4 }?.name ?: "Switch 4"
                SwitchActionRow(label = s4Name, state = r4, onStateChange = { r4 = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        initialScene.copy(
                            name = name.trim().ifEmpty { "Custom Scene" },
                            relay1 = r1,
                            relay2 = r2,
                            relay3 = r3,
                            relay4 = r4
                        )
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.brandAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SwitchActionRow(
    label: String,
    state: Boolean?,
    onStateChange: (Boolean?) -> Unit
) {
    val extraColors = LocalSwitchboardColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = extraColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // ON
            FilterChip(
                selected = state == true,
                onClick = { onStateChange(if (state == true) null else true) },
                label = { Text("ON", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = extraColors.brandAccent,
                    selectedLabelColor = Color(0xFF0E1013)
                )
            )
            // OFF
            FilterChip(
                selected = state == false,
                onClick = { onStateChange(if (state == false) null else false) },
                label = { Text("OFF", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF6B6B),
                    selectedLabelColor = Color.White
                )
            )
            // Unchanged
            FilterChip(
                selected = state == null,
                onClick = { onStateChange(null) },
                label = { Text("Leave", fontSize = 11.sp) }
            )
        }
    }
}
