package app.cellar.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.cellar.R
import app.cellar.data.Reason
import app.cellar.data.Verdict
import app.cellar.domain.Insight
import app.cellar.domain.Seal
import app.cellar.domain.Size
import app.cellar.domain.Storm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Words and dates in the app's language. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]
    private fun zone() = ZoneId.systemDefault()

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "24 October", or with the year when it isn't this one. */
    fun date(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        return d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", locale(c)))
    }

    /** "22.10.2026", the vintage on a label. */
    fun vintage(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    /** "today", "yesterday", "5 days ago", or the date for anything older than two weeks. */
    fun ago(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        val days = ChronoUnit.DAYS.between(d, today).toInt()
        return when {
            days <= 0 -> c.getString(R.string.today)
            days == 1 -> c.getString(R.string.yesterday)
            days < 14 -> plural(c, R.plurals.days_ago, days)
            else -> date(c, at, today)
        }
    }

    fun days(c: Context, n: Int) = plural(c, R.plurals.days, n)

    /** "3 days", "2 weeks", "a month", "3 months" for the ageing chips. */
    fun age(c: Context, days: Int): String = when {
        days % 30 == 0 -> plural(c, R.plurals.months, days / 30)
        days % 7 == 0 -> plural(c, R.plurals.weeks, days / 7)
        else -> days(c, days)
    }

    fun reason(c: Context, r: Reason): String = c.getString(
        when (r) {
            Reason.NEED -> R.string.r_need
            Reason.BORED -> R.string.r_bored
            Reason.SAD -> R.string.r_sad
            Reason.AD -> R.string.r_ad
            Reason.ENVY -> R.string.r_envy
            Reason.SALE -> R.string.r_sale
        },
    )

    /** "out of boredom", to finish a sentence. */
    fun because(c: Context, r: Reason): String = c.getString(
        when (r) {
            Reason.NEED -> R.string.b_need
            Reason.BORED -> R.string.b_bored
            Reason.SAD -> R.string.b_sad
            Reason.AD -> R.string.b_ad
            Reason.ENVY -> R.string.b_envy
            Reason.SALE -> R.string.b_sale
        },
    )

    fun size(c: Context, s: Size): String = c.getString(
        when (s) {
            Size.VIAL -> R.string.sz_vial
            Size.BOTTLE -> R.string.sz_bottle
            Size.MAGNUM -> R.string.sz_magnum
            Size.CASK -> R.string.sz_cask
        },
    )

    fun verdict(c: Context, v: Verdict): String = c.getString(
        when (v) {
            Verdict.VINTAGE -> R.string.v_vintage
            Verdict.SOSO -> R.string.v_soso
            Verdict.VINEGAR -> R.string.v_vinegar
        },
    )

    fun verdictSub(c: Context, v: Verdict): String = c.getString(
        when (v) {
            Verdict.VINTAGE -> R.string.v_vintage_sub
            Verdict.SOSO -> R.string.v_soso_sub
            Verdict.VINEGAR -> R.string.v_vinegar_sub
        },
    )

    fun storm(c: Context, s: Storm): String = c.getString(
        when (s) {
            Storm.SINGLES -> R.string.storm_singles
            Storm.BLACK_FRIDAY -> R.string.storm_black_friday
            Storm.TWELVE -> R.string.storm_twelve
            Storm.MIDYEAR -> R.string.storm_midyear
        },
    )

    fun seal(c: Context, s: Seal): Pair<String, String> = when (s) {
        Seal.FIRST_LAID -> R.string.seal_first_laid to R.string.seal_first_laid_sub
        Seal.FIRST_POURED -> R.string.seal_first_poured to R.string.seal_first_poured_sub
        Seal.FIRST_RIPE -> R.string.seal_first_ripe to R.string.seal_first_ripe_sub
        Seal.TEN_POURED -> R.string.seal_ten to R.string.seal_ten_sub
        Seal.STORM -> R.string.seal_storm to R.string.seal_storm_sub
        Seal.HALF_YEAR -> R.string.seal_half_year to R.string.seal_half_year_sub
        Seal.BARREL -> R.string.seal_full to R.string.seal_full_sub
        Seal.CLEAN_FIVE -> R.string.seal_clean to R.string.seal_clean_sub
        Seal.HUNDRED -> R.string.seal_hundred to R.string.seal_hundred_sub
    }.let { (a, b) -> c.getString(a) to c.getString(b) }

    /** The cellarman's line in the ledger. */
    fun insight(c: Context, i: Insight): String = when (i) {
        is Insight.Faded -> c.getString(R.string.i_faded, i.n, i.of, because(c, i.reason))
        is Insight.Sour -> c.getString(R.string.i_sour, reason(c, i.reason), i.n, i.of)
        is Insight.Needed -> c.getString(R.string.i_needed, i.n, i.of)
        is Insight.Joy -> c.getString(R.string.i_joy, i.n, i.of)
        is Insight.Early -> if (i.vinegar > 0) c.getString(R.string.i_early_sour, i.n, i.vinegar) else c.getString(R.string.i_early, i.n)
    }
}

object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
