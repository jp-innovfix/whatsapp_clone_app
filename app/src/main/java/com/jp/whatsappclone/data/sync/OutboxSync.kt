package com.jp.whatsappclone.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.jp.whatsappclone.di.ApplicationScope
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

data class OutboxDrainResult(
    val processedCount: Int,
    val failedCount: Int,
    val hasMore: Boolean,
    val nextAttemptAtMillis: Long? = null,
)

interface OutboxProcessor {
    suspend fun drain(limit: Int = 50): OutboxDrainResult
}

@Singleton
class OutboxScheduler @Inject constructor(
    private val workManager: WorkManager,
    @ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope,
    private val networkMonitor: com.jp.whatsappclone.data.remote.NetworkMonitor,
) {
    @Volatile private var immediateProcessor: OutboxProcessor? = null
    private val immediateDrainMutex = kotlinx.coroutines.sync.Mutex()

    fun attach(processor: OutboxProcessor) {
        immediateProcessor = processor
    }

    fun schedule() {
        if (networkMonitor.isConnected()) {
            applicationScope.launch {
                immediateDrainMutex.withLock {
                    try {
                        immediateProcessor?.drain(50)
                    } catch (cancellation: kotlinx.coroutines.CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        // WorkManager remains the durable retry path.
                    }
                }
            }
        }
        enqueue(notBeforeMillis = System.currentTimeMillis())
    }

    fun scheduleDeferred(notBeforeMillis: Long) {
        enqueue(notBeforeMillis)
    }

    private fun enqueue(notBeforeMillis: Long) {
        val delayMillis = (notBeforeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<OutboxWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "innovfix-message-outbox"
    }
}

@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val processor: OutboxProcessor,
    private val scheduler: OutboxScheduler,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            var batches = 0
            var result: OutboxDrainResult
            do {
                result = processor.drain(BATCH_SIZE)
                batches++
            } while (
                result.hasMore &&
                (result.nextAttemptAtMillis ?: Long.MAX_VALUE) <= System.currentTimeMillis() &&
                batches < MAX_BATCHES
            )
            if (result.hasMore) {
                scheduler.scheduleDeferred(
                    result.nextAttemptAtMillis ?: System.currentTimeMillis() + MIN_RETRY_DELAY_MILLIS,
                )
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private companion object {
        const val BATCH_SIZE = 50
        const val MAX_BATCHES = 10
        const val MIN_RETRY_DELAY_MILLIS = 10_000L
    }
}
