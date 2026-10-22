package app.cellar.data

import android.content.Context
import app.cellar.AppContainer
import app.cellar.localized
import app.cellar.ru

/**
 * A cellar half a year in, for the demo build and the screenshots: a rack of wishes at every age (three
 * of them ripe), a gift shelf, opened bottles with and without verdicts (one waiting for one), a barrel
 * for Japan well on its way and an old one already sealed.
 */
object DemoSeed {
    private const val DAY = 86_400_000L

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.bottles().isNotEmpty()) return
        val ru = context.localized().ru
        val now = System.currentTimeMillis()
        fun n(en: String, r: String) = if (ru) r else en
        fun p(usd: Long, rub: Long) = if (ru) rub else usd

        val bike = dao.insert(Dream(name = n("A bicycle", "Велосипед"), target = p(600, 45_000), startedAt = now - 200 * DAY, sealedAt = now - 70 * DAY))
        val japan = dao.insert(Dream(name = n("Japan", "Япония"), target = p(2_500, 180_000), startedAt = now - 70 * DAY + 1))

        suspend fun b(
            name: String, price: Long, reason: Reason, laidDaysAgo: Int, days: Int, shape: Long,
            state: State = State.AGING, endedDaysAgo: Int = 0, verdict: Verdict? = null, early: Boolean = false, extra: Int = 0,
            note: String = "", url: String = "", dream: Long = 0, storm: Boolean = false,
            kind: Kind = Kind.WISH, giftFor: String = "", giftInDays: Int? = null,
        ) {
            dao.insert(
                Bottle(
                    kind = kind.name, name = name, price = price, reason = reason.name, laidAt = now - laidDaysAgo * DAY - 3_600_000L * (laidDaysAgo % 9),
                    days = days, extraDays = extra, storm = storm, state = state.name,
                    endedAt = if (state == State.AGING) 0 else now - endedDaysAgo * DAY, early = early,
                    verdict = verdict?.name, verdictAt = if (verdict != null) now - (endedDaysAgo - 30).coerceAtLeast(0) * DAY else 0,
                    dreamId = if (state == State.POURED) dream else 0, shape = shape, note = note, url = url,
                    giftFor = giftFor, giftAt = giftInDays?.let { now + it * DAY } ?: 0, told = true,
                ),
            )
        }

        // On the rack: ripe ones first, then young ones.
        b(n("Wireless headphones", "Беспроводные наушники"), p(180, 14_990), Reason.ENVY, 33, 30, 101, note = n("The noise cancelling everyone talks about", "Шумодав, про который все говорят"), url = "https://www.ozon.ru/product/1")
        b(n("Linen shirt", "Льняная рубашка"), p(45, 3_490), Reason.SALE, 16, 14, 102, url = "https://www.wildberries.ru/catalog/2")
        b(n("Scented candle", "Ароматическая свеча"), p(12, 890), Reason.SAD, 4, 3, 103)
        b(n("Mechanical keyboard", "Механическая клавиатура"), p(140, 11_500), Reason.BORED, 20, 30, 104)
        b(n("Smart watch", "Умные часы"), p(320, 24_990), Reason.AD, 12, 30, 105)
        b(n("Board game", "Настольная игра"), p(40, 3_200), Reason.BORED, 6, 14, 106)
        b(n("Coffee grinder", "Кофемолка"), p(90, 6_900), Reason.NEED, 3, 14, 107, note = n("The old one grinds unevenly", "Старая мелет неровно"))
        b(n("Air fryer", "Аэрогриль"), p(130, 9_990), Reason.AD, 45, 60, 108, storm = false)
        b(n("Camera lens", "Объектив 35 мм"), p(650, 52_000), Reason.ENVY, 50, 90, 109)
        b(n("Sneakers", "Кроссовки"), p(110, 8_990), Reason.SALE, 2, 28, 110, storm = true)
        b(n("Phone case", "Чехол для телефона"), p(9, 690), Reason.BORED, 1, 3, 111)

        // The gift shelf.
        b(n("Fountain pen", "Перьевая ручка"), p(60, 4_500), Reason.NEED, 20, 0, 201, kind = Kind.GIFT, giftFor = n("Dad", "Папа"), giftInDays = 9, note = n("He still signs everything in ballpoint", "Всё ещё подписывает всё шариковой"))
        b(n("Tea set", "Чайный набор"), p(35, 2_800), Reason.NEED, 8, 0, 202, kind = Kind.GIFT, giftFor = n("Lena", "Лена"), giftInDays = 40)

        // Opened.
        b(n("Hiking boots", "Трекинговые ботинки"), p(160, 12_900), Reason.NEED, 80, 30, 301, State.OPENED, 50, Verdict.VINTAGE)
        b(n("E-reader", "Электронная книга"), p(130, 11_000), Reason.NEED, 120, 30, 302, State.OPENED, 90, Verdict.VINTAGE)
        b(n("Blender", "Блендер"), p(70, 5_400), Reason.AD, 100, 14, 303, State.OPENED, 86, Verdict.VINEGAR)
        b(n("Yoga mat", "Коврик для йоги"), p(30, 2_300), Reason.BORED, 70, 3, 304, State.OPENED, 66, Verdict.SOSO)
        b(n("Designer hoodie", "Дизайнерское худи"), p(95, 7_900), Reason.SALE, 60, 14, 305, State.OPENED, 55, Verdict.VINEGAR, early = true)
        b(n("Drawing tablet", "Графический планшет"), p(85, 6_500), Reason.BORED, 74, 30, 306, State.OPENED, 40, Verdict.VINEGAR)
        b(n("Cast-iron pan", "Чугунная сковорода"), p(50, 3_900), Reason.NEED, 52, 14, 307, State.OPENED, 36)
        b(n("Houseplant", "Комнатное растение"), p(25, 1_900), Reason.SAD, 14, 3, 308, State.OPENED, 10)

        // Poured: the old barrel (a bicycle), then Japan.
        b(n("Gaming console", "Игровая приставка"), p(450, 39_000), Reason.ENVY, 190, 90, 401, State.POURED, 100, dream = bike)
        b(n("Smart speaker", "Умная колонка"), p(110, 9_000), Reason.AD, 150, 30, 402, State.POURED, 118, dream = bike)
        b(n("Fitness bracelet", "Фитнес-браслет"), p(60, 4_200), Reason.BORED, 130, 14, 403, State.POURED, 115, dream = bike)
        b(n("Leather bag", "Кожаная сумка"), p(220, 17_500), Reason.SALE, 68, 28, 404, State.POURED, 38, dream = japan, storm = true)
        b(n("Robot vacuum", "Робот-пылесос"), p(300, 26_000), Reason.AD, 66, 30, 405, State.POURED, 35, dream = japan)
        b(n("Sunglasses", "Солнцезащитные очки"), p(140, 11_900), Reason.ENVY, 55, 30, 406, State.POURED, 24, dream = japan)
        b(n("Cushion set", "Набор подушек"), p(40, 3_100), Reason.BORED, 40, 14, 407, State.POURED, 26, dream = japan)
        b(n("Perfume", "Парфюм"), p(95, 8_500), Reason.SAD, 36, 14, 408, State.POURED, 21, dream = japan)
        b(n("Tablet", "Планшет"), p(520, 42_000), Reason.BORED, 120, 90, 409, State.POURED, 20, dream = japan, extra = 7)
        b(n("Mini projector", "Мини-проектор"), p(180, 14_000), Reason.AD, 25, 14, 410, State.POURED, 10, dream = japan)
        b(n("Jacket", "Куртка"), p(150, 12_500), Reason.SALE, 20, 14, 411, State.POURED, 5, dream = japan)
        b(n("Smart lamp", "Умная лампа"), p(35, 2_700), Reason.BORED, 9, 3, 412, State.POURED, 4, dream = japan)

        c.settings.update { it.copy(onboarded = true, currency = if (ru) Currency.RUB else Currency.USD) }
    }
}
