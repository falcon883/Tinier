package com.durvank883.tinier.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.durvank883.tinier.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

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


    fun setPhotos(newPhotos: List<Uri>) = viewModelScope.launch {
        _photos.value = _photos.value.plus(newPhotos.map { uri -> Photo(uri) })
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
}
