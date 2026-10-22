package app.cellar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.cellar.data.DemoSeed
import app.cellar.domain.Rules
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.art.Art
import app.cellar.ui.barrel.BarrelScreen
import app.cellar.ui.bottle.BottleScreen
import app.cellar.ui.cellar.CellarScreen
import app.cellar.ui.journal.JournalScreen
import app.cellar.ui.lay.LayScreen
import app.cellar.ui.ledger.LedgerScreen
import app.cellar.ui.onboarding.GuideScreen
import app.cellar.ui.onboarding.WelcomeScreen
import app.cellar.ui.tasting.TastingScreen
import app.cellar.ui.theme.CellarTheme
import app.cellar.ui.theme.S
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo cellar, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
    }

    private fun home(storm: Boolean = false): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<CellarApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val dao = c.db.dao()
        val now = if (storm) Rules.nextStorm(java.time.LocalDate.now()).second.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() else System.currentTimeMillis()
        Home(dao.bottles(), dao.dreams(), c.settings.now(), now, app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            CellarTheme { Box(Modifier.fillMaxSize().background(S.c.bg)) { content(h, Go(nav)) } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun guideLay() = home().let { shot("0-guide-1", it) { _, _ -> GuideScreen(startPage = 0) {} } }
    @Test fun guideAge() = home().let { shot("0-guide-2", it) { _, _ -> GuideScreen(startPage = 1) {} } }
    @Test fun guideTaste() = home().let { shot("0-guide-3", it) { _, _ -> GuideScreen(startPage = 2) {} } }
    @Test fun guideBarrel() = home().let { shot("0-guide-4", it) { _, _ -> GuideScreen(startPage = 3) {} } }
    @Test fun guideJournal() = home().let { shot("0-guide-5", it) { _, _ -> GuideScreen(startPage = 4) {} } }
    @Test fun cellar() = home().let { shot("1-cellar", it) { h, go -> CellarScreen(h, go) } }
    @Test fun tasting() = home().let { shot("2-tasting", it) { h, go -> TastingScreen(h, go) } }
    @Test fun bottle() = home().let { h -> shot("3-bottle", h) { hh, go -> BottleScreen(hh.aging.first { !Rules.ripe(it, hh.now) && it.days >= 30 }.id, hh, go) } }
    @Test fun lay() = home().let { shot("4-lay", it) { h, go -> LayScreen(0, false, h, go) } }
    @Test fun barrel() = home().let { shot("5-barrel", it) { h, go -> BarrelScreen(h, go) } }
    @Test fun journal() = home().let { shot("6-journal", it) { h, go -> JournalScreen(h, go) } }
    @Test fun ledger() = home().let { shot("7-ledger", it) { h, go -> LedgerScreen(h, go) } }
    @Test fun storm() = home(storm = true).let { shot("8-storm", it) { h, go -> CellarScreen(h, go) } }
    @Test fun gift() = home().let { h -> shot("9-gift", h) { hh, go -> BottleScreen(hh.gifts.first().id, hh, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
