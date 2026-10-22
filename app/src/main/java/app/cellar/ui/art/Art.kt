package app.cellar.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.cellar.BuildConfig
import kotlin.math.PI
import kotlin.math.sin

/** Shared bits of the drawn cellar. */
object Art {
    /** Screenshots and renders want a still picture: candles that don't flicker, rain that holds still. */
    var still: Boolean = BuildConfig.SHOTS

    const val TAU = (2 * PI).toFloat()

    val Glass = listOf(Color(0xFF2E4A2B), Color(0xFF4B5A26), Color(0xFF7A4A1C), Color(0xFF452617), Color(0xFF34404A), Color(0xFF5A2230))
    val Capsules = listOf(Color(0xFFA3262A), Color(0xFFC49A3A), Color(0xFF2A2523), Color(0xFF6B1E2E), Color(0xFF2F5A3A), Color(0xFF8C8F94))
    val Dust = Color(0xFFB9AE9E)
    val Web = Color(0xFFD9D2C6)
    val Ribbon = Color(0xFFC23B3B)
    val Amber = Color(0xFFF2B35A)
    val Spirit = Color(0xFF9CC9BC)
}

/** A slow 0..TAU clock for flames and rain; 0 when still. */
@Composable
fun rememberClock(periodMs: Int = 6000): State<Float> {
    if (Art.still) return remember { mutableFloatStateOf(0f) }
    val t = rememberInfiniteTransition(label = "clock")
    return t.animateFloat(0f, Art.TAU, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "phase")
}

/** A candle's flicker at clock [t]: 0.85..1.1, never quite regular. */
fun flicker(t: Float, seed: Int = 0): Float =
    1f + 0.06f * sin(t * 7 + seed) + 0.04f * sin(t * 13 + seed * 2.1f) + 0.03f * sin(t * 23 + seed * 0.7f)

fun Color.mix(other: Color, t: Float) = lerp(this, other, t.coerceIn(0f, 1f))
fun Color.darker(t: Float) = mix(Color.Black, t)
fun Color.lighter(t: Float) = mix(Color.White, t)

/** One of these, picked by a bottle's [seed]; a different [salt] picks independently. */
fun <T> List<T>.pick(seed: Long, salt: Int = 0): T {
    val x = (seed + salt * 0x45D9F3BL) * 0x2545F4914F6CDD1DL
    return this[Math.floorMod((x xor (x ushr 29)).toInt(), size)]
}
