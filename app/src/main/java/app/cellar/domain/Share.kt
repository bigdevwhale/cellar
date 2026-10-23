package app.cellar.domain

import app.cellar.data.Currency

/** What a shop's "Share" hands over: usually a name and a link, sometimes a price. */
data class Shared(val name: String, val url: String, val price: Long?, val currency: Currency?)

object Share {
    private val URL = Regex("""https?://\S+""")
    private val PRICE = Regex("""(?u)(?:([$€£])\s?(\d[\d\s  ]*(?:[.,]\d{1,2})?)|(\d[\d\s  ]*(?:[.,]\d{1,2})?)\s?(₽|руб\.?|р\.|₸|₴|грн|€|\$))""", RegexOption.IGNORE_CASE)
    /** "… for", "… за" left hanging where a price was cut out. */
    private val TAIL = Regex("""(?iu)\s+(за|по|for|at|только|only)$""")
    private val NOISE = Regex("""(?iu)(посмотрите|смотри|look at|check out|check this out|on ozon|на ozon|в приложении|в wildberries|on wildberries|on amazon|на маркете)[^\n]*?[:—–-]?\s*""")

    fun parse(text: String, subject: String = ""): Shared? {
        val url = URL.find(text)?.value?.let(::clean) ?: ""
        val price = PRICE.find(text)
        var name = text.replace(URL, " ")
        if (price != null) name = name.replace(price.value, " ")
        name = name.replace(NOISE, " ").lines().map { it.trim() }.firstOrNull { it.isNotEmpty() && it.any(Char::isLetter) }.orEmpty()
        name = name.trim(' ', '"', '«', '»', ':', '—', '–', '-', ',', '.').replace(Regex("\\s+"), " ").replace(TAIL, "")
        if (name.isEmpty()) name = subject.trim()
        if (name.isEmpty()) name = host(url)
        if (name.isEmpty()) return null
        val amount = price?.let { m -> (m.groupValues[2].ifEmpty { m.groupValues[3] }) }?.let(::amount)
        val cur = price?.let { m -> currency(m.groupValues[1].ifEmpty { m.groupValues[4] }) }
        return Shared(name.take(80), url, amount, cur)
    }

    /**
     * What the link field keeps after an edit. A paste with a link in it ("Name https://…") leaves just the link;
     * text with spaces and no link isn't a link at all, so the field stays as it was. Plain typing passes through.
     */
    fun link(input: String, old: String): String {
        val fresh = if (old.isNotEmpty() && old in input) input.replaceFirst(old, " ") else input
        URL.find(fresh)?.let { return clean(it.value) }
        return if (input.any(Char::isWhitespace)) old else input
    }

    private fun clean(url: String) = url.trimEnd('.', ',', ')', '»', '"')

    /** "ozon.ru" from a link, without "www.". */
    fun host(url: String): String = Regex("""https?://(?:www\.)?([^/\s?#]+)""").find(url)?.groupValues?.get(1).orEmpty()

    private fun amount(s: String): Long? = s.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        .let { if (it.count { c -> c == '.' } > 1 || (it.substringAfterLast('.', "").length == 3)) it.replace(".", "") else it }
        .toDoubleOrNull()?.toLong()

    private fun currency(s: String): Currency? = when (s.lowercase()) {
        "$" -> Currency.USD
        "€" -> Currency.EUR
        "£" -> Currency.GBP
        "₸" -> Currency.KZT
        "₴", "грн" -> Currency.UAH
        "₽", "руб", "руб.", "р." -> Currency.RUB
        else -> null
    }
}
