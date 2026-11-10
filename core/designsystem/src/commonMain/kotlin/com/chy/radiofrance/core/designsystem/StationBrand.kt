package com.chy.radiofrance.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * How a brand looks in the app: its colour and the monogram that stands in for its
 * logo.
 *
 * The colours are **approximations** picked by eye from the brands' public
 * identities. They are not official assets (the API serves no colours or logos),
 * and should be replaced by the real values from the brand guidelines.
 */
data class StationBrand(val color: Color, val monogram: String)

private val knownBrands: Map<String, StationBrand> = mapOf(
    "FRANCEINTER" to StationBrand(Color(0xFFE2001A), "IN"),
    "FRANCEINFO" to StationBrand(Color(0xFFFFC800), "FI"),
    "FRANCEMUSIQUE" to StationBrand(Color(0xFFA3005B), "FM"),
    "FRANCECULTURE" to StationBrand(Color(0xFF6A2C91), "FC"),
    "MOUV" to StationBrand(Color(0xFF3A3AD6), "M'"),
    "FIP" to StationBrand(Color(0xFFE2007A), "FIP"),
    // The ICI network (ex France Bleu) keeps its historic id in the API.
    "FRANCEBLEU" to StationBrand(Color(0xFF0055B8), "ICI")
)

/** Brand ids [stationBrand] knows, for tests and previews. */
val knownBrandIds: Set<String> get() = knownBrands.keys

/** Neutral slate for a brand the app doesn't know yet (a new API brand). */
val FallbackBrandColor = Color(0xFF4A5568)

/**
 * The look of [brandId]. An unknown brand still gets a readable look: the fallback
 * colour and initials taken from [brandName].
 */
fun stationBrand(brandId: String, brandName: String): StationBrand =
    knownBrands[brandId] ?: StationBrand(FallbackBrandColor, initialsOf(brandName))

/** First letter of up to two words, uppercase: "Radio Bonheur" -> "RB", "Mouv'" -> "M". */
internal fun initialsOf(name: String): String = name
    .split(' ', '-')
    .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
    .take(2)
    .joinToString("")
    .uppercase()
    .ifEmpty { "?" }
