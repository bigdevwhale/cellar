package app.cellar.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.cellar.domain.Rules
import java.io.File
import java.util.UUID
import kotlin.random.Random

/** Everything done in the cellar, as writes. */
class Cellar(private val context: Context, private val db: CellarDatabase) {
    private val dao = db.dao()

    /** The barrel the money goes into now: the newest unsealed one, made (without a name) if there is none. */
    suspend fun barrel(): Dream = dao.dreams().lastOrNull { it.sealedAt == 0L } ?: Dream(name = "").let { it.copy(id = dao.insert(it)) }

    suspend fun lay(b: Bottle): Long = dao.insert(b.copy(shape = if (b.shape == 0L) Random.nextLong() else b.shape))

    suspend fun edit(b: Bottle) = dao.update(b)

    suspend fun open(id: Long, now: Long = System.currentTimeMillis()) {
        val b = dao.bottle(id) ?: return
        dao.update(b.copy(state = State.OPENED.name, endedAt = now, early = !Rules.ripe(b, now)))
    }

    suspend fun pour(id: Long, now: Long = System.currentTimeMillis()) {
        val b = dao.bottle(id) ?: return
        dao.update(b.copy(state = State.POURED.name, endedAt = now, dreamId = barrel().id))
    }

    /** "Let it age another week." */
    suspend fun extend(id: Long) {
        val b = dao.bottle(id) ?: return
        dao.update(b.copy(extraDays = b.extraDays + Rules.EXTEND, told = false))
    }

    suspend fun verdict(id: Long, v: Verdict, now: Long = System.currentTimeMillis()) {
        val b = dao.bottle(id) ?: return
        dao.update(b.copy(verdict = v.name, verdictAt = now))
    }

    suspend fun give(id: Long, now: Long = System.currentTimeMillis()) {
        val b = dao.bottle(id) ?: return
        dao.update(b.copy(state = State.GIVEN.name, endedAt = now))
    }

    /** Back on the rack: an open or a pour tapped by mistake. */
    suspend fun undo(id: Long) {
        val b = dao.bottle(id) ?: return
        dao.update(b.copy(state = State.AGING.name, endedAt = 0, early = false, verdict = null, verdictAt = 0, dreamId = 0))
    }

    suspend fun delete(id: Long) {
        dao.bottle(id)?.photo?.let { File(photos(), it).delete() }
        dao.deleteBottle(id)
    }

    /** Chalk a new dream on the barrel (or rename the current one). */
    suspend fun dream(name: String, target: Long) {
        val d = barrel()
        dao.update(d.copy(name = name.trim(), target = target.coerceAtLeast(0)))
    }

    /** The barrel is full: seal it and roll in an empty one. */
    suspend fun seal(now: Long = System.currentTimeMillis()) {
        val d = barrel()
        dao.update(d.copy(sealedAt = now))
        dao.insert(Dream(name = "", startedAt = now + 1))
    }

    fun photos(): File = File(context.filesDir, "photos").apply { mkdirs() }

    /** Copies a picture (from the picker, or shared in with a link) into the app, no larger than 1280 px. */
    fun keep(uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1280) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val name = "${UUID.randomUUID()}.jpg"
        File(photos(), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 86, it) }
        name
    }.getOrNull()
}
