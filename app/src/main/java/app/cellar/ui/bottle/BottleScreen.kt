package app.cellar.ui.bottle

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.cellar.R
import app.cellar.container
import app.cellar.data.Bottle
import app.cellar.data.Kind
import app.cellar.data.State
import app.cellar.data.Verdict
import app.cellar.domain.Rules
import app.cellar.domain.Share
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.BottleArt
import app.cellar.ui.components.Photo
import app.cellar.ui.components.PrimaryButton
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.SoftButton
import app.cellar.ui.components.TextLink
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.Hand
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType

@Composable
fun BottleScreen(id: Long, home: Home, go: Go) {
    val c = ctx()
    val b = home.bottle(id) ?: return Box(Modifier.fillMaxSize().statusBarsPadding()) { TopBar("", go::back) }
    val container = c.container
    var deleting by remember { mutableStateOf(false) }
    var early by remember { mutableStateOf(false) }
    val state = b.stateOf
    val gift = b.kindOf == Kind.GIFT

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar("", go::back) {
            if (state == State.AGING) IconButton(onClick = { go.edit(b.id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit_bottle), tint = S.c.ink) }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = S.c.ink) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BottleArt(home.look(b), Modifier.size(width = 120.dp, height = 190.dp), halo = true)
                Spacer(Modifier.width(10.dp))
                Label(b, home, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            when (state) {
                State.AGING -> if (gift) GiftStatus(b, home) else Aging(b, home)
                State.OPENED -> Line(Icons.Rounded.LocalBar, stringResource(if (b.early) R.string.opened_early_on else R.string.opened_on, T.date(c, b.endedAt, home.today)), S.c.wine)
                State.POURED -> {
                    val dream = home.dreams.firstOrNull { it.id == b.dreamId }?.name.orEmpty()
                    Line(
                        Icons.Rounded.WaterDrop,
                        if (dream.isBlank()) stringResource(R.string.poured_on, T.date(c, b.endedAt, home.today)) else stringResource(R.string.poured_into, dream, T.date(c, b.endedAt, home.today)),
                        S.c.pour,
                    )
                }
                State.GIVEN -> Line(Icons.Rounded.CardGiftcard, stringResource(R.string.given_on, T.date(c, b.endedAt, home.today)), S.c.wine)
            }
            if (!gift) Text(stringResource(R.string.laid_because, T.because(c, b.reasonOf)), style = SType.Muted, modifier = Modifier.padding(top = 10.dp))

            if (state == State.OPENED) {
                SectionTitle(stringResource(R.string.how_is_it))
                Text(
                    if (Rules.verdictDue(b, home.now) || b.verdictOf != null) stringResource(R.string.how_is_it_sub) else stringResource(R.string.how_is_it_early, T.days(c, Rules.ASK_AFTER)),
                    style = SType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                VerdictPicker(b.verdictOf) { v -> container.write { cellar.verdict(b.id, v) } }
            }
            if (b.note.isNotBlank()) {
                SectionTitle(stringResource(R.string.note_title))
                Text(b.note, style = SType.Hand)
            }
            if (b.photo != null) {
                Spacer(Modifier.height(16.dp))
                Photo(b.photo, Modifier.fillMaxWidth().height(220.dp))
            }
            if (b.url.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().clip(S.RMd).border(1.dp, S.c.line, S.RMd).clickable {
                        runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, b.url.toUri())) }
                    }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = S.c.muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(Share.host(b.url).ifBlank { b.url }, style = SType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                gift && state == State.AGING -> PrimaryButton(stringResource(R.string.given), { container.write { cellar.give(b.id) }; go.back() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CardGiftcard)
                state == State.AGING && Rules.ripe(b, home.now) -> {
                    PrimaryButton(stringResource(R.string.to_tasting), go::tasting, Modifier.fillMaxWidth(), icon = Icons.Rounded.LocalBar)
                }
                state == State.AGING -> {
                    SoftButton(stringResource(R.string.pour_now), { container.write { cellar.pour(b.id) }; toast(c, R.string.poured_toast); go.back() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.WaterDrop)
                    TextLink(stringResource(R.string.open_early), { early = true }, Modifier.align(Alignment.CenterHorizontally))
                }
                else -> TextLink(stringResource(R.string.back_on_rack), { container.write { cellar.undo(b.id) } }, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_text)) },
        confirmButton = { TextButton(onClick = { deleting = false; container.write { cellar.delete(b.id) }; go.back() }) { Text(stringResource(R.string.delete), color = S.c.wine) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } },
    )
    if (early) AlertDialog(
        onDismissRequest = { early = false },
        title = { Text(stringResource(R.string.open_early_title)) },
        text = { Text(stringResource(R.string.open_early_text, T.days(c, Rules.daysLeft(b, home.now)))) },
        confirmButton = { TextButton(onClick = { early = false; container.write { cellar.open(b.id) } }) { Text(stringResource(R.string.open_anyway), color = S.c.wine) } },
        dismissButton = { TextButton(onClick = { early = false }) { Text(stringResource(R.string.let_it_age)) } },
    )
}

/** The paper label, written by hand: name, vintage, price and size. */
@Composable
private fun Label(b: Bottle, home: Home, modifier: Modifier) {
    val c = ctx()
    Column(
        modifier.rotate(-1.5f).clip(S.RSm).background(S.c.label).border(1.dp, S.c.labelInk.copy(alpha = 0.3f), S.RSm).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(b.name, style = SType.Hand.copy(color = S.c.labelInk, fontSize = 28.sp, lineHeight = 30.sp), maxLines = 4, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.vintage, T.vintage(b.laidAt)), style = SType.Small.copy(color = S.c.labelInk.copy(alpha = 0.7f), fontFamily = Hand, fontSize = 18.sp))
        if (b.price > 0) Text(
            "${home.money(b.price)} · ${T.size(c, home.scale.size(b.price)).lowercase()}",
            style = SType.Body.copy(color = S.c.labelInk, fontFamily = Hand, fontSize = 21.sp),
        )
        if (b.giftFor.isNotBlank()) Text(stringResource(R.string.for_whom, b.giftFor), style = SType.Body.copy(color = S.c.labelInk, fontFamily = Hand, fontSize = 21.sp))
    }
}

