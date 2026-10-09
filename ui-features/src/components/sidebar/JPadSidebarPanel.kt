package org.andrew.jpad.ui.components.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.andrew.jpad.ui.theme.JPadTheme

enum class SidebarTab(val title: String, val icon: String) {
    FILES("Files", "📁"),
    DATABASES("Databases", "⚡")
}

@Composable
fun JPadSidebarPanel(
    selectedTab: SidebarTab,
    onTabSelected: (SidebarTab) -> Unit,
    filesContent: @Composable () -> Unit,
    databasesContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundSidebar)
    ) {
        // Top Tab Switcher: [ 📁 Files ]  [ ⚡ Databases ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(colors.backgroundPanel)
        ) {
            SidebarTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .background(if (isSelected) colors.backgroundSidebar else colors.backgroundPanel)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tab.icon} ${tab.title}",
                        style = JPadTheme.typography.uiSmall.copy(
                            color = if (isSelected) colors.accentPrimary else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // Active View
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                SidebarTab.FILES -> filesContent()
                SidebarTab.DATABASES -> databasesContent()
            }
        }
    }
}
