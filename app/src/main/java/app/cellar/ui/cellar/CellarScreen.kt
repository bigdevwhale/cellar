package app.cellar.ui.cellar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cellar.R
import app.cellar.data.Bottle
import app.cellar.domain.Rules
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.BottleArt
import app.cellar.ui.art.CellarScene
import app.cellar.ui.components.PrimaryButton
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.SoftButton
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType
import app.cellar.ui.theme.Serif

@Composable
fun CellarScreen(home: Home, go: Go) {
    val c = ctx()
    var page by rememberSaveable { mutableIntStateOf(0) }
    if (page >= home.pages) page = home.pages - 1
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = SType.Hero.copy(fontSize = 27.sp), maxLines = 1, modifier = Modifier.weight(1f))
            Badged(home.due.size) { IconButton(onClick = go::journal) { Icon(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.journal), tint = S.c.ink) } }
            IconButton(onClick = go::ledger) { Icon(Icons.Rounded.ReceiptLong, stringResource(R.string.ledger), tint = S.c.ink) }
            IconButton(onClick = go::guide) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.how_it_works), tint = S.c.ink) }
            IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = S.c.ink) }
        }
        CellarScene(
            shelves = home.shelves(page),
            barrel = home.barrelLook(stringResource(R.string.barrel_unnamed)),
            storm = home.storm != null,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.92f),
            onBottle = go::bottle,
            onBarrel = go::barrel,
        )
        if (home.pages > 1) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { page-- }, enabled = page > 0) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = if (page > 0) S.c.ink else S.c.line) }
                Text(stringResource(R.string.rack_page, page + 1, home.pages), style = SType.Muted)
                IconButton(onClick = { page++ }, enabled = page < home.pages - 1) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (page < home.pages - 1) S.c.ink else S.c.line) }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            home.storm?.let { s ->
                Hint(Icons.Rounded.Bolt, stringResource(R.string.storm_now, T.storm(c, s)), S.c.pour, sub = stringResource(R.string.storm_now_sub))
            }
            if (home.ripe.isNotEmpty()) {
                val text = if (home.ripe.size == 1) stringResource(R.string.ripe_one_hint, home.ripe[0].name) else T.plural(c, R.plurals.ripe_hint, home.ripe.size)
                Hint(Icons.Rounded.WineBar, text, S.c.flame, sub = stringResource(R.string.ripe_hint_sub), onClick = go::tasting)
            }
            if (home.full) Hint(Icons.Rounded.Inventory2, stringResource(R.string.barrel_full_hint), S.c.pour, onClick = go::barrel)
            home.due.forEach { b ->
                Hint(Icons.Rounded.LocalBar, stringResource(R.string.due_hint, b.name), S.c.wine, sub = stringResource(R.string.due_hint_sub)) { go.bottle(b.id) }
            }
            home.giftsSoon.forEach { (b, days) ->
                val whenText = if (days == 0) stringResource(R.string.today) else T.plural(c, R.plurals.in_days, days)
                Hint(Icons.Rounded.CardGiftcard, stringResource(R.string.gift_soon, whenText, b.giftFor.ifBlank { b.name }), S.c.wine, sub = b.name.takeIf { b.giftFor.isNotBlank() }) { go.bottle(b.id) }
            }
            if (home.bottles.isEmpty()) FirstSteps(go)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.lay_bottle), { go.lay() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.lay_gift), { go.lay(gift = true) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CardGiftcard)
            if (home.aging.isNotEmpty()) {
                SectionTitle(stringResource(R.string.on_the_rack))
                home.aging.forEach { b -> RackRow(b, home) { go.bottle(b.id) } }
            }
            if (home.gifts.isNotEmpty()) {
                SectionTitle(stringResource(R.string.gift_shelf))
                home.gifts.forEach { b -> RackRow(b, home) { go.bottle(b.id) } }
            }
            if (home.pouredAll > 0) {
                Text(
                    stringResource(R.string.poured_all, home.money(home.pouredAll)), style = SType.Muted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** A bottle in the list under the rack: its picture, name, price and how long it has left. */
@Composable
fun RackRow(b: Bottle, home: Home, onClick: () -> Unit) {
    val c = ctx()
    Row(
        Modifier.fillMaxWidth().clip(S.RMd).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottleArt(home.look(b), Modifier.size(width = 40.dp, height = 56.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.name, style = SType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = buildList {
                if (b.price > 0) add(home.money(b.price))
                if (b.giftFor.isNotBlank()) add(b.giftFor)
                if (b.giftAt > 0) add(T.date(c, b.giftAt, home.today))
            }.joinToString(" · ")
            if (line.isNotEmpty()) Text(line, style = SType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (Rules.aging(b)) {
            val ripe = Rules.ripe(b, home.now)
            Text(
                if (ripe) stringResource(R.string.ripe) else T.days(c, Rules.daysLeft(b, home.now)),
                style = SType.Small.copy(color = if (ripe) S.c.flame else S.c.muted),
                modifier = Modifier.clip(S.RSm).background(if (ripe) S.c.flameSoft else S.c.surface2).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
    }
}

@Composable
private fun Hint(icon: ImageVector, text: String, tint: Color, sub: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(S.RMd).background(S.c.surface).border(1.dp, S.c.line, S.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Column(Modifier.weight(1f)) {
            Text(text, style = SType.Body)
            if (sub != null) Text(sub, style = SType.Muted)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
    }
}

@Composable
private fun Badged(count: Int, content: @Composable () -> Unit) {
    Box {
        content()
        if (count > 0) Box(
            Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(17.dp).clip(CircleShape).background(S.c.wine),
            contentAlignment = Alignment.Center,
        ) { Text("$count", style = SType.Small.copy(color = S.c.ink, fontSize = 10.sp)) }
    }
}

/** For an empty cellar: the three steps, and where the guide is. */
@Composable
private fun FirstSteps(go: Go) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp).clip(S.RMd).background(S.c.surface).border(1.dp, S.c.line, S.RMd).padding(16.dp)) {
        Text(stringResource(R.string.first_steps), style = SType.Title.copy(fontFamily = Serif))
        Spacer(Modifier.height(10.dp))
        listOf(R.string.first_step_1, R.string.first_step_2, R.string.first_step_3).forEachIndexed { i, t ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("${i + 1}.", style = SType.Strong.copy(color = S.c.flame), modifier = Modifier.width(24.dp))
                Text(stringResource(t), style = SType.Body, modifier = Modifier.weight(1f))
            }
        }
        Text(stringResource(R.string.how_it_works), style = SType.Button.copy(color = S.c.flame, fontSize = 15.sp), modifier = Modifier.padding(top = 8.dp).clip(S.RSm).clickable(onClick = go::guide).padding(vertical = 6.dp))
    }
}
