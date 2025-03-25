package com.androidkotlin.elinandroidkt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.androidkotlin.elinandroidkt.databinding.ActivityMainBinding

import com.androidkotlin.elinandroidkt.presentation.CanViewModel
import com.androidkotlin.elinandroidkt.presentation.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()


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