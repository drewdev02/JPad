package org.andrew.jpad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class JPadColorScheme(
    // Backgrounds
    val backgroundWindow: Color = Color(0xFF1E1E1E),
    val backgroundEditor: Color = Color(0xFF1E1E1E),
    val backgroundSidebar: Color = Color(0xFF252526),
    val backgroundPanel: Color = Color(0xFF2D2D30),
    val backgroundPanelHeader: Color = Color(0xFF333337),
    val backgroundSurface: Color = Color(0xFF1E1E1E),
    val backgroundInput: Color = Color(0xFF3C3C3C),
    val backgroundHover: Color = Color(0xFF3E3E40),
    val backgroundSelected: Color = Color(0xFF094771),

    // Borders & Dividers
    val borderSubtle: Color = Color(0xFF333337),
    val borderStrong: Color = Color(0xFF3F3F46),
    val borderFocus: Color = Color(0xFF007ACC),
    val gridLine: Color = Color(0xFF2A2D2E),

    // LINQPad signature accents
    val accentPrimary: Color = Color(0xFF007ACC),       // VS/LINQPad blue
    val accentHeaderTable: Color = Color(0xFF1D609C),   // LINQPad dump table root header blue
    val accentHeaderNested: Color = Color(0xFF184975),  // LINQPad nested dump table header
    val accentHeaderLeaf: Color = Color(0xFF2B5B84),

    // Actions & States
    val runGreen: Color = Color(0xFF4EC9B0),            // Play action green / turquoise
    val runSuccess: Color = Color(0xFF4CAF50),
    val stopRed: Color = Color(0xFFF44747),
    val warningAmber: Color = Color(0xFFCCA700),
    val errorRed: Color = Color(0xFFF14C4C),

    // Typography
    val textPrimary: Color = Color(0xFFD4D4D4),
    val textSecondary: Color = Color(0xFF9E9E9E),
    val textMuted: Color = Color(0xFF6E6E6E),
    val textOnAccent: Color = Color(0xFFFFFFFF),

    // Syntax & Data Types for Dump Viewer
    val syntaxKeyword: Color = Color(0xFF569CD6),
    val syntaxType: Color = Color(0xFF4EC9B0),
    val syntaxString: Color = Color(0xFFCE9178),
    val syntaxNumber: Color = Color(0xFFB5CEA8),
    val syntaxBoolean: Color = Color(0xFF569CD6),
    val syntaxNull: Color = Color(0xFF808080),
    val syntaxComment: Color = Color(0xFF6A9955)
)

@Immutable
data class JPadTypography(
    val code: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Color(0xFFD4D4D4)
    ),
    val codeSmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = Color(0xFFD4D4D4)
    ),
    val uiTitle: TextStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = Color(0xFFD4D4D4)
    ),
    val uiBody: TextStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color(0xFFD4D4D4)
    ),
    val uiSmall: TextStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color(0xFF9E9E9E)
    ),
    val gridHeader: TextStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = Color(0xFFFFFFFF)
    ),
    val gridCell: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color(0xFFD4D4D4)
    )
)

@Immutable
data class JPadDimensions(
    val toolbarHeight: Dp = 34.dp,
    val statusBarHeight: Dp = 22.dp,
    val tabBarHeight: Dp = 28.dp,
    val splitterThickness: Dp = 4.dp,
    val paddingCompact: Dp = 4.dp,
    val paddingStandard: Dp = 8.dp,
    val paddingLarge: Dp = 12.dp,
    val buttonHeightCompact: Dp = 24.dp,
    val iconSizeCompact: Dp = 16.dp,
    val gridRowHeight: Dp = 22.dp,
    val gridHeaderHeight: Dp = 24.dp
)

val LocalJPadColors = staticCompositionLocalOf { JPadColorScheme() }
val LocalJPadTypography = staticCompositionLocalOf { JPadTypography() }
val LocalJPadDimensions = staticCompositionLocalOf { JPadDimensions() }

object JPadTheme {
    val colors: JPadColorScheme
        @Composable
        get() = LocalJPadColors.current

    val typography: JPadTypography
        @Composable
        get() = LocalJPadTypography.current

    val dimensions: JPadDimensions
        @Composable
        get() = LocalJPadDimensions.current
}

@Composable
fun JPadTheme(
    colors: JPadColorScheme = JPadColorScheme(),
    typography: JPadTypography = JPadTypography(),
    dimensions: JPadDimensions = JPadDimensions(),
    content: @Composable () -> Unit
) {
    val m3ColorScheme = darkColorScheme(
        primary = colors.accentPrimary,
        background = colors.backgroundWindow,
        surface = colors.backgroundSurface,
        onPrimary = colors.textOnAccent,
        onBackground = colors.textPrimary,
        onSurface = colors.textPrimary
    )

    CompositionLocalProvider(
        LocalJPadColors provides colors,
        LocalJPadTypography provides typography,
        LocalJPadDimensions provides dimensions
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            content = content
        )
    }
}
