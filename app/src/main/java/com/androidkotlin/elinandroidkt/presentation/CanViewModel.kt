package com.androidkotlin.elinandroidkt.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidkotlin.elinandroidkt.data.CanData
import com.androidkotlin.elinandroidkt.data.CanRepository
import com.androidkotlin.elinandroidkt.data.CommunicationResult
import com.androidkotlin.elinandroidkt.domain.CanCommunication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CanViewModel @Inject constructor(
    //private val canRepository: CanRepository
    private val canCommunication: CanCommunication
) : ViewModel() {

    private val _connectionState = MutableStateFlow<CommunicationResult<Unit>>(CommunicationResult.Loading)
    val connectionState: StateFlow<CommunicationResult<Unit>> = _connectionState.asStateFlow()

    private val _canData = MutableStateFlow<CanData?>(null)
    val canData: StateFlow<CanData?> = _canData.asStateFlow()

    fun startCanCommunication() {
        viewModelScope.launch {
            _connectionState.value = CommunicationResult.Loading

            when (val result = canCommunication.connect()) {
                is CommunicationResult.Success -> {
                    _connectionState.value = result
                    startDataCollection()
                }
                is CommunicationResult.Error -> {
                    _connectionState.value = result
                }
                else -> Unit
            }
        }
    }

    private fun startDataCollection() {
        viewModelScope.launch {
            canCommunication.receiveData().collect { result ->
                when (result) {
                    is CommunicationResult.Success -> {
                        _canData.value = result.data
                    }
                    is CommunicationResult.Error -> {
                        // 에러 처리
                    }
                    else -> Unit
                }
            }
        }
    }

    // 테스트용 함수
    fun testCanSend() {
        viewModelScope.launch {
            // 1. USB-CAN 장치 연결 시도
            when (val connectResult = canCommunication.connect()) {
                is CommunicationResult.Success -> {

                    // 2. 연결 성공시 테스트 데이터 전송
                    val canData = CanData(
                        id = 0x040,  // Ceiling 기본
                        cmd = 0x00,  // Command
                        dlc = 6,     // 데이터 길이
                        data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
                    )

                    // 3. 데이터 전송 시도
                    when (val sendResult = canCommunication.sendData(canData)) {
                        is CommunicationResult.Success -> {
                            Log.d("CAN_TEST", "Data sent successfully")
                        }
                        is CommunicationResult.Error -> {
                            Log.e("CAN_TEST", "Send failed", sendResult.exception)
                        }
                        else -> Unit
                    }
                }
                is CommunicationResult.Error -> {
                    Log.e("CAN_TEST", "Connection failed", connectResult.exception)
                }
                else -> Unit
            }
        }
    }


}