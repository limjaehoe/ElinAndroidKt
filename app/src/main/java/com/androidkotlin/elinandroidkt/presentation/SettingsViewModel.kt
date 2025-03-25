package com.androidkotlin.elinandroidkt.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.elinandroidkt.data.UserPreferences
import com.androidkotlin.elinandroidkt.domain.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
// SettingsViewModel.kt 수정

class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // 디바운싱을 위한 변수
    private var textChangeJob: Job? = null

    // 마지막으로 가져온 설정값 (불필요한 UI 업데이트 방지용)
    private var lastLoadedPreferences: UserPreferences? = null

    // StateFlow 대신 간단한 지연 초기화 변수 사용
    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences = _userPreferences.asStateFlow()

    init {
        // 앱 시작 시 한 번만 설정 로드
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow
                .collect { preferences ->
                    if (preferences != lastLoadedPreferences) {
                        lastLoadedPreferences = preferences
                        _userPreferences.value = preferences
                    }
                }
        }
    }

    // 디버그 모드 업데이트
    fun updateDebugMode(isDebugMode: Boolean) {
        if (userPreferences.value.isDebugMode != isDebugMode) {
            viewModelScope.launch {
                userPreferencesRepository.updateDebugMode(isDebugMode)
            }
        }
    }

    // CAN 주소 업데이트 (디바운싱 적용)
    fun updateCanDeviceAddress(address: String) {
        textChangeJob?.cancel()
        textChangeJob = viewModelScope.launch {
            delay(500) // 타이핑 후 0.5초 기다림
            if (userPreferences.value.canDeviceAddress != address) {
                userPreferencesRepository.updateCanDeviceAddress(address)
            }
        }
    }

    // 자동 연결 업데이트
    fun updateAutoConnect(autoConnect: Boolean) {
        if (userPreferences.value.autoConnect != autoConnect) {
            viewModelScope.launch {
                userPreferencesRepository.updateAutoConnect(autoConnect)
            }
        }
    }

    // UI 테마 업데이트
    fun updateUiTheme(theme: Int) {
        if (userPreferences.value.uiTheme != theme) {
            viewModelScope.launch {
                userPreferencesRepository.updateUiTheme(theme)
            }
        }
    }

    // 언어 업데이트
    fun updateLanguage(language: String) {
        if (userPreferences.value.language != language) {
            viewModelScope.launch {
                userPreferencesRepository.updateLanguage(language)
            }
        }
    }
}