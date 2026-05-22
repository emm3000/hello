package com.emm.hello.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFile
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R

// ─────────────────────────────────────────────────────────────────────────────
// Google Fonts provider (Phase 0)
// Certificates declared in res/values/font_certs.xml
// ─────────────────────────────────────────────────────────────────────────────

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// ─────────────────────────────────────────────────────────────────────────────
// Font families
// ─────────────────────────────────────────────────────────────────────────────

private val geistFont = GoogleFont("Geist")
private val geistMonoFont = GoogleFont("Geist Mono")

/**
 * instrumentSerif — serif family used for display / headline roles.
 * Bundled as local TTFs (regular + italic) because Google Fonts' provider serves
 * a mobile-optimised variant whose italic strokes render flatter on Skia/Android
 * than the upstream binary the designer mocked against.
 */
val instrumentSerif: FontFamily = FontFamily(
    Font(R.font.instrument_serif_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(R.font.instrument_serif_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
)

/**
 * geist — sans-serif family used for title / body / label roles.
 * Regular (400) and Medium (500) weights.
 */
val geist: FontFamily = FontFamily(
    GoogleFontFile(
        googleFont = geistFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal,
    ),
    GoogleFontFile(
        googleFont = geistFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium,
    ),
)

/**
 * geistMono — monospace family used for section labels and metadata.
 * Regular (400) weight only.
 */
val geistMono: FontFamily = FontFamily(
    GoogleFontFile(googleFont = geistMonoFont, fontProvider = googleFontProvider),
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography scale — Ember Dark (Phase 0)
//
// | Compose role    | Family             | Size   | Usage                           |
// |-----------------|--------------------|---------|---------------------------------|
// | displayLarge    | instrumentSerif ‡  | 56 sp  | Study card front, Start hero    |
// | displayMedium   | Instrument Serif   | 46 sp  | Deck Detail header, Settings    |
// | displaySmall    | Instrument Serif   | 38 sp  | Dashboard hero, empty states    |
// | headlineMedium  | Instrument Serif   | 30 sp  | Step titles                     |
// | headlineSmall   | Instrument Serif   | 24 sp  | Deck row title, big stats no.   |
// | titleLarge      | Geist 500          | 16 sp  | TopAppBar                       |
// | titleMedium     | Geist 500          | 14.5 sp| Settings row title, deck meta   |
// | bodyLarge       | Geist 400          | 15.5 sp| Form inputs                     |
// | bodyMedium      | Geist 400          | 14 sp  | Body copy                       |
// | bodySmall       | Geist 400          | 13 sp  | Microcopy                       |
// | labelLarge      | Geist 500          | 14.5 sp| Buttons                         |
// | labelMedium     | Geist Mono         | 11 sp  | Section labels (uppercase +0.12em)|
// | labelSmall      | Geist Mono         | 10 sp  | Faint metadata (intervals, "hoy")|
//
// ‡ displayLarge uses italic style.
// ─────────────────────────────────────────────────────────────────────────────

internal val metadataTextStyle = TextStyle(
    fontFamily = geistMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.12.em,
)

val Typography.metadata: TextStyle
    get() = metadataTextStyle

val appTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 46.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = instrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Normal,
        fontSize = 15.5.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.12.em,
    ),
    labelSmall = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
)
