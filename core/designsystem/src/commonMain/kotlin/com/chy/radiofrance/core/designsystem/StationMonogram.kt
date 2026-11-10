package com.chy.radiofrance.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A rounded square in the brand colour with its initials: stands in for a logo.
 *
 * Decorative: it has no semantics, since the row next to it already says the
 * station name. Its text is sized in dp, not sp, like an image: at 200 % font
 * scale "FIP" would not fit in the square, and nobody reads a logo for its text.
 */
@Composable
fun StationMonogram(brand: StationBrand, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val textSize = with(LocalDensity.current) { (size * monogramTextRatio(brand.monogram)).toSp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(brand.color)
            .clearAndSetSemantics { }
    ) {
        Text(
            text = brand.monogram,
            color = onBrandColor(brand.color),
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// Three letters need a smaller size than two to fit the same square.
private fun monogramTextRatio(monogram: String): Float = if (monogram.length >= 3) 0.3f else 0.36f
