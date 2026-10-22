package app.cellar.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cellar.BuildConfig
import app.cellar.R
import app.cellar.container
import app.cellar.data.Currency
import app.cellar.domain.Rules
import app.cellar.notify.Ripe
import app.cellar.ui.Go
import app.cellar.ui.Home
import app.cellar.ui.Language
import app.cellar.ui.T
import app.cellar.ui.components.Chip
import app.cellar.ui.components.Segmented
import app.cellar.ui.components.SectionTitle
import app.cellar.ui.components.Toggle
import app.cellar.ui.components.TopBar
import app.cellar.ui.ctx
import app.cellar.ui.theme.SType
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(home: Home, go: Go) {
    val c = ctx()
    val container = c.container
    val s = home.settings
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionTitle(stringResource(R.string.language))
            Segmented(
                listOf(Language.SYSTEM, Language.EN, Language.RU), Language.current(),
                { stringResource(when (it) { Language.EN -> R.string.lang_en; Language.RU -> R.string.lang_ru; else -> R.string.lang_system }) },
                Language::set,
            )

            SectionTitle(stringResource(R.string.currency))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Currency.entries.forEach { cur ->
                    Chip("${cur.symbol} ${cur.name}", s.currency == cur) { container.write { settings.update { it.copy(currency = cur) } } }
                }
            }
            Text(
                stringResource(R.string.currency_sub, s.currency.format(s.currency.bottle), s.currency.format(s.currency.magnum), s.currency.format(s.currency.cask)),
                style = SType.Muted, modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )

            SectionTitle(stringResource(R.string.cellar_rules))
            Toggle(stringResource(R.string.notify), stringResource(R.string.notify_sub), s.notify) { on ->
                container.write { settings.update { it.copy(notify = on) } }
                if (on && Build.VERSION.SDK_INT >= 33 && !Ripe.allowed(c)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Spacer(Modifier.height(6.dp))
            val (storm, day) = Rules.nextStorm(home.today)
            Toggle(
                stringResource(R.string.storms),
                stringResource(R.string.storms_sub, T.storm(c, storm), T.date(c, day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), home.today)),
                s.storms,
            ) { on -> container.write { settings.update { it.copy(storms = on) } } }

            SectionTitle(stringResource(R.string.about))
            Text(stringResource(R.string.about_text), style = SType.Muted)
            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME), style = SType.Small, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
            )
        }
    }
}
