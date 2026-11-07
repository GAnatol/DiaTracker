package com.knz.diatracker

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knz.diatracker.data.AppDatabase
import com.knz.diatracker.data.DiabetesRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).diabetesDao()


    val recordsList: StateFlow<List<DiabetesRecord>> = dao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val context = application.applicationContext

    // Ключи для настроек
    private object PreferencesKeys {
        val GENERAL_INSULIN = booleanPreferencesKey("show_general_insulin")
        val SHORT_INSULIN = booleanPreferencesKey("show_short_insulin")
        val LONG_INSULIN = booleanPreferencesKey("show_long_insulin")
        val MIXED_INSULIN = booleanPreferencesKey("show_mixed_insulin")
    }

    val uiState: StateFlow<UserSettings> = context.dataStore.data
        .map { preferences ->
            UserSettings(
                general = preferences[PreferencesKeys.GENERAL_INSULIN] ?: true,
                short = preferences[PreferencesKeys.SHORT_INSULIN] ?: false,
                long = preferences[PreferencesKeys.LONG_INSULIN] ?: false,
                mixed = preferences[PreferencesKeys.MIXED_INSULIN] ?: false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings(general = true, short = false, long = false, mixed = false)
        )

    fun toggleGeneralInsulin(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[PreferencesKeys.GENERAL_INSULIN] = value }
        }
    }

    fun toggleShortInsulin(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[PreferencesKeys.SHORT_INSULIN] = value }
        }
    }

    fun toggleLongInsulin(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[PreferencesKeys.LONG_INSULIN] = value }
        }
    }

    fun toggleMixedInsulin(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[PreferencesKeys.MIXED_INSULIN] = value }
        }
    }

    var currentScreen by mutableStateOf(Screen.MAIN)

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }
}

data class UserSettings(
    val general: Boolean,
    val short: Boolean,
    val long: Boolean,
    val mixed: Boolean
)