package com.androidkotlin.elinandroidkt

import com.androidkotlin.elinandroidkt.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun updateDebugMode(isDebugMode: Boolean)
    suspend fun updateCanDeviceAddress(address: String)
    suspend fun updateAutoConnect(autoConnect: Boolean)
    suspend fun updateUiTheme(theme: Int)
    suspend fun updateLanguage(language: String)
}