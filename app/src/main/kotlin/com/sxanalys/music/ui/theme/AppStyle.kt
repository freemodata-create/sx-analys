package com.sxanalys.music.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.sxanalys.music.R

enum class GlassMaterial(
    val id: String,
    @StringRes val label: Int,
    @StringRes val note: Int,
) {
    CLEAR("clear", R.string.sx_glass_clear, R.string.sx_glass_clear_note),
    FROSTED("frosted", R.string.sx_glass_frosted, R.string.sx_glass_frosted_note),
    TINTED("tinted", R.string.sx_glass_tinted, R.string.sx_glass_tinted_note),
    METAL("metal", R.string.sx_glass_metal, R.string.sx_glass_metal_note),
    MATTE("matte", R.string.sx_glass_matte, R.string.sx_glass_matte_note),
    OUTLINE("outline", R.string.sx_glass_outline, R.string.sx_glass_outline_note),
    INK("ink", R.string.sx_glass_ink, R.string.sx_glass_ink_note),
    ;

    companion object {
        fun byId(id: String): GlassMaterial? = entries.firstOrNull { it.id == id }
    }
}

val DefaultGlassAccent = Color(0xFFFF8A4C)

val LocalGlassMaterial = staticCompositionLocalOf { GlassMaterial.CLEAR }
val LocalGlassAccent = staticCompositionLocalOf { DefaultGlassAccent }
val LocalLargeNav = staticCompositionLocalOf { false }

class AppStyle(
    val id: String,
    @StringRes val label: Int,
    @StringRes val note: Int,
    val ios: Boolean,
    val overrides: Boolean,
    val glass: GlassMaterial,
    val accentLight: Color,
    val accentDark: Color,
    val bgLight: Color,
    val cardLight: Color,
    val inkLight: Color,
    val bgDark: Color,
    val cardDark: Color,
    val inkDark: Color,
) {
    fun accent(dark: Boolean): Color = if (dark) accentDark else accentLight

    fun bg(dark: Boolean): Color = if (dark) bgDark else bgLight

    fun card(dark: Boolean): Color = if (dark) cardDark else cardLight

    fun ink(dark: Boolean): Color = if (dark) inkDark else inkLight

    fun scheme(
        base: ColorScheme,
        dark: Boolean,
    ): ColorScheme {
        if (!overrides) return base
        val bg = bg(dark)
        val card = card(dark)
        val ink = ink(dark)
        val accent = accent(dark)
        val onAccent = if (accent.luminance() > 0.55f) Color(0xFF111111) else Color.White
        val secondary = lerp(accent, ink, 0.35f)
        val onSecondary = if (secondary.luminance() > 0.5f) Color(0xFF111111) else Color.White
        val secondaryContainer = lerp(bg, accent, if (dark) 0.20f else 0.12f)
        return base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = lerp(bg, accent, if (dark) 0.28f else 0.16f),
            onPrimaryContainer = if (dark) lerp(accent, Color.White, 0.55f) else lerp(accent, Color.Black, 0.45f),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = ink,
            tertiary = secondary,
            onTertiary = onSecondary,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = ink,
            background = bg,
            onBackground = ink,
            surface = bg,
            onSurface = ink,
            surfaceVariant = lerp(bg, ink, 0.08f),
            onSurfaceVariant = lerp(ink, bg, 0.38f),
            surfaceTint = accent,
            inverseSurface = ink,
            inverseOnSurface = bg,
            outline = lerp(bg, ink, 0.45f),
            outlineVariant = lerp(bg, ink, 0.16f),
            surfaceDim = lerp(bg, Color.Black, 0.05f),
            surfaceBright = card,
            surfaceContainerLowest = if (dark) lerp(bg, Color.Black, 0.4f) else card,
            surfaceContainerLow = lerp(bg, card, 0.5f),
            surfaceContainer = card,
            surfaceContainerHigh = lerp(card, ink, 0.05f),
            surfaceContainerHighest = lerp(card, ink, 0.10f),
        )
    }
}

object AppStyles {
    private val iosBgLight = Color(0xFFF2F2F7)
    private val iosCardLight = Color(0xFFFFFFFF)
    private val iosInkLight = Color(0xFF1C1C1E)
    private val iosBgDark = Color(0xFF000000)
    private val iosCardDark = Color(0xFF1C1C1E)
    private val iosInkDark = Color(0xFFF2F2F7)

    private fun ios(
        id: String,
        @StringRes label: Int,
        @StringRes note: Int,
        light: Long,
        dark: Long,
    ) = AppStyle(
        id = id,
        label = label,
        note = note,
        ios = true,
        overrides = true,
        glass = GlassMaterial.CLEAR,
        accentLight = Color(light),
        accentDark = Color(dark),
        bgLight = iosBgLight,
        cardLight = iosCardLight,
        inkLight = iosInkLight,
        bgDark = iosBgDark,
        cardDark = iosCardDark,
        inkDark = iosInkDark,
    )

