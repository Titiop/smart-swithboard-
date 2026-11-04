package com.example.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BoardStatus
import com.example.data.model.RelayTimerInfo
import com.example.data.model.ScheduleItem
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

private enum class SchedulesSheetTab {
    SCHEDULES,
    TIMERS,
    AUTO_OFF
}

private sealed interface ScheduleFilter {
    data object All : ScheduleFilter
    data object Active : ScheduleFilter
    data object Paused : ScheduleFilter
    data class SwitchFilter(val relayId: Int) : ScheduleFilter
}

private val DAY_SHORT_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

internal fun formatScheduleDaysSummary(days: Int, everyDayLabel: String, weekdaysLabel: String): String {
    val mask = days and 127
    if (mask == 127) return everyDayLabel
    if (mask == 31) return weekdaysLabel
    val active = mutableListOf<String>()
    for (i in 0..6) {
        if ((mask and (1 shl i)) != 0) {
            active.add(DAY_SHORT_NAMES[i])
        }
    }
    return if (active.isEmpty()) everyDayLabel else active.joinToString(", ")
}

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

private fun computeNextScheduleLine(
    schedules: List<ScheduleItem>,
    switches: List<SwitchRelay>,
    is24Hour: Boolean,
    noActiveText: String
): String {
    val enabledSchedules = schedules.filter { it.enabled && it.days in 1..127 }
    if (enabledSchedules.isEmpty()) return noActiveText

    val now = ZonedDateTime.now(ZoneId.systemDefault())
    var bestSchedule: ScheduleItem? = null
    var bestDateTime: ZonedDateTime? = null

    for (schedule in enabledSchedules) {
        val clampedMin = schedule.minute.coerceIn(0, 1439)
        val hour = clampedMin / 60
        val min = clampedMin % 60
        val targetTime = LocalTime.of(hour, min)

        for (dayOffset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val bitIndex = candidateDate.dayOfWeek.value - 1 // 0 = Monday .. 6 = Sunday
            if ((schedule.days and (1 shl bitIndex)) != 0) {
                val candidateDateTime = ZonedDateTime.of(candidateDate, targetTime, now.zone)
                if (candidateDateTime.isAfter(now)) {
                    val diffSeconds = Duration.between(now, candidateDateTime).seconds
                    if (diffSeconds <= 7L * 24L * 3600L) {
                        if (bestDateTime == null || candidateDateTime.isBefore(bestDateTime)) {
                            bestDateTime = candidateDateTime
                            bestSchedule = schedule
                        }
                        break
                    }
                }
            }
        }
    }

    val chosen = bestSchedule ?: return noActiveText
    val targetDt = bestDateTime ?: return noActiveText

    val totalMinutes = ((Duration.between(now, targetDt).seconds + 59L) / 60L).coerceAtLeast(1L)
    val countdownStr = when {
        totalMinutes < 60L -> "in $totalMinutes min"
        totalMinutes < 24L * 60L -> {
            val h = totalMinutes / 60L
            val m = totalMinutes % 60L
            "in $h h $m min"
        }
        else -> {
            val d = totalMinutes / (24L * 60L)
            val h = (totalMinutes % (24L * 60L)) / 60L
            "in $d d $h h"
        }
    }

    val switchName = switches.find { it.id == chosen.relay }?.name ?: "Switch ${chosen.relay}"
    val actionWord = if (chosen.action) "ON" else "OFF"
    val timeStr = formatMinuteOfDay(chosen.minute, is24Hour)
    return "Next: $switchName turns $actionWord at $timeStr ($countdownStr)"
}

