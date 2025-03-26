// 새로운 SettingsActivity 구현 - 명시적인 저장 방식 사용

package com.androidkotlin.elinandroidkt.presentation.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidkotlin.elinandroidkt.R
import com.androidkotlin.elinandroidkt.data.preferences.UserPreferences
import com.androidkotlin.elinandroidkt.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    // 메모리에 임시 저장할 현재 설정값
    private var currentPreferences = UserPreferences()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            saveSettings()
            finish()
        }

        setupListeners()
        loadSettings()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveSettings()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupListeners() {
        // 컨트롤에 대한 리스너 설정 - 임시 변수에만 저장
        binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentPreferences = currentPreferences.copy(isDebugMode = isChecked)
        }

        binding.autoConnectSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentPreferences = currentPreferences.copy(autoConnect = isChecked)
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.themeSystemRadio -> 0
                R.id.themeLightRadio -> 1
                R.id.themeDarkRadio -> 2
                else -> 0
            }
            currentPreferences = currentPreferences.copy(uiTheme = theme)
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            // 저장된 설정 로드
            val preferences = viewModel.userPreferences.first()
            currentPreferences = preferences

            // UI 업데이트
            updateUI(preferences)
        }
    }

    private fun updateUI(preferences: UserPreferences) {
        binding.debugModeSwitch.isChecked = preferences.isDebugMode
        binding.canDeviceAddressInput.setText(preferences.canDeviceAddress)
        binding.autoConnectSwitch.isChecked = preferences.autoConnect

        val radioButtonId = when (preferences.uiTheme) {
            0 -> R.id.themeSystemRadio
            1 -> R.id.themeLightRadio
            2 -> R.id.themeDarkRadio
            else -> R.id.themeSystemRadio
        }
        binding.themeRadioGroup.check(radioButtonId)
    }

    private fun saveSettings() {
        // 텍스트 입력 값 가져오기 (이 값은 이벤트 리스너가 없으므로 수동으로 가져와야 함)
        val deviceAddress = binding.canDeviceAddressInput.text?.toString() ?: ""
        currentPreferences = currentPreferences.copy(canDeviceAddress = deviceAddress)

        // 설정 저장
        lifecycleScope.launch {
            viewModel.saveAllPreferences(currentPreferences)
        }
    }
}