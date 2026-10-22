package app.cellar.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.sin
import kotlin.random.Random

/** A bottle on the rack, by its id. */
@Immutable
data class Slot(val id: Long, val look: BottleLook)

const val SHELVES = 4
const val PER_SHELF = 5

/** Where things stand in the cellar, as fractions of its width and height. */
private object Plan {
    const val RACK_L = 0.035f
    const val RACK_R = 0.655f
    const val RACK_T = 0.25f
    const val RACK_B = 0.915f
    const val FLOOR = 0.915f
    val BARREL = Rect(0.665f, 0.585f, 0.985f, 0.935f)
    val NICHE = Offset(0.83f, 0.43f)
    val WINDOW = Rect(0.29f, 0.075f, 0.45f, 0.205f)
}

private fun slotRect(w: Float, h: Float, shelf: Int, i: Int): Rect {
    val l = w * Plan.RACK_L + w * 0.02f
    val r = w * Plan.RACK_R - w * 0.02f
    val sw = (r - l) / PER_SHELF
    val sh = h * (Plan.RACK_B - Plan.RACK_T) / SHELVES
    val top = h * Plan.RACK_T + shelf * sh
    return Rect(l + i * sw, top + sh * 0.07f, l + (i + 1) * sw, top + sh - h * 0.012f)
}

/**
 * The cellar by candlelight: a vaulted stone room, a barred window (with a storm in it on the big sale
 * days), an oak rack of [shelves] (up to four shelves of five), and the dream's barrel.
 * Tapping a bottle or the barrel opens it.
 */
@Composable
fun CellarScene(
    shelves: List<List<Slot>>,
    barrel: BarrelLook,
    storm: Boolean,
    modifier: Modifier = Modifier,
    onBottle: (Long) -> Unit = {},
    onBarrel: () -> Unit = {},
) {
    val clock by rememberClock(9000)
    val text = rememberTextMeasurer()
    val slots by rememberUpdatedState(shelves)
    val tapBottle by rememberUpdatedState(onBottle)
    val tapBarrel by rememberUpdatedState(onBarrel)
    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures { p ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val barrelRect = Rect(Plan.BARREL.left * w, Plan.BARREL.top * h, Plan.BARREL.right * w, Plan.BARREL.bottom * h)
                if (barrelRect.contains(p)) { tapBarrel(); return@detectTapGestures }
                slots.forEachIndexed { s, row ->
                    row.forEachIndexed { i, slot -> if (slotRect(w, h, s, i).contains(p)) { tapBottle(slot.id); return@detectTapGestures } }
                }
            }
        },
    ) {
        val w = size.width
        val h = size.height
        wall(w, h)
        vault(w, h)
        window(Rect(Plan.WINDOW.left * w, Plan.WINDOW.top * h, Plan.WINDOW.right * w, Plan.WINDOW.bottom * h), storm, clock)
        floor(w, h)
        // The niche and its candle.
        val niche = Offset(Plan.NICHE.x * w, Plan.NICHE.y * h)
        val nw = w * 0.1f
        val nicheShape = Path().apply {
            moveTo(niche.x - nw, niche.y)
            lineTo(niche.x - nw, niche.y - nw * 1.3f)
            quadraticTo(niche.x, niche.y - nw * 2.6f, niche.x + nw, niche.y - nw * 1.3f)
            lineTo(niche.x + nw, niche.y)
            close()
        }
        drawPath(nicheShape, Brush.verticalGradient(listOf(Color(0xFF120D0A), Color(0xFF2A1F18)), niche.y - nw * 2.2f, niche.y))
        drawRect(Color(0xFF4A3D33), Offset(niche.x - nw * 1.15f, niche.y), Size(nw * 2.3f, h * 0.012f))
        val f1 = flicker(clock, 1)
        val f2 = flicker(clock + 1.7f, 4)
        val flame1 = candle(Offset(niche.x, niche.y), h * 0.075f, f1, sin(clock * 3) * 0.3f)
        rack(w, h, shelves, glow = 0.55f)
        // A candle on the top of the rack.
        val flame2 = candle(Offset(w * 0.1f, h * Plan.RACK_T - h * 0.012f), h * 0.06f, f2, sin(clock * 2 + 1) * 0.3f)
        val bRect = Rect(Plan.BARREL.left * w, Plan.BARREL.top * h, Plan.BARREL.right * w, Plan.BARREL.bottom * h)
        barrel(barrel, bRect, text, clock)
        // Candlelight over everything, then the dark at the edges.
        listOf(flame1 to f1, flame2 to f2).forEach { (c, f) ->
            drawCircle(Brush.radialGradient(listOf(Art.Amber.copy(alpha = 0.16f * f), Art.Amber.copy(alpha = 0.05f), Color.Transparent), c, w * 0.55f), w * 0.55f, c)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE2A8).copy(alpha = 0.35f * f), Color.Transparent), c, w * 0.05f), w * 0.05f, c)
        }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)), Offset(w * 0.5f, h * 0.55f), maxOf(w, h) * 0.75f))
    }
}

