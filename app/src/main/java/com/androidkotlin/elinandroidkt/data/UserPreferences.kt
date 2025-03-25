package com.androidkotlin.elinandroidkt.data

data class UserPreferences(
    val isDebugMode: Boolean = false,
    val canDeviceAddress: String = "",
    val autoConnect: Boolean = true,
    val uiTheme: Int = 0, // 0: 시스템 기본, 1: 라이트 모드, 2: 다크 모드
    val language: String = "ko" // 언어 설정 (ko: 한국어, en: 영어)
)