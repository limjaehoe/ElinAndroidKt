package com.androidkotlin.elinandroidkt.presentation

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.androidkotlin.elinandroidkt.R
import com.androidkotlin.elinandroidkt.data.UserPreferences
import com.androidkotlin.elinandroidkt.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        setupListeners()
        observePreferences()
    }

    private fun setupListeners() {
        // 디버그 모드 스위치 - 리스너 제거 후 재설정
        binding.debugModeSwitch.setOnCheckedChangeListener(null)
        binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDebugMode(isChecked)
        }

        // CAN 장치 주소 - 입력 완료 후에만 저장하도록 수정
        binding.canDeviceAddressInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.canDeviceAddressInput.text?.toString() ?: ""
                viewModel.updateCanDeviceAddress(text)
            }
        }

        // 자동 연결 스위치 - 리스너 제거 후 재설정
        binding.autoConnectSwitch.setOnCheckedChangeListener(null)
        binding.autoConnectSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAutoConnect(isChecked)
        }

        // 테마 라디오 버튼 - 리스너 제거 후 재설정
        binding.themeRadioGroup.setOnCheckedChangeListener(null)
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.themeSystemRadio -> 0
                R.id.themeLightRadio -> 1
                R.id.themeDarkRadio -> 2
                else -> 0
            }
            viewModel.updateUiTheme(theme)
        }
    }

    private fun observePreferences() {
        lifecycleScope.launch {
            viewModel.userPreferences
                // distinctUntilChanged() 제거
                .collect { preferences ->
                    // 현재 UI 상태와 수신된 상태가 다를 때만 업데이트
                    updateUiIfNeeded(preferences)
                }
        }
    }

    private fun updateUiIfNeeded(preferences: UserPreferences) {
        // 입력 중이 아닐 때만 EditText 업데이트
        if (!binding.canDeviceAddressInput.hasFocus()) {
            val currentText = binding.canDeviceAddressInput.text?.toString() ?: ""
            if (currentText != preferences.canDeviceAddress) {
                binding.canDeviceAddressInput.setText(preferences.canDeviceAddress)
            }
        }

        // 현재 상태와 다를 때만 스위치 업데이트
        if (binding.debugModeSwitch.isChecked != preferences.isDebugMode) {
            binding.debugModeSwitch.isChecked = preferences.isDebugMode
        }

        if (binding.autoConnectSwitch.isChecked != preferences.autoConnect) {
            binding.autoConnectSwitch.isChecked = preferences.autoConnect
        }

        // 현재 선택된 라디오 버튼과 다를 때만 업데이트
        val selectedId = binding.themeRadioGroup.checkedRadioButtonId
        val expectedId = when (preferences.uiTheme) {
            0 -> R.id.themeSystemRadio
            1 -> R.id.themeLightRadio
            2 -> R.id.themeDarkRadio
            else -> R.id.themeSystemRadio
        }

        if (selectedId != expectedId) {
            binding.themeRadioGroup.check(expectedId)
        }
    }


}