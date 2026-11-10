package com.chy.radiofrance.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Colour maths for text on brand colours. Pure functions, unit-tested: the rule
// "yellow gets black text" is decided here, never by eye in a composable.

/** WCAG 2 relative luminance (0 = black, 1 = white), from sRGB. */
fun relativeLuminance(color: Color): Float = color.luminance()

/** WCAG 2 contrast ratio, from 1:1 (same colour) to 21:1 (black on white). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * Black or white text on [background], whichever contrasts more. With the brand
 * colours of [stationBrand] this always meets WCAG AA for normal text (4.5:1):
 * a test checks every brand on every surface it is drawn on.
 */
fun onBrandColor(background: Color): Color =
    if (contrastRatio(background, Color.Black) >= contrastRatio(background, Color.White)) {
        Color.Black
    } else {
        Color.White
    }

/** [color] mixed with black by [fraction] (0 = unchanged, 1 = black). */
fun darken(color: Color, fraction: Float): Color = lerp(color, Color.Black, fraction)

/** [color] mixed with white by [fraction] (0 = unchanged, 1 = white). */
fun lighten(color: Color, fraction: Float): Color = lerp(color, Color.White, fraction)

/** The two stops of a national station card: the brand colour, then 25 % darker. */
fun brandGradient(brand: Color): List<Color> = listOf(brand, darken(brand, 0.25f))

/**
 * The two stops of the mini-player: the brand colour darkened, so it reads as a
 * bar. Not darker than 25 %: black text on franceinfo yellow drops below 4.5:1.
 */
fun miniPlayerGradient(brand: Color): List<Color> =
    listOf(darken(brand, 0.1f), darken(brand, 0.25f))

/**
 * The brand colour for thin marks drawn on [background] (the playing row's accent
 * bar, the equalizer). WCAG asks 3:1 for graphics: when the brand colour is too
 * close to the background (franceinfo yellow on the light theme, ICI blue on the
 * dark one), it is pushed toward black or white, in small steps, until it passes.
 */
fun brandAccent(brand: Color, background: Color): Color {
    val toward = if (relativeLuminance(background) < 0.5f) Color.White else Color.Black
    var accent = brand
    var step = 0
    while (contrastRatio(accent, background) < GRAPHICS_CONTRAST && step < 10) {
        step++
        accent = lerp(brand, toward, step / 10f)
    }
    return accent
}

/** WCAG AA contrast for normal text. */
const val TEXT_CONTRAST = 4.5f

/** WCAG AA contrast for graphics and large text. */
const val GRAPHICS_CONTRAST = 3f
