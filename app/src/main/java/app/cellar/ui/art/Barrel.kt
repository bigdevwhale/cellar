package app.cellar.ui.art

import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import app.cellar.ui.theme.Hand
import kotlin.math.sin

/** The barrel of a dream: the dream chalked on it, how full it is (0..1), and whether it's sealed. */
@Immutable
data class BarrelLook(val name: String, val sum: String, val fill: Float, val sealed: Boolean = false)

/**
 * An upright oak barrel filling [r]: iron hoops, the dream in chalk, a glass gauge showing the level
 * of what was poured into it and a brass tap. [t] ripples the spirit in the gauge.
 */
fun DrawScope.barrel(look: BarrelLook, r: Rect, text: TextMeasurer, t: Float = 0f, glow: Float = 0.6f) {
    val w = r.width
    val h = r.height
    val cx = r.center.x
    val e = h * 0.07f
    val b = w * 0.06f
    val l = r.left + w * 0.08f
    val rr = r.right - w * 0.08f
    val top = r.top + e
    val bottom = r.bottom - e * 0.6f
    val mid = (top + bottom) / 2
    val oak = Color(0xFF7A5232)

    drawOval(Color.Black.copy(alpha = 0.45f), Offset(r.left, bottom - e * 0.4f), Size(w, e * 1.4f))
    val body = Path().apply {
        moveTo(l, top)
        quadraticTo(l - b, mid, l, bottom)
        quadraticTo(cx, bottom + e, rr, bottom)
        quadraticTo(rr + b, mid, rr, top)
        quadraticTo(cx, top + e, l, top)
        close()
    }
    drawPath(
        body,
        Brush.horizontalGradient(
            0f to oak.darker(0.6f), 0.25f to oak.mix(Art.Amber, 0.25f * glow), 0.55f to oak, 1f to oak.darker(0.6f),
            startX = l - b, endX = rr + b,
        ),
    )
    clipPath(body) {
        // Staves.
        for (i in 1..7) {
            val f = i / 8f
            val x = l + (rr - l) * f
            val bulge = (f - 0.5f) * 2 * b
            val p = Path().apply { moveTo(x, top - e); quadraticTo(x + bulge, mid, x, bottom + e) }
            drawPath(p, Color.Black.copy(alpha = 0.22f), style = Stroke(w * 0.006f))
        }
        // Iron hoops.
        listOf(0.1f, 0.3f, 0.7f, 0.9f).forEach { f ->
            val y = top + (bottom - top) * f
            val p = Path().apply { moveTo(l - b * 2, y); quadraticTo(cx, y + e * 1.1f, rr + b * 2, y) }
            drawPath(p, Color(0xFF26221F), style = Stroke(h * 0.035f))
            drawPath(p, Color.White.copy(alpha = 0.07f), style = Stroke(h * 0.008f))
        }
    }
    // The lid.
    drawOval(oak.darker(0.25f), Offset(l, top - e), Size(rr - l, e * 2))
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(l, top - e), Size(rr - l, e * 2), style = Stroke(w * 0.012f))
    drawOval(oak.darker(0.4f), Offset(cx - w * 0.06f, top - e * 0.3f), Size(w * 0.12f, e * 0.6f))

    // The dream in chalk, a little crooked.
    val chalk = Color(0xFFE8E2D4)
    rotate(-4f, Offset(cx, top + (bottom - top) * 0.47f)) {
        val nameStyle = TextStyle(fontFamily = Hand, fontSize = (h * 0.1f / density).sp, color = chalk.copy(alpha = 0.92f), textAlign = TextAlign.Center)
        val name = text.measure(look.name, nameStyle, constraints = Constraints(maxWidth = (w * 0.66f).toInt()), maxLines = 2)
        val sum = text.measure(look.sum, nameStyle.copy(fontSize = (h * 0.07f / density).sp, color = chalk.copy(alpha = 0.75f)), constraints = Constraints(maxWidth = (w * 0.74f).toInt()), maxLines = 1, softWrap = false)
        val y0 = top + (bottom - top) * 0.47f - (name.size.height + sum.size.height) / 2f
        drawText(name, topLeft = Offset(cx - w * 0.04f - name.size.width / 2f, y0))
        drawText(sum, topLeft = Offset(cx - w * 0.04f - sum.size.width / 2f, y0 + name.size.height))
    }

    // The gauge: a glass tube on the right, with the poured spirit in it.
    val gx = rr - w * 0.15f
    val gTop = top + (bottom - top) * 0.16f
    val gBottom = bottom - (bottom - top) * 0.12f
    val gw = w * 0.07f
    drawRoundRect(Color.Black.copy(alpha = 0.45f), Offset(gx - gw / 2, gTop), Size(gw, gBottom - gTop), CornerRadius(gw / 2))
    val level = gBottom - (gBottom - gTop) * look.fill.coerceIn(0f, 1f)
    if (look.fill > 0f) {
        val wave = sin(t * 2) * h * 0.004f
        drawRoundRect(
            Brush.verticalGradient(listOf(Art.Spirit, Art.Spirit.mix(Art.Amber, 0.35f)), level, gBottom),
            Offset(gx - gw * 0.36f, level + wave), Size(gw * 0.72f, gBottom - level - wave - gw * 0.14f), CornerRadius(gw * 0.36f),
        )
    }
    drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(gx - gw * 0.3f, gTop + gw * 0.2f), Size(gw * 0.16f, gBottom - gTop - gw * 0.4f), CornerRadius(gw * 0.1f))
    drawRect(Color(0xFFB08A4A), Offset(gx - gw * 0.65f, gTop - gw * 0.2f), Size(gw * 1.3f, gw * 0.35f))
    drawRect(Color(0xFFB08A4A), Offset(gx - gw * 0.65f, gBottom - gw * 0.15f), Size(gw * 1.3f, gw * 0.35f))
    // Marks every quarter.
    for (i in 1..3) {
        val y = gBottom - (gBottom - gTop) * i / 4f
        drawLine(chalk.copy(alpha = 0.6f), Offset(gx + gw * 0.6f, y), Offset(gx + gw * 1.05f, y), w * 0.008f, StrokeCap.Round)
    }

    // The brass tap.
    val ty = bottom - (bottom - top) * 0.2f
    drawRoundRect(Color(0xFFC49A52), Offset(cx - w * 0.2f, ty - h * 0.025f), Size(w * 0.1f, h * 0.05f), CornerRadius(h * 0.01f))
    drawRect(Color(0xFF9E7A3C), Offset(cx - w * 0.13f, ty + h * 0.02f), Size(w * 0.03f, h * 0.06f))
    drawCircle(Color(0xFF8C6A30), w * 0.025f, Offset(cx - w * 0.15f, ty - h * 0.035f))

    if (look.sealed) waxSeal(Offset(cx - w * 0.04f, top + (bottom - top) * 0.78f), w * 0.09f, Color(0xFFA3262A))
}

