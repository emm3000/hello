package com.emm.hello.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFile
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.google_fonts_certs,
)

private val bricolageFont = GoogleFont("Bricolage Grotesque")
private val schibstedFont = GoogleFont("Schibsted Grotesk")

val bricolage: FontFamily = FontFamily(
    GoogleFontFile(
        googleFont = bricolageFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium,
    ),
    GoogleFontFile(
        googleFont = bricolageFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold,
    ),
    GoogleFontFile(
        googleFont = bricolageFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.ExtraBold,
    ),
)

val schibsted: FontFamily = FontFamily(
    GoogleFontFile(
        googleFont = schibstedFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal,
    ),
    GoogleFontFile(
        googleFont = schibstedFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium,
    ),
    GoogleFontFile(
        googleFont = schibstedFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold,
    ),
)

internal val metadataTextStyle = TextStyle(
    fontFamily = schibsted,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.12.em,
)

val Typography.metadata: TextStyle
    get() = metadataTextStyle

val appTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = bricolage,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontFamily = bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.02).em,
    ),
    displaySmall = TextStyle(
        fontFamily = bricolage,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.02).em,
    ),
    titleLarge = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = schibsted,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
)
