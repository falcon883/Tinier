package com.durvank883.tinier.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        val THEME_MODE = intPreferencesKey(name = "theme_mode")
        val SHOW_IMAGE_RESOLUTION = booleanPreferencesKey(name = "show_image_resolution")
        val APPEND_NAME_AT_START = booleanPreferencesKey(name = "append_name_at_start")
    }

    private val dataStore: DataStore<Preferences> = context.dataStore

    val themeModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: 0
    }

    suspend fun setThemeMode(mode: Int): Preferences {
        return dataStore.edit { settings ->
            settings[THEME_MODE] = mode
        }
    }

    val showImageResolutionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_IMAGE_RESOLUTION] ?: false
    }

    suspend fun setShowImageResolution(shouldShow: Boolean): Preferences {
        return dataStore.edit { settings ->
            settings[SHOW_IMAGE_RESOLUTION] = shouldShow
        }
    }

    val appendNameAtStartFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[APPEND_NAME_AT_START] ?: false
    }

    suspend fun setAppendNameAtStart(pos: Boolean): Preferences {
        return dataStore.edit { settings ->
            settings[APPEND_NAME_AT_START] = pos
        }
    }
}
