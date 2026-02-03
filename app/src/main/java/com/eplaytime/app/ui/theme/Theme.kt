package com.eplaytime.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.eplaytime.app.R

// Outfit Font from Google Fonts (Soft, Rounded, Premium)
val OutfitFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = GoogleFont("Outfit")

val OutfitFontFamily = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = OutfitFontProvider, weight = FontWeight.Light),
    Font(googleFont = OutfitFont, fontProvider = OutfitFontProvider, weight = FontWeight.Normal),
    Font(googleFont = OutfitFont, fontProvider = OutfitFontProvider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = OutfitFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = OutfitFontProvider, weight = FontWeight.Bold),
    Font(googleFont = OutfitFont, fontProvider = OutfitFontProvider, weight = FontWeight.ExtraBold)
)

/**
 * OLED Dark Color Scheme - Always Dark, Never Light
 * This is the flagship "Rich & Soft" theme with Soft Gold accent
 */
private val OLEDDarkColorScheme = darkColorScheme(
    // Background colors - True OLED Black
    background = BlackBackground,
    surface = BlackSurface,
    surfaceVariant = BlackCard,

    // Primary - Soft Gold accent
    primary = SoftGold,
    onPrimary = Color.Black,
    primaryContainer = SoftGoldDark,
    onPrimaryContainer = TextPrimary,

    // Secondary - Subtle white
    secondary = GlassWhite,
    onSecondary = TextPrimary,
    secondaryContainer = GlassBorder,
    onSecondaryContainer = TextSecondary,

    // Tertiary - Glass highlight
    tertiary = GlassHighlight,
    onTertiary = TextPrimary,

    // Error
    error = ErrorRed,
    onError = TextPrimary,

    // Text colors
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,

    // Outline
    outline = GlassBorder,
    outlineVariant = GlassWhite
)

// Custom Typography with Outfit Font
val PlayTimeTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

@Composable
fun PlayTimeTheme(
    darkTheme: Boolean = true, // ALWAYS TRUE - OLED Dark only
    content: @Composable () -> Unit
) {
    // FORCE dark theme regardless of system setting
    val colorScheme = OLEDDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Make system bars TRANSPARENT for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Make icons light (for dark theme)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PlayTimeTypography,
        content = content
    )
}
