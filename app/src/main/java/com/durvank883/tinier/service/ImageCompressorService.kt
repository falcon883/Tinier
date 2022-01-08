package com.durvank883.tinier.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import com.durvank883.tinier.model.CompressionConfig
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.notification.ImageCompressorNotification
import com.durvank883.tinier.util.ImageCompressor
import com.hbisoft.pickit.PickiTCallbacks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject


@Suppress("DeferredResultUnused")
@AndroidEntryPoint
class ImageCompressorService : Service(), PickiTCallbacks {

    companion object {
        const val COMPRESS_NOT_STARTED = "Not Started"
        const val COMPRESS_STARTING = "Starting"
        const val COMPRESS_STARTED = "Compressing"
        const val COMPRESS_CLEANING_UP = "Cleaning Up"
        const val COMPRESS_STOPPED = "Stopped"
        const val COMPRESS_DONE = "Done"
    }

    @Inject
    lateinit var imageCompressorNotification: ImageCompressorNotification

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var compressJob: Job

    private val binder = ICBinder()

    /*
    * Compression Path Channel
    * */
    private var compressionChannel: Channel<String?> = Channel()

    private val _photos = MutableStateFlow(setOf<Photo>())
    private val photos: StateFlow<Set<Photo>> = _photos

    private val _toCompress = MutableStateFlow(0)
    val toCompress: StateFlow<Int> = _toCompress

    private val _compressionStatus = MutableStateFlow(COMPRESS_NOT_STARTED)
    val compressionStatus: StateFlow<String> = _compressionStatus

    private val _totalCompressed = MutableStateFlow(0)
    val totalCompressed: StateFlow<Int> = _totalCompressed

    private val _totalImagePathResolved = MutableStateFlow(0)
    val totalImagePathResolved: StateFlow<Int> = _totalImagePathResolved

    @Inject
    lateinit var imageCompressor: ImageCompressor

    /*
    * Client Methods
    * */

    fun setCompressConfig(config: CompressionConfig) {
        imageCompressor.setConfig(
            quality = config.quality,
            format = config.exportFormat,
            size = config.maxImageSize,
            appendName = config.appendName,
            appendNameAtStart = config.appendNameAtStart,
            imageRes = config.imageRes
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun compress(activity: ComponentActivity, photoSet: Set<Photo>): Job {
        val compressJob = scope.launch {
            Log.d("TAG", "compress: Service Compress Called")
            imageCompressorNotification.showNotifications(show = false)

            _photos.value = photoSet
            _totalImagePathResolved.value = 0
            _totalCompressed.value = 0
            _toCompress.value = photos.value.size
            _compressionStatus.value = COMPRESS_STARTING

            if (compressionChannel.isClosedForSend || compressionChannel.isClosedForReceive) {
                compressionChannel = Channel()
            }

            async {
                photos.value.forEach { photo ->
                    imageCompressor.getPath(
                        activity = activity,
                        listener = this@ImageCompressorService,
                        imageUri = photo.uri
                    )
                }
            }

            _compressionStatus.value = COMPRESS_STARTED
            for (path in compressionChannel) {
                imageCompressor.compressImage(path)
                _totalCompressed.value += 1

                imageCompressorNotification.updateProgress(
                    maxProgress = _toCompress.value,
                    currentProgress = _totalCompressed.value
                )
            }

            _compressionStatus.value = COMPRESS_CLEANING_UP
            imageCompressorNotification.updateContentText(COMPRESS_CLEANING_UP)
            activity.applicationContext.cacheDir.deleteRecursively()

            _compressionStatus.value = COMPRESS_DONE
            imageCompressorNotification.completeProgress()
        }
        this.compressJob = compressJob

        return compressJob
    }

    fun cancelJob(context: Context, onCompletion: () -> Unit = {}) {
        Log.i("TAG", "cancelJob: Called")
        scope.launch {
            imageCompressorNotification.removeActions()
            compressJob.cancelAndJoin()
            Log.i("TAG", "cancelJob: Cancelled Job")

            _compressionStatus.value = COMPRESS_CLEANING_UP
            imageCompressorNotification.updateContentText(COMPRESS_CLEANING_UP)

            Log.i("TAG", "cancelJob: Launching cleaner")
            scope.launch(Dispatchers.IO) {
                val cleaned = context.cacheDir.deleteRecursively()
                Log.i("TAG", "cancelJob: Cleaned: $cleaned")
            }.join()

            Log.i("TAG", "cancelJob: Stopping")
            _compressionStatus.value = COMPRESS_STOPPED
            imageCompressorNotification.updateContentText(COMPRESS_STOPPED)

            Log.i("TAG", "cancelJob: Other calls")
            onCompletion()
            Log.i("TAG", "cancelJob: DONE")
            job.cancel()
        }
    }

    override fun PickiTonUriReturned() {

    }

    override fun PickiTonStartListener() {

    }

    override fun PickiTonProgressUpdate(progress: Int) {

    }

    override fun PickiTonCompleteListener(
        path: String?,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        scope.launch {
            if (wasSuccessful) {
                _totalImagePathResolved.value++
                Log.d("TAG", "PickiTonCompleteListener: Resolved: ${_totalImagePathResolved.value}")
                compressionChannel.send(path)
            } else {
                _toCompress.value--
                Log.w("TAG", "PickiTonCompleteListener: ", Throwable(Reason))
            }

            if (_totalImagePathResolved.value == photos.value.size) {
                Log.i("TAG", "PickiTonCompleteListener: Channel Closed")
                compressionChannel.close()
            }

        }
    }

    override fun PickiTonMultipleCompleteListener(
        paths: ArrayList<String>?,
        wasSuccessful: Boolean,
        Reason: String?
    ) {

    }

    inner class ICBinder : Binder() {
        fun getService(): ImageCompressorService = this@ImageCompressorService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            cancelJob(applicationContext) {
                Log.i("TAG", "cancelJob: Inside onCompletion")
                imageCompressorNotification.dismissNotifications()
                stopForeground(true)
                stopSelf()
                Log.i("TAG", "cancelJob: onCompletion DONE")
            }
        }
        return START_STICKY
    }

    fun moveToForeground() {
        startForeground(
            ImageCompressorNotification.PROGRESS_NOTIFICATION_ID,
            imageCompressorNotification.builder.build()
        )
        imageCompressorNotification.showNotifications(show = true)
    }
}