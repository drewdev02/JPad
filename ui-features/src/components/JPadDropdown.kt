package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

data class DropdownOption<T>(
    val value: T,
    val label: String,
    val description: String? = null,
    val icon: (@Composable () -> Unit)? = null
)

@Composable
fun <T> JPadDropdown(
    options: List<DropdownOption<T>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelPrefix: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val colors = JPadTheme.colors
    val selectedItem = options.firstOrNull { it.value == selectedOption }

    val bgColor = when {
        !enabled -> Color.Transparent
        expanded -> colors.backgroundSelected.copy(alpha = 0.4f)
        isHovered -> colors.backgroundHover
        else -> colors.backgroundInput
    }

    val borderColor = when {
        !enabled -> Color.Transparent
        expanded -> colors.borderFocus
        isHovered -> colors.borderStrong
        else -> colors.borderSubtle
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(JPadTheme.dimensions.buttonHeightCompact)
                .clip(RoundedCornerShape(3.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(3.dp))
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { expanded = !expanded }
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (labelPrefix != null) {
                Text(
                    text = labelPrefix,
                    style = JPadTheme.typography.uiSmall.copy(
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            if (selectedItem?.icon != null) {
                Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                    selectedItem.icon.invoke()
                }
            }

            Text(
                text = selectedItem?.label ?: "",
                style = JPadTheme.typography.uiBody.copy(
                    color = if (enabled) colors.textPrimary else colors.textMuted
                )
            )

            // Chevron down
            Text(
                text = "▾",
                style = JPadTheme.typography.uiSmall.copy(
                    color = if (enabled) colors.textSecondary else colors.textMuted
                )
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 160.dp)
                .background(colors.backgroundPanel)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(4.dp))
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedOption
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (option.icon != null) {
                                Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                                    option.icon.invoke()
                                }
                            }
                            Text(
                                text = option.label,
                                style = JPadTheme.typography.uiBody.copy(
                                    color = if (isSelected) colors.accentPrimary else colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    },
                    onClick = {
                        onOptionSelected(option.value)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(if (isSelected) colors.backgroundSelected.copy(alpha = 0.3f) else Color.Transparent)
                )
            }
        }
    }
}
