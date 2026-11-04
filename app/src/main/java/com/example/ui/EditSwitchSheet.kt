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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import android.text.format.DateFormat
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ScheduleItem
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
    onSaveAutoOffMinutes: (Int) -> Unit = {},
    onTurnOffSwitch: () -> Unit = {},
    schedules: List<ScheduleItem> = emptyList(),
    allSwitches: List<SwitchRelay> = emptyList(),
    onSaveSchedule: (ScheduleItem, Boolean) -> Unit = { _, _ -> },
    onToggleScheduleEnabled: (ScheduleItem, Boolean) -> Unit = { _, _ -> },
    onDeleteSchedule: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val extraColors = LocalSwitchboardColors.current
    var name by remember { mutableStateOf(relay.name) }
    var selectedIcon by remember { mutableStateOf(relay.iconKey) }
    var isVisible by remember { mutableStateOf(initialVisible) }
    var showHideWhileOnDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleItem?>(null) }

    val switchSchedules = remember(schedules, relay.id) {
        schedules.filter { it.relay == relay.id }.sortedBy { it.minute }
    }
    val activeSchedulesCount = remember(switchSchedules) { switchSchedules.count { it.enabled } }
    val pausedSchedulesCount = remember(switchSchedules) { switchSchedules.size - activeSchedulesCount }

    // Timer state
    val focusManager = LocalFocusManager.current
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
                        onClick = {
                            customTimerMinutes = ""
                            focusManager.clearFocus()
                            onStartTimer(timerAction, mins)
                        },
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
                            customTimerMinutes = ""
                            focusManager.clearFocus()
                            onStartTimer(timerAction, parsedCustomMins)
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

            // Schedules for this switch Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedules_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = extraColors.textPrimary
                )
                Text(
                    text = "$activeSchedulesCount active · $pausedSchedulesCount paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = extraColors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (switchSchedules.isEmpty()) {
                Text(
                    text = stringResource(R.string.edit_switch_schedules_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = extraColors.textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                val everyDayLabel = stringResource(R.string.schedule_every_day)
                val weekdaysLabel = stringResource(R.string.schedule_weekdays)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    switchSchedules.forEach { sched ->
                        val timeText = formatMinuteOfDay(sched.minute, is24Hour)
                        val daysSummary = formatScheduleDaysSummary(sched.days, everyDayLabel, weekdaysLabel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (sched.enabled) 1f else 0.6f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(extraColors.offCard)
                                .border(1.dp, extraColors.cardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = extraColors.textPrimary
                                    )
                                    Text(
                                        text = if (sched.action) "ON" else "OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sched.action) extraColors.brandAccent else Color(0xFFFF6B6B)
                                    )
                                }
                                Text(
                                    text = daysSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extraColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Switch(
                                    checked = sched.enabled,
                                    onCheckedChange = { enabled ->
                                        onToggleScheduleEnabled(sched, enabled)
                                    },
                                    enabled = isConnected,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF0E1013),
                                        checkedTrackColor = extraColors.brandAccent
                                    )
                                )
                                Text(
                                    text = if (sched.enabled) stringResource(R.string.schedule_status_active) else stringResource(R.string.schedule_status_paused),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (sched.enabled) extraColors.brandAccent else extraColors.textSecondary
                                )
                            }

                            IconButton(
                                onClick = { scheduleToDelete = sched },
                                enabled = isConnected,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Delete,
                                    contentDescription = "Delete schedule",
                                    tint = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showAddScheduleDialog = true },
                enabled = isConnected,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_switch_add_schedule_button")
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = null,
                    tint = extraColors.brandAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.edit_switch_add_schedule),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.textPrimary
                )
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
                        if (!isVisible && relay.isOn) {
                            showHideWhileOnDialog = true
                        } else {
                            onSaveVisibility(isVisible)
                            onSave(name.trim().ifEmpty { "Switch ${relay.id}" }, selectedIcon)
                        }
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

    if (showHideWhileOnDialog) {
        val resolvedName = name.trim().ifEmpty { "Switch ${relay.id}" }
        AlertDialog(
            onDismissRequest = { showHideWhileOnDialog = false },
            title = {
                Text(text = stringResource(R.string.hide_on_switch_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.hide_on_switch_dialog_message))
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            showHideWhileOnDialog = false
                            onSaveVisibility(false)
                            onSave(resolvedName, selectedIcon)
                        },
                        modifier = Modifier.testTag("hide_only_button")
                    ) {
                        Text(stringResource(R.string.hide_on_switch_hide_only))
                    }
                    Button(
                        onClick = {
                            showHideWhileOnDialog = false
                            onTurnOffSwitch()
                            onSaveVisibility(false)
                            onSave(resolvedName, selectedIcon)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extraColors.brandAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("turn_off_and_hide_button")
                    ) {
                        Text(stringResource(R.string.hide_on_switch_turn_off_and_hide))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showHideWhileOnDialog = false },
                    modifier = Modifier.testTag("cancel_hide_dialog_button")
                ) {
                    Text(stringResource(R.string.hide_on_switch_cancel))
                }
            }
        )
    }

    if (showAddScheduleDialog) {
        val switchesForEditor = if (allSwitches.isNotEmpty()) allSwitches else listOf(relay)
        ScheduleEditorDialog(
            initialSchedule = ScheduleItem(
                id = "",
                relay = relay.id,
                action = true,
                minute = 8 * 60,
                days = 127,
                enabled = true,
                label = ""
            ),
            isNew = true,
            switches = switchesForEditor,
            is24Hour = is24Hour,
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { newSchedule ->
                onSaveSchedule(newSchedule, true)
                showAddScheduleDialog = false
            },
            preselectedRelay = relay.id
        )
    }

    scheduleToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text(stringResource(R.string.schedule_delete_title)) },
            text = { Text(stringResource(R.string.schedule_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSchedule(item.id)
                        scheduleToDelete = null
                    },
                    enabled = isConnected
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
