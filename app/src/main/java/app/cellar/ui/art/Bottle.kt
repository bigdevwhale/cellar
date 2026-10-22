package app.cellar.ui.art

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.cellar.data.State
import app.cellar.domain.Size
import kotlin.random.Random

/** How one bottle looks: its size, the glass and label its [shape] number picks, and what time has done to it. */
@Immutable
data class BottleLook(
    val size: Size,
    val shape: Long,
    val dust: Float = 0f,
    val cobweb: Boolean = false,
    val ripe: Boolean = false,
    val gift: Boolean = false,
    val state: State = State.AGING,
    /** 0..1 how much is left inside; 1 for anything still sealed. */
    val level: Float = 1f,
)

private val Labels = listOf(Color(0xFFEADCBE), Color(0xFFF1E8D6), Color(0xFFE3CFA6), Color(0xFFDCD3C2))

/** How tall a size stands in its slot. */
fun Size.height(): Float = when (this) {
    Size.VIAL -> 0.56f
    Size.BOTTLE -> 0.82f
    Size.MAGNUM -> 1f
    Size.CASK -> 0.62f
}

/**
 * Draws a bottle standing on the bottom of [box], centred. A cask lies on a little stand instead.
 * [glow] is the warmth of a nearby candle on the glass, 0..1.
 */
fun DrawScope.bottle(look: BottleLook, box: Rect, glow: Float = 0.5f) {
    val h = box.height * look.size.height()
    val cx = box.center.x
    val bottom = box.bottom
    if (look.ripe) {
        drawCircle(
            Brush.radialGradient(listOf(Art.Amber.copy(alpha = 0.32f), Color.Transparent), Offset(cx, bottom - h * 0.5f), h * 0.75f),
            h * 0.75f, Offset(cx, bottom - h * 0.5f),
        )
    }
    if (look.size == Size.CASK) {
        cask(look, Rect(cx - box.width * 0.47f, bottom - h, cx + box.width * 0.47f, bottom), glow)
    } else {
        glass(look, cx, bottom, h, glow)
    }
    // A cobweb needs room between the neck and the shelf above.
    if (look.cobweb && bottom - h - box.top > box.height * 0.12f) cobweb(box, Offset(cx, bottom - h), look.shape)
}

