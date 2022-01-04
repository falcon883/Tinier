package com.durvank883.tinier.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.durvank883.tinier.prefs.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _isCleaningCache: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    val isCleaningCache: StateFlow<Boolean> = _isCleaningCache.asStateFlow()

    val themeMode: StateFlow<Int> = settingsDataStore.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 0
    )

    val appendNameAtStart: StateFlow<Boolean> = settingsDataStore.appendNameAtStartFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val showResolution: StateFlow<Boolean> = settingsDataStore.showImageResolutionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    fun setThemeMode(mode: Int) = viewModelScope.launch {
        settingsDataStore.setThemeMode(mode = mode)
    }

    fun setAppendNameAtStart(pos: Boolean) = viewModelScope.launch {
        settingsDataStore.setAppendNameAtStart(pos = pos)
    }

    fun setShowResolution(shouldShow: Boolean) = viewModelScope.launch {
        settingsDataStore.setShowImageResolution(shouldShow = shouldShow)
    }

    fun clearCacheDir(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            _isCleaningCache.value = true
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Some error occurred while cleaning cache.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } finally {
            _isCleaningCache.value = false
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Cached cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }
}