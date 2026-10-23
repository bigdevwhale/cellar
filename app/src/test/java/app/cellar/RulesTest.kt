package app.cellar

import app.cellar.data.Bottle
import app.cellar.data.Currency
import app.cellar.data.Dream
import app.cellar.data.Kind
import app.cellar.data.Reason
import app.cellar.data.State
import app.cellar.data.Verdict
import app.cellar.domain.Insight
import app.cellar.domain.Ledger
import app.cellar.domain.Rules
import app.cellar.domain.Rules.DAY
import app.cellar.domain.Seal
import app.cellar.domain.Share
import app.cellar.domain.Size
import app.cellar.domain.Storm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    private val t0 = 1_760_000_000_000L

    private fun wish(laidDaysAgo: Int, days: Int, now: Long = t0, extra: Int = 0) =
        Bottle(name = "x", price = 1_000, laidAt = now - laidDaysAgo * DAY, days = days, extraDays = extra)

    @Test fun sizesByPrice() {
        assertEquals(Size.VIAL, Rules.size(999, Currency.RUB))
        assertEquals(Size.BOTTLE, Rules.size(1_000, Currency.RUB))
        assertEquals(Size.MAGNUM, Rules.size(10_000, Currency.RUB))
        assertEquals(Size.CASK, Rules.size(50_000, Currency.RUB))
        assertEquals(Size.BOTTLE, Rules.size(15, Currency.USD))
        assertEquals(3, Rules.suggest(Size.VIAL))
        assertEquals(90, Rules.suggest(Size.CASK))
    }

    @Test fun customScale() {
        val s = app.cellar.data.Settings(currency = Currency.RUB, prices = mapOf(Size.BOTTLE to 2_000L, Size.CASK to 100_000L), days = mapOf(Size.VIAL to 1))
        val scale = s.scale
        assertEquals(Size.VIAL, scale.size(1_999))
        assertEquals(Size.BOTTLE, scale.size(2_000))
        assertEquals(Size.MAGNUM, scale.size(99_999))
        assertEquals(Size.CASK, scale.size(100_000))
        assertEquals(1, scale.days(Size.VIAL))
        assertEquals(14, scale.days(Size.BOTTLE))
        assertEquals(2_001L..99_999L, app.cellar.domain.Scale.bounds(scale, Size.MAGNUM))
        assertFalse(app.cellar.data.Settings().customScale)
    }

    @Test fun ripensOnTimeAndExtends() {
        assertFalse(Rules.ripe(wish(13, 14), t0))
        assertTrue(Rules.ripe(wish(14, 14), t0))
        assertFalse(Rules.ripe(wish(14, 14, extra = 7), t0))
        assertEquals(7, Rules.daysLeft(wish(14, 14, extra = 7), t0))
        assertEquals(1, Rules.daysLeft(wish(13, 14), t0 + 1))
        assertEquals(0.5f, Rules.progress(wish(7, 14), t0), 0.001f)
    }

    @Test fun giftsAndDecidedBottlesNeverRipen() {
        assertFalse(Rules.ripe(wish(30, 3).copy(kind = Kind.GIFT.name), t0))
        assertFalse(Rules.ripe(wish(30, 3).copy(state = State.OPENED.name), t0))
    }

    @Test fun dustAndCobwebs() {
        assertEquals(0f, Rules.dust(wish(0, 14), t0), 0.001f)
        assertTrue(Rules.dust(wish(14, 14), t0) >= 0.75f)
        assertFalse(Rules.cobweb(wish(2, 3), t0))
        assertTrue(Rules.cobweb(wish(7, 14), t0))
    }

    @Test fun storms() {
        assertEquals(Storm.SINGLES, Rules.storm(LocalDate.of(2026, 11, 11)))
        // Thanksgiving 2026 is 26 November: Black Friday on the 27th to Cyber Monday on the 30th.
        assertNull(Rules.storm(LocalDate.of(2026, 11, 26)))
        assertEquals(Storm.BLACK_FRIDAY, Rules.storm(LocalDate.of(2026, 11, 27)))
        assertEquals(Storm.BLACK_FRIDAY, Rules.storm(LocalDate.of(2026, 11, 30)))
        assertNull(Rules.storm(LocalDate.of(2026, 12, 1)))
        assertEquals(Storm.TWELVE, Rules.storm(LocalDate.of(2026, 12, 12)))
        assertEquals(Storm.MIDYEAR, Rules.storm(LocalDate.of(2027, 6, 18)))
        assertEquals(Storm.SINGLES to LocalDate.of(2026, 11, 11), Rules.nextStorm(LocalDate.of(2026, 10, 22)))
        assertEquals(28, Rules.layDays(14, stormy = true))
        assertEquals(14, Rules.layDays(14, stormy = false))
    }

    @Test fun verdictIsAskedAfterAMonth() {
        val opened = wish(40, 3).copy(state = State.OPENED.name, endedAt = t0 - 29 * DAY)
        assertFalse(Rules.verdictDue(opened, t0))
        assertTrue(Rules.verdictDue(opened, t0 + DAY))
        assertFalse(Rules.verdictDue(opened.copy(verdict = Verdict.VINTAGE.name), t0 + DAY))
    }

    @Test fun giftDays() {
        val today = LocalDate.of(2026, 10, 22)
        val g = Bottle(name = "pen", kind = Kind.GIFT.name, giftAt = LocalDate.of(2026, 10, 27).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 12 * 3_600_000L)
        assertEquals(5, Rules.giftIn(g, today))
        assertNull(Rules.giftIn(g, LocalDate.of(2026, 10, 28)))
        assertNull(Rules.giftIn(g.copy(giftAt = 0), today))
    }

    @Test fun barrelsFill() {
        val d = Dream(id = 1, name = "Japan", target = 10_000)
        val bottles = listOf(
            wish(10, 3).copy(price = 6_000, state = State.POURED.name, dreamId = 1),
            wish(10, 3).copy(price = 3_000, state = State.POURED.name, dreamId = 1),
            wish(10, 3).copy(price = 9_000, state = State.POURED.name, dreamId = 2),
            wish(10, 3).copy(price = 9_000, state = State.OPENED.name),
        )
        assertEquals(9_000, Rules.poured(bottles, d))
        assertFalse(Rules.full(bottles, d))
        assertTrue(Rules.full(bottles + wish(1, 3).copy(price = 1_000, state = State.POURED.name, dreamId = 1), d))
        assertFalse(Rules.full(bottles, d.copy(target = 0)))
    }

    @Test fun seals() {
        val poured = (1..10).map { i -> wish(50, 3).copy(price = 20_000, state = State.POURED.name, endedAt = t0 - (20 - i) * DAY, storm = i == 3) }
        val seals = Rules.seals(poured, emptyList(), Currency.RUB)
        assertNotNull(seals[Seal.FIRST_LAID])
        assertEquals(t0 - 19 * DAY, seals[Seal.FIRST_POURED])
        assertEquals(t0 - 10 * DAY, seals[Seal.TEN_POURED])
        assertEquals(t0 - 17 * DAY, seals[Seal.STORM])
        // A hundred bottles' worth: 100 000 ₽, reached on the fifth.
        assertEquals(t0 - 15 * DAY, seals[Seal.HUNDRED])
        assertNull(seals[Seal.FIRST_RIPE])
        assertNull(seals[Seal.CLEAN_FIVE])
        val tasted = (1..5).map { i -> wish(60, 3).copy(state = State.OPENED.name, endedAt = t0 - 40 * DAY, verdict = Verdict.SOSO.name, verdictAt = t0 - i * DAY) }
        assertEquals(t0 - 1 * DAY, Rules.seals(tasted, emptyList(), Currency.RUB)[Seal.CLEAN_FIVE])
    }

    @Test fun ledgerNotes() {
        fun b(r: Reason, s: State, v: Verdict? = null, early: Boolean = false) =
            wish(20, 3).copy(reason = r.name, state = s.name, endedAt = t0 - DAY, verdict = v?.name, early = early)
        val all = listOf(
            b(Reason.BORED, State.POURED), b(Reason.BORED, State.POURED), b(Reason.BORED, State.POURED), b(Reason.AD, State.POURED),
            b(Reason.SALE, State.OPENED, Verdict.VINEGAR, early = true), b(Reason.SALE, State.OPENED, Verdict.VINEGAR, early = true), b(Reason.SALE, State.OPENED, Verdict.VINTAGE),
            b(Reason.NEED, State.OPENED, Verdict.VINTAGE), b(Reason.NEED, State.OPENED), b(Reason.NEED, State.OPENED),
            b(Reason.NEED, State.AGING),
        )
        val l = Ledger(all)
        assertEquals(11, l.laid)
        assertEquals(4, l.poured.size)
        assertTrue(Insight.Faded(Reason.BORED, 3, 4) in l.insights)
        assertTrue(Insight.Sour(Reason.SALE, 2, 3) in l.insights)
        assertTrue(Insight.Needed(3, 3) in l.insights)
        assertTrue(Insight.Joy(2, 4) in l.insights)
        assertTrue(Insight.Early(2, 2) in l.insights)
        assertTrue(Ledger(all, from = t0).insights.isEmpty())
    }

    @Test fun money() {
        assertEquals("12 990 ₽", Currency.RUB.format(12_990))
        assertEquals("$129", Currency.USD.format(129))
        assertEquals("€1 200", Currency.EUR.format(1_200))
    }

    @Test fun sharedFromShops() {
        val ozon = Share.parse("Беспроводные наушники Sony WH-1000XM5 https://ozon.ru/t/AbC123")!!
        assertEquals("Беспроводные наушники Sony WH-1000XM5", ozon.name)
        assertEquals("https://ozon.ru/t/AbC123", ozon.url)
        assertNull(ozon.price)

        val wb = Share.parse("https://www.wildberries.ru/catalog/12345/detail.aspx", "Льняная рубашка")!!
        assertEquals("Льняная рубашка", wb.name)

        val bare = Share.parse("https://www.wildberries.ru/catalog/12345/detail.aspx")!!
        assertEquals("wildberries.ru", bare.name)

        val priced = Share.parse("Посмотрите: Кофемолка ручная за 2 490 ₽ https://market.yandex.ru/cc/xyz")!!
        assertEquals(2_490L, priced.price)
        assertEquals(Currency.RUB, priced.currency)
        assertEquals("Кофемолка ручная", priced.name)

        val amazon = Share.parse("Check out Mechanical Keyboard for \$129.99 on Amazon https://a.co/d/xyz")!!
        assertEquals(129L, amazon.price)
        assertEquals(Currency.USD, amazon.currency)
        assertEquals("Mechanical Keyboard", amazon.name)

        assertNull(Share.parse("   "))
    }

    @Test fun linkFieldTakesOnlyLinks() {
        assertEquals("https://ozon.ru/t/AbC123", Share.link("Беспроводные наушники Sony https://ozon.ru/t/AbC123", ""))
        assertEquals("https://ozon.ru/t/AbC123", Share.link("Наушники\nhttps://ozon.ru/t/AbC123", ""))
        assertEquals("", Share.link("Беспроводные наушники Sony", ""))
        assertEquals("ozon.ru/t", Share.link("ozon.ru/t", "ozon.ru/"))
        // Pasted over an old link: the new one wins.
        assertEquals("https://ozon.ru/t/New", Share.link("https://wb.ru/oldНаушники https://ozon.ru/t/New", "https://wb.ru/old"))
    }
}
