package com.durvank883.tinier.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.durvank883.tinier.model.CompressionConfig
import com.durvank883.tinier.model.ImageRes
import com.durvank883.tinier.model.Photo
import com.durvank883.tinier.prefs.SettingsDataStore
import com.durvank883.tinier.service.ImageCompressorService
import com.durvank883.tinier.service.ImageCompressorServiceBinder
import com.durvank883.tinier.util.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val compressorServiceBinder: ImageCompressorServiceBinder,
    settingsDataStore: SettingsDataStore
) : ViewModel() {

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
    * Compression Config
    * */
    private val _compressionConfig = MutableStateFlow(CompressionConfig())

    /*
    * Compress Progress
    * */

    private val _toCompress = MutableStateFlow(0)
    val toCompress: StateFlow<Int> = _toCompress

    private val _compressionStatus = MutableStateFlow("Not Started")
    val compressionStatus: StateFlow<String> = _compressionStatus

    private val _totalCompressed = MutableStateFlow(0)
    val totalCompressed: StateFlow<Int> = _totalCompressed

    private val _totalImagePathResolved = MutableStateFlow(0)
    val totalImagePathResolved: StateFlow<Int> = _totalImagePathResolved

    /*
    * Datastore
    * */
    private val appendNameAtStart: StateFlow<Boolean> =
        settingsDataStore.appendNameAtStartFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    val showResolution: StateFlow<Boolean> = settingsDataStore.showImageResolutionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

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
        appendName: String,
        resolution: ImageRes,
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

        _compressionConfig.value = CompressionConfig(
            quality = quality,
            maxImageSize = size,
            exportFormat = format,
            appendName = appendName,
            appendNameAtStart = appendNameAtStart.value,
            imageRes = resolution
        )
    }

    fun startCompressService(context: Context, activity: ComponentActivity) {

        compressorServiceBinder.doBindService(context = context)

        if (_compressionStatus.value in listOf(
                ImageCompressorService.COMPRESS_NOT_STARTED,
                ImageCompressorService.COMPRESS_STOPPED,
                ImageCompressorService.COMPRESS_DONE
            )
        ) {
            viewModelScope.launch(context = Dispatchers.IO) {
                compressorServiceBinder.mService.collect { compressorService ->
                    val config = _compressionConfig.value
                    Log.d(TAG, "startCompressService: Started")
                    compressorService?.let { service ->
                        launch {
                            service.toCompress.collect {
                                _toCompress.value = it
                            }
                        }

                        launch {
                            service.compressionStatus.collect {
                                _compressionStatus.value = it
                            }
                        }

                        launch {
                            service.totalCompressed.collect {
                                _totalCompressed.value = it
                            }
                        }

                        launch {
                            service.totalImagePathResolved.collect {
                                _totalImagePathResolved.value = it
                            }
                        }

                        service.setImageCompressor(ImageCompressor(context))
                        service.setCompressConfig(config = config)

                        service.compress(
                            activity = activity,
                            photoSet = photos.value
                        ).join()
                    }
                }
            }
        }
    }

    fun stopCompressService(context: Context): Boolean {
        compressorServiceBinder.cancelJob(context = context)
        return compressorServiceBinder.doUnbindService(context = context)
    }

    fun unbindCompressService(context: Context): Boolean {
        return compressorServiceBinder.doUnbindService(context = context)
    }
}
