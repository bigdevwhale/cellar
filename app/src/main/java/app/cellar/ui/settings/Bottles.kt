package app.cellar.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.cellar.R
import app.cellar.container
import app.cellar.domain.Scale
import app.cellar.domain.Size
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.BottleArt
import app.cellar.ui.art.BottleLook
import app.cellar.ui.components.Chip
import app.cellar.ui.components.Field
import app.cellar.ui.components.TextLink
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType

/** The four bottles: where each starts by price and how long it ages by default. Tap one to change it. */
@Composable
fun BottlesSection(home: Home) {
    val c = ctx()
    val container = c.container
    val scale = home.scale
    var editing by remember { mutableStateOf<Size?>(null) }

    Text(stringResource(R.string.bottles_sub), style = SType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    Size.entries.forEach { size ->
        Row(
            Modifier.fillMaxWidth().clip(S.RMd).clickable { editing = size }.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottleArt(BottleLook(size, 40L + size.ordinal), Modifier.size(width = 40.dp, height = 56.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(T.size(c, size), style = SType.Strong)
                Text("${range(c, scale, size, home)} · ${T.age(c, scale.days(size))}", style = SType.Muted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = S.c.faint)
        }
    }
    if (home.settings.customScale) {
        TextLink(stringResource(R.string.bottles_reset), { container.write { settings.update { it.copy(prices = emptyMap(), days = emptyMap()) } } })
    }

    editing?.let { size -> EditBottle(size, home) { editing = null } }
}

/** "under 1 000 ₽", "from 10 000 ₽". */
private fun range(c: android.content.Context, scale: Scale, size: Size, home: Home): String =
    if (size == Size.VIAL) c.getString(R.string.price_under, home.money(scale.bottle)) else c.getString(R.string.price_from, home.money(scale.from(size)))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditBottle(size: Size, home: Home, onDone: () -> Unit) {
    val c = ctx()
    val container = c.container
    val scale = home.scale
    var price by remember { mutableStateOf(if (size == Size.VIAL) "" else scale.from(size).toString()) }
    var days by remember { mutableIntStateOf(scale.days(size)) }
    val bounds = Scale.bounds(scale, size)
    val amount = price.toLongOrNull() ?: 0L
    val ok = size == Size.VIAL || amount in bounds

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(T.size(c, size)) },
        text = {
            Column {
                if (size == Size.VIAL) {
                    Text(stringResource(R.string.vial_price, home.money(scale.bottle)), style = SType.Muted)
                } else {
                    Field(
                        price, { price = it.filter(Char::isDigit).take(12) }, stringResource(R.string.starts_from),
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = home.currency.symbol,
                    )
                    if (!ok) {
                        val text = when (size) {
                            Size.CASK -> stringResource(R.string.must_be_above, home.money(scale.magnum))
                            else -> stringResource(R.string.must_be_between, home.money(bounds.first - 1), home.money(bounds.last + 1))
                        }
                        Text(text, style = SType.Small.copy(color = S.c.wine), modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.ages_by_default), style = SType.Strong)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Scale.CHOICES.forEach { d -> Chip(T.age(c, d), days == d) { days = d } }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = ok, onClick = {
                container.write {
                    settings.update { s ->
                        val defaults = Scale.of(s.currency)
                        val prices = if (size == Size.VIAL) s.prices else {
                            if (amount == defaults.from(size)) s.prices - size else s.prices + (size to amount)
                        }
                        val ds = if (days == Scale.DEFAULT_DAYS.getValue(size)) s.days - size else s.days + (size to days)
                        s.copy(prices = prices, days = ds)
                    }
                }
                onDone()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) } },
    )
}
