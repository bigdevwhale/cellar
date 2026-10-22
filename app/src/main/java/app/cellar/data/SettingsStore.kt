package app.cellar.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        currency = Currency.of(this[CURRENCY]),
        notify = this[NOTIFY] ?: true,
        storms = this[STORMS] ?: true,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[CURRENCY] = s.currency.name
            it[NOTIFY] = s.notify
            it[STORMS] = s.storms
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CURRENCY = stringPreferencesKey("currency")
        val NOTIFY = booleanPreferencesKey("notify")
        val STORMS = booleanPreferencesKey("storms")
    }
}
