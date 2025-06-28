@file:Suppress("ConstPropertyName")

package com.emm.hello.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteGenerator
import com.emm.domain.quote.QuoteLastFetcher
import com.emm.hello.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    val quoteGenerator: QuoteGenerator by inject<QuoteGenerator>()
    val quoteLastFetcher: QuoteLastFetcher by inject<QuoteLastFetcher>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            quoteGenerator.generateQuote()
            val lastQuote: Quote? = quoteLastFetcher.fetch().firstOrNull()?.firstOrNull()
            lastQuote?.let(::showTestNotification)
            Result.success()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo(
        channelName = SyncWorker,
        notificationId = SYNC_NOTIFICATION_ID,
        channelId = SYNC_NOTIFICATION_CHANNEL_ID,
    )

    fun showTestNotification(lastQuote: Quote) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "quote_channel"

        val channel = NotificationChannel(
            channelId,
            "Frases del día",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.outline_save_24)
            .setContentTitle(lastQuote.title)
            .setContentText(lastQuote.phrase)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {

        const val SyncWorker: String = "SyncWorker"

        private const val SYNC_NOTIFICATION_ID = 0
        private const val SYNC_NOTIFICATION_CHANNEL_ID = "SyncNotificationChannel"

        fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()

        fun startUpSyncWorkPeriodic(): PeriodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(SyncConstraints)
            .build()
    }
}