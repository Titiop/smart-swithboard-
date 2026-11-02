package com.example.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ScheduleItem
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import java.util.Locale

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

fun formatMinuteOfDay(minute: Int, is24Hour: Boolean): String {
    val clamped = minute.coerceIn(0, 1439)
    val hour24 = clamped / 60
    val min = clamped % 60
    return if (is24Hour) {
        String.format(Locale.US, "%02d:%02d", hour24, min)
    } else {
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        String.format(Locale.US, "%d:%02d %s", hour12, min, amPm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesSheet(
    schedules: List<ScheduleItem>,
    switches: List<SwitchRelay>,
    isConnected: Boolean,
    onDismiss: () -> Unit,
    onSaveSchedule: (ScheduleItem, Boolean) -> Unit,
    onToggleScheduleEnabled: (ScheduleItem, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit
) {
    val context = LocalContext.current
    val extraColors = LocalSwitchboardColors.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editingSchedule by remember { mutableStateOf<Pair<ScheduleItem, Boolean>?>(null) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleItem?>(null) }

    val sortedSchedules = remember(schedules) {
        schedules.sortedBy { it.minute }
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
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedules_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.textPrimary
                )

                Button(
                    onClick = {
                        editingSchedule = ScheduleItem(
                            id = "",
                            relay = 1,
                            action = true,
                            minute = 8 * 60,
                            days = 127,
                            enabled = true,
                            label = ""
                        ) to true
                    },
                    enabled = isConnected,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.brandAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("create_schedule_button")
                ) {
                    Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.schedule_new))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.schedules_helper),
                style = MaterialTheme.typography.bodySmall,
                color = extraColors.textSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Offline Banner when not connected to Firebase
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
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

            if (sortedSchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.schedule_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(sortedSchedules, key = { it.id }) { schedule ->
                        val switchName = switches.find { it.id == schedule.relay }?.name ?: "Switch ${schedule.relay}"
                        ScheduleListItem(
                            schedule = schedule,
                            switchName = switchName,
                            is24Hour = is24Hour,
                            isEnabled = isConnected,
                            onToggleEnabled = { enabled ->
                                onToggleScheduleEnabled(schedule, enabled)
                            },
                            onEdit = {
                                editingSchedule = schedule to false
                            },
                            onDelete = {
                                scheduleToDelete = schedule
                            }
                        )
                    }
                }
            }
        }
    }

    editingSchedule?.let { (item, isNew) ->
        ScheduleEditorDialog(
            initialSchedule = item,
            isNew = isNew,
            switches = switches,
            is24Hour = is24Hour,
            onDismiss = { editingSchedule = null },
            onConfirm = { updated ->
                onSaveSchedule(updated, isNew)
                editingSchedule = null
            }
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

@Composable
private fun ScheduleListItem(
    schedule: ScheduleItem,
    switchName: String,
    is24Hour: Boolean,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    val timeText = remember(schedule.minute, is24Hour) {
        formatMinuteOfDay(schedule.minute, is24Hour)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extraColors.offCard)
            .border(1.dp, extraColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("schedule_item_${schedule.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.textPrimary
                    )
                    Text(
                        text = if (schedule.action) stringResource(R.string.schedule_turn_on) else stringResource(R.string.schedule_turn_off),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (schedule.action) extraColors.brandAccent else Color(0xFFFF6B6B)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = switchName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (schedule.label.isNotBlank()) {
                        Text(
                            text = "• ${schedule.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = extraColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Switch(
                checked = schedule.enabled,
                onCheckedChange = onToggleEnabled,
                enabled = isEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0E1013),
                    checkedTrackColor = extraColors.brandAccent
                ),
                modifier = Modifier.testTag("schedule_toggle_${schedule.id}")
            )

            IconButton(
                onClick = onEdit,
                enabled = isEnabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit schedule",
                    tint = if (isEnabled) extraColors.textSecondary else extraColors.textSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                enabled = isEnabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "Delete schedule",
                    tint = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day chips (M T W T F S S)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DAY_LABELS.forEachIndexed { bitIndex, dayLetter ->
                val isDayActive = (schedule.days and (1 shl bitIndex)) != 0
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDayActive) extraColors.brandAccent.copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDayActive) extraColors.brandAccent else extraColors.cardBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayLetter,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isDayActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDayActive) extraColors.brandAccent else extraColors.textSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorDialog(
    initialSchedule: ScheduleItem,
    isNew: Boolean,
    switches: List<SwitchRelay>,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleItem) -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    var selectedRelay by remember { mutableIntStateOf(initialSchedule.relay.coerceIn(1, 4)) }
    var selectedAction by remember { mutableStateOf(initialSchedule.action) }
    var daysMask by remember {
        mutableIntStateOf(if (initialSchedule.days in 1..127) initialSchedule.days else 127)
    }
    var label by remember { mutableStateOf(initialSchedule.label.take(30)) }
    var enabled by remember { mutableStateOf(initialSchedule.enabled) }

    val initialHour = (initialSchedule.minute.coerceIn(0, 1439)) / 60
    val initialMin = (initialSchedule.minute.coerceIn(0, 1439)) % 60
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMin,
        is24Hour = is24Hour
    )

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) stringResource(R.string.schedule_new) else stringResource(R.string.schedule_edit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Switch selection (4 options)
                Text(
                    text = "Switch",
                    style = MaterialTheme.typography.labelMedium,
                    color = extraColors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..4).forEach { id ->
                        val swName = switches.find { it.id == id }?.name ?: "Switch $id"
                        val isSelected = selectedRelay == id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRelay = id },
                            label = {
                                Text(
                                    text = swName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extraColors.brandAccent,
                                selectedLabelColor = Color(0xFF0E1013)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action selection (Turn ON / Turn OFF)
                Text(
                    text = "Action",
                    style = MaterialTheme.typography.labelMedium,
                    color = extraColors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAction,
                        onClick = { selectedAction = true },
                        label = { Text(stringResource(R.string.schedule_turn_on), fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extraColors.brandAccent,
                            selectedLabelColor = Color(0xFF0E1013)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !selectedAction,
                        onClick = { selectedAction = false },
                        label = { Text(stringResource(R.string.schedule_turn_off), fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF6B6B),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Material 3 TimePicker
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days selection (7 toggle chips + quick buttons)
                Text(
                    text = "Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = extraColors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DAY_LABELS.forEachIndexed { bitIndex, letter ->
                        val bit = 1 shl bitIndex
                        val isSelected = (daysMask and bit) != 0
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) extraColors.brandAccent else extraColors.offCard)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) extraColors.brandAccent else extraColors.cardBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val candidate = if (isSelected) (daysMask and bit.inv()) else (daysMask or bit)
                                    // At least one day must remain selected
                                    if (candidate != 0) {
                                        daysMask = candidate
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0E1013) else extraColors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick buttons: Every day (127) and Weekdays (31)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { daysMask = 127 },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.schedule_every_day), fontSize = 12.sp, color = extraColors.textPrimary)
                    }
                    OutlinedButton(
                        onClick = { daysMask = 31 },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.schedule_weekdays), fontSize = 12.sp, color = extraColors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional label (max 30 chars)
                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 30) label = it },
                    label = { Text(stringResource(R.string.schedule_label_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_label_input"),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Enabled toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.textPrimary
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0E1013),
                            checkedTrackColor = extraColors.brandAccent
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minuteOfDay = (timePickerState.hour * 60 + timePickerState.minute).coerceIn(0, 1439)
                    onConfirm(
                        initialSchedule.copy(
                            relay = selectedRelay.coerceIn(1, 4),
                            action = selectedAction,
                            minute = minuteOfDay,
                            days = if (daysMask in 1..127) daysMask else 127,
                            enabled = enabled,
                            label = label.trim().take(30)
                        )
                    )
                },
                enabled = daysMask in 1..127,
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.brandAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("save_schedule_button")
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
