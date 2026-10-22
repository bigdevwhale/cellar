package app.cellar.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.cellar.MainActivity
import app.cellar.R
import app.cellar.container
import app.cellar.domain.Rules
import app.cellar.localized
import java.util.concurrent.TimeUnit

/** A few times a day: bottles that came ripe get one quiet notification, once. */
class RipeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        if (!c.settings.now().notify) return Result.success()
        val now = System.currentTimeMillis()
        val dao = c.db.dao()
        val fresh = dao.bottles().filter { Rules.ripe(it, now) && !it.told }
        if (fresh.isEmpty()) return Result.success()
        Ripe.tell(applicationContext, fresh.map { it.name })
        fresh.forEach { dao.update(it.copy(told = true)) }
        return Result.success()
    }
}

object Ripe {
    const val EXTRA_TASTING = "tasting"
    private const val CHANNEL = "ripe"
    private const val WORK = "ripe"

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<RipeWorker>(6, TimeUnit.HOURS).build(),
        )
    }

    fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun tell(context: Context, names: List<String>) {
        if (!allowed(context)) return
        val c = context.localized()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, c.getString(R.string.channel_ripe), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java).putExtra(EXTRA_TASTING, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = if (names.size == 1) c.getString(R.string.ripe_one, names[0]) else c.resources.getQuantityString(R.plurals.ripe_many, names.size, names.size)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(c.getString(R.string.ripe_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(1, n)
    }
}
