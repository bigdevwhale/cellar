package app.cellar.ui.barrel

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cellar.R
import app.cellar.container
import app.cellar.data.State
import app.cellar.domain.Rules
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.BarrelArt
import app.cellar.ui.art.BottleArt
import app.cellar.ui.components.Field
import app.cellar.ui.components.PrimaryButton
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.SoftButton
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType

/** The dream's barrel: what it's for, how full, what went into it, and the barrels already sealed. */
@Composable
fun BarrelScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val d = home.dream
    var editing by remember { mutableStateOf(false) }
    var sealing by remember { mutableStateOf(false) }
    val unnamed = stringResource(R.string.barrel_unnamed)
    val into = home.poured.filter { it.dreamId == d.id && it.stateOf == State.POURED }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.barrel), go::back) {
            IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, stringResource(R.string.dream_edit), tint = S.c.ink) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BarrelArt(home.barrelLook(unnamed), Modifier.width(220.dp).height(250.dp))
            Spacer(Modifier.height(12.dp))
            Text(d.name.ifBlank { unnamed }, style = SType.Display, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    d.target <= 0 -> stringResource(R.string.barrel_no_target, home.money(home.inBarrel))
                    home.full -> stringResource(R.string.barrel_full, home.money(home.inBarrel))
                    else -> stringResource(R.string.barrel_left, home.money(home.inBarrel), home.money(d.target - home.inBarrel))
                },
                style = SType.Sub, textAlign = TextAlign.Center,
            )
            if (d.name.isBlank()) {
                Spacer(Modifier.height(14.dp))
                SoftButton(stringResource(R.string.dream_chalk), { editing = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
            }
            if (home.full) {
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.seal_barrel), { sealing = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Verified)
            }
            Text(stringResource(R.string.barrel_about), style = SType.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))

            if (into.isNotEmpty()) {
                SectionTitle(stringResource(R.string.poured_in), Modifier.fillMaxWidth())
                into.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        BottleArt(home.look(b), Modifier.size(width = 30.dp, height = 44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, style = SType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.poured_line, T.date(c, b.endedAt, home.today), T.days(c, Rules.aged(b, home.now))), style = SType.Small)
                        }
                        Text(home.money(b.price), style = SType.Figures.copy(color = S.c.pour))
                    }
                }
            }
            if (home.sealed.isNotEmpty()) {
                SectionTitle(stringResource(R.string.sealed_barrels), Modifier.fillMaxWidth())
                home.sealed.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BarrelArt(home.barrelLook("", s).copy(name = "", sum = ""), Modifier.size(width = 44.dp, height = 56.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name.ifBlank { unnamed }, style = SType.Strong)
                            Text(stringResource(R.string.sealed_on, T.date(c, s.sealedAt, home.today)), style = SType.Small)
                        }
                        Text(home.money(Rules.poured(home.bottles, s)), style = SType.Figures.copy(color = S.c.pour))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (editing) {
        var name by remember { mutableStateOf(d.name) }
        var target by remember { mutableStateOf(d.target.takeIf { it > 0 }?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.dream_edit)) },
            text = {
                Column {
                    Text(stringResource(R.string.dream_sub), style = SType.Muted)
                    Spacer(Modifier.height(12.dp))
                    Field(name, { name = it }, stringResource(R.string.dream_name))
                    Spacer(Modifier.height(10.dp))
                    Field(
                        target, { target = it.filter(Char::isDigit).take(12) }, stringResource(R.string.dream_target),
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = home.currency.symbol,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editing = false
                    container.write { cellar.dream(name, target.toLongOrNull() ?: 0) }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (sealing) AlertDialog(
        onDismissRequest = { sealing = false },
        title = { Text(stringResource(R.string.seal_barrel)) },
        text = { Text(stringResource(R.string.seal_barrel_text, d.name.ifBlank { unnamed })) },
        confirmButton = { TextButton(onClick = { sealing = false; container.write { cellar.seal() } }) { Text(stringResource(R.string.seal_it)) } },
        dismissButton = { TextButton(onClick = { sealing = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
