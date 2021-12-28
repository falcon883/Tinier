package com.durvank883.tinier.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.util.ImageCompressor
import com.hbisoft.pickit.PickiTCallbacks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val imageCompressor: ImageCompressor
) : ViewModel(), PickiTCallbacks {

    private val TAG: String? = MainViewModel::class.java.canonicalName

    /*
    * Photo List
    * */
    private val _photos = MutableStateFlow(setOf<Photo>())
    val photos: StateFlow<Set<Photo>> = _photos

    /*
    * Action Mode
    * */
    private val _isActionMode = MutableStateFlow(false)
    val isActionMode: StateFlow<Boolean> = _isActionMode

    private val _totalSelected = MutableStateFlow(0)
    val totalSelected: StateFlow<Int> = _totalSelected


    /*
    * Compression Path Channel
    * */
    private val compressionChannel = Channel<String?>()


    /*
    * Compress Progress
    * */

    private val _totalCompressed = MutableStateFlow(0)
    val totalCompressed: StateFlow<Int> = _totalCompressed

    private val _totalImagePathResolved = MutableStateFlow(0)
    val totalImagePathResolved: StateFlow<Int> = _totalImagePathResolved


    fun setPhotos(context: Context, newPhotos: List<Uri>) = viewModelScope.launch {

        val contentResolver = context.contentResolver

        _photos.value = _photos.value.plus(newPhotos.filter { uri ->
            val type = contentResolver.getType(uri)
            type != null && type.startsWith("image")
        }.map { uri -> Photo(uri) })

        Log.d(TAG, "setPhotos: Photos added")
        Log.d(TAG, photos.value.toString())
    }

    fun toggleActionMode() = viewModelScope.launch {
        _isActionMode.value = !_isActionMode.value

        if (!_isActionMode.value) {
            _photos.value = _photos.value.map { photo ->
                photo.copy(isSelected = false)
            }.toSet()
            _totalSelected.value = 0
        }

        Log.d(TAG, "toggleActionMode: ${isActionMode.value}")
    }

    fun togglePhotoSelection(photo: Photo) = viewModelScope.launch {
        _photos.value = _photos.value.map { p ->
            if (p.uri == photo.uri) {
                Log.d(TAG, "togglePhotoSelection: $p")
                p.copy(isSelected = !p.isSelected)
            } else {
                p
            }
        }.toSet()

        _totalSelected.value = _photos.value.filter { p -> p.isSelected }.size
    }

    fun removeSelectedPhotos() = viewModelScope.launch {
        _photos.value = _photos.value.filter { photo -> !photo.isSelected }.toSet()
        _totalSelected.value = 0
        toggleActionMode()
    }

    fun selectAllPhotos() = viewModelScope.launch {
        if (_photos.value.all { photo -> photo.isSelected }) {
            toggleActionMode()
        } else {
            _photos.value = _photos.value.map { photo ->
                photo.copy(isSelected = true)
            }.toSet()
            _totalSelected.value = _photos.value.size
        }
    }

    fun setCompressConfig(
        quality: Int,
        maxImageSize: Map<String, String>,
        exportFormat: String,
        trailingName: String
    ) = viewModelScope.launch {
        val format: Bitmap.CompressFormat? = when (exportFormat) {
            "png" -> Bitmap.CompressFormat.PNG
            "jpg" -> Bitmap.CompressFormat.JPEG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> null
        }

        val (sizeIn, maxSize) = maxImageSize.entries.first()
        val size = if (sizeIn == "MB") {
            1024 * 1024 * maxSize.toLong()
        } else {
            1024 * maxSize.toLong()
        }

        imageCompressor.setConfig(
            quality = quality,
            format = format,
            size = size,
            trailingName = trailingName
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun compress(activity: ComponentActivity) = viewModelScope.launch(context = Dispatchers.IO) {
        _totalImagePathResolved.value = 0
        _totalCompressed.value = 0

        photos.value.forEach {
            imageCompressor.getPath(
                activity = activity,
                listener = this@MainViewModel,
                imageUri = it.uri
            )
        }

        compressionChannel.consumeEach { path ->
            _totalCompressed.value += 1
            imageCompressor.compressImage(path)
        }

        Log.d(TAG, "compress: ${compressionChannel.isClosedForReceive}")
        Log.d(TAG, "compress: ${compressionChannel.isClosedForSend}")
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
        viewModelScope.launch {
            _totalImagePathResolved.value += 1
            compressionChannel.send(path)
        }
    }

    override fun PickiTonMultipleCompleteListener(
        paths: ArrayList<String>?,
        wasSuccessful: Boolean,
        Reason: String?
    ) {

    }
}
