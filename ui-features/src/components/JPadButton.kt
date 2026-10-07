package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

enum class JPadButtonStyle {
    DEFAULT,
    RUN,
    STOP,
    PRIMARY,
    GHOST
}

@Composable
fun JPadToolbarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: (@Composable () -> Unit)? = null,
    shortcut: String? = null,
    enabled: Boolean = true,
    style: JPadButtonStyle = JPadButtonStyle.DEFAULT,
    shape: Shape = RoundedCornerShape(3.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val colors = JPadTheme.colors

    val (bgColor, borderColor, contentColor) = when {
        !enabled -> Triple(Color.Transparent, Color.Transparent, colors.textMuted)
        style == JPadButtonStyle.RUN -> when {
            isPressed -> Triple(colors.runSuccess.copy(alpha = 0.4f), colors.runSuccess, colors.textPrimary)
            isHovered -> Triple(colors.runSuccess.copy(alpha = 0.25f), colors.runSuccess.copy(alpha = 0.8f), colors.textPrimary)
            else -> Triple(colors.runSuccess.copy(alpha = 0.15f), colors.runSuccess.copy(alpha = 0.4f), colors.runSuccess)
        }
        style == JPadButtonStyle.STOP -> when {
            isPressed -> Triple(colors.stopRed.copy(alpha = 0.4f), colors.stopRed, colors.textPrimary)
            isHovered -> Triple(colors.stopRed.copy(alpha = 0.25f), colors.stopRed.copy(alpha = 0.8f), colors.textPrimary)
            else -> Triple(colors.stopRed.copy(alpha = 0.15f), colors.stopRed.copy(alpha = 0.4f), colors.stopRed)
        }
        style == JPadButtonStyle.PRIMARY -> when {
            isPressed -> Triple(colors.accentPrimary.copy(alpha = 0.8f), colors.accentPrimary, colors.textOnAccent)
            isHovered -> Triple(colors.accentPrimary.copy(alpha = 0.6f), colors.accentPrimary, colors.textOnAccent)
            else -> Triple(colors.accentPrimary, colors.accentPrimary, colors.textOnAccent)
        }
        style == JPadButtonStyle.GHOST -> when {
            isPressed -> Triple(colors.backgroundSelected, Color.Transparent, colors.textPrimary)
            isHovered -> Triple(colors.backgroundHover, Color.Transparent, colors.textPrimary)
            else -> Triple(Color.Transparent, Color.Transparent, colors.textSecondary)
        }
        else -> when {
            isPressed -> Triple(colors.backgroundSelected, colors.borderStrong, colors.textPrimary)
            isHovered -> Triple(colors.backgroundHover, colors.borderStrong, colors.textPrimary)
            else -> Triple(colors.backgroundInput.copy(alpha = 0.5f), colors.borderSubtle, colors.textPrimary)
        }
    }

    Box(
        modifier = modifier
            .height(JPadTheme.dimensions.buttonHeightCompact)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(modifier = Modifier.size(JPadTheme.dimensions.iconSizeCompact), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            if (text != null) {
                Text(
                    text = text,
                    style = JPadTheme.typography.uiBody.copy(
                        color = contentColor,
                        fontWeight = if (style == JPadButtonStyle.RUN || style == JPadButtonStyle.PRIMARY) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
            if (shortcut != null) {
                Text(
                    text = "($shortcut)",
                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted)
                )
            }
        }
    }
}