    private fun custom(
        id: String,
        @StringRes label: Int,
        @StringRes note: Int,
        glass: GlassMaterial,
        accentLight: Long,
        accentDark: Long,
        bgLight: Long,
        cardLight: Long,
        inkLight: Long,
        bgDark: Long,
        cardDark: Long,
        inkDark: Long,
    ) = AppStyle(
        id = id,
        label = label,
        note = note,
        ios = false,
        overrides = true,
        glass = glass,
        accentLight = Color(accentLight),
        accentDark = Color(accentDark),
        bgLight = Color(bgLight),
        cardLight = Color(cardLight),
        inkLight = Color(inkLight),
        bgDark = Color(bgDark),
        cardDark = Color(cardDark),
        inkDark = Color(inkDark),
    )

    val Default =
        AppStyle(
            id = "default",
            label = R.string.sx_style_default,
            note = R.string.sx_style_default_note,
            ios = false,
            overrides = false,
            glass = GlassMaterial.CLEAR,
            accentLight = DefaultGlassAccent,
            accentDark = DefaultGlassAccent,
            bgLight = Color(0xFFF6F2FB),
            cardLight = Color(0xFFFFFFFF),
            inkLight = Color(0xFF1D1830),
            bgDark = Color(0xFF14101F),
            cardDark = Color(0xFF1E1830),
            inkDark = Color(0xFFEDE8F7),
        )

    val all: List<AppStyle> =
        listOf(
            Default,
            ios("ios_blue", R.string.sx_style_ios_blue, R.string.sx_style_ios_blue_note, 0xFF007AFF, 0xFF0A84FF),
            ios("ios_green", R.string.sx_style_ios_green, R.string.sx_style_ios_green_note, 0xFF34C759, 0xFF30D158),
            ios("ios_indigo", R.string.sx_style_ios_indigo, R.string.sx_style_ios_indigo_note, 0xFF5856D6, 0xFF5E5CE6),
            ios("ios_orange", R.string.sx_style_ios_orange, R.string.sx_style_ios_orange_note, 0xFFFF9500, 0xFFFF9F0A),
            ios("ios_pink", R.string.sx_style_ios_pink, R.string.sx_style_ios_pink_note, 0xFFFF2D55, 0xFFFF375F),
            ios("ios_purple", R.string.sx_style_ios_purple, R.string.sx_style_ios_purple_note, 0xFFAF52DE, 0xFFBF5AF2),
            ios("ios_teal", R.string.sx_style_ios_teal, R.string.sx_style_ios_teal_note, 0xFF30B0C7, 0xFF40C8E0),
            ios("ios_red", R.string.sx_style_ios_red, R.string.sx_style_ios_red_note, 0xFFFF3B30, 0xFFFF453A),
            custom(
                "graphite", R.string.sx_style_graphite, R.string.sx_style_graphite_note, GlassMaterial.MATTE,
                0xFF3D5A80, 0xFF9DB7D5,
                0xFFEDEEF0, 0xFFFFFFFF, 0xFF17181A,
                0xFF101113, 0xFF1A1C1F, 0xFFE8E9EB,
            ),
            custom(
                "paper", R.string.sx_style_paper, R.string.sx_style_paper_note, GlassMaterial.FROSTED,
                0xFFB5532B, 0xFFE0845C,
                0xFFF4EFE6, 0xFFFBF8F2, 0xFF2B2622,
                0xFF1E1B18, 0xFF28241F, 0xFFEDE6DA,
            ),
            custom(
                "ink", R.string.sx_style_ink, R.string.sx_style_ink_note, GlassMaterial.TINTED,
                0xFF2F5FD0, 0xFF6C97F0,
                0xFFEEF1F7, 0xFFFFFFFF, 0xFF111827,
                0xFF0B1220, 0xFF121B2E, 0xFFE6EBF5,
            ),
            custom(
                "forest", R.string.sx_style_forest, R.string.sx_style_forest_note, GlassMaterial.FROSTED,
                0xFF2F6F4F, 0xFF6FB58B,
                0xFFEDF1EC, 0xFFFFFFFF, 0xFF16201A,
                0xFF0D1411, 0xFF151F1A, 0xFFE4EDE7,
            ),
            custom(
                "dusk", R.string.sx_style_dusk, R.string.sx_style_dusk_note, GlassMaterial.METAL,
                0xFFB26A1E, 0xFFE9A15B,
                0xFFF3EEF1, 0xFFFFFFFF, 0xFF241A1F,
                0xFF16121A, 0xFF1F1A25, 0xFFEFE8F2,
            ),
            custom(
                "ocean", R.string.sx_style_ocean, R.string.sx_style_ocean_note, GlassMaterial.CLEAR,
                0xFF0E7C86, 0xFF4FC3CC,
                0xFFEAF2F4, 0xFFFFFFFF, 0xFF0E2128,
                0xFF081418, 0xFF0F2026, 0xFFE0EEF1,
            ),
        )

    fun byId(id: String): AppStyle = all.firstOrNull { it.id == id } ?: Default
}
