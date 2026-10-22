package app.cellar.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.cellar.domain.Scale
import app.cellar.domain.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    val currency: Currency = Currency.RUB,
    /** A quiet notification when a bottle is ripe. */
    val notify: Boolean = true,
    /** Big sale days double the ageing of what's laid on them. */
    val storms: Boolean = true,
    /** Where a bottle, a magnum and a cask start, if changed from the currency's defaults (0 = default). */
    val prices: Map<Size, Long> = emptyMap(),
    /** Each bottle's default ageing in days, if changed (missing = default). */
    val days: Map<Size, Int> = emptyMap(),
) {
    val scale: Scale
        get() = Scale.of(currency).let { d ->
            Scale(prices[Size.BOTTLE] ?: d.bottle, prices[Size.MAGNUM] ?: d.magnum, prices[Size.CASK] ?: d.cask, Scale.DEFAULT_DAYS + days)
        }

    /** Anything changed from the defaults. */
    val customScale: Boolean get() = prices.isNotEmpty() || days.isNotEmpty()
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        currency = Currency.of(this[CURRENCY]),
        notify = this[NOTIFY] ?: true,
        storms = this[STORMS] ?: true,
        prices = Size.entries.mapNotNull { s -> this[longPreferencesKey("from_${s.name}")]?.takeIf { it > 0 }?.let { s to it } }.toMap(),
        days = Size.entries.mapNotNull { s -> this[intPreferencesKey("days_${s.name}")]?.takeIf { it > 0 }?.let { s to it } }.toMap(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[CURRENCY] = s.currency.name
            it[NOTIFY] = s.notify
            it[STORMS] = s.storms
            Size.entries.forEach { size ->
                it[longPreferencesKey("from_${size.name}")] = s.prices[size] ?: 0L
                it[intPreferencesKey("days_${size.name}")] = s.days[size] ?: 0
            }
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CURRENCY = stringPreferencesKey("currency")
        val NOTIFY = booleanPreferencesKey("notify")
        val STORMS = booleanPreferencesKey("storms")
    }
}
