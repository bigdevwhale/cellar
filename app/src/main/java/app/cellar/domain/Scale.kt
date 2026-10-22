package app.cellar.domain

import app.cellar.data.Currency

/**
 * Which bottle a price is laid in and how long each bottle ages by default. Below [bottle] it's a vial,
 * then a bottle, a magnum from [magnum], a cask from [cask]. The currency gives the defaults; the
 * settings can change any of them.
 */
data class Scale(val bottle: Long, val magnum: Long, val cask: Long, val days: Map<Size, Int>) {
    fun size(price: Long): Size = when {
        price >= cask -> Size.CASK
        price >= magnum -> Size.MAGNUM
        price >= bottle -> Size.BOTTLE
        else -> Size.VIAL
    }

    fun days(size: Size): Int = days[size] ?: DEFAULT_DAYS.getValue(size)

    /** Where a size starts (a vial starts at nothing). */
    fun from(size: Size): Long = when (size) {
        Size.VIAL -> 0
        Size.BOTTLE -> bottle
        Size.MAGNUM -> magnum
        Size.CASK -> cask
    }

    companion object {
        val DEFAULT_DAYS = mapOf(Size.VIAL to 3, Size.BOTTLE to 14, Size.MAGNUM to 30, Size.CASK to 90)

        /** Ageing to choose from as a bottle's default, in days. */
        val CHOICES = listOf(1, 3, 7, 14, 21, 30, 60, 90, 180)

        fun of(currency: Currency) = Scale(currency.bottle, currency.magnum, currency.cask, DEFAULT_DAYS)

        /** Each size must start above the one before it: [size] may start at a price between these two. */
        fun bounds(scale: Scale, size: Size): LongRange = when (size) {
            Size.VIAL -> 0L..0L
            Size.BOTTLE -> 1L until scale.magnum
            Size.MAGNUM -> scale.bottle + 1 until scale.cask
            Size.CASK -> scale.magnum + 1..Long.MAX_VALUE
        }
    }
}