/** A round wax seal with a pressed ring. */
fun DrawScope.waxSeal(c: Offset, radius: Float, wax: Color) {
    val p = Path()
    val n = 14
    for (i in 0..n) {
        val a = i * Art.TAU / n
        val rr = radius * (1f + 0.08f * sin(i * 2.3f))
        val pt = Offset(c.x + kotlin.math.cos(a) * rr, c.y + sin(a) * rr)
        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
    }
    p.close()
    drawPath(p, Brush.radialGradient(listOf(wax.lighter(0.15f), wax, wax.darker(0.35f)), c - Offset(radius * 0.3f, radius * 0.3f), radius * 1.3f))
    drawCircle(wax.darker(0.3f), radius * 0.66f, c, style = Stroke(radius * 0.1f))
    drawCircle(wax.lighter(0.2f).copy(alpha = 0.5f), radius * 0.66f, c + Offset(-radius * 0.04f, -radius * 0.04f), style = Stroke(radius * 0.04f))
}

/** A candle in a brass dish standing on [base], its flame at [f] (flicker). Returns the flame's centre. */
fun DrawScope.candle(base: Offset, height: Float, f: Float, sway: Float = 0f): Offset {
    val w = height * 0.28f
    val wax = Color(0xFFEFE3C8)
    // Dish.
    drawOval(Color(0xFF8C6A30), Offset(base.x - w * 1.1f, base.y - w * 0.22f), Size(w * 2.2f, w * 0.44f))
    drawOval(Color(0xFFC49A52), Offset(base.x - w * 1.0f, base.y - w * 0.3f), Size(w * 2.0f, w * 0.36f))
    // Wax with a drip.
    val top = base.y - height
    drawRect(Brush.horizontalGradient(listOf(wax.darker(0.3f), wax, wax.darker(0.15f)), base.x - w / 2, base.x + w / 2), Offset(base.x - w / 2, top), Size(w, height - w * 0.1f))
    drawOval(wax.lighter(0.1f), Offset(base.x - w / 2, top - w * 0.12f), Size(w, w * 0.24f))
    drawRoundRect(wax.lighter(0.05f), Offset(base.x + w * 0.18f, top), Size(w * 0.2f, height * 0.35f), CornerRadius(w * 0.1f))
    // Wick and flame.
    val wickTop = Offset(base.x, top - w * 0.3f)
    drawLine(Color(0xFF2A2018), Offset(base.x, top), wickTop, w * 0.08f)
    val fh = height * 0.42f * f
    val fw = w * 0.5f
    val tip = Offset(base.x + sway * fw * 0.6f, wickTop.y - fh)
    val flame = Path().apply {
        moveTo(base.x, wickTop.y + fw * 0.4f)
        cubicTo(base.x - fw * 1.1f, wickTop.y, base.x - fw * 0.5f, wickTop.y - fh * 0.6f, tip.x, tip.y)
        cubicTo(base.x + fw * 0.5f, wickTop.y - fh * 0.6f, base.x + fw * 1.1f, wickTop.y, base.x, wickTop.y + fw * 0.4f)
        close()
    }
    val centre = Offset(base.x, wickTop.y - fh * 0.35f)
    drawPath(flame, Brush.radialGradient(listOf(Color(0xFFFFF4D6), Color(0xFFFFC765), Color(0xFFE9762F)), centre + Offset(0f, fh * 0.15f), fh * 0.7f))
    drawOval(Color(0xFF6FA0E0).copy(alpha = 0.5f), Offset(base.x - fw * 0.35f, wickTop.y - fw * 0.1f), Size(fw * 0.7f, fw * 0.5f))
    return centre
}
