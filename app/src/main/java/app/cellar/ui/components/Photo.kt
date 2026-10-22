package app.cellar.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.cellar.container
import app.cellar.ui.ctx
import app.cellar.ui.theme.S
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A picture kept with a bottle, loaded off the main thread. */
@Composable
fun Photo(name: String, modifier: Modifier = Modifier, crop: Boolean = true) {
    val c = ctx()
    val image by produceState<ImageBitmap?>(null, name) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(File(c.container.cellar.photos(), name).path)?.asImageBitmap() }.getOrNull()
        }
    }
    image?.let { Image(it, null, modifier.clip(S.RMd), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit) }
}