private fun silhouette(size: Size, cx: Float, bottom: Float, h: Float): Path {
    val top = bottom - h
    return Path().apply {
        when (size) {
            Size.VIAL -> {
                // A squat apothecary flask: round shoulders, short neck.
                val bw = h * 0.36f
                val nw = h * 0.1f
                moveTo(cx - bw, bottom - h * 0.04f)
                quadraticTo(cx - bw, bottom, cx - bw + h * 0.05f, bottom)
                lineTo(cx + bw - h * 0.05f, bottom)
                quadraticTo(cx + bw, bottom, cx + bw, bottom - h * 0.04f)
                lineTo(cx + bw, top + h * 0.5f)
                cubicTo(cx + bw, top + h * 0.32f, cx + nw, top + h * 0.34f, cx + nw, top + h * 0.2f)
                lineTo(cx + nw, top + h * 0.06f)
                lineTo(cx + nw * 1.25f, top + h * 0.04f)
                lineTo(cx + nw * 1.25f, top)
                lineTo(cx - nw * 1.25f, top)
                lineTo(cx - nw * 1.25f, top + h * 0.04f)
                lineTo(cx - nw, top + h * 0.06f)
                lineTo(cx - nw, top + h * 0.2f)
                cubicTo(cx - nw, top + h * 0.34f, cx - bw, top + h * 0.32f, cx - bw, top + h * 0.5f)
                close()
            }
            Size.BOTTLE -> {
                // Bordeaux: straight sides, a sharp shoulder.
                val bw = h * 0.135f
                val nw = h * 0.048f
                moveTo(cx - bw, bottom - h * 0.02f)
                quadraticTo(cx - bw, bottom, cx - bw + h * 0.02f, bottom)
                lineTo(cx + bw - h * 0.02f, bottom)
                quadraticTo(cx + bw, bottom, cx + bw, bottom - h * 0.02f)
                lineTo(cx + bw, top + h * 0.42f)
                cubicTo(cx + bw, top + h * 0.33f, cx + nw, top + h * 0.34f, cx + nw, top + h * 0.26f)
                lineTo(cx + nw, top + h * 0.02f)
                lineTo(cx + nw * 1.2f, top)
                lineTo(cx - nw * 1.2f, top)
                lineTo(cx - nw, top + h * 0.02f)
                lineTo(cx - nw, top + h * 0.26f)
                cubicTo(cx - nw, top + h * 0.34f, cx - bw, top + h * 0.33f, cx - bw, top + h * 0.42f)
                close()
            }
            else -> {
                // Burgundy magnum: long sloping shoulders.
                val bw = h * 0.15f
                val nw = h * 0.045f
                moveTo(cx - bw, bottom - h * 0.02f)
                quadraticTo(cx - bw, bottom, cx - bw + h * 0.02f, bottom)
                lineTo(cx + bw - h * 0.02f, bottom)
                quadraticTo(cx + bw, bottom, cx + bw, bottom - h * 0.02f)
                lineTo(cx + bw, top + h * 0.56f)
                cubicTo(cx + bw, top + h * 0.38f, cx + nw, top + h * 0.36f, cx + nw, top + h * 0.2f)
                lineTo(cx + nw, top + h * 0.02f)
                lineTo(cx + nw * 1.2f, top)
                lineTo(cx - nw * 1.2f, top)
                lineTo(cx - nw, top + h * 0.02f)
                lineTo(cx - nw, top + h * 0.2f)
                cubicTo(cx - nw, top + h * 0.36f, cx - bw, top + h * 0.38f, cx - bw, top + h * 0.56f)
                close()
            }
        }
    }
}

