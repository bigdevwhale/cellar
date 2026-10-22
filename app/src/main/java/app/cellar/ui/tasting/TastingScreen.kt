package app.cellar.ui.tasting

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cellar.R
import app.cellar.container
import app.cellar.data.Bottle
import app.cellar.data.State
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.Art
import app.cellar.ui.art.BottleLook
import app.cellar.ui.art.bottle
import app.cellar.ui.art.height
import app.cellar.ui.components.PrimaryButton
import app.cellar.ui.components.SoftButton
import app.cellar.ui.components.TextLink
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

private enum class Act { NONE, OPEN, POUR, WAIT }

/**
 * The tasting: ripe bottles one at a time, each with the one question. Open it (the cork pops), pour it
 * into the barrel (the bottle tips and empties), or let it age another week.
 */
@Composable
fun TastingScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val scope = rememberCoroutineScope()
    // The ripe bottles as they were on coming down, so deciding doesn't reshuffle the line.
    val queue = remember { home.ripe.map { it.id } }
    var index by remember { mutableIntStateOf(0) }
    var act by remember { mutableStateOf(Act.NONE) }
    val t = remember { Animatable(0f) }
    val opened = remember { mutableStateListOf<Long>() }
    var pouredSum by remember { mutableLongStateOf(0L) }
    var pouredCount by remember { mutableIntStateOf(0) }
    var waited by remember { mutableIntStateOf(0) }

    val current: Bottle? = queue.getOrNull(index)?.let(home::bottle)

    fun decide(a: Act, b: Bottle) {
        if (act != Act.NONE) return
        act = a
        scope.launch {
            t.snapTo(0f)
            t.animateTo(1f, tween(if (a == Act.WAIT) 500 else 1500, easing = if (a == Act.POUR) LinearEasing else FastOutSlowInEasing))
            container.write {
                when (a) {
                    Act.OPEN -> cellar.open(b.id)
                    Act.POUR -> cellar.pour(b.id)
                    Act.WAIT -> cellar.extend(b.id)
                    Act.NONE -> Unit
                }
            }
            when (a) {
                Act.OPEN -> opened += b.id
                Act.POUR -> { pouredSum += b.price; pouredCount++ }
                Act.WAIT -> waited++
                Act.NONE -> Unit
            }
            index++
            act = Act.NONE
            t.snapTo(0f)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.tasting), go::back)
        if (current == null) {
            Done(opened.size, pouredCount, home.money(pouredSum), waited, queue.isEmpty(), go)
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (queue.size > 1) Text(stringResource(R.string.tasting_of, index + 1, queue.size), style = SType.Small)
            Stage(home.look(current).copy(cobweb = false), act, t.value, Modifier.fillMaxWidth().height(300.dp))
            Text(current.name, style = SType.Hand.copy(fontSize = 34.sp, lineHeight = 36.sp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            val line = listOfNotNull(
                current.price.takeIf { it > 0 }?.let(home::money),
                stringResource(R.string.laid_ago, T.ago(c, current.laidAt, home.today)),
                T.because(c, current.reasonOf),
            ).joinToString(" · ")
            Text(line, style = SType.Muted, textAlign = TextAlign.Center)
            if (current.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.quote, current.note), style = SType.Hand.copy(color = S.c.muted, fontSize = 22.sp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.still_want), style = SType.Display, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(stringResource(R.string.open_it), { decide(Act.OPEN, current) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.LocalBar)
            SoftButton(if (home.dream.name.isBlank()) stringResource(R.string.pour_it) else stringResource(R.string.pour_into, home.dream.name), { decide(Act.POUR, current) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.WaterDrop)
            TextLink(stringResource(R.string.another_week), { decide(Act.WAIT, current) }, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** The bottle under the candle, and what happens to it. */
@Composable
private fun Stage(look: BottleLook, act: Act, t: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val glow = Offset(w / 2, h * 0.5f)
        drawCircle(Brush.radialGradient(listOf(Art.Amber.copy(alpha = 0.2f), Color.Transparent), glow, h * 0.55f), h * 0.55f, glow)
        drawOval(Color.Black.copy(alpha = 0.3f), Offset(w * 0.25f, h * 0.93f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.05f))
        val box = Rect(w / 2 - h * 0.3f, h * 0.08f, w / 2 + h * 0.3f, h * 0.95f)
        val bh = box.height * look.size.height()
        when (act) {
            Act.OPEN -> {
                // The bottle stands open; the cork flies up and away, spinning, with a few sparks.
                bottle(look.copy(state = State.OPENED, level = 1f, dust = look.dust * (1 - t), ripe = false), box)
                val neck = Offset(box.center.x, box.bottom - bh)
                val p = Offset(neck.x + w * 0.28f * t, neck.y - h * 0.5f * t + h * 0.55f * t * t)
                rotate(720f * t, p) {
                    drawRoundRect(Color(0xFFC9A273).copy(alpha = 1 - t * 0.7f), Offset(p.x - h * 0.018f, p.y - h * 0.035f), androidx.compose.ui.geometry.Size(h * 0.036f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(h * 0.01f))
                }
                val r = Random(9)
                repeat(14) {
                    val a = r.nextFloat() * Art.TAU
                    val d = h * (0.06f + r.nextFloat() * 0.2f) * t
                    drawCircle(Color(0xFFFFE2A8).copy(alpha = (1 - t) * 0.9f), h * 0.007f, Offset(neck.x + kotlin.math.cos(a) * d, neck.y + sin(a) * d - h * 0.05f * t))
                }
            }
            Act.POUR -> {
                // Tip over to the right, the stream falling into the dark below, the bottle emptying.
                val tilt = (t / 0.3f).coerceAtMost(1f) * 105f
                val pivot = Offset(box.center.x, box.bottom - bh * 0.45f)
                val empty = ((t - 0.3f) / 0.6f).coerceIn(0f, 1f)
                translate(left = w * 0.1f * (t / 0.3f).coerceAtMost(1f)) {
                    rotate(tilt, pivot) {
                        bottle(look.copy(state = if (empty >= 1f) State.POURED else State.OPENED, level = 1 - empty, ripe = false), box)
                    }
                }
                if (t > 0.25f && t < 0.95f) {
                    // Where the neck ends up after tipping, roughly.
                    val mouth = Offset(pivot.x + w * 0.1f + bh * 0.55f * 0.97f, pivot.y + bh * 0.55f * 0.26f)
                    val thick = h * 0.022f * (1 - empty * 0.7f)
                    drawLine(Brush.verticalGradient(listOf(Art.Spirit, Art.Spirit.copy(alpha = 0.2f)), mouth.y, h), mouth, Offset(mouth.x + w * 0.02f, h), thick, StrokeCap.Round)
                    val r = Random(4)
                    repeat(8) {
                        val y = mouth.y + ((r.nextFloat() + t * 4) % 1f) * (h - mouth.y)
                        drawCircle(Art.Spirit.copy(alpha = 0.6f), h * 0.006f, Offset(mouth.x + w * 0.02f + (r.nextFloat() - 0.5f) * w * 0.05f, y))
                    }
                }
            }
            Act.WAIT -> {
                // A little more dust, and back to the rack.
                translate(left = w * 0.6f * t * t) { bottle(look.copy(dust = (look.dust + 0.2f * t).coerceAtMost(1f)), box) }
            }
            Act.NONE -> bottle(look, box)
        }
    }
}

/** The end of a tasting: what was opened, what went into the barrel. */
@Composable
private fun Done(opened: Int, poured: Int, sum: String, waited: Int, nothing: Boolean, go: Go) {
    val c = ctx()
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(if (nothing) R.string.tasting_none else R.string.tasting_done), style = SType.Display, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        if (nothing) {
            Text(stringResource(R.string.tasting_none_sub), style = SType.Sub, textAlign = TextAlign.Center)
        } else {
            val lines = buildList {
                if (opened > 0) add(T.plural(c, R.plurals.opened_n, opened))
                if (poured > 0) add(T.plural(c, R.plurals.poured_n, poured) + " · " + sum)
                if (waited > 0) add(T.plural(c, R.plurals.waited_n, waited))
            }
            lines.forEach { Text(it, style = SType.Sub, textAlign = TextAlign.Center) }
            if (opened > 0) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.tasting_opened_sub), style = SType.Muted, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(stringResource(R.string.back_to_cellar), go::cellar, Modifier.fillMaxWidth())
    }
}