private fun findConflictingScheduleIds(schedules: List<ScheduleItem>): Set<String> {
    val enabledList = schedules.filter { it.enabled }
    val conflicts = mutableSetOf<String>()
    for (i in enabledList.indices) {
        for (j in i + 1 until enabledList.size) {
            val a = enabledList[i]
            val b = enabledList[j]
            if (a.relay == b.relay &&
                a.minute == b.minute &&
                (a.days and b.days) != 0 &&
                a.action != b.action
            ) {
                conflicts.add(a.id)
                conflicts.add(b.id)
            }
        }
    }
    return conflicts
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SchedulesSheet(
    schedules: List<ScheduleItem>,
    switches: List<SwitchRelay>,
    isConnected: Boolean,
    onDismiss: () -> Unit,
    onSaveSchedule: (ScheduleItem, Boolean) -> Unit,
    onToggleScheduleEnabled: (ScheduleItem, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    switchVisibility: Map<Int, Boolean> = mapOf(1 to true, 2 to true, 3 to true, 4 to true),
    boardStatus: BoardStatus = BoardStatus(hasLastSeen = false, isBoardOnline = false, statusText = ""),
    onSetSchedulesEnabled: (List<String>, Boolean) -> Unit = { _, _ -> },
    onDeleteSchedules: (List<String>) -> Unit = {},
    onRunScheduleNow: (ScheduleItem) -> Unit = {},
    initialSelectedRelayFilter: Int? = null,
    timers: Map<Int, RelayTimerInfo> = emptyMap(),
    serverTimeOffsetMs: Long = 0L,
    onCancelRelayTimer: (Int) -> Unit = {},
    autoOffMap: Map<Int, Int> = emptyMap(),
    onAutoOffTabVisibleChange: (Boolean) -> Unit = {},
    onSaveAutoOffMinutes: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val extraColors = LocalSwitchboardColors.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableStateOf(SchedulesSheetTab.SCHEDULES) }
    var editingSchedule by remember { mutableStateOf<Pair<ScheduleItem, Boolean>?>(null) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleItem?>(null) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var selectedFilter by remember(initialSelectedRelayFilter) {
        mutableStateOf<ScheduleFilter>(
            if (initialSelectedRelayFilter != null && initialSelectedRelayFilter in 1..4) {
                ScheduleFilter.SwitchFilter(initialSelectedRelayFilter)
            } else {
                ScheduleFilter.All
            }
        )
    }

    // Attach / remove /{boardId}_config/autoOff listener strictly when the Auto-off tab is visible
    DisposableEffect(selectedTab == SchedulesSheetTab.AUTO_OFF) {
        val isAutoOff = selectedTab == SchedulesSheetTab.AUTO_OFF
        onAutoOffTabVisibleChange(isAutoOff)
        onDispose {
            if (isAutoOff) {
                onAutoOffTabVisibleChange(false)
            }
        }
    }

    // Swipe-to-delete 5-second undo state
    var pendingUndoSchedule by remember { mutableStateOf<ScheduleItem?>(null) }
    var undoToken by remember { mutableLongStateOf(0L) }

    LaunchedEffect(undoToken) {
        if (pendingUndoSchedule != null) {
            delay(5000L)
            pendingUndoSchedule = null
        }
    }

    // Recompute Next schedule summary and remaining timer minutes once per minute
    var minuteTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(schedules, timers, serverTimeOffsetMs) {
        while (true) {
            minuteTick = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    val visibleSwitches = remember(switches, switchVisibility) {
        switches.filter { switchVisibility[it.id] != false }
    }

    // Fall back to All if the filtered switch was hidden
    LaunchedEffect(visibleSwitches, selectedFilter) {
        val current = selectedFilter
        if (current is ScheduleFilter.SwitchFilter && visibleSwitches.none { it.id == current.relayId }) {
            selectedFilter = ScheduleFilter.All
        }
    }

    val sortedSchedules = remember(schedules) {
        schedules.sortedBy { it.minute }
    }

    val filteredSchedules = remember(sortedSchedules, selectedFilter) {
        when (val f = selectedFilter) {
            is ScheduleFilter.All -> sortedSchedules
            is ScheduleFilter.Active -> sortedSchedules.filter { it.enabled }
            is ScheduleFilter.Paused -> sortedSchedules.filter { !it.enabled }
            is ScheduleFilter.SwitchFilter -> sortedSchedules.filter { it.relay == f.relayId }
        }
    }

    val conflictingIds = remember(schedules) {
        findConflictingScheduleIds(schedules)
    }

    val noActiveText = stringResource(R.string.schedule_no_active)
    val nextSummaryText = remember(schedules, switches, is24Hour, minuteTick, noActiveText) {
        computeNextScheduleLine(schedules, switches, is24Hour, noActiveText)
    }

    val activeCount = remember(schedules) { schedules.count { it.enabled } }
    val pausedCount = remember(schedules) { schedules.size - activeCount }

    val estimatedServerNowMs = minuteTick + serverTimeOffsetMs
    val activeTimers = remember(timers, estimatedServerNowMs) {
        timers.values
            .filter { it.atMs - estimatedServerNowMs > 0L }
            .sortedBy { it.relayId }
    }
    val activeAutoOffCount = remember(autoOffMap) { autoOffMap.values.count { it > 0 } }
    val wholeSheetSummary = remember(activeCount, activeTimers.size, activeAutoOffCount) {
        buildList {
            if (activeCount > 0) {
                add(if (activeCount == 1) "1 schedule" else "$activeCount schedules")
            }
            if (activeTimers.isNotEmpty()) {
                val tCount = activeTimers.size
                add(if (tCount == 1) "1 timer" else "$tCount timers")
            }
            if (activeAutoOffCount > 0) {
                add(if (activeAutoOffCount == 1) "1 auto-off" else "$activeAutoOffCount auto-offs")
            }
        }.joinToString(" · ")
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
            // Header row: Title + New Schedule button + Bulk Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedules_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Box {
                        IconButton(
                            onClick = { showBulkMenu = true },
                            enabled = isConnected && schedules.isNotEmpty(),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("schedules_bulk_menu_button")
                        ) {
                            Icon(
                                imageVector = AppIcons.MoreVert,
                                contentDescription = "More schedule actions",
                                tint = if (isConnected && schedules.isNotEmpty()) extraColors.textPrimary else extraColors.textSecondary.copy(alpha = 0.4f)
                            )
                        }

                        DropdownMenu(
                            expanded = showBulkMenu,
                            onDismissRequest = { showBulkMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.schedule_pause_all)) },
                                enabled = isConnected && schedules.any { it.enabled },
                                onClick = {
                                    showBulkMenu = false
                                    val ids = schedules.filter { it.enabled }.map { it.id }
                                    onSetSchedulesEnabled(ids, false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.schedule_resume_all)) },
                                enabled = isConnected && schedules.any { !it.enabled },
                                onClick = {
                                    showBulkMenu = false
                                    val ids = schedules.filter { !it.enabled }.map { it.id }
                                    onSetSchedulesEnabled(ids, true)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.schedule_delete_all),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                enabled = isConnected && schedules.isNotEmpty(),
                                onClick = {
                                    showBulkMenu = false
                                    showDeleteAllConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            // Whole-sheet header summary ("3 schedules · 1 timer · 2 auto-offs", only parts > 0)
            if (wholeSheetSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wholeSheetSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = extraColors.brandAccent,
                    modifier = Modifier.testTag("schedules_sheet_header_summary")
                )
            }

            // 3 Tabs at the top of the Schedules sheet: "Schedules", "Timers", "Auto-off"
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    SchedulesSheetTab.SCHEDULES to stringResource(R.string.schedules_tab_schedules),
                    SchedulesSheetTab.TIMERS to stringResource(R.string.schedules_tab_timers),
                    SchedulesSheetTab.AUTO_OFF to stringResource(R.string.schedules_tab_auto_off)
                ).forEach { (tab, tabLabel) ->
                    val isTabSelected = selectedTab == tab
                    val tagSuffix = when (tab) {
                        SchedulesSheetTab.SCHEDULES -> "schedules"
                        SchedulesSheetTab.TIMERS -> "timers"
                        SchedulesSheetTab.AUTO_OFF -> "auto_off"
                    }
                    FilterChip(
                        selected = isTabSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = tabLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isTabSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extraColors.brandAccent,
                            selectedLabelColor = Color(0xFF0E1013),
                            containerColor = extraColors.offCard,
                            labelColor = extraColors.textSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedules_sheet_tab_$tagSuffix")
                    )
                }
            }

            when (selectedTab) {
                SchedulesSheetTab.SCHEDULES -> {
            // A. Summary header
            Spacer(modifier = Modifier.height(6.dp))
            if (schedules.isNotEmpty()) {
                Text(
                    text = "$activeCount active · $pausedCount paused",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = extraColors.textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = nextSummaryText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (activeCount > 0) extraColors.brandAccent else extraColors.textSecondary
            )

            // H. One-line hint when app is offline explaining why controls are disabled
            if (!isConnected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.schedule_connect_internet_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.schedules_helper),
                style = MaterialTheme.typography.bodySmall,
                color = extraColors.textSecondary,
                lineHeight = 16.sp
            )

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

            // G. Board offline yellow banner (when board has lastSeen but is offline; never blocks editing)
            if (boardStatus.hasLastSeen && !boardStatus.isBoardOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(R.string.schedule_board_offline_banner),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            // B. Filter chips row: All, Active, Paused, and one chip per visible switch
            if (schedules.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterOptions = buildList {
                        add(ScheduleFilter.All to stringResource(R.string.schedule_filter_all))
                        add(ScheduleFilter.Active to stringResource(R.string.schedule_filter_active))
                        add(ScheduleFilter.Paused to stringResource(R.string.schedule_filter_paused))
                        visibleSwitches.forEach { sw ->
                            add(ScheduleFilter.SwitchFilter(sw.id) to sw.name)
                        }
                    }

                    filterOptions.forEach { (filter, labelText) ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = labelText,
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
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // F. Empty state with 3 templates when there are no schedules
            if (sortedSchedules.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.schedule_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingSchedule = ScheduleItem(
                                    id = "",
                                    relay = 1,
                                    action = true,
                                    minute = 6 * 60,
                                    days = 127,
                                    enabled = true,
                                    label = ""
                                ) to true
                            },
                            enabled = isConnected,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("schedule_template_morning")
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_template_morning),
                                fontSize = 12.sp,
                                color = extraColors.textPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                editingSchedule = ScheduleItem(
                                    id = "",
                                    relay = 1,
                                    action = false,
                                    minute = 23 * 60,
                                    days = 127,
                                    enabled = true,
                                    label = ""
                                ) to true
                            },
                            enabled = isConnected,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("schedule_template_night")
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_template_night),
                                fontSize = 12.sp,
                                color = extraColors.textPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                editingSchedule = ScheduleItem(
                                    id = "",
                                    relay = 1,
                                    action = true,
                                    minute = 9 * 60,
                                    days = 31,
                                    enabled = true,
                                    label = ""
                                ) to true
                            },
                            enabled = isConnected,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("schedule_template_weekdays")
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_template_weekdays),
                                fontSize = 12.sp,
                                color = extraColors.textPrimary
                            )
                        }
                    }
                }
            } else if (filteredSchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
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
                        .heightIn(max = 400.dp)
                ) {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        val baseSwitchName = switches.find { it.id == schedule.relay }?.name ?: "Switch ${schedule.relay}"
                        val isSwitchVisible = switchVisibility[schedule.relay] != false
                        val switchName = if (isSwitchVisible) baseSwitchName else "$baseSwitchName (hidden)"
                        val hasConflict = schedule.id in conflictingIds

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart && isConnected) {
                                    pendingUndoSchedule = schedule
                                    undoToken = System.currentTimeMillis()
                                    onDeleteSchedule(schedule.id)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = isConnected,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Delete,
                                        contentDescription = "Delete schedule",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        ) {
                            ScheduleListItem(
                                schedule = schedule,
                                switchName = switchName,
                                is24Hour = is24Hour,
                                isEnabled = isConnected,
                                isSwitchVisible = isSwitchVisible,
                                hasConflict = hasConflict,
                                onToggleEnabled = { enabled ->
                                    onToggleScheduleEnabled(schedule, enabled)
                                },
                                onEdit = {
                                    editingSchedule = schedule to false
                                },
                                onDelete = {
                                    scheduleToDelete = schedule
                                },
                                onDuplicate = {
                                    editingSchedule = schedule.copy(id = UUID.randomUUID().toString()) to true
                                },
                                onRunNow = {
                                    onRunScheduleNow(schedule)
                                }
                            )
                        }
                    }
                }
            }

            // 5-second Undo snackbar for swipe-to-delete
            pendingUndoSchedule?.let { deletedItem ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(extraColors.offCard)
                        .border(1.dp, extraColors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.schedule_deleted_snackbar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textPrimary
                    )
                    TextButton(
                        onClick = {
                            onSaveSchedule(deletedItem, false)
                            pendingUndoSchedule = null
                        },
                        enabled = isConnected,
                        modifier = Modifier.testTag("schedule_undo_button")
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_undo),
                            fontWeight = FontWeight.Bold,
                            color = extraColors.brandAccent
                        )
                    }
                }
            }
                }

                SchedulesSheetTab.TIMERS -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (activeTimers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.timers_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = extraColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            items(activeTimers, key = { it.relayId }) { timer ->
                                val baseSwitchName = switches.find { it.id == timer.relayId }?.name ?: "Switch ${timer.relayId}"
                                val isSwitchVisible = switchVisibility[timer.relayId] != false
                                val switchName = if (isSwitchVisible) baseSwitchName else "$baseSwitchName (hidden)"
                                val remainingMs = (timer.atMs - estimatedServerNowMs).coerceAtLeast(1L)
                                val remainingMinutes = ((remainingMs + 59_000L) / 60_000L).coerceAtLeast(1L)
                                val actionText = if (timer.action) {
                                    stringResource(R.string.schedule_turn_on)
                                } else {
                                    stringResource(R.string.schedule_turn_off)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(extraColors.offCard)
                                        .border(1.dp, extraColors.cardBorder, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                        .testTag("timer_tab_row_${timer.relayId}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = switchName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = extraColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = actionText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (timer.action) extraColors.brandAccent else Color(0xFFFF6B6B)
                                            )
                                            Text(
                                                text = "in $remainingMinutes min",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extraColors.textSecondary
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onCancelRelayTimer(timer.relayId) },
                                        enabled = isConnected,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("timer_tab_cancel_${timer.relayId}")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hide_on_switch_cancel),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SchedulesSheetTab.AUTO_OFF -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.auto_off_tab_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.textSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val neverLabel = stringResource(R.string.auto_off_never)
                    val autoOffChipOptions = remember(neverLabel) {
                        listOf(
                            0 to neverLabel,
                            60 to "1 h",
                            120 to "2 h",
                            240 to "4 h",
                            480 to "8 h"
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        items((1..4).toList(), key = { it }) { relayId ->
                            val baseSwitchName = switches.find { it.id == relayId }?.name ?: "Switch $relayId"
                            val isSwitchVisible = switchVisibility[relayId] != false
                            val switchName = if (isSwitchVisible) baseSwitchName else "$baseSwitchName (hidden)"
                            val currentMinutes = (autoOffMap[relayId] ?: 0).coerceAtLeast(0)
                            val currentValueText = when (currentMinutes) {
                                0 -> neverLabel
                                60 -> "1 h"
                                120 -> "2 h"
                                240 -> "4 h"
                                480 -> "8 h"
                                else -> if (currentMinutes % 60 == 0) "${currentMinutes / 60} h" else "$currentMinutes min"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(extraColors.offCard)
                                    .border(1.dp, extraColors.cardBorder, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("auto_off_tab_row_$relayId")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = switchName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = extraColors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = currentValueText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentMinutes > 0) extraColors.brandAccent else extraColors.textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    autoOffChipOptions.forEach { (mins, chipLabel) ->
                                        val isSelected = currentMinutes == mins
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                onSaveAutoOffMinutes(relayId, mins)
                                            },
                                            enabled = isConnected,
                                            label = { Text(chipLabel, fontSize = 11.sp, maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = extraColors.brandAccent,
                                                selectedLabelColor = Color(0xFF0E1013)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("auto_off_tab_chip_${relayId}_$mins")
                                        )
                                    }
                                }
                            }
                        }
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

    if (showDeleteAllConfirm) {
        val countToDelete = schedules.size
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.schedule_delete_all_title)) },
            text = {
                Text("This will permanently delete all $countToDelete schedule${if (countToDelete == 1) "" else "s"}.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val allIds = schedules.map { it.id }
                        showDeleteAllConfirm = false
                        onDeleteSchedules(allIds)
                    },
                    enabled = isConnected
                ) {
                    Text("Delete ($countToDelete)", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
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
    isSwitchVisible: Boolean,
    hasConflict: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRunNow: () -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    val timeText = remember(schedule.minute, is24Hour) {
        formatMinuteOfDay(schedule.minute, is24Hour)
    }
    var showRowMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (schedule.enabled) 1f else 0.6f)
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
                    if (hasConflict) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_conflict),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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

            // Toggle + small "Active" or "Paused" label underneath
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 4.dp)
            ) {
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
                Text(
                    text = if (schedule.enabled) stringResource(R.string.schedule_status_active) else stringResource(R.string.schedule_status_paused),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (schedule.enabled) extraColors.brandAccent else extraColors.textSecondary
                )
            }

            // 40 dp touch target Edit button
            IconButton(
                onClick = onEdit,
                enabled = isEnabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit schedule",
                    tint = if (isEnabled) extraColors.textSecondary else extraColors.textSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 40 dp touch target Delete button
            IconButton(
                onClick = onDelete,
                enabled = isEnabled,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Delete,
                    contentDescription = "Delete schedule",
                    tint = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // 40 dp touch target Row Overflow Menu (Duplicate, Run now)
            Box {
                IconButton(
                    onClick = { showRowMenu = true },
                    enabled = isEnabled,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("schedule_row_menu_${schedule.id}")
                ) {
                    Icon(
                        imageVector = AppIcons.MoreVert,
                        contentDescription = "More options",
                        tint = if (isEnabled) extraColors.textSecondary else extraColors.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showRowMenu,
                    onDismissRequest = { showRowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.schedule_duplicate)) },
                        enabled = isEnabled,
                        onClick = {
                            showRowMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.schedule_run_now)) },
                        enabled = isEnabled && isSwitchVisible,
                        onClick = {
                            showRowMenu = false
                            onRunNow()
                        }
                    )
                }
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
internal fun ScheduleEditorDialog(
    initialSchedule: ScheduleItem,
    isNew: Boolean,
    switches: List<SwitchRelay>,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleItem) -> Unit,
    preselectedRelay: Int? = null
) {
    val extraColors = LocalSwitchboardColors.current
    var selectedRelay by remember {
        mutableIntStateOf((preselectedRelay ?: initialSchedule.relay).coerceIn(1, 4))
    }
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