private fun DrawScope.glass(look: BottleLook, cx: Float, bottom: Float, h: Float, glow: Float) {
    val top = bottom - h
    val path = silhouette(look.size, cx, bottom, h)
    val bw = when (look.size) {
        Size.VIAL -> h * 0.36f
        Size.BOTTLE -> h * 0.135f
        else -> h * 0.15f
    }
    val nw = when (look.size) {
        Size.VIAL -> h * 0.1f
        Size.BOTTLE -> h * 0.048f
        else -> h * 0.045f
    }
    val glass = Art.Glass.pick(look.shape)
    val empty = look.state == State.POURED
    // Shadow on the shelf.
    drawOval(Color.Black.copy(alpha = 0.35f), Offset(cx - bw * 1.15f, bottom - h * 0.02f), GSize(bw * 2.3f, h * 0.045f))
    // The glass: dark at the edges, the candle's warmth on the left, a cool highlight strip.
    val body = if (empty) glass.lighter(0.25f).copy(alpha = 0.45f) else glass
    drawPath(
        path,
        Brush.horizontalGradient(
            0f to body.darker(0.55f), 0.22f to body.mix(Art.Amber, 0.18f * glow), 0.5f to body, 0.85f to body.darker(0.35f), 1f to body.darker(0.65f),
            startX = cx - bw, endX = cx + bw,
        ),
    )
    clipPath(path) {
        // What's left inside an opened bottle: wine below the line, air above.
        if (look.state == State.OPENED && look.level < 1f) {
            val line = bottom - (h * 0.62f) * look.level
            drawRect(Color.Black.copy(alpha = 0.18f), Offset(cx - bw, top), GSize(bw * 2, line - top))
        }
        drawRect(Color.White.copy(alpha = if (empty) 0.18f else 0.13f), Offset(cx - bw * 0.62f, top + h * 0.08f), GSize(bw * 0.16f, h * 0.86f))
        drawRect(Color.White.copy(alpha = 0.05f), Offset(cx + bw * 0.45f, top + h * 0.3f), GSize(bw * 0.08f, h * 0.6f))
        // Dust settles thickest on the shoulders.
        if (look.dust > 0.02f) {
            drawRect(
                Brush.verticalGradient(
                    0f to Art.Dust.copy(alpha = 0.7f * look.dust), 0.45f to Art.Dust.copy(alpha = 0.38f * look.dust), 1f to Art.Dust.copy(alpha = 0.12f * look.dust),
                    startY = top, endY = bottom,
                ),
                Offset(cx - bw, top), GSize(bw * 2, h),
            )
            val r = Random(look.shape)
            repeat((look.dust * 30).toInt()) {
                val x = cx - bw + r.nextFloat() * bw * 2
                val y = top + h * (0.15f + r.nextFloat() * r.nextFloat() * 0.85f)
                drawCircle(Art.Dust.copy(alpha = 0.3f * look.dust), h * 0.0035f * (1 + r.nextFloat()), Offset(x, y))
            }
        }
    }
    // The label.
    if (!empty) {
        val label = Labels.pick(look.shape, 3)
        val aged = label.mix(Color(0xFFC9A66B), look.dust * 0.55f)
        val ly0 = when (look.size) {
            Size.VIAL -> bottom - h * 0.46f
            Size.BOTTLE -> bottom - h * 0.5f
            else -> bottom - h * 0.44f
        }
        val ly1 = bottom - h * 0.14f
        val lx = bw * 0.78f
        val style = Math.floorMod(look.shape.toInt() ushr 3, 3)
        when (style) {
            0 -> drawRoundRect(aged, Offset(cx - lx, ly0), GSize(lx * 2, ly1 - ly0), CornerRadius(h * 0.01f))
            1 -> drawOval(aged, Offset(cx - lx, ly0), GSize(lx * 2, ly1 - ly0))
            else -> {
                val p = Path().apply {
                    moveTo(cx - lx, ly0); lineTo(cx + lx, ly0); lineTo(cx + lx, ly1 - (ly1 - ly0) * 0.25f)
                    quadraticTo(cx + lx, ly1, cx, ly1); quadraticTo(cx - lx, ly1, cx - lx, ly1 - (ly1 - ly0) * 0.25f); close()
                }
                drawPath(p, aged)
            }
        }
        // A rule and a few hand-written strokes standing for the name.
        val ink = Color(0xFF3A2A1C).copy(alpha = 0.75f)
        val mid = (ly0 + ly1) / 2
        drawLine(ink.copy(alpha = 0.4f), Offset(cx - lx * 0.6f, ly0 + (ly1 - ly0) * 0.2f), Offset(cx + lx * 0.6f, ly0 + (ly1 - ly0) * 0.2f), h * 0.004f)
        val r = Random(look.shape xor 0x5f)
        var x = cx - lx * 0.55f
        while (x < cx + lx * 0.45f) {
            val w = lx * (0.12f + r.nextFloat() * 0.22f)
            drawLine(ink, Offset(x, mid), Offset((x + w).coerceAtMost(cx + lx * 0.55f), mid + (r.nextFloat() - 0.5f) * h * 0.008f), h * 0.009f, StrokeCap.Round)
            x += w + lx * 0.07f
        }
        drawLine(ink.copy(alpha = 0.5f), Offset(cx - lx * 0.35f, mid + (ly1 - ly0) * 0.22f), Offset(cx + lx * 0.3f, mid + (ly1 - ly0) * 0.22f), h * 0.006f, StrokeCap.Round)
        // Dust on the label too.
        if (look.dust > 0.02f) drawRect(Art.Dust.copy(alpha = 0.22f * look.dust), Offset(cx - lx, ly0), GSize(lx * 2, ly1 - ly0))
    }
    // Capsule over the cork, or the bare lip of an opened bottle.
    val capTop = top
    val capBottom = top + h * (if (look.size == Size.VIAL) 0.1f else 0.13f)
    val cap = if (look.gift) Art.Ribbon.darker(0.1f) else Art.Capsules.pick(look.shape, 7)
    when (look.state) {
        State.AGING, State.GIVEN -> {
            drawRect(
                Brush.horizontalGradient(listOf(cap.darker(0.4f), cap.lighter(0.15f), cap, cap.darker(0.5f)), cx - nw * 1.25f, cx + nw * 1.25f),
                Offset(cx - nw * 1.25f, capTop), GSize(nw * 2.5f, capBottom - capTop),
            )
            drawLine(Color.Black.copy(alpha = 0.25f), Offset(cx - nw * 1.2f, capBottom), Offset(cx + nw * 1.2f, capBottom), h * 0.005f)
            if (look.dust > 0.02f) drawRect(Art.Dust.copy(alpha = 0.6f * look.dust), Offset(cx - nw * 1.25f, capTop), GSize(nw * 2.5f, (capBottom - capTop) * 0.5f))
        }
        State.OPENED -> {
            // The cork stands a little out of the neck, a ring of torn foil below it.
            drawRect(cap.darker(0.2f), Offset(cx - nw * 1.22f, capBottom - h * 0.03f), GSize(nw * 2.44f, h * 0.03f))
            drawRoundRect(Color(0xFFC9A273), Offset(cx - nw * 0.85f, capTop - h * 0.05f), GSize(nw * 1.7f, h * 0.08f), CornerRadius(nw * 0.3f))
        }
        State.POURED -> Unit
    }
    if (look.gift && look.state == State.AGING) bow(cx, capBottom + h * 0.02f, h)
}

