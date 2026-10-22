package app.cellar.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cellar.R
import app.cellar.container
import app.cellar.data.Currency
import app.cellar.data.State
import app.cellar.domain.Size
import app.cellar.ui.Language
import app.cellar.ui.art.BarrelArt
import app.cellar.ui.art.BarrelLook
import app.cellar.ui.art.BottleArt
import app.cellar.ui.art.BottleLook
import app.cellar.ui.art.CellarScene
import app.cellar.ui.art.Slot
import app.cellar.ui.bottle.VerdictMark
import app.cellar.ui.components.Chip
import app.cellar.ui.components.Field
import app.cellar.ui.components.PrimaryButton
import app.cellar.ui.components.TextLink
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import app.cellar.ui.theme.SType
import kotlinx.coroutines.launch

private const val GUIDE = 5

/** First run: the cellar, the five pages of how it works, then the currency and the dream. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(startPage: Int = 0, onDone: () -> Unit) {
    val c = ctx()
    val container = c.container
    val pager = rememberPagerState(startPage) { GUIDE + 2 }
    val scope = rememberCoroutineScope()
    var currency by rememberSaveable { mutableStateOf(if (c.resources.configuration.locales[0].language == "ru" || Language.current() == Language.RU) Currency.RUB.name else Currency.USD.name) }
    var dream by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }

    fun finish() {
        container.write {
            settings.update { it.copy(onboarded = true, currency = Currency.of(currency)) }
            if (dream.isNotBlank() || target.isNotBlank()) cellar.dream(dream, target.toLongOrNull() ?: 0)
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> Hero()
                in 1..GUIDE -> GuidePage(page - 1)
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(stringResource(R.string.setup_title), style = SType.Display)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.setup_currency), style = SType.Sub)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Currency.entries.forEach { cur -> Chip("${cur.symbol} ${cur.name}", currency == cur.name) { currency = cur.name } }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.setup_dream), style = SType.Sub)
                    Spacer(Modifier.height(12.dp))
                    Field(dream, { dream = it }, stringResource(R.string.dream_name))
                    Spacer(Modifier.height(10.dp))
                    Field(
                        target, { target = it.filter(Char::isDigit).take(12) }, stringResource(R.string.dream_target),
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = Currency.of(currency).symbol,
                    )
                    Text(stringResource(R.string.setup_dream_later), style = SType.Small, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                }
            }
        }
        Dots(pager)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage in 1..GUIDE) TextLink(stringResource(R.string.skip), { scope.launch { pager.animateScrollToPage(GUIDE + 1) } })
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == GUIDE + 1
            PrimaryButton(
                stringResource(if (last) R.string.go_down else if (pager.currentPage == 0) R.string.how_it_works else R.string.next),
                { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.width(if (last || pager.currentPage == 0) 260.dp else 160.dp),
            )
        }
    }
}

/** The guide on its own, from the "?" in the cellar. */
@Composable
fun GuideScreen(startPage: Int = 0, onDone: () -> Unit) {
    val pager = rememberPagerState(startPage) { GUIDE }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { GuidePage(it) }
        Dots(pager)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextLink(stringResource(R.string.close), onDone)
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == GUIDE - 1
            PrimaryButton(
                stringResource(if (last) R.string.got_it else R.string.next),
                { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.width(160.dp),
            )
        }
    }
}

@Composable
private fun Hero() {
    val sample = listOf(
        listOf(Slot(1, BottleLook(Size.BOTTLE, 11, gift = true)), Slot(2, BottleLook(Size.VIAL, 12, gift = true))),
        listOf(Slot(3, BottleLook(Size.BOTTLE, 21, dust = 0.8f, cobweb = true, ripe = true)), Slot(4, BottleLook(Size.MAGNUM, 22, dust = 0.4f)), Slot(5, BottleLook(Size.VIAL, 23, dust = 0.1f)), Slot(6, BottleLook(Size.BOTTLE, 24, dust = 0.55f, cobweb = true))),
        listOf(Slot(7, BottleLook(Size.CASK, 31, dust = 0.3f)), Slot(8, BottleLook(Size.BOTTLE, 32)), Slot(9, BottleLook(Size.MAGNUM, 33, dust = 0.7f, cobweb = true))),
        listOf(Slot(10, BottleLook(Size.VIAL, 41, dust = 0.9f)), Slot(11, BottleLook(Size.BOTTLE, 42, dust = 0.2f))),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CellarScene(sample, BarrelLook(stringResource(R.string.sample_dream), "", 0.45f), storm = false, modifier = Modifier.fillMaxWidth().aspectRatio(0.92f))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.app_name), style = SType.Hero)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.tagline), style = SType.Title.copy(color = S.c.flame))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.welcome_text), style = SType.Sub)
        }
    }
}

/** One page of how it works, with its picture. */
@Composable
fun GuidePage(i: Int) {
    val titles = listOf(R.string.g1_title, R.string.g2_title, R.string.g3_title, R.string.g4_title, R.string.g5_title)
    val texts = listOf(R.string.g1_text, R.string.g2_text, R.string.g3_text, R.string.g4_text, R.string.g5_text)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(260.dp).clip(S.RLg).background(S.c.surface), contentAlignment = Alignment.Center) {
            when (i) {
                0 -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    BottleArt(BottleLook(Size.VIAL, 5), Modifier.size(70.dp, 160.dp))
                    BottleArt(BottleLook(Size.BOTTLE, 6), Modifier.size(70.dp, 160.dp))
                    BottleArt(BottleLook(Size.MAGNUM, 7), Modifier.size(70.dp, 160.dp))
                    BottleArt(BottleLook(Size.CASK, 8), Modifier.size(90.dp, 160.dp))
                }
                1 -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                    BottleArt(BottleLook(Size.BOTTLE, 9, dust = 0f), Modifier.size(80.dp, 190.dp))
                    BottleArt(BottleLook(Size.BOTTLE, 9, dust = 0.45f), Modifier.size(80.dp, 190.dp))
                    BottleArt(BottleLook(Size.BOTTLE, 9, dust = 0.9f, cobweb = true), Modifier.size(80.dp, 190.dp))
                }
                2 -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                    BottleArt(BottleLook(Size.BOTTLE, 14, state = State.OPENED, level = 0.6f), Modifier.size(80.dp, 190.dp))
                    BottleArt(BottleLook(Size.BOTTLE, 15, dust = 0.8f, ripe = true, cobweb = true), Modifier.size(90.dp, 210.dp), halo = true)
                    BottleArt(BottleLook(Size.BOTTLE, 16, state = State.POURED), Modifier.size(80.dp, 190.dp))
                }
                3 -> BarrelArt(BarrelLook(stringResource(R.string.sample_dream), "", 0.6f), Modifier.size(170.dp, 200.dp))
                else -> Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    app.cellar.data.Verdict.entries.forEach { VerdictMark(it, Modifier.size(64.dp)) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(titles[i]), style = SType.Display, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(texts[i]), style = SType.Sub, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Dots(pager: PagerState) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        repeat(pager.pageCount) { i ->
            Box(Modifier.padding(horizontal = 4.dp).size(if (i == pager.currentPage) 9.dp else 7.dp).clip(CircleShape).background(if (i == pager.currentPage) S.c.flame else S.c.line))
        }
    }
}
