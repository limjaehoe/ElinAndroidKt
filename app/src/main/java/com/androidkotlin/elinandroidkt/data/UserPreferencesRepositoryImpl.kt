package com.androidkotlin.elinandroidkt.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.androidkotlin.elinandroidkt.domain.UserPreferencesRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Context의 확장 프로퍼티로 DataStore 정의
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    // 키 정의
    private object PreferencesKeys {
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val CAN_DEVICE_ADDRESS = stringPreferencesKey("can_device_address")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val UI_THEME = intPreferencesKey("ui_theme")
        val LANGUAGE = stringPreferencesKey("language")
    }

    // 사용자 환경설정을 Flow로 제공
    override val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isDebugMode = preferences[PreferencesKeys.DEBUG_MODE] ?: false
            val canDeviceAddress = preferences[PreferencesKeys.CAN_DEVICE_ADDRESS] ?: ""
            val autoConnect = preferences[PreferencesKeys.AUTO_CONNECT] ?: true
            val uiTheme = preferences[PreferencesKeys.UI_THEME] ?: 0
            val language = preferences[PreferencesKeys.LANGUAGE] ?: "ko"

            UserPreferences(
                isDebugMode = isDebugMode,
                canDeviceAddress = canDeviceAddress,
                autoConnect = autoConnect,
                uiTheme = uiTheme,
                language = language
            )
        }

    // 설정 업데이트 메서드들
    override suspend fun updateDebugMode(isDebugMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEBUG_MODE] = isDebugMode
        }
    }

    override suspend fun updateCanDeviceAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CAN_DEVICE_ADDRESS] = address
        }
    }

    override suspend fun updateAutoConnect(autoConnect: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CONNECT] = autoConnect
        }
    }

    override suspend fun updateUiTheme(theme: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UI_THEME] = theme
        }
    }

    override suspend fun updateLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
}