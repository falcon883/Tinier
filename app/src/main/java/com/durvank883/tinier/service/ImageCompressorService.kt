package com.durvank883.tinier.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.util.ImageCompressor
import com.hbisoft.pickit.PickiTCallbacks
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*


@Suppress("DeferredResultUnused")
class ImageCompressorService : Service(), PickiTCallbacks {

    companion object {
        const val COMPRESS_NOT_STARTED = "Not Started"
        const val COMPRESS_STARTING = "Starting"
        const val COMPRESS_STARTED = "Compressing"
        const val COMPRESS_CLEANING_UP = "Cleaning Up"
        const val COMPRESS_STOPPED = "Stopped"
        const val COMPRESS_DONE = "Done"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

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

    private lateinit var imageCompressor: ImageCompressor

    /* Client Methods */
    fun setImageCompressor(imageCompressor: ImageCompressor) {
        this.imageCompressor = imageCompressor
    }

    fun setCompressConfig(
        quality: Int,
        maxImageSize: Long,
        exportFormat: Bitmap.CompressFormat?,
        trailingName: String
    ) {
        imageCompressor.setConfig(
            quality = quality,
            format = exportFormat,
            size = maxImageSize,
            trailingName = trailingName
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun compress(activity: ComponentActivity, photoSet: Set<Photo>) = scope.launch {
        Log.d("TAG", "compress: Service Compress Called")
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
        }

        _compressionStatus.value = COMPRESS_CLEANING_UP
        activity.applicationContext.cacheDir.deleteRecursively()

        _compressionStatus.value = COMPRESS_DONE
    }

    fun cancelJob(context: Context) {
        job.cancel()
        _compressionStatus.value = COMPRESS_STOPPED

        scope.launch(Dispatchers.IO) {
            context.cacheDir.deleteRecursively()
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
        Log.d("TAG", "PickiTonMultipleCompleteListener: $wasSuccessful")
        Log.d("TAG", "PickiTonMultipleCompleteListener: ${paths?.size}")
        Log.d("TAG", "PickiTonMultipleCompleteListener: $Reason")
    }

    inner class ICBinder : Binder() {
        fun getService(): ImageCompressorService = this@ImageCompressorService
    }

    override fun onBind(intent: Intent): IBinder = binder
}