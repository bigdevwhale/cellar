package app.cellar.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.cellar.container
import app.cellar.data.Bottle
import app.cellar.data.Dream
import app.cellar.data.Kind
import app.cellar.data.Settings
import app.cellar.data.State
import app.cellar.domain.Ledger
import app.cellar.domain.Rules
import app.cellar.domain.Storm
import app.cellar.ru
import app.cellar.ui.art.BarrelLook
import app.cellar.ui.art.BottleLook
import app.cellar.ui.art.PER_SHELF
import app.cellar.ui.art.SHELVES
import app.cellar.ui.art.Slot
import app.cellar.ui.barrel.BarrelScreen
import app.cellar.ui.bottle.BottleScreen
import app.cellar.ui.cellar.CellarScreen
import app.cellar.ui.components.rememberNow
import app.cellar.ui.journal.JournalScreen
import app.cellar.ui.lay.LayScreen
import app.cellar.ui.ledger.LedgerScreen
import app.cellar.ui.onboarding.GuideScreen
import app.cellar.ui.onboarding.WelcomeScreen
import app.cellar.ui.settings.SettingsScreen
import app.cellar.ui.tasting.TastingScreen
import app.cellar.ui.theme.S
import java.time.LocalDate

/** Everything written down, plus what follows from it, at one moment. Screens read through here. */
@Immutable
class Home(
    val bottles: List<Bottle>,
    val dreams: List<Dream>,
    val settings: Settings,
    val now: Long,
    val ru: Boolean,
) {
    val today: LocalDate = Rules.date(now)
    val currency = settings.currency
    fun money(amount: Long) = currency.format(amount)

    fun bottle(id: Long) = bottles.firstOrNull { it.id == id }

    /** Wishes on the rack, the ripest first. */
    val aging: List<Bottle> = bottles.filter(Rules::aging).sortedBy(Rules::readyAt)
    val ripe: List<Bottle> = aging.filter { Rules.ripe(it, now) }
    val gifts: List<Bottle> = bottles.filter { it.kindOf == Kind.GIFT && it.stateOf == State.AGING }
        .sortedWith(compareBy({ if (it.giftAt == 0L) Long.MAX_VALUE else it.giftAt }, { it.laidAt }))
    val given: List<Bottle> = bottles.filter { it.stateOf == State.GIVEN }.sortedByDescending { it.endedAt }
    val opened: List<Bottle> = bottles.filter { it.kindOf == Kind.WISH && it.stateOf == State.OPENED }.sortedByDescending { it.endedAt }
    val poured: List<Bottle> = bottles.filter { it.stateOf == State.POURED }.sortedByDescending { it.endedAt }
    val due: List<Bottle> = opened.filter { Rules.verdictDue(it, now) }
    val giftsSoon: List<Pair<Bottle, Int>> = gifts.mapNotNull { b -> Rules.giftIn(b, today)?.takeIf { it <= Rules.GIFT_SOON }?.let { b to it } }

    /** The barrel being filled now (an unnamed one if none was ever chalked). */
    val dream: Dream = dreams.lastOrNull { it.sealedAt == 0L } ?: Dream(id = -1, name = "")
    val sealed: List<Dream> = dreams.filter { it.sealedAt > 0 }.sortedByDescending { it.sealedAt }
    val inBarrel: Long = Rules.poured(bottles, dream)
    val full: Boolean = Rules.full(bottles, dream)
    val pouredAll: Long = bottles.filter { it.stateOf == State.POURED }.sumOf { it.price }

    /** How full the barrel looks: against the dream's sum, or against a cask's worth when there's none. */
    val fill: Float = if (dream.target > 0) inBarrel.toFloat() / dream.target else (inBarrel.toFloat() / (currency.cask * 2)).coerceAtMost(0.9f)

    val storm: Storm? = if (settings.storms) Rules.storm(today) else null

    fun look(b: Bottle) = BottleLook(
        size = Rules.size(b.price, currency), shape = b.shape,
        dust = if (b.stateOf == State.AGING) Rules.dust(b, now) else 0f, cobweb = Rules.cobweb(b, now),
        ripe = Rules.ripe(b, now), gift = b.kindOf == Kind.GIFT, state = b.stateOf,
        level = if (b.stateOf == State.OPENED) 0.55f else 1f,
    )

    /** The rack, a page at a time: the gift shelf on top if there are gifts, the wishes below, oldest first. */
    fun shelves(page: Int): List<List<Slot>> {
        val giftRow = gifts.take(PER_SHELF).map { Slot(it.id, look(it)) }
        val rows = if (giftRow.isEmpty()) SHELVES else SHELVES - 1
        val wishes = bottles.filter(Rules::aging).sortedBy { it.laidAt }.drop(page * rows * PER_SHELF).take(rows * PER_SHELF).map { Slot(it.id, look(it)) }
        val out = wishes.chunked(PER_SHELF)
        return if (giftRow.isEmpty()) out else listOf(giftRow) + out
    }

    val pages: Int = maxOf(1, (aging.size + perPage() - 1) / perPage())
    private fun perPage() = (if (gifts.isEmpty()) SHELVES else SHELVES - 1) * PER_SHELF

    fun barrelLook(name: String, d: Dream = dream): BarrelLook {
        val sum = Rules.poured(bottles, d)
        // Chalk has little room: only what's in it; the gauge shows how far to the dream.
        val label = money(sum)
        val fill = if (d.sealedAt > 0) 1f else if (d.id == dream.id) fill else 0f
        return BarrelLook(d.name.ifBlank { name }, label, fill, sealed = d.sealedAt > 0)
    }

    fun ledger(from: Long = 0) = Ledger(bottles, from)
    val seals = Rules.seals(bottles, dreams, currency)
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun lay(gift: Boolean = false) = nav.navigate("lay/0/$gift") { launchSingleTop = true }
    fun edit(id: Long) = nav.navigate("lay/$id/false") { launchSingleTop = true }
    fun bottle(id: Long) = nav.navigate("bottle/$id") { launchSingleTop = true }
    fun tasting() = nav.navigate("tasting") { launchSingleTop = true }
    fun barrel() = nav.navigate("barrel") { launchSingleTop = true }
    fun journal() = nav.navigate("journal") { launchSingleTop = true }
    fun ledger() = nav.navigate("ledger") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun guide() = nav.navigate("guide") { launchSingleTop = true }
    fun back() { nav.popBackStack() }

    /** Just laid or decided: back down to the cellar. */
    fun cellar() { nav.popBackStack("cellar", false) }
}

