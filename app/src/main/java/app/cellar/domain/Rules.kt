package app.cellar.domain

import app.cellar.data.Bottle
import app.cellar.data.Currency
import app.cellar.data.Dream
import app.cellar.data.Kind
import app.cellar.data.Reason
import app.cellar.data.State
import app.cellar.data.Verdict
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

/** The bottle a wish is laid in, by its price. */
enum class Size { VIAL, BOTTLE, MAGNUM, CASK }

/** The big sale days. Whatever is laid down while one rages ages twice as long. */
enum class Storm { SINGLES, BLACK_FRIDAY, TWELVE, MIDYEAR }

/** The wax seals on the cellar door: things that happened once. */
enum class Seal { FIRST_LAID, FIRST_POURED, FIRST_RIPE, TEN_POURED, STORM, HALF_YEAR, BARREL, CLEAN_FIVE, HUNDRED }

object Rules {
    const val DAY = 86_400_000L

    /** Ageing to choose from when laying a bottle, in days. */
    val AGES = listOf(3, 7, 14, 30, 60, 90)

    /** "Another week" at a tasting. */
    const val EXTEND = 7

    /** Days after opening when the sommelier asks how it turned out. */
    const val ASK_AFTER = 30

    /** Days before a gift's date when the gift shelf says so. */
    const val GIFT_SOON = 14

    fun size(price: Long, currency: Currency): Size = when {
        price >= currency.cask -> Size.CASK
        price >= currency.magnum -> Size.MAGNUM
        price >= currency.bottle -> Size.BOTTLE
        else -> Size.VIAL
    }

    /** The ageing a size suggests: three days for small things, three months for a cask. */
    fun suggest(size: Size): Int = when (size) {
        Size.VIAL -> 3
        Size.BOTTLE -> 14
        Size.MAGNUM -> 30
        Size.CASK -> 90
    }

