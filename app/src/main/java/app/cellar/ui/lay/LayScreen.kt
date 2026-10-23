package app.cellar.ui.lay

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.cellar.R
import app.cellar.container
import app.cellar.data.Bottle
import app.cellar.data.Kind
import app.cellar.data.Reason
import app.cellar.domain.Rules
import app.cellar.domain.Share
import app.cellar.notify.Ripe
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.T
import app.cellar.ui.art.BottleArt
import app.cellar.ui.art.BottleLook
import app.cellar.ui.components.Chip
import app.cellar.ui.components.Field
import app.cellar.ui.components.Photo
import app.cellar.ui.components.PrimaryButton
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random

/** Laying a bottle down (or a gift on its shelf), or correcting one already laid ([id] > 0). */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LayScreen(id: Long, giftArg: Boolean, home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val old = remember(id) { home.bottle(id) }
    val gift = old?.kindOf == Kind.GIFT || (old == null && giftArg)

    var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var price by rememberSaveable { mutableStateOf(old?.price?.takeIf { it > 0 }?.toString() ?: "") }
    var url by rememberSaveable { mutableStateOf(old?.url ?: "") }
    var note by rememberSaveable { mutableStateOf(old?.note ?: "") }
    var reason by rememberSaveable { mutableStateOf(old?.reasonOf?.name) }
    var photo by rememberSaveable { mutableStateOf(old?.photo) }
    var giftFor by rememberSaveable { mutableStateOf(old?.giftFor ?: "") }
    var giftAt by rememberSaveable { mutableLongStateOf(old?.giftAt ?: 0L) }
    var days by rememberSaveable { mutableIntStateOf(0) }
    val shape = rememberSaveable { old?.shape ?: Random.nextLong() }
    var picking by remember { mutableStateOf(false) }

    // Shared in from a shop: fill the form once.
    LaunchedEffect(Unit) {
        if (old != null) return@LaunchedEffect
        container.incoming.value?.let { inc ->
            container.incoming.value = null
            if (inc.shared.name.isNotBlank()) name = inc.shared.name
            url = inc.shared.url
            inc.shared.price?.takeIf { inc.shared.currency == null || inc.shared.currency == home.currency }?.let { price = it.toString() }
            if (inc.photo != null) photo = inc.photo
        }
    }

    val amount = price.filter(Char::isDigit).take(12).toLongOrNull() ?: 0L
    val size = home.scale.size(amount)
    val chosen = if (days > 0) days else home.scale.days(size)
    val stormy = home.storm != null && old == null
    val laidFor = Rules.layDays(chosen, stormy)

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) container.write { cellar.keep(uri)?.let { photo = it } }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val ready = name.isNotBlank() && (gift || reason != null)
    fun save() {
        val b = (old ?: Bottle(name = "", shape = shape)).copy(
            kind = if (gift) Kind.GIFT.name else Kind.WISH.name,
            name = name.trim(), price = amount, url = url.trim(), note = note.trim(),
            reason = reason ?: Reason.NEED.name, photo = photo, giftFor = giftFor.trim(), giftAt = giftAt,
        ).let { if (old == null) it.copy(days = laidFor, storm = stormy, laidAt = System.currentTimeMillis()) else it }
        container.write { if (old == null) cellar.lay(b) else cellar.edit(b) }
        if (old == null && !gift && home.settings.notify && Build.VERSION.SDK_INT >= 33 && !Ripe.allowed(c)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (old == null) go.cellar() else go.back()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(
            stringResource(if (old != null) R.string.edit_bottle else if (gift) R.string.lay_gift_title else R.string.lay_title),
            onBack = go::back,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BottleArt(BottleLook(size, shape, gift = gift), Modifier.size(width = 74.dp, height = 104.dp), halo = true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(T.size(c, size), style = SType.Title)
                    if (!gift && old == null) {
                        Text(stringResource(R.string.ages_for, T.age(c, laidFor)), style = SType.Muted)
                    } else if (old != null && !gift) {
                        Text(stringResource(R.string.laid_on, T.date(c, old.laidAt, home.today)), style = SType.Muted)
                    } else {
                        Text(stringResource(R.string.gift_waits), style = SType.Muted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Field(name, { name = it }, stringResource(if (gift) R.string.gift_what else R.string.what_want))
            Spacer(Modifier.height(10.dp))
            Field(
                price, { price = it.filter(Char::isDigit).take(12) }, stringResource(R.string.price),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = home.currency.symbol,
            )
            Spacer(Modifier.height(10.dp))
            Field(url, { url = Share.link(it, url) }, stringResource(R.string.link), keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None))

            if (gift) {
                Spacer(Modifier.height(10.dp))
                Field(giftFor, { giftFor = it }, stringResource(R.string.gift_for), keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(S.RMd).background(S.c.surface).border(1.dp, S.c.line, S.RMd).clickable { picking = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Event, null, tint = S.c.flame)
                    Spacer(Modifier.width(12.dp))
                    Text(if (giftAt > 0) T.date(c, giftAt, home.today) else stringResource(R.string.gift_date), style = if (giftAt > 0) SType.Body else SType.Body.copy(color = S.c.faint), modifier = Modifier.weight(1f))
                    if (giftAt > 0) IconButton(onClick = { giftAt = 0 }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Close, null, tint = S.c.faint) }
                }
            } else {
                SectionTitle(stringResource(R.string.why_now))
                Text(stringResource(R.string.why_now_sub), style = SType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Reason.entries.forEach { r -> Chip(T.reason(c, r), reason == r.name) { reason = r.name } }
                }
                if (old == null) {
                    SectionTitle(stringResource(R.string.ageing))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (Rules.AGES + chosen).distinct().sorted().forEach { d -> Chip(T.age(c, d), chosen == d) { days = d } }
                    }
                    if (stormy) {
                        Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(S.RMd).background(S.c.pourSoft).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, null, tint = S.c.pour)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.storm_doubles, T.storm(c, home.storm!!), T.age(c, laidFor)), style = SType.Muted.copy(color = S.c.ink))
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.note_title))
            Field(note, { note = it }, stringResource(if (gift) R.string.gift_note else R.string.note_hint), singleLine = false, minLines = 2)
            Spacer(Modifier.height(12.dp))
            if (photo != null) {
                Box {
                    Photo(photo!!, Modifier.fillMaxWidth().height(180.dp))
                    IconButton(onClick = { photo = null }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = S.c.ink) }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().clip(S.RMd).border(1.dp, S.c.line, S.RMd).clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Image, null, tint = S.c.muted)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.add_picture), style = SType.Muted)
                }
            }
            if (old == null && !gift) Text(stringResource(R.string.share_tip), style = SType.Small, modifier = Modifier.padding(top = 14.dp, start = 4.dp))
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(if (old != null) R.string.save else if (gift) R.string.put_on_shelf else R.string.lay_down),
            ::save, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), enabled = ready,
        )
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (giftAt > 0) giftAt else null)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker hands back UTC midnight; keep the same calendar day here.
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        giftAt = d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 12 * 3_600_000L
                    }
                    picking = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
}
