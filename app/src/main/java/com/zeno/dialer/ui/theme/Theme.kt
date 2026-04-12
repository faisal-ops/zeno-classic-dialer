package com.zeno.dialer.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.zeno.dialer.AppPreferences
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 35.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 31.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 37.sp,
        lineHeight = 48.sp,
        letterSpacing = 3.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 31.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 31.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 29.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 1.sp
    )
)

private val AppTypographyModernClassic = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 37.sp, lineHeight = 43.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 39.sp, letterSpacing = 0.2.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp)
)

enum class DialerStyle { ORIGINAL_CLASSIC, MODERN_CLASSIC }

internal data class DialerColorTokens(
    val bgPage: Color,
    val bgSurface: Color,
    val bgElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val border: Color,
    val focusBorder: Color,
    val surfaceActive: Color,
    val accent: Color,
    val accentMuted: Color,
    val danger: Color,
    val badgeStar: Color,
)

internal val LocalDialerColors = staticCompositionLocalOf<DialerColorTokens> {
    DialerColorTokens(
        bgPage = Color.Unspecified,
        bgSurface = Color.Unspecified,
        bgElevated = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textHint = Color.Unspecified,
        border = Color.Unspecified,
        focusBorder = Color.Unspecified,
        surfaceActive = Color.Unspecified,
        accent = Color.Unspecified,
        accentMuted = Color.Unspecified,
        danger = Color.Unspecified,
        badgeStar = Color.Unspecified,
    )
}

internal data class DialerMotionTokens(
    val microMs: Int,
    val standardMs: Int,
)
internal val LocalDialerMotion = staticCompositionLocalOf { DialerMotionTokens(70, 150) }
internal val LocalDialerStyle = staticCompositionLocalOf { DialerStyle.MODERN_CLASSIC }

// ── BB Classic color schemes ─────────────────────────────────────────────────

// BB Blue = #1278c8, Call Green = #5a8a1c, End Red = #c0392b

private val BBClassicLightColorScheme = lightColorScheme(
    primary                = Color(0xFF1278C8),   // BB blue
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFD6E8F8),
    onPrimaryContainer     = Color(0xFF0A3A6E),
    secondary              = Color(0xFF5A8A1C),   // call green
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFDEF0C4),
    onSecondaryContainer   = Color(0xFF1C3A06),
    tertiary               = Color(0xFF0D5FA0),   // darker BB blue
    onTertiary             = Color(0xFFFFFFFF),
    background             = Color(0xFFFFFFFF),
    onBackground           = Color(0xFF1A1A1A),
    surface                = Color(0xFFF5F5F5),
    onSurface              = Color(0xFF1A1A1A),
    surfaceVariant         = Color(0xFFEBEBEB),
    onSurfaceVariant       = Color(0xFF666666),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFF5F5F5),
    surfaceContainer       = Color(0xFFEBEBEB),
    surfaceContainerHigh   = Color(0xFFE2E2E2),
    surfaceContainerHighest= Color(0xFFD8D8D8),
    outline                = Color(0xFFDDDDDD),
    outlineVariant         = Color(0xFFEEEEEE),
    error                  = Color(0xFFC0392B),   // end red
    onError                = Color(0xFFFFFFFF),
)

private val BBClassicDarkColorScheme = darkColorScheme(
    primary                = Color(0xFF4A9DE8),   // lighter BB blue for dark
    onPrimary              = Color(0xFF0A1A2E),
    primaryContainer       = Color(0xFF0D3A68),
    onPrimaryContainer     = Color(0xFFB8D8F4),
    secondary              = Color(0xFF7ABF3C),
    onSecondary            = Color(0xFF141414),
    secondaryContainer     = Color(0xFF1C3A06),
    onSecondaryContainer   = Color(0xFFDEF0C4),
    tertiary               = Color(0xFF1278C8),
    onTertiary             = Color(0xFFE8E8E8),
    background             = Color(0xFF141414),
    onBackground           = Color(0xFFE8E8E8),
    surface                = Color(0xFF1C1C1E),
    onSurface              = Color(0xFFE8E8E8),
    surfaceVariant         = Color(0xFF252528),
    onSurfaceVariant       = Color(0xFF8E8E93),
    surfaceContainerLowest = Color(0xFF141414),
    surfaceContainerLow    = Color(0xFF1C1C1E),
    surfaceContainer       = Color(0xFF252528),
    surfaceContainerHigh   = Color(0xFF252528),
    surfaceContainerHighest= Color(0xFF2C2C2E),
    outline                = Color(0xFF3A3A3C),
    outlineVariant         = Color(0xFF2C2C2E),
    error                  = Color(0xFFCC4444),
    onError                = Color(0xFFE8E8E8),
)

