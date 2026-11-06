package ir.heydar.monetra

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import ir.heydar.monetra.core.notification.MonetraNotificationChannel
import ir.heydar.monetra.core.worker.NotificationExpiryWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MonetraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MonetraNotificationChannel.createChannels(this)
        scheduleExpiryWorker()
    }

    private fun scheduleExpiryWorker() {
        val expiryWorkRequest = PeriodicWorkRequestBuilder<NotificationExpiryWorker>(
            6, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NotificationExpiryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            expiryWorkRequest
        )
    }
}