/** A ribbon tied round the neck of a gift. */
private fun DrawScope.bow(cx: Float, y: Float, h: Float) {
    val r = h * 0.05f
    val c = Art.Ribbon
    val left = Path().apply { moveTo(cx, y); cubicTo(cx - r * 1.8f, y - r * 1.2f, cx - r * 2.0f, y + r * 0.9f, cx, y); close() }
    val right = Path().apply { moveTo(cx, y); cubicTo(cx + r * 1.8f, y - r * 1.2f, cx + r * 2.0f, y + r * 0.9f, cx, y); close() }
    drawPath(left, c)
    drawPath(right, c.darker(0.15f))
    drawLine(c, Offset(cx, y), Offset(cx - r * 0.8f, y + r * 2.2f), r * 0.45f, StrokeCap.Round)
    drawLine(c.darker(0.2f), Offset(cx, y), Offset(cx + r * 0.6f, y + r * 2.4f), r * 0.45f, StrokeCap.Round)
    drawCircle(c.lighter(0.1f), r * 0.35f, Offset(cx, y))
}

/** A small oak cask lying on its stand, its head towards us at the right. */
private fun DrawScope.cask(look: BottleLook, r: Rect, glow: Float) {
    val oak = Color(0xFF7A5232).mix(Color(0xFF5E3D22), Math.floorMod(look.shape.toInt(), 3) / 3f)
    val w = r.width
    val h = r.height * 0.82f
    val top = r.top
    val l = r.left + w * 0.04f
    val rr = r.right - w * 0.1f
    drawOval(Color.Black.copy(alpha = 0.35f), Offset(r.left + w * 0.05f, r.bottom - r.height * 0.06f), GSize(w * 0.9f, r.height * 0.08f))
    // Stand.
    drawRect(Color(0xFF3F2A19), Offset(l + w * 0.12f, top + h * 0.8f), GSize(w * 0.06f, r.bottom - top - h * 0.8f))
    drawRect(Color(0xFF3F2A19), Offset(rr - w * 0.2f, top + h * 0.8f), GSize(w * 0.06f, r.bottom - top - h * 0.8f))
    val body = Path().apply {
        moveTo(l, top + h * 0.12f)
        quadraticTo((l + rr) / 2, top - h * 0.04f, rr, top + h * 0.12f)
        lineTo(rr, top + h * 0.88f)
        quadraticTo((l + rr) / 2, top + h * 1.04f, l, top + h * 0.88f)
        close()
    }
    drawPath(body, Brush.verticalGradient(0f to oak.mix(Art.Amber, 0.2f * glow), 0.4f to oak, 1f to oak.darker(0.5f), startY = top, endY = top + h))
    clipPath(body) {
        for (i in 1..4) {
            val y = top + h * i / 5f
            drawLine(Color.Black.copy(alpha = 0.22f), Offset(l, y), Offset(rr, y), w * 0.006f)
        }
        listOf(0.2f, 0.78f).forEach { f ->
            val x = l + (rr - l) * f
            drawRect(Color(0xFF2B2622), Offset(x - w * 0.018f, top - h * 0.1f), GSize(w * 0.036f, h * 1.2f))
            drawRect(Color.White.copy(alpha = 0.08f), Offset(x - w * 0.018f, top - h * 0.1f), GSize(w * 0.01f, h * 1.2f))
        }
        if (look.dust > 0.02f) drawRect(
            Brush.verticalGradient(0f to Art.Dust.copy(alpha = 0.6f * look.dust), 0.5f to Color.Transparent, startY = top, endY = top + h),
            Offset(l, top - h * 0.1f), GSize(rr - l, h),
        )
    }
    // The head, with its bung and the little tap.
    drawOval(oak.darker(0.15f), Offset(rr - w * 0.05f, top + h * 0.06f), GSize(w * 0.12f, h * 0.88f))
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(rr - w * 0.05f, top + h * 0.06f), GSize(w * 0.12f, h * 0.88f), style = Stroke(w * 0.012f))
    drawRect(Color(0xFFB08A4A), Offset(rr + w * 0.04f, top + h * 0.62f), GSize(w * 0.05f, h * 0.07f))
    drawRect(Color(0xFF8E6E38), Offset(rr + w * 0.075f, top + h * 0.69f), GSize(w * 0.018f, h * 0.08f))
    if (look.gift && look.state == State.AGING) bow((l + rr) / 2, top + h * 0.02f, r.height * 1.4f)
}

