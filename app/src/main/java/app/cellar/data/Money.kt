package app.cellar.data

/**
 * The cellar's currency and where its bottle sizes start: below [bottle] it's a vial, then a bottle,
 * a magnum from [magnum], a cask from [cask]. Roughly the same real money in each currency.
 */
enum class Currency(val symbol: String, val before: Boolean, val bottle: Long, val magnum: Long, val cask: Long) {
    RUB("₽", false, 1_000, 10_000, 50_000),
    USD("$", true, 15, 120, 600),
    EUR("€", true, 15, 120, 600),
    GBP("£", true, 12, 100, 500),
    KZT("₸", false, 5_000, 50_000, 250_000),
    UAH("₴", false, 500, 5_000, 20_000);

    /** "12 990 ₽", "$129". */
    fun format(amount: Long): String = if (before) "$symbol${digits(amount)}" else "${digits(amount)} $symbol"

    /** "12 990", grouped by thousands, without the sign. */
    fun digits(amount: Long): String = amount.toString().reversed().chunked(3).joinToString(" ").reversed()

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: RUB
    }
}