    fun date(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /** The storm raging on [d], if any: 11.11, Black Friday to Cyber Monday, 12.12 and 6.18. */
    fun storm(d: LocalDate): Storm? {
        val thanksgiving = LocalDate.of(d.year, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
        return when {
            d.monthValue == 11 && d.dayOfMonth == 11 -> Storm.SINGLES
            !d.isBefore(thanksgiving.plusDays(1)) && !d.isAfter(thanksgiving.plusDays(4)) -> Storm.BLACK_FRIDAY
            d.monthValue == 12 && d.dayOfMonth == 12 -> Storm.TWELVE
            d.monthValue == 6 && d.dayOfMonth == 18 -> Storm.MIDYEAR
            else -> null
        }
    }

    /** The next storm on or after [from], and its first day. */
    fun nextStorm(from: LocalDate): Pair<Storm, LocalDate> {
        var d = from
        while (true) {
            storm(d)?.let { s -> return s to d }
            d = d.plusDays(1)
        }
    }

    /** The ageing a bottle is laid for: doubled in a storm. */
    fun layDays(chosen: Int, stormy: Boolean): Int = if (stormy) chosen * 2 else chosen

    fun readyAt(b: Bottle): Long = b.laidAt + (b.days + b.extraDays) * DAY

    fun aging(b: Bottle) = b.kindOf == Kind.WISH && b.stateOf == State.AGING

    fun ripe(b: Bottle, now: Long) = aging(b) && now >= readyAt(b)

    /** How far through its ageing, 0..1. */
    fun progress(b: Bottle, now: Long): Float {
        val total = (readyAt(b) - b.laidAt).coerceAtLeast(1)
        return ((now - b.laidAt).toFloat() / total).coerceIn(0f, 1f)
    }

    /** Whole days until ripe, counted up: 0 once it is. */
    fun daysLeft(b: Bottle, now: Long): Int = ceil((readyAt(b) - now).coerceAtLeast(0) / DAY.toDouble()).toInt()

    /** Days on the rack so far (or until it left it). */
    fun aged(b: Bottle, now: Long): Int = (((if (b.endedAt > 0) b.endedAt else now) - b.laidAt) / DAY).toInt().coerceAtLeast(0)

    /** Dust settles with the ageing, and a little more on bottles past their time. */
    fun dust(b: Bottle, now: Long): Float {
        if (b.kindOf == Kind.GIFT) return (aged(b, now) / 60f).coerceIn(0f, 0.6f)
        val over = if (ripe(b, now)) ((now - readyAt(b)) / (30f * DAY)).coerceIn(0f, 0.25f) else 0f
        return (progress(b, now) * 0.75f + over).coerceIn(0f, 1f)
    }

    /** A cobweb shows on anything past halfway that has been down at least five days. */
    fun cobweb(b: Bottle, now: Long) = b.stateOf == State.AGING && progress(b, now) >= 0.5f && aged(b, now) >= 5

    /** An opened bottle the sommelier wants to hear about. */
    fun verdictDue(b: Bottle, now: Long) = b.kindOf == Kind.WISH && b.stateOf == State.OPENED && b.verdictOf == null && now - b.endedAt >= ASK_AFTER * DAY

    /** Days until a gift's date, if it's set and still ahead (0 is today). */
    fun giftIn(b: Bottle, today: LocalDate): Int? {
        if (b.kindOf != Kind.GIFT || b.stateOf != State.AGING || b.giftAt == 0L) return null
        val days = ChronoUnit.DAYS.between(today, date(b.giftAt)).toInt()
        return days.takeIf { it >= 0 }
    }

    /** What went into a barrel. */
    fun poured(bottles: List<Bottle>, dream: Dream): Long =
        bottles.filter { it.stateOf == State.POURED && it.dreamId == dream.id }.sumOf { it.price }

    fun full(bottles: List<Bottle>, dream: Dream) = dream.target > 0 && poured(bottles, dream) >= dream.target

    /** The wax seals earned, with the day each was. */
    fun seals(bottles: List<Bottle>, dreams: List<Dream>, currency: Currency): Map<Seal, Long> {
        val wishes = bottles.filter { it.kindOf == Kind.WISH }
        val poured = wishes.filter { it.stateOf == State.POURED }.sortedBy { it.endedAt }
        val out = mutableMapOf<Seal, Long>()
        bottles.minOfOrNull { it.laidAt }?.let { out[Seal.FIRST_LAID] = it }
        poured.firstOrNull()?.let { out[Seal.FIRST_POURED] = it.endedAt }
        wishes.filter { it.stateOf == State.OPENED && !it.early }.minOfOrNull { it.endedAt }?.let { out[Seal.FIRST_RIPE] = it }
        poured.getOrNull(9)?.let { out[Seal.TEN_POURED] = it.endedAt }
        poured.firstOrNull { it.storm }?.let { out[Seal.STORM] = it.endedAt }
        wishes.filter { it.endedAt > 0 && it.endedAt - it.laidAt >= 180 * DAY }.minOfOrNull { it.endedAt }?.let { out[Seal.HALF_YEAR] = it }
        dreams.filter { it.sealedAt > 0 }.minOfOrNull { it.sealedAt }?.let { out[Seal.BARREL] = it }
        // Five verdicts in a row without vinegar.
        var run = 0
        for (b in wishes.filter { it.verdictOf != null }.sortedBy { it.verdictAt }) {
            run = if (b.verdictOf == Verdict.VINEGAR) 0 else run + 1
            if (run == 5) { out[Seal.CLEAN_FIVE] = b.verdictAt; break }
        }
        // Poured a hundred bottles' worth: a hundred times where a bottle starts.
        var sum = 0L
        for (b in poured) {
            sum += b.price
            if (sum >= currency.bottle * 100) { out[Seal.HUNDRED] = b.endedAt; break }
        }
        return out
    }
}

/** One reason's line in the ledger. */
data class ReasonRow(val reason: Reason, val laid: Int, val opened: Int, val poured: Int, val vintage: Int, val vinegar: Int) {
    val decided get() = opened + poured
}

/** A line the cellarman writes in the ledger, from the numbers. */
sealed interface Insight {
    /** Most of what was poured had been laid out of [reason]. */
    data class Faded(val reason: Reason, val n: Int, val of: Int) : Insight

    /** [reason] gives the most vinegar. */
    data class Sour(val reason: Reason, val n: Int, val of: Int) : Insight

    /** Everything (or almost) laid out of need was opened: the cellar doesn't stop real needs. */
    data class Needed(val n: Int, val of: Int) : Insight

    /** Of what was opened and tasted, this share is still a joy. */
    data class Joy(val n: Int, val of: Int) : Insight

    /** Bottles opened before they were ripe. */
    data class Early(val n: Int, val vinegar: Int) : Insight
}

/** The ledger over the bottles laid since [from]. */
class Ledger(all: List<Bottle>, from: Long = 0) {
    private val wishes = all.filter { it.kindOf == Kind.WISH && it.laidAt >= from }

    val laid = wishes.size
    val opened = wishes.filter { it.stateOf == State.OPENED }
    val poured = wishes.filter { it.stateOf == State.POURED }
    val pouredSum = poured.sumOf { it.price }
    val openedSum = opened.sumOf { it.price }

    val rows: List<ReasonRow> = Reason.entries.map { r ->
        val mine = wishes.filter { it.reasonOf == r }
        ReasonRow(
            r, mine.size, mine.count { it.stateOf == State.OPENED }, mine.count { it.stateOf == State.POURED },
            mine.count { it.verdictOf == Verdict.VINTAGE }, mine.count { it.verdictOf == Verdict.VINEGAR },
        )
    }.filter { it.laid > 0 }

    val insights: List<Insight> = buildList {
        val top = rows.maxByOrNull { it.poured }
        if (top != null && top.poured >= 2 && poured.size >= 3) add(Insight.Faded(top.reason, top.poured, poured.size))
        val sour = rows.filter { it.vinegar >= 2 }.maxByOrNull { it.vinegar.toFloat() / (it.vintage + it.vinegar + 0.5f) }
        if (sour != null) add(Insight.Sour(sour.reason, sour.vinegar, opened.count { it.reasonOf == sour.reason && it.verdictOf != null }))
        rows.firstOrNull { it.reason == Reason.NEED }?.let { need ->
            if (need.decided >= 2 && need.opened * 4 >= need.decided * 3) add(Insight.Needed(need.opened, need.decided))
        }
        val tasted = opened.filter { it.verdictOf != null }
        if (tasted.size >= 3) add(Insight.Joy(tasted.count { it.verdictOf == Verdict.VINTAGE }, tasted.size))
        val early = opened.filter { it.early }
        if (early.size >= 2) add(Insight.Early(early.size, early.count { it.verdictOf == Verdict.VINEGAR }))
    }
}
