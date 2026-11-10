package com.chy.radiofrance.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StationBrandTest {
    private val allBrandColors: Map<String, Color> =
        knownBrandIds.associateWith { stationBrand(it, it).color } +
            ("fallback" to FallbackBrandColor)

    @Test
    fun contrast_ratio_goes_from_1_to_21() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), absoluteTolerance = 0.01f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), absoluteTolerance = 0.01f)
    }

    @Test
    fun text_on_a_brand_colour_is_black_or_white_by_luminance() {
        assertEquals(Color.Black, onBrandColor(stationBrand("FRANCEINFO", "franceinfo").color))
        assertEquals(Color.White, onBrandColor(stationBrand("FRANCEINTER", "France Inter").color))
        assertEquals(Color.Black, onBrandColor(Color.White))
        assertEquals(Color.White, onBrandColor(Color.Black))
    }

    // The colour of the text is picked once per surface (from its first stop), so
    // it must meet AA on every stop the gradient goes through.
    @Test
    fun text_meets_wcag_aa_on_every_brand_card_and_mini_player() {
        for ((id, color) in allBrandColors) {
            for ((surface, stops) in listOf(
                "card" to brandGradient(color),
                "mini-player" to miniPlayerGradient(color)
            )) {
                val text = onBrandColor(stops.first())
                for (stop in stops) {
                    val ratio = contrastRatio(text, stop)
                    assertTrue(ratio >= TEXT_CONTRAST, "$id $surface: $ratio < $TEXT_CONTRAST")
                }
            }
            val monogramText = onBrandColor(color)
            assertTrue(contrastRatio(monogramText, color) >= TEXT_CONTRAST, "$id monogram")
        }
    }

    @Test
    fun accents_meet_the_graphics_contrast_on_both_themes() {
        for ((id, color) in allBrandColors) {
            for ((theme, background) in listOf(
                "dark" to DarkBackground,
                "light" to LightBackground
            )) {
                val ratio = contrastRatio(brandAccent(color, background), background)
                assertTrue(ratio >= GRAPHICS_CONTRAST, "$id on $theme: $ratio < $GRAPHICS_CONTRAST")
            }
        }
    }

    @Test
    fun an_accent_that_already_contrasts_is_left_unchanged() {
        val inter = stationBrand("FRANCEINTER", "France Inter").color
        assertEquals(inter, brandAccent(inter, LightBackground))
    }

    @Test
    fun known_brands_have_their_monogram() {
        assertEquals("FIP", stationBrand("FIP", "FIP").monogram)
        assertEquals("FI", stationBrand("FRANCEINFO", "franceinfo").monogram)
        assertEquals("ICI", stationBrand("FRANCEBLEU", "ICI").monogram)
    }

    @Test
    fun an_unknown_brand_gets_the_fallback_colour_and_its_initials() {
        val brand = stationBrand("NEW_BRAND", "Radio Bonheur")
        assertEquals(FallbackBrandColor, brand.color)
        assertEquals("RB", brand.monogram)
    }

    @Test
    fun initials_skip_punctuation_and_keep_two_words_at_most() {
        assertEquals("M", initialsOf("Mouv'"))
        assertEquals("FB", initialsOf("France Bleu Nord"))
        assertEquals("SÉ", initialsOf("Saint-Étienne"))
        assertEquals("?", initialsOf(""))
    }
}