private fun DrawScope.wall(w: Float, h: Float) {
    drawRect(Color(0xFF231C17))
    val r = Random(7)
    val rows = 16
    val rh = h / rows
    for (row in 0 until rows) {
        var x = -r.nextFloat() * w * 0.12f
        while (x < w) {
            val bw = w * (0.1f + r.nextFloat() * 0.1f)
            val tone = 0.75f + r.nextFloat() * 0.4f
            val c = Color(0xFF3A3029).mix(Color(0xFF4A3E33), r.nextFloat()).darker(1f - tone.coerceAtMost(1f))
            drawRoundRect(c, Offset(x + w * 0.004f, row * rh + h * 0.003f), Size(bw - w * 0.008f, rh - h * 0.006f), CornerRadius(w * 0.012f))
            drawRect(Color.White.copy(alpha = 0.03f), Offset(x + w * 0.006f, row * rh + h * 0.004f), Size(bw - w * 0.012f, rh * 0.18f))
            x += bw
        }
    }
}

/** The vaulted ceiling: dark above a brick arch. */
private fun DrawScope.vault(w: Float, h: Float) {
    val springs = h * 0.24f
    val crown = -h * 0.11f
    val ceiling = Path().apply {
        moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, springs)
        quadraticTo(w / 2, crown, 0f, springs); close()
    }
    drawPath(ceiling, Brush.verticalGradient(listOf(Color(0xFF0B0807), Color(0xFF1A1411)), 0f, springs))
    // The arch's bricks.
    val n = 22
    fun at(t: Float, off: Float): Offset {
        val x = w * t
        val y = (1 - t) * (1 - t) * springs + 2 * (1 - t) * t * crown + t * t * springs
        return Offset(x, y + off)
    }
    val band = h * 0.03f
    val arch = Path().apply {
        moveTo(0f, springs)
        quadraticTo(w / 2, crown, w, springs)
        lineTo(w, springs + band)
        quadraticTo(w / 2, crown + band, 0f, springs + band)
        close()
    }
    drawPath(arch, Color(0xFF5A4636))
    for (i in 0..n) {
        val t = i / n.toFloat()
        drawLine(Color(0xFF2A1F18), at(t, 0f), at(t, band), w * 0.004f)
    }
    drawPath(Path().apply { moveTo(0f, springs + band); quadraticTo(w / 2, crown + band, w, springs + band) }, Color.Black.copy(alpha = 0.35f), style = Stroke(h * 0.006f))
}

/** A small barred window high up; rain and lightning in it when there's a storm. */
private fun DrawScope.window(r: Rect, storm: Boolean, t: Float) {
    val shape = Path().apply {
        moveTo(r.left, r.bottom)
        lineTo(r.left, r.top + r.height * 0.4f)
        quadraticTo(r.center.x, r.top - r.height * 0.25f, r.right, r.top + r.height * 0.4f)
        lineTo(r.right, r.bottom)
        close()
    }
    drawPath(shape, Color(0xFF15100D), style = Stroke(r.width * 0.12f))
    clipPath(shape) {
        if (storm) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF10151C), Color(0xFF263040)), r.top, r.bottom), r.topLeft, r.size)
            // Lightning: a bolt, and the sky lit behind it now and then.
            val flash = if (Art.still) 1f else ((sin(t * 3) - 0.86f) / 0.14f).coerceIn(0f, 1f)
            drawRect(Color(0xFFBFD3F2).copy(alpha = 0.45f * flash), r.topLeft, r.size)
            if (flash > 0f) {
                val bolt = Path().apply {
                    moveTo(r.left + r.width * 0.62f, r.top)
                    lineTo(r.left + r.width * 0.45f, r.top + r.height * 0.45f)
                    lineTo(r.left + r.width * 0.58f, r.top + r.height * 0.45f)
                    lineTo(r.left + r.width * 0.38f, r.bottom)
                }
                drawPath(bolt, Color(0xFFF4F7FF).copy(alpha = flash), style = Stroke(r.width * 0.035f, cap = StrokeCap.Round))
            }
            val rain = Random(3)
            repeat(26) {
                val x0 = r.left + rain.nextFloat() * r.width * 1.3f
                val y0 = r.top + ((rain.nextFloat() + t / Art.TAU * 6f) % 1f) * r.height * 1.2f - r.height * 0.2f
                drawLine(Color(0xFF9FB4CF).copy(alpha = 0.55f), Offset(x0, y0), Offset(x0 - r.width * 0.08f, y0 + r.height * 0.16f), r.width * 0.012f)
            }
        } else {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1C2638), Color(0xFF3A4660)), r.top, r.bottom), r.topLeft, r.size)
            val stars = Random(11)
            repeat(9) {
                drawCircle(Color.White.copy(alpha = 0.5f + stars.nextFloat() * 0.4f), r.width * 0.012f, Offset(r.left + stars.nextFloat() * r.width, r.top + stars.nextFloat() * r.height * 0.7f))
            }
            drawCircle(Color(0xFFF1E9CF), r.width * 0.1f, Offset(r.left + r.width * 0.7f, r.top + r.height * 0.32f))
        }
        // Grass at the foot of the window: the cellar is below the ground.
        drawRect(Color(0xFF1E2A1A), Offset(r.left, r.bottom - r.height * 0.14f), Size(r.width, r.height * 0.14f))
    }
    // Bars.
    val bar = Color(0xFF2B2622)
    for (i in 1..3) {
        val x = r.left + r.width * i / 4f
        drawLine(bar, Offset(x, r.top - r.height * 0.1f), Offset(x, r.bottom), r.width * 0.045f)
    }
    drawLine(bar, Offset(r.left, r.top + r.height * 0.55f), Offset(r.right, r.top + r.height * 0.55f), r.width * 0.04f)
    drawRect(Color(0xFF4A3D33), Offset(r.left - r.width * 0.1f, r.bottom), Size(r.width * 1.2f, r.height * 0.08f))
}

