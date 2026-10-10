package org.andrew.jpad.ui.components.editor.completion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.andrew.jpad.core.completion.LookupElementKind
import org.andrew.jpad.core.completion.LookupItem
import org.andrew.jpad.ui.theme.JPadTheme

@Composable
fun JPadLookupPopup(
    items: List<LookupItem>,
    selectedIndex: Int,
    onItemSelected: (LookupItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JPadTheme.colors
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = modifier
            .widthIn(min = 280.dp, max = 400.dp)
            .heightIn(max = 240.dp)
            .shadow(8.dp, RoundedCornerShape(4.dp))
            .background(colors.backgroundPanelHeader, RoundedCornerShape(4.dp))
            .border(1.dp, colors.borderFocus, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items, key = { index, item -> "${item.kind}_${item.lookupString}_$index" }) { index, item ->
                LookupItemRow(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun LookupItemRow(
    item: LookupItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = JPadTheme.colors
    val background = if (isSelected) colors.backgroundSelected else Color.Transparent
    val textPrimaryColor = if (isSelected) colors.textOnAccent else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // IntelliJ-style kind badge
        KindBadge(kind = item.kind)

        // Lookup symbol name
        Text(
            text = item.lookupString,
            style = JPadTheme.typography.code.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = textPrimaryColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Tail text (parameters or description)
        if (item.tailText.isNotBlank()) {
            Text(
                text = item.tailText,
                style = JPadTheme.typography.codeSmall.copy(
                    color = if (isSelected) colors.textOnAccent.copy(alpha = 0.8f) else colors.textMuted,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Return type or element category (right-aligned)
        if (item.typeText.isNotBlank()) {
            Text(
                text = item.typeText,
                style = JPadTheme.typography.codeSmall.copy(
                    color = if (isSelected) colors.textOnAccent.copy(alpha = 0.7f) else colors.syntaxType,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun KindBadge(kind: LookupElementKind) {
    val (char, badgeColor) = when (kind) {
        LookupElementKind.METHOD -> "m" to Color(0xFF61AFEF)   // Blue
        LookupElementKind.FIELD -> "f" to Color(0xFF98C379)    // Teal/Green
        LookupElementKind.CLASS -> "c" to Color(0xFF4EC9B0)    // Cyan
        LookupElementKind.INTERFACE -> "i" to Color(0xFFC678DD)// Purple
        LookupElementKind.KEYWORD -> "k" to Color(0xFFE5C07B)  // Orange
        LookupElementKind.VARIABLE -> "v" to Color(0xFFE06C75) // Red/Salmon
        LookupElementKind.SNIPPET -> "s" to Color(0xFFD19A66)  // Gold
        LookupElementKind.DIRECTIVE -> "d" to Color(0xFFC678DD)// Magenta
        LookupElementKind.TABLE -> "t" to Color(0xFFE5C07B)    // Amber / Table
        LookupElementKind.COLUMN -> "c" to Color(0xFF61AFEF)   // Blue / Column
        LookupElementKind.SQL_FUNCTION -> "f" to Color(0xFFC678DD) // Purple / Function
    }

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.2f))
            .border(1.dp, badgeColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            style = JPadTheme.typography.uiSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        )
    }
}