@Composable
private fun Aging(b: Bottle, home: Home) {
    val c = ctx()
    val ripe = Rules.ripe(b, home.now)
    val total = b.days + b.extraDays
    val aged = Rules.aged(b, home.now).coerceAtMost(total)
    Column(Modifier.fillMaxWidth().clip(S.RMd).background(if (ripe) S.c.flameSoft else S.c.surface).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ripe) Icons.Rounded.LocalBar else Icons.Rounded.HourglassTop, null, tint = if (ripe) S.c.flame else S.c.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                if (ripe) stringResource(R.string.ripe_still_want) else stringResource(R.string.ages_left, T.days(c, Rules.daysLeft(b, home.now))),
                style = SType.Strong,
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { Rules.progress(b, home.now) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(S.RSm),
            color = S.c.flame, trackColor = S.c.line, strokeCap = StrokeCap.Round, drawStopIndicator = {},
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.aged_of, aged, T.days(c, total)) + if (b.storm) " · " + stringResource(R.string.laid_in_storm) else "",
            style = SType.Small,
        )
    }
}

@Composable
private fun GiftStatus(b: Bottle, home: Home) {
    val c = ctx()
    val days = Rules.giftIn(b, home.today)
    Line(
        Icons.Rounded.CardGiftcard,
        when {
            b.giftAt == 0L -> stringResource(R.string.gift_waits)
            days == null -> stringResource(R.string.gift_date_passed, T.date(c, b.giftAt, home.today))
            days == 0 -> stringResource(R.string.gift_today)
            else -> stringResource(R.string.gift_in, T.plural(c, R.plurals.in_days, days))
        },
        S.c.wine,
    )
}

@Composable
private fun Line(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(Modifier.fillMaxWidth().clip(S.RMd).background(S.c.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = SType.Body)
    }
}

/** Three choices for how an opened bottle turned out. */
@Composable
fun VerdictPicker(current: Verdict?, onPick: (Verdict) -> Unit) {
    val c = ctx()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Verdict.entries.forEach { v ->
            val on = current == v
            val tint = when (v) {
                Verdict.VINTAGE -> S.c.wine
                Verdict.SOSO -> S.c.flame
                Verdict.VINEGAR -> S.c.pour
            }
            Column(
                Modifier.weight(1f).clip(S.RMd).background(if (on) tint.copy(alpha = 0.18f) else S.c.surface)
                    .border(1.dp, if (on) tint else S.c.line, S.RMd).clickable { onPick(v) }.padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VerdictMark(v, Modifier.size(30.dp))
                Spacer(Modifier.height(6.dp))
                Text(T.verdict(c, v), style = SType.Button.copy(fontSize = 14.sp, color = if (on) tint else S.c.ink), maxLines = 1)
                Text(T.verdictSub(c, v), style = SType.Small, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** A glass of wine, a plain tumbler or a vinegar cruet. */
@Composable
fun VerdictMark(v: Verdict, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val glass = Color(0xFFD9D2C6)
        when (v) {
            Verdict.VINTAGE -> {
                val bowl = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.25f, h * 0.08f); lineTo(w * 0.75f, h * 0.08f)
                    cubicTo(w * 0.78f, h * 0.45f, w * 0.62f, h * 0.55f, w * 0.5f, h * 0.55f)
                    cubicTo(w * 0.38f, h * 0.55f, w * 0.22f, h * 0.45f, w * 0.25f, h * 0.08f); close()
                }
                drawPath(bowl, glass.copy(alpha = 0.25f))
                val wine = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.27f, h * 0.28f); lineTo(w * 0.73f, h * 0.28f)
                    cubicTo(w * 0.72f, h * 0.47f, w * 0.6f, h * 0.53f, w * 0.5f, h * 0.53f)
                    cubicTo(w * 0.4f, h * 0.53f, w * 0.28f, h * 0.47f, w * 0.27f, h * 0.28f); close()
                }
                drawPath(wine, Color(0xFFB2333F))
                drawPath(bowl, glass, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.05f))
                drawLine(glass, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.55f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.88f), w * 0.05f)
                drawLine(glass, androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.9f), androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.9f), w * 0.06f, StrokeCap.Round)
            }
            Verdict.SOSO -> {
                val cup = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.24f, h * 0.15f); lineTo(w * 0.76f, h * 0.15f); lineTo(w * 0.7f, h * 0.9f); lineTo(w * 0.3f, h * 0.9f); close()
                }
                drawPath(cup, glass.copy(alpha = 0.2f))
                drawRect(Color(0xFFE9A447).copy(alpha = 0.8f), androidx.compose.ui.geometry.Offset(w * 0.29f, h * 0.55f), androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.33f))
                drawPath(cup, glass, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.05f))
            }
            Verdict.VINEGAR -> {
                drawCircle(Color(0xFF8FB36A).copy(alpha = 0.55f), w * 0.26f, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.66f))
                drawCircle(glass, w * 0.26f, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.66f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.05f))
                drawRect(glass, androidx.compose.ui.geometry.Offset(w * 0.43f, h * 0.18f), androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.24f))
                drawCircle(glass, w * 0.1f, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.13f))
            }
        }
    }
}

private fun toast(c: android.content.Context, id: Int) = Toast.makeText(c, id, Toast.LENGTH_SHORT).show()
