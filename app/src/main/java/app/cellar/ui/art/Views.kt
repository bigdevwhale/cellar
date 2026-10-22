package app.cellar.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer

/** One bottle on its own, standing in the box it's given; cobwebs belong to the rack, so none here. */
@Composable
fun BottleArt(look: BottleLook, modifier: Modifier = Modifier, glow: Float = 0.6f, halo: Boolean = false) {
    Canvas(modifier) {
        if (halo) drawCircle(
            Brush.radialGradient(listOf(Art.Amber.copy(alpha = 0.18f), Color.Transparent), Offset(size.width / 2, size.height * 0.55f), size.minDimension * 0.6f),
            size.minDimension * 0.6f, Offset(size.width / 2, size.height * 0.55f),
        )
        bottle(look.copy(cobweb = false), Rect(0f, 0f, size.width, size.height), glow)
    }
}

/** The dream's barrel on its own. */
@Composable
fun BarrelArt(look: BarrelLook, modifier: Modifier = Modifier) {
    val clock by rememberClock(9000)
    val text = rememberTextMeasurer()
    Canvas(modifier) {
        barrel(look, Rect(0f, 0f, size.width, size.height), text, clock)
    }
}
