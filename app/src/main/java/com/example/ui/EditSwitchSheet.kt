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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSwitchSheet(
    relay: SwitchRelay,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String) -> Unit,
    initialVisible: Boolean = true,
    onSaveVisibility: (Boolean) -> Unit = {},
    isConnected: Boolean = true,
    activeTimerLabel: String? = null,
    onStartTimer: (action: Boolean, minutes: Int) -> Unit = { _, _ -> },
    onCancelTimer: () -> Unit = {},
    onFetchAutoOffMinutes: suspend () -> Int = { 0 },
    onSaveAutoOffMinutes: (Int) -> Unit = {}
) {
    val extraColors = LocalSwitchboardColors.current
    var name by remember { mutableStateOf(relay.name) }
    var selectedIcon by remember { mutableStateOf(relay.iconKey) }
    var isVisible by remember { mutableStateOf(initialVisible) }

    // Timer state
    var timerAction by remember { mutableStateOf(false) } // false = Turn OFF after, true = Turn ON after
    var customTimerMinutes by remember { mutableStateOf("") }

    // Auto-Off state (0 = Never, 60 = 1h, 120 = 2h, 240 = 4h, 480 = 8h)
    var autoOffMinutes by remember { mutableIntStateOf(0) }

    LaunchedEffect(relay.id) {
        autoOffMinutes = onFetchAutoOffMinutes()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Edit Switch ${relay.id}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Switch Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_switch_name_input"),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Show this switch toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.show_this_switch),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.textPrimary
                )
                Switch(
                    checked = isVisible,
                    onCheckedChange = { isVisible = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0E1013),
                        checkedTrackColor = extraColors.brandAccent
                    ),
                    modifier = Modifier.testTag("show_this_switch_toggle")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose Appliance Icon",
                style = MaterialTheme.typography.titleMedium,
                color = extraColors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 8 icons grid (fixed height inside scrollable Column)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                items(AppIcons.AVAILABLE_SWITCH_ICONS) { (key, vector) ->
                    val isSelected = selectedIcon.equals(key, ignoreCase = true)
                    val bgColor = if (isSelected) extraColors.brandAccent else extraColors.offCard
                    val iconTint = if (isSelected) Color(0xFF0E1013) else extraColors.textPrimary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedIcon = key }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) extraColors.brandAccent else extraColors.cardBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = key,
                                tint = iconTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) extraColors.brandAccent else extraColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Countdown Timer Section
            Text(
                text = stringResource(R.string.timer_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = extraColors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!activeTimerLabel.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(extraColors.offCard)
                        .border(1.dp, extraColors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active: $activeTimerLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.brandAccent
                    )
                    OutlinedButton(
                        onClick = onCancelTimer,
                        enabled = isConnected,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("cancel_timer_button")
                    ) {
                        Text(
                            text = stringResource(R.string.timer_cancel),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Selector: Turn OFF after / Turn ON after
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !timerAction,
                    onClick = { timerAction = false },
                    label = { Text(stringResource(R.string.timer_turn_off_after), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B6B),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = timerAction,
                    onClick = { timerAction = true },
                    label = { Text(stringResource(R.string.timer_turn_on_after), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = extraColors.brandAccent,
                        selectedLabelColor = Color(0xFF0E1013)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset chips: 15 min, 30 min, 1 h, 2 h
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    15 to "15 min",
                    30 to "30 min",
                    60 to "1 h",
                    120 to "2 h"
                ).forEach { (mins, labelText) ->
                    OutlinedButton(
                        onClick = { onStartTimer(timerAction, mins) },
                        enabled = isConnected,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("timer_preset_$mins")
                    ) {
                        Text(labelText, fontSize = 11.sp, color = extraColors.textPrimary, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom minutes field (1..1440) + Start button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customTimerMinutes,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 4) {
                            customTimerMinutes = input
                        }
                    },
                    label = { Text(stringResource(R.string.timer_custom_minutes), fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("timer_custom_minutes_input"),
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

                val parsedCustomMins = customTimerMinutes.toIntOrNull()
                val isValidCustom = parsedCustomMins != null && parsedCustomMins in 1..1440
                Button(
                    onClick = {
                        if (parsedCustomMins != null && parsedCustomMins in 1..1440) {
                            onStartTimer(timerAction, parsedCustomMins)
                            customTimerMinutes = ""
                        }
                    },
                    enabled = isConnected && isValidCustom,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.brandAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("timer_custom_start_button")
                ) {
                    Text(stringResource(R.string.timer_start))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Auto-Off Safety Section
            Text(
                text = stringResource(R.string.auto_off_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = extraColors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auto_off_helper),
                style = MaterialTheme.typography.labelSmall,
                color = extraColors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to stringResource(R.string.auto_off_never),
                    60 to "1 h",
                    120 to "2 h",
                    240 to "4 h",
                    480 to "8 h"
                ).forEach { (mins, labelText) ->
                    val isSelected = autoOffMinutes == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isConnected) {
                                autoOffMinutes = mins
                                onSaveAutoOffMinutes(mins)
                            }
                        },
                        enabled = isConnected,
                        label = { Text(labelText, fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extraColors.brandAccent,
                            selectedLabelColor = Color(0xFF0E1013)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auto_off_chip_$mins")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = extraColors.textSecondary)
                }

                Button(
                    onClick = {
                        onSaveVisibility(isVisible)
                        onSave(name.trim().ifEmpty { "Switch ${relay.id}" }, selectedIcon)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_switch_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.brandAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