@Composable
fun CellarRoot(settings: Settings, tasting: Boolean = false) {
    val context = ctx()
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "cellar" else "welcome" }

    val dao = container.db.dao()
    val bottles by dao.observeBottles().collectAsStateWithLifecycle(initialValue = emptyList())
    val dreams by dao.observeDreams().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow(60_000)
    val ru = context.ru
    val home = remember(bottles, dreams, settings, now, ru) { Home(bottles, dreams, settings, now, ru) }
    val go = remember(nav) { Go(nav) }

    // Onboarded elsewhere (the demo seed): leave the welcome screen.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("cellar") { popUpTo("welcome") { inclusive = true }; launchSingleTop = true }
    }
    // From the "ripe" notification: straight to the tasting.
    LaunchedEffect(tasting, settings.onboarded) { if (tasting && settings.onboarded) go.tasting() }
    // A wish shared in from a shop: the laying form, filled in.
    val incoming by container.incoming.collectAsStateWithLifecycle()
    LaunchedEffect(incoming, settings.onboarded, route) {
        if (incoming != null && settings.onboarded && route != null && !route.startsWith("lay")) go.lay()
    }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(S.c.bg),
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("welcome") { WelcomeScreen { nav.navigate("cellar") { popUpTo("welcome") { inclusive = true } } } }
            composable("guide") { GuideScreen { nav.popBackStack() } }
            composable("cellar") { CellarScreen(home, go) }
            composable("lay/{id}/{gift}", arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("gift") { type = NavType.BoolType })) { e ->
                LayScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getBoolean("gift") ?: false, home, go)
            }
            composable("bottle/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e -> BottleScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("tasting") { TastingScreen(home, go) }
            composable("barrel") { BarrelScreen(home, go) }
            composable("journal") { JournalScreen(home, go) }
            composable("ledger") { LedgerScreen(home, go) }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
