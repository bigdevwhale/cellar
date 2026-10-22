package app.cellar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cellar.data.Settings
import app.cellar.domain.Share
import app.cellar.domain.Shared
import app.cellar.notify.Ripe
import app.cellar.ui.CellarRoot
import app.cellar.ui.theme.CellarTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        if (savedInstanceState == null) opened(intent)
        Ripe.schedule(this)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Wait for settings so the welcome screen doesn't flash for someone already here.
            settings?.let { s -> CellarTheme { CellarRoot(s, intent?.getBooleanExtra(Ripe.EXTRA_TASTING, false) == true) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        opened(intent)
    }

    /** "Share → Cellar" from a shop: a link and a name, sometimes a picture. */
    private fun opened(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val image = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        val c = container
        c.scope.launch {
            val photo = image?.let(c.cellar::keep)
            val shared = Share.parse(text, subject) ?: Shared("", "", null, null)
            c.incoming.value = Incoming(shared, photo)
        }
    }
}
