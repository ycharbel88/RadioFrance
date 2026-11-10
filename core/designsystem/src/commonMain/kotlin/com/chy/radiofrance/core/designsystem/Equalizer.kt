package com.chy.radiofrance.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The "on air" mark: three bars bouncing at different speeds.
 *
 * Decorative (no semantics): the state ("En lecture") is announced by the row or
 * the mini-player that shows it. With [animated] false the bars stand still, for
 * a paused station and for previews.
 */
@Composable
fun Equalizer(
    color: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    size: Dp = 20.dp
) {
    val heights = if (animated) {
        val transition = rememberInfiniteTransition(label = "equalizer")
        // Three periods that don't divide each other, so the bars never sync up.
        listOf(520, 380, 610).mapIndexed { index, periodMs ->
            transition.animateFloat(
                initialValue = 0.25f + 0.15f * index,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(periodMs, easing = LinearEasing),
                    RepeatMode.Reverse
                ),
                label = "bar$index"
            )
        }
    } else {
        remember { StillBars }
    }
    Canvas(modifier.size(size)) {
        val gap = this.size.width / 7
        val barWidth = gap * 5 / 3
        heights.forEachIndexed { index, height ->
            val barHeight = this.size.height * height.value
            drawRoundRect(
                color = color,
                topLeft = Offset(x = index * (barWidth + gap), y = this.size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

private val StillBars: List<State<Float>> = listOf(0.55f, 1f, 0.35f).map { height ->
    object : State<Float> {
        override val value = height
    }
}
