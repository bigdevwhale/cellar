package app.cellar.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cellar.R
import app.cellar.container
import app.cellar.data.Bottle
import app.cellar.data.Verdict
import app.cellar.domain.Rules
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.BottleArt
import app.cellar.ui.bottle.VerdictMark
import app.cellar.ui.bottle.VerdictPicker
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType

/** The sommelier's journal: what was opened, and how it turned out a month later. */
@Composable
fun JournalScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.journal), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.journal_sub), style = SType.Muted)
            val tasted = home.opened.mapNotNull { it.verdictOf }
            if (tasted.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Verdict.entries.forEach { v ->
                        Column(Modifier.weight(1f).clip(S.RMd).background(S.c.surface).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            VerdictMark(v, Modifier.size(26.dp))
                            Text("${tasted.count { it == v }}", style = SType.Display.copy(fontSize = 24.sp))
                            Text(T.verdict(c, v), style = SType.Small, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (home.due.isNotEmpty()) {
                SectionTitle(stringResource(R.string.waiting_verdict))
                home.due.forEach { b ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(S.RMd).background(S.c.surface).padding(14.dp)) {
                        Entry(b, home) { go.bottle(b.id) }
                        Spacer(Modifier.height(10.dp))
                        VerdictPicker(null) { v -> container.write { cellar.verdict(b.id, v) } }
                    }
                }
            }
            val rest = home.opened.filter { it !in home.due }
            if (rest.isNotEmpty()) {
                SectionTitle(stringResource(R.string.opened_bottles))
                rest.forEach { b -> Entry(b, home) { go.bottle(b.id) } }
            }
            if (home.given.isNotEmpty()) {
                SectionTitle(stringResource(R.string.given_gifts))
                home.given.forEach { b -> Entry(b, home) { go.bottle(b.id) } }
            }
            if (home.opened.isEmpty() && home.given.isEmpty()) {
                Text(stringResource(R.string.journal_empty), style = SType.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 40.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Entry(b: Bottle, home: Home, onClick: () -> Unit) {
    val c = ctx()
    Row(Modifier.fillMaxWidth().clip(S.RMd).clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BottleArt(home.look(b), Modifier.size(width = 34.dp, height = 50.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.name, style = SType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = listOfNotNull(
                b.price.takeIf { it > 0 }?.let(home::money),
                T.date(c, b.endedAt, home.today),
                if (b.early) stringResource(R.string.early) else null,
                if (b.giftFor.isNotBlank()) b.giftFor else null,
            ).joinToString(" · ")
            Text(line, style = SType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        b.verdictOf?.let { v ->
            VerdictMark(v, Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text(T.verdict(c, v), style = SType.Small.copy(color = S.c.muted))
        } ?: run {
            if (b.kindOf == app.cellar.data.Kind.WISH && !Rules.verdictDue(b, home.now)) {
                val left = Rules.ASK_AFTER - Rules.aged(b.copy(laidAt = b.endedAt, endedAt = 0), home.now)
                if (left > 0) Text(T.days(c, left), style = SType.Small)
            }
        }
    }
}
