package com.androidkotlin.elinandroidkt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.androidkotlin.elinandroidkt.databinding.ActivityMainBinding
import com.androidkotlin.elinandroidkt.presentation.can.CanViewModel
import com.androidkotlin.elinandroidkt.presentation.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CanViewModel by viewModels()

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadSettings()
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            userPreferencesRepository.userPreferencesFlow.collectLatest { preferences ->
                // 테마 설정 적용
                val nightMode = when (preferences.uiTheme) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)

                // CAN 장치 주소 표시
                updateCanAddressText(preferences.canDeviceAddress)
                Log.d("dd",preferences.canDeviceAddress)

                // 자동 연결 설정에 따라 CAN 통신 시작 (선택적)
                if (preferences.autoConnect) {
                    viewModel.startCanCommunication()
                }

                // 디버그 모드 설정을 UI에 표시 (예시)
                updateDebugVisibility(preferences.isDebugMode)
            }
        }
    }

    private fun updateCanAddressText(address: String) {
        // CAN 장치 주소가 비어있지 않으면 표시, 비어있으면 "설정되지 않음" 표시
        val displayAddress = if (address.isNotBlank()) address else "설정되지 않음"
        binding.canAddressTextView.text = "CAN 장치 주소: $displayAddress"
    }

    private fun updateDebugVisibility(isDebugMode: Boolean) {
        // 디버그 모드일 때만 보여줄 UI 요소가 있다면 여기서 처리
    }

    private fun setupViews() {
        // 테스트 버튼 추가
        binding.testButton.setOnClickListener {
            viewModel.testCanSend()
        }

        // 설정 버튼 추가
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}