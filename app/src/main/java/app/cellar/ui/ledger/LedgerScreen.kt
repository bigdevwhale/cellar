package app.cellar.ui.ledger

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cellar.R
import app.cellar.domain.Ledger
import app.cellar.domain.Rules
import app.cellar.domain.Seal
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.waxSeal
import app.cellar.ui.components.Segmented
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.Hand
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType
import java.time.ZoneId

/** The ledger: the month, the year or all of it in figures, the cellarman's notes on your reasons, and the seals. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerScreen(home: Home, go: Go) {
    val c = ctx()
    var span by rememberSaveable { mutableIntStateOf(0) }
    val zone = ZoneId.systemDefault()
    val from = when (span) {
        0 -> home.today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        1 -> home.today.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
        else -> 0L
    }
    val ledger: Ledger = home.ledger(from)

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.ledger), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Segmented(listOf(0, 1, 2), span, { stringResource(listOf(R.string.this_month, R.string.this_year, R.string.all_time)[it]) }, { span = it })
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Figure("${ledger.laid}", stringResource(R.string.f_laid), S.c.ink, Modifier.weight(1f))
                Figure("${ledger.opened.size}", stringResource(R.string.f_opened), S.c.wine, Modifier.weight(1f))
                Figure("${ledger.poured.size}", stringResource(R.string.f_poured), S.c.pour, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(S.RMd).background(S.c.pourSoft).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.f_saved), style = SType.Body, modifier = Modifier.weight(1f))
                Text(home.money(ledger.pouredSum), style = SType.Figures.copy(fontSize = 20.sp, color = S.c.pour))
            }
            if (ledger.openedSum > 0) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.f_spent), style = SType.Muted, modifier = Modifier.weight(1f))
                    Text(home.money(ledger.openedSum), style = SType.Figures.copy(color = S.c.muted))
                }
            }

            SectionTitle(stringResource(R.string.notes))
            if (ledger.insights.isEmpty()) {
                Text(stringResource(R.string.notes_empty), style = SType.Muted)
            } else {
                Column(Modifier.fillMaxWidth().clip(S.RMd).background(S.c.label).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledger.insights.forEach { i -> Text("— " + T.insight(c, i), style = SType.Hand.copy(color = S.c.labelInk, fontSize = 22.sp, lineHeight = 25.sp)) }
                }
            }

            if (ledger.rows.isNotEmpty()) {
                SectionTitle(stringResource(R.string.by_reason))
                Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Key(S.c.wine, stringResource(R.string.f_opened))
                    Key(S.c.pour, stringResource(R.string.f_poured))
                    Key(S.c.line, stringResource(R.string.f_aging))
                }
                ledger.rows.sortedByDescending { it.laid }.forEach { r ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(T.reason(c, r.reason), style = SType.Strong, modifier = Modifier.weight(1f))
                            Text(
                                listOfNotNull(
                                    "${r.laid}",
                                    if (r.vinegar > 0) stringResource(R.string.vinegar_n, r.vinegar) else null,
                                ).joinToString(" · "),
                                style = SType.Figures.copy(color = S.c.muted, fontSize = 13.sp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Bar(r.opened, r.poured, r.laid - r.opened - r.poured)
                    }
                }
            }

            SectionTitle(stringResource(R.string.seals))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                Seal.entries.forEach { s -> SealCard(s, home.seals[s], home, Modifier.weight(1f)) }
            }
            if (home.settings.storms) {
                val (storm, day) = Rules.nextStorm(home.today)
                Text(
                    stringResource(R.string.next_storm, T.storm(c, storm), T.date(c, day.atStartOfDay(zone).toInstant().toEpochMilli(), home.today)),
                    style = SType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Figure(value: String, label: String, tint: Color, modifier: Modifier) {
    Column(modifier.clip(S.RMd).background(S.c.surface).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = SType.Display.copy(color = tint))
        Text(label, style = SType.Small, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Key(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(S.RSm).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = SType.Small)
    }
}

/** Opened, poured and still ageing, side by side in one bar. */
@Composable
private fun Bar(opened: Int, poured: Int, aging: Int) {
    val total = (opened + poured + aging).coerceAtLeast(1).toFloat()
    val wine = S.c.wine
    val pour = S.c.pour
    val line = S.c.line
    Canvas(Modifier.fillMaxWidth().height(10.dp).clip(S.RSm)) {
        var x = 0f
        listOf(opened to wine, poured to pour, aging to line).forEach { (n, col) ->
            val w = size.width * n / total
            drawRect(col, Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height))
            x += w
        }
    }
}

@Composable
private fun SealCard(s: Seal, at: Long?, home: Home, modifier: Modifier) {
    val c = ctx()
    val (title, sub) = T.seal(c, s)
    val on = at != null
    Column(
        modifier.clip(S.RMd).background(if (on) S.c.surface2 else S.c.surface).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(44.dp)) {
            waxSeal(center, size.minDimension * 0.42f, if (on) Color(0xFFA3262A) else Color(0xFF3A2D24))
        }
        Spacer(Modifier.height(6.dp))
        Text(title, style = SType.Small.copy(color = if (on) S.c.ink else S.c.faint, fontFamily = Hand, fontSize = 18.sp), textAlign = TextAlign.Center, maxLines = 2)
        Text(if (on) T.date(c, at!!, home.today) else sub, style = SType.Small.copy(fontSize = 11.sp), textAlign = TextAlign.Center, maxLines = 3)
    }
}