/** A cobweb between the top of a bottle and the corner of its slot. */
private fun DrawScope.cobweb(box: Rect, neck: Offset, seed: Long) {
    val left = Math.floorMod(seed.toInt(), 2) == 0
    val corner = Offset(if (left) box.left + box.width * 0.04f else box.right - box.width * 0.04f, box.top + box.height * 0.02f)
    val c = Art.Web.copy(alpha = 0.42f)
    val sw = box.width * 0.008f
    val anchors = listOf(
        neck + Offset(0f, box.height * 0.02f),
        Offset(corner.x, corner.y + (neck.y - corner.y) * 0.9f),
        Offset(corner.x + (neck.x - corner.x) * 0.9f, corner.y),
    )
    anchors.forEach { drawLine(c, corner, it, sw) }
    for (k in 1..3) {
        val t = k / 4f
        val pts = anchors.map { corner + (it - corner) * t }
        val p = Path().apply {
            moveTo(pts[1].x, pts[1].y)
            quadraticTo(corner.x + (pts[0].x - corner.x) * 0.8f, corner.y + (pts[0].y - corner.y) * 0.8f, pts[0].x, pts[0].y)
            quadraticTo(corner.x + (pts[2].x - corner.x) * 0.8f + (pts[0].x - pts[2].x) * 0.1f, corner.y + (pts[0].y - corner.y) * 0.5f, pts[2].x, pts[2].y)
        }
        drawPath(p, c, style = Stroke(sw * 0.8f))
    }
}
