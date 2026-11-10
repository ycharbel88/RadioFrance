package com.chy.radiofrance.core.designsystem

import com.chy.radiofrance.core.designsystem.resources.Res
import com.chy.radiofrance.core.designsystem.resources.ic_equalizer
import com.chy.radiofrance.core.designsystem.resources.ic_error
import com.chy.radiofrance.core.designsystem.resources.ic_expand_more
import com.chy.radiofrance.core.designsystem.resources.ic_pause
import com.chy.radiofrance.core.designsystem.resources.ic_play
import org.jetbrains.compose.resources.DrawableResource

/**
 * The app's icons, shared by the station list and the mini-player. Material icons
 * as vector resources (Apache 2.0): `material-icons-core` is not published for
 * Compose Multiplatform 1.8+, and a handful of icons don't justify an icon library.
 *
 * Use with `painterResource(RadioFranceIcons.Play)`. The generated `Res` stays
 * internal: only these values leave the module.
 */
object RadioFranceIcons {
    val Play: DrawableResource get() = Res.drawable.ic_play
    val Pause: DrawableResource get() = Res.drawable.ic_pause
    val Equalizer: DrawableResource get() = Res.drawable.ic_equalizer
    val Error: DrawableResource get() = Res.drawable.ic_error
    val ExpandMore: DrawableResource get() = Res.drawable.ic_expand_more
}
