package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SwitchRelay
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwitchCard(
    relay: SwitchRelay,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalSwitchboardColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press scale animation to 0.96 with a spring
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isEnabled) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "switchCardScale"
    )

    // Animated colors (250 ms tween)
    val cardBackgroundStart by animateColorAsState(
        targetValue = if (relay.isOn) {
            if (extraColors.isDark) Color(0xFF144D2B) else Color(0xFFD4F8E2)
        } else {
            extraColors.offCard
        },
        animationSpec = tween(durationMillis = 250),
        label = "cardBgStart"
    )

    val cardBackgroundEnd by animateColorAsState(
        targetValue = if (relay.isOn) {
            if (extraColors.isDark) Color(0xFF0F361E) else Color(0xFFB9F1CE)
        } else {
            extraColors.offCard
        },
        animationSpec = tween(durationMillis = 250),
        label = "cardBgEnd"
    )

    val borderColor by animateColorAsState(
        targetValue = if (relay.isOn) {
            extraColors.brandAccent.copy(alpha = if (extraColors.isDark) 0.55f else 0.8f)
        } else {
            extraColors.cardBorder
        },
        animationSpec = tween(durationMillis = 250),
        label = "cardBorder"
    )

    val badgeBackground by animateColorAsState(
        targetValue = if (relay.isOn) {
            extraColors.brandAccent
        } else {
            if (extraColors.isDark) Color(0xFF282E39) else Color(0xFFDCE2EB)
        },
        animationSpec = tween(durationMillis = 250),
        label = "badgeBg"
    )

    val badgeIconTint by animateColorAsState(
        targetValue = if (relay.isOn) {
            Color(0xFF0E1013)
        } else {
            extraColors.textSecondary
        },
        animationSpec = tween(durationMillis = 250),
        label = "badgeIcon"
    )

    val cardShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(cardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(cardBackgroundStart, cardBackgroundEnd)
                )
            )
            .border(width = 1.5.dp, color = borderColor, shape = cardShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                role = Role.Switch,
                onClick = onToggle,
                onLongClick = onLongPress
            )
            .semantics {
                contentDescription = "${relay.name}, ${if (relay.isOn) "ON" else "OFF"}"
            }
            .testTag("switch_card_${relay.id}")
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon badge + pending progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Badge (top left)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(badgeBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.getSwitchIcon(relay.iconKey),
                        contentDescription = null,
                        tint = badgeIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tiny progress indicator on card until server acknowledges
                if (relay.isPending) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = extraColors.brandAccent,
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("pending_indicator_${relay.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Row: Name + State text on left, Material 3 toggle on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = relay.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (relay.isOn) "ON" else "OFF",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (relay.isOn) extraColors.brandAccent else extraColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Custom M3 styled pill toggle at bottom right
                SwitchPill(
                    checked = relay.isOn,
                    enabled = isEnabled
                )
            }
        }
    }
}

@Composable
fun SwitchPill(
    checked: Boolean,
    enabled: Boolean
) {
    val extraColors = LocalSwitchboardColors.current

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> extraColors.cardBorder.copy(alpha = 0.4f)
            checked -> extraColors.brandAccent
            else -> if (extraColors.isDark) Color(0xFF2B313D) else Color(0xFFCBD2DC)
        },
        animationSpec = tween(250),
        label = "pillTrack"
    )

    val thumbColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Gray
            checked -> Color(0xFF0E1013)
            else -> Color.White
        },
        animationSpec = tween(250),
        label = "pillThumb"
    )

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "pillThumbOffset"
    )

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .background(trackColor, RoundedCornerShape(14.dp))
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(22.dp)
                .background(thumbColor, CircleShape)
        )
    }
}