private fun DrawScope.floor(w: Float, h: Float) {
    val y = h * Plan.FLOOR
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A221C), Color(0xFF15100D)), y, h), Offset(0f, y), Size(w, h - y))
    val r = Random(5)
    var x = 0f
    while (x < w) {
        val fw = w * (0.12f + r.nextFloat() * 0.1f)
        drawLine(Color.Black.copy(alpha = 0.4f), Offset(x, y), Offset(x - w * 0.03f, h), w * 0.004f)
        x += fw
    }
    drawLine(Color.Black.copy(alpha = 0.4f), Offset(0f, y + (h - y) * 0.45f), Offset(w, y + (h - y) * 0.45f), w * 0.004f)
}

/** The oak rack and the bottles on it. */
private fun DrawScope.rack(w: Float, h: Float, shelves: List<List<Slot>>, glow: Float) {
    val l = w * Plan.RACK_L
    val r = w * Plan.RACK_R
    val t = h * Plan.RACK_T
    val b = h * Plan.RACK_B
    val oak = Color(0xFF6E4B2E)
    val post = w * 0.022f
    // The back of the rack, darker than the wall.
    drawRect(Color.Black.copy(alpha = 0.38f), Offset(l, t), Size(r - l, b - t))
    val sh = (b - t) / SHELVES
    for (s in 0 until SHELVES) {
        val y = t + (s + 1) * sh - h * 0.012f
        shelves.getOrNull(s)?.forEachIndexed { i, slot ->
            if (i < PER_SHELF) bottle(slot.look, slotRect(w, h, s, i), glow)
        }
        // The board.
        drawRect(Brush.verticalGradient(listOf(oak.lighter(0.08f), oak, oak.darker(0.45f)), y, y + h * 0.018f), Offset(l, y), Size(r - l, h * 0.018f))
        drawRect(Color.Black.copy(alpha = 0.3f), Offset(l, y + h * 0.018f), Size(r - l, h * 0.008f))
    }
    // Top beam and posts.
    drawRect(Brush.verticalGradient(listOf(oak.lighter(0.1f), oak.darker(0.3f)), t - h * 0.014f, t + h * 0.006f), Offset(l - post * 0.3f, t - h * 0.014f), Size(r - l + post * 0.6f, h * 0.02f))
    listOf(l, r).forEach { x ->
        drawRect(Brush.horizontalGradient(listOf(oak.darker(0.4f), oak, oak.darker(0.5f)), x - post / 2, x + post / 2), Offset(x - post / 2, t - h * 0.014f), Size(post, b - t + h * 0.03f))
    }
    // Chalked numbers at the end of every shelf.
    for (s in 0 until SHELVES) {
        val y = t + s * sh + sh * 0.5f
        repeat(s + 1) { k -> drawLine(Color(0xFFE8E2D4).copy(alpha = 0.5f), Offset(l - post * 0.15f + k * post * 0.2f - post * 0.2f, y - sh * 0.06f), Offset(l - post * 0.15f + k * post * 0.2f - post * 0.2f, y + sh * 0.06f), w * 0.003f) }
    }
}
