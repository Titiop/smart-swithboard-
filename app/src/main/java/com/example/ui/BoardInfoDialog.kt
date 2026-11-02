package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalSwitchboardColors

data class BoardDeviceInfo(
    val fw: String? = null,
    val rssi: Int? = null,
    val ip: String? = null,
    val uptimeSec: Long? = null
)

@Composable
fun BoardInfoDialog(
    boardId: String,
    isDemoMode: Boolean,
    onFetchBoardInfo: suspend (String) -> BoardDeviceInfo?,
    onDismiss: () -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    var isLoading by remember { mutableStateOf(true) }
    var deviceInfo by remember { mutableStateOf<BoardDeviceInfo?>(null) }

    LaunchedEffect(boardId, isDemoMode) {
        isLoading = true
        deviceInfo = if (isDemoMode) {
            BoardDeviceInfo(
                fw = "1.4.2-esp8266",
                rssi = -56,
                ip = "192.168.1.120",
                uptimeSec = 192300L
            )
        } else {
            onFetchBoardInfo(boardId)
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.board_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.textPrimary
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = extraColors.brandAccent,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                val info = deviceInfo
                if (info == null) {
                    Text(
                        text = stringResource(R.string.board_info_not_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.textSecondary
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BoardInfoRow(
                            label = stringResource(R.string.board_info_firmware),
                            value = info.fw?.ifBlank { "—" } ?: "—"
                        )
                        BoardInfoRow(
                            label = stringResource(R.string.board_info_wifi),
                            value = formatRssi(info.rssi)
                        )
                        BoardInfoRow(
                            label = stringResource(R.string.board_info_ip),
                            value = info.ip?.ifBlank { "—" } ?: "—"
                        )
                        BoardInfoRow(
                            label = stringResource(R.string.board_info_uptime),
                            value = formatUptime(info.uptimeSec)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = extraColors.brandAccent)
            }
        }
    )
}

@Composable
private fun BoardInfoRow(label: String, value: String) {
    val extraColors = LocalSwitchboardColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(extraColors.offCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extraColors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = extraColors.textPrimary
        )
    }
}

fun formatRssi(rssi: Int?): String {
    if (rssi == null) return "—"
    val quality = when {
        rssi >= -60 -> "Good"
        rssi >= -75 -> "OK"
        else -> "Weak"
    }
    return "$quality ($rssi dBm)"
}

fun formatUptime(uptimeSec: Long?): String {
    if (uptimeSec == null || uptimeSec < 0L) return "—"
    val totalMinutes = uptimeSec / 60L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L
    return "${days}d ${hours}h ${minutes}m"
}
