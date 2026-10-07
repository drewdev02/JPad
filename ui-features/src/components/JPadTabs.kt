package org.andrew.jpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

data class QueryTabItem(
    val id: String,
    val title: String,
    val isModified: Boolean = false,
    val icon: (@Composable () -> Unit)? = null
)

@Composable
fun JPadQueryTabBar(
    tabs: List<QueryTabItem>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JPadTheme.dimensions.tabBarHeight)
            .background(colors.backgroundPanelHeader)
            .border(1.dp, colors.borderSubtle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.id == selectedTabId
            JPadQueryTab(
                tab = tab,
                isSelected = isSelected,
                onClick = { onTabSelected(tab.id) },
                onClose = { onTabClosed(tab.id) }
            )
        }

        // New tab button "+"
        val addInteractionSource = remember { MutableInteractionSource() }
        val isAddHovered by addInteractionSource.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isAddHovered) colors.backgroundHover else Color.Transparent)
                .clickable(
                    interactionSource = addInteractionSource,
                    indication = null,
                    onClick = onNewTab
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = JPadTheme.typography.uiBody.copy(
                    color = if (isAddHovered) colors.textPrimary else colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun JPadQueryTab(
    tab: QueryTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val closeInteractionSource = remember { MutableInteractionSource() }
    val isCloseHovered by closeInteractionSource.collectIsHoveredAsState()

    val bgColor = when {
        isSelected -> colors.backgroundEditor
        isHovered -> colors.backgroundHover
        else -> colors.backgroundPanelHeader
    }

    val textColor = when {
        isSelected -> colors.textPrimary
        isHovered -> colors.textPrimary
        else -> colors.textSecondary
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(1.dp, if (isSelected) colors.accentPrimary else Color.Transparent)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tab.icon != null) {
                Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                    tab.icon.invoke()
                }
            }

            Text(
                text = tab.title,
                style = JPadTheme.typography.uiBody.copy(
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )

            if (tab.isModified) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accentPrimary)
                )
            }

            // Close button (x)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isCloseHovered) colors.backgroundHover else Color.Transparent)
                    .clickable(
                        interactionSource = closeInteractionSource,
                        indication = null,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    style = JPadTheme.typography.uiSmall.copy(
                        color = if (isCloseHovered) colors.textPrimary else colors.textMuted
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun JPadTabsPreview() {
    JPadTheme {
        Box(
            modifier = Modifier
                .background(JPadTheme.colors.backgroundPanel)
                .padding(12.dp)
        ) {
            val tabs = listOf(
                QueryTabItem(id = "1", title = "Scratchpad.java", isModified = true),
                QueryTabItem(id = "2", title = "QueryDatabase.java", isModified = false),
                QueryTabItem(id = "3", title = "Benchmark.java", isModified = false)
            )
            JPadQueryTabBar(
                tabs = tabs,
                selectedTabId = "1",
                onTabSelected = {},
                onTabClosed = {},
                onNewTab = {}
            )
        }
    }
}
