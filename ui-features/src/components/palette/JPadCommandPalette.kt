package org.andrew.jpad.ui.components.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.andrew.jpad.ui.theme.JPadTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JPadCommandPalette(
    actions: List<CommandPaletteAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CommandCategory.ALL) }
    var selectedIndex by remember { mutableStateOf(0) }

    val filteredActions = remember(actions, query, selectedCategory) {
        CommandPaletteSearch.filter(actions, query, selectedCategory)
    }

    // Keep selectedIndex within bounds
    LaunchedEffect(filteredActions.size) {
        if (selectedIndex >= filteredActions.size) {
            selectedIndex = (filteredActions.size - 1).coerceAtLeast(0)
        }
    }

    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedIndex) {
        if (filteredActions.isNotEmpty() && selectedIndex in filteredActions.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth(0.65f)
            .height(480.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.backgroundWindow)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.backgroundPanelHeader)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔍",
                    style = JPadTheme.typography.uiTitle.copy(fontSize = 16.sp),
                    modifier = Modifier.padding(end = 8.dp)
                )

                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selectedIndex = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        if (filteredActions.isNotEmpty()) {
                                            selectedIndex = (selectedIndex + 1).coerceAtMost(filteredActions.size - 1)
                                        }
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        if (filteredActions.isNotEmpty()) {
                                            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                        }
                                        true
                                    }
                                    Key.Enter -> {
                                        if (filteredActions.isNotEmpty() && selectedIndex in filteredActions.indices) {
                                            val action = filteredActions[selectedIndex]
                                            onDismiss()
                                            action.onExecute()
                                        }
                                        true
                                    }
                                    Key.Escape -> {
                                        onDismiss()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    placeholder = {
                        Text(
                            "Type a command or search actions...",
                            style = JPadTheme.typography.uiBody.copy(color = colors.textMuted, fontSize = 13.sp)
                        )
                    },
                    textStyle = JPadTheme.typography.uiBody.copy(color = colors.textPrimary, fontSize = 13.sp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Category Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(colors.backgroundPanel)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CommandCategory.values().forEach { category ->
                    val isCatSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCatSelected) colors.backgroundSelected else Color.Transparent)
                            .clickable {
                                selectedCategory = category
                                selectedIndex = 0
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${category.icon} ${category.title}",
                            style = JPadTheme.typography.uiSmall.copy(
                                fontSize = 11.sp,
                                color = if (isCatSelected) colors.textOnAccent else colors.textMuted,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Action List
            if (filteredActions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No actions matching \"$query\"",
                        style = JPadTheme.typography.uiBody.copy(color = colors.textMuted)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    itemsIndexed(filteredActions, key = { _, action -> action.id }) { index, action ->
                        val isSelected = index == selectedIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) colors.backgroundSelected.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    onDismiss()
                                    action.onExecute()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Category Icon
                                Text(
                                    text = action.icon ?: action.category.icon,
                                    style = JPadTheme.typography.uiBody.copy(fontSize = 14.sp),
                                    modifier = Modifier.padding(end = 10.dp)
                                )

                                Column {
                                    Text(
                                        text = action.title,
                                        style = JPadTheme.typography.uiBody.copy(
                                            color = colors.textPrimary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                    if (action.subtitle != null) {
                                        Text(
                                            text = action.subtitle,
                                            style = JPadTheme.typography.uiSmall.copy(
                                                color = colors.textMuted,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Shortcut badge
                            if (action.shortcut != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colors.backgroundPanelHeader)
                                        .border(0.5.dp, colors.borderStrong, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = action.shortcut,
                                        style = JPadTheme.typography.codeSmall.copy(
                                            color = colors.textSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer / Shortcuts guide
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(colors.backgroundPanelHeader)
                    .border(0.5.dp, colors.borderSubtle)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "↑ ↓ Navigate  •  ↵ Execute  •  esc Close",
                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 11.sp)
                )

                Text(
                    text = "${filteredActions.size} actions",
                    style = JPadTheme.typography.uiSmall.copy(color = colors.textMuted, fontSize = 11.sp)
                )
            }
        }
    }
}