// ── BB Classic custom color tokens ───────────────────────────────────────────

private val BBLightTokens = DialerColorTokens(
    bgPage        = Color(0xFFFFFFFF),
    bgSurface     = Color(0xFFF5F5F5),
    bgElevated    = Color(0xFFEBEBEB),
    textPrimary   = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF666666),
    textHint      = Color(0xFFAAAAAA),
    border        = Color(0xFFDDDDDD),
    focusBorder   = Color(0xFF1278C8),
    surfaceActive = Color(0xFFE3F0FB),
    accent        = Color(0xFF1278C8),
    accentMuted   = Color(0xFF0D5FA0),
    danger        = Color(0xFFC0392B),
    badgeStar     = Color(0xFFC31924),
)

private val BBDarkTokens = DialerColorTokens(
    bgPage        = Color(0xFF141414),
    bgSurface     = Color(0xFF1C1C1E),
    bgElevated    = Color(0xFF252528),
    textPrimary   = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF8E8E93),
    textHint      = Color(0xFF48484A),
    border        = Color(0xFF3A3A3C),
    focusBorder   = Color(0xFF4A9DE8),
    surfaceActive = Color(0xFF0D2A48),
    accent        = Color(0xFF4A9DE8),
    accentMuted   = Color(0xFF1278C8),
    danger        = Color(0xFFCC4444),
    badgeStar     = Color(0xFFD94A53),
)

@Composable
fun DialerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(AppPreferences.FILE_SETTINGS, Context.MODE_PRIVATE)
    var themeChoice by remember { mutableIntStateOf(prefs.getInt(AppPreferences.KEY_CHOOSE_THEME, 0)) } // 0=system default, 1=light, 2=dark
    var dialerStylePref by remember { mutableIntStateOf(prefs.getInt(AppPreferences.KEY_DIALER_STYLE, AppPreferences.DIALER_STYLE_MODERN_CLASSIC)) }

    // Keep theme in sync with Display Options without forcing an app restart.
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPreferences.KEY_CHOOSE_THEME) {
                themeChoice = prefs.getInt(AppPreferences.KEY_CHOOSE_THEME, 0)
            }
            if (key == AppPreferences.KEY_DIALER_STYLE) {
                dialerStylePref = prefs.getInt(AppPreferences.KEY_DIALER_STYLE, AppPreferences.DIALER_STYLE_MODERN_CLASSIC)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // BB Classic UI is light-first; match debug appearance in release too (system dark does not apply).
    // User can still pick explicit Dark in Display Options (choose_theme = 2).
    val isDark = when (themeChoice) {
        1 -> false
        2 -> true
        else -> false
    }

    val colorScheme = if (isDark) BBClassicDarkColorScheme else BBClassicLightColorScheme
    val baseTokens = if (isDark) BBDarkTokens else BBLightTokens
    val dialerStyle = if (dialerStylePref == AppPreferences.DIALER_STYLE_ORIGINAL_CLASSIC) {
        DialerStyle.ORIGINAL_CLASSIC
    } else {
        DialerStyle.MODERN_CLASSIC
    }
    val tokens = when (dialerStyle) {
        DialerStyle.ORIGINAL_CLASSIC -> baseTokens
        DialerStyle.MODERN_CLASSIC -> baseTokens.copy(
            bgPage = if (isDark) Color(0xFF000000) else Color(0xFF050608),
            bgSurface = if (isDark) Color(0xFF0A0C10) else Color(0xFF0E1115),
            bgElevated = if (isDark) Color(0xFF121720) else Color(0xFF181E28),
            border = if (isDark) Color(0xFF293242) else Color(0xFF323D50),
            textPrimary = Color(0xFFF4F8FF),
            textSecondary = Color(0xFF9FB0C8),
            textHint = Color(0xFF67758A),
            accent = if (isDark) Color(0xFF8DB4FF) else Color(0xFF7AA7FF),
            accentMuted = if (isDark) Color(0xFF5D83CC) else Color(0xFF638ED9),
            surfaceActive = if (isDark) Color(0xFF15253D) else Color(0xFF182A45)
        )
    }
    val motion = when (dialerStyle) {
        DialerStyle.ORIGINAL_CLASSIC -> DialerMotionTokens(70, 150)
        DialerStyle.MODERN_CLASSIC -> DialerMotionTokens(60, 100)
    }
    val typography = if (dialerStyle == DialerStyle.MODERN_CLASSIC) AppTypographyModernClassic else AppTypography

    CompositionLocalProvider(
        LocalDialerColors provides tokens,
        LocalDialerMotion provides motion,
        LocalDialerStyle provides dialerStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
