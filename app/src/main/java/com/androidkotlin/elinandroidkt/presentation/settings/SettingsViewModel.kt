package com.androidkotlin.elinandroidkt.presentation.settings

import androidx.lifecycle.ViewModel
import com.androidkotlin.elinandroidkt.data.preferences.UserPreferences
import com.androidkotlin.elinandroidkt.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences = userPreferencesRepository.userPreferencesFlow

    suspend fun saveAllPreferences(preferences: UserPreferences) {
        userPreferencesRepository.updateDebugMode(preferences.isDebugMode)
        userPreferencesRepository.updateCanDeviceAddress(preferences.canDeviceAddress)
        userPreferencesRepository.updateAutoConnect(preferences.autoConnect)
        userPreferencesRepository.updateUiTheme(preferences.uiTheme)
        userPreferencesRepository.updateLanguage(preferences.language)
    }


}