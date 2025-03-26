package com.androidkotlin.elinandroidkt.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.androidkotlin.elinandroidkt.data.model.CanData
import com.androidkotlin.elinandroidkt.data.model.CanUsbConstants
import com.androidkotlin.elinandroidkt.data.model.CommunicationResult
import com.androidkotlin.elinandroidkt.domain.communication.CanCommunication
import com.androidkotlin.elinandroidkt.util.CheckSum
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CanRepository"

@Singleton
class CanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val checkSum: CheckSum
) : CanCommunication {

    // USB 관련 필드들
    private val usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private var connection: UsbDeviceConnection? = null
    private var device: UsbDevice? = null
    private var interface_: UsbInterface? = null
    private var epIN: UsbEndpoint? = null
    private var epOUT: UsbEndpoint? = null

    // 데이터 스트림을 위한 Flow
    private val _canDataFlow = MutableSharedFlow<CommunicationResult<CanData>>()

    // 수신 작업을 위한 코루틴 스코프
    private var receiveScope = CoroutineScope(ioDispatcher + SupervisorJob())

    // 연결 상태
    private var isConnected = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == CanUsbConstants.ACTION_USB_PERMISSION) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.let { setupDevice(it) }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(CanUsbConstants.ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
    }

    override suspend fun connect(): CommunicationResult<Unit> = withContext(ioDispatcher) {
        try {
            if (isConnected) {
                return@withContext CommunicationResult.Success(Unit)
            }

            usbManager.deviceList.values.find {
                it.vendorId == CanUsbConstants.VID && it.productId == CanUsbConstants.PID
            }?.let { device ->
                if (!usbManager.hasPermission(device)) {
                    val permissionIntent = PendingIntent.getBroadcast(
                        context, 0,
                        Intent(CanUsbConstants.ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    usbManager.requestPermission(device, permissionIntent)
                    // 권한 요청 후 성공 여부는 BroadcastReceiver에서 처리됨
                    CommunicationResult.Loading
                } else {
                    setupDevice(device)
                    CommunicationResult.Success(Unit)
                }
            } ?: CommunicationResult.Error(Exception("USB CAN 디바이스를 찾을 수 없습니다"))
        } catch (e: Exception) {
            Log.e(TAG, "연결 오류", e)
            CommunicationResult.Error(e)
        }
    }

    private fun setupDevice(device: UsbDevice) {
        this.device = device
        interface_ = device.getInterface(0)
        connection = usbManager.openDevice(device)
        connection?.claimInterface(interface_, true)

        // 엔드포인트 찾기
        for (i in 0 until (interface_?.endpointCount ?: 0)) {
            val endpoint = interface_?.getEndpoint(i) ?: continue

            if (endpoint.direction == UsbConstants.USB_DIR_OUT &&
                endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                epOUT = endpoint
                Log.d(TAG, "OUT 엔드포인트 찾음: $epOUT")
            }

            if (endpoint.direction == UsbConstants.USB_DIR_IN &&
                endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                epIN = endpoint
                Log.d(TAG, "IN 엔드포인트 찾음: $epIN")
            }
        }

        if (epIN != null && epOUT != null) {
            isConnected = true
            Log.d(TAG, "USB CAN 디바이스 설정 완료")
        } else {
            Log.e(TAG, "엔드포인트를.찾을 수 없음")
        }
    }

    /**
     * 데이터 수신 시작
     */
    override suspend fun startReceiving(): CommunicationResult<Unit> = withContext(ioDispatcher) {
        try {
            if (!isConnected || connection == null || epIN == null) {
                return@withContext CommunicationResult.Error(Exception("USB 연결이 설정되지 않았습니다"))
            }

            // 기존 수신 코루틴 취소 후 새로 시작
            receiveScope.cancel()
            receiveScope = CoroutineScope(ioDispatcher + SupervisorJob())

            // 데이터 수신 코루틴 시작
            receiveScope.launch {
                val buffer = ByteArray(128)

                while (isActive) {
                    connection?.let { conn ->
                        epIN?.let { endpoint ->
                            val bytesRead = conn.bulkTransfer(endpoint, buffer, buffer.size, 0)

                            if (bytesRead > 2) {
                                // 유효한 데이터가 있으면 처리
                                val dataChunk = ByteArray(bytesRead)
                                System.arraycopy(buffer, 0, dataChunk, 0, bytesRead)

                                // 데이터 처리
                                processReceivedPacket(dataChunk)
                            }
                        }
                    } ?: break

                    // 짧은 지연으로 CPU 사용량 감소
                    delay(1)
                }
            }

            CommunicationResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "데이터 수신 시작 오류", e)
            CommunicationResult.Error(e)
        }
    }

    /**
     * 수신된 CAN 패킷 처리
     */
    private suspend fun processReceivedPacket(rawPacket: ByteArray) {
        try {
            // 패킷 형식 검증
            if (rawPacket.size < 3) return

            // 패킷 구조 분석
            val packetLen = rawPacket[2].toInt()

            // 처리할 데이터 추출
            val processBuffer = ByteArray(packetLen)
            System.arraycopy(rawPacket, 3, processBuffer, 0, packetLen)

            // CAN 메시지 파싱
            val canId = ((processBuffer[0].toInt() shl 24) +
                    (processBuffer[1].toInt() shl 16) +
                    (processBuffer[2].toInt() shl 8) +
                    (processBuffer[3].toInt() and 0xff))

            val dlc = processBuffer[5].toInt() and 0xff
            val cmd = processBuffer[7].toInt() and 0xff

            // 장치 유형에 따라 다른 처리
            val canData = if (canId == 256 || canId == 2032 || canId == 2033) {
                // Collimator 패킷 처리
                val dataColli = ByteArray(dlc)
                for (i in 0 until dlc) {
                    dataColli[i] = (processBuffer[6 + i].toInt() and 0xff).toByte()
                }

                CanData(
                    id = canId,
                    cmd = cmd,
                    dlc = dlc,
                    dataColli = dataColli
                )
            } else {
                // 일반 패킷 처리
                val dataLen = (dlc - 2).coerceAtLeast(0)
                val data = ByteArray(dataLen)

                for (i in 0 until dataLen) {
                    data[i] = (processBuffer[8 + i].toInt() and 0xff).toByte()
                }

                CanData(
                    id = canId,
                    cmd = cmd,
                    dlc = dlc,
                    data = data
                )
            }

            // 로깅
            Log.d(TAG, "수신 데이터 - ID: 0x${Integer.toHexString(canId)}, CMD: 0x${Integer.toHexString(cmd)}, DLC: $dlc")

            // 결과 Flow로 전송
            _canDataFlow.emit(CommunicationResult.Success(canData))
        } catch (e: Exception) {
            Log.e(TAG, "패킷 처리 오류", e)
            _canDataFlow.emit(CommunicationResult.Error(Exception("패킷 처리 오류: ${e.message}", e)))
        }
    }

    /**
     * CAN 패킷 생성 및 전송
     */
    override suspend fun sendPacket(
        id: ByteArray,
        cmd: Byte,
        dataLen: Int,
        data: ByteArray
    ): CommunicationResult<Unit> = withContext(ioDispatcher) {
        try {
            if (!isConnected || connection == null || epOUT == null) {
                return@withContext CommunicationResult.Error(Exception("USB 연결이 설정되지 않았습니다"))
            }

            val sendPacket = ByteArray(20)
            val calcChecksum = ByteArray(dataLen + 8)

            // 패킷 구성
            sendPacket[0] = 0x00                           // High Command (미사용)
            sendPacket[1] = 0x00                           // PC/Android -> CAN Converter
            sendPacket[2] = ((dataLen + 10) and 0xFF).toByte() // Length(전체 Frame 길이, Max16)

            // ID 설정 (4 bytes)
            sendPacket[3] = id[0]                          // ID(상위)
            sendPacket[4] = id[1]
            sendPacket[5] = id[2]
            sendPacket[6] = id[3]                          // ID(하위)

            sendPacket[7] = 0x00                           // CAN Data/Remote 구분(0x00 고정)
            sendPacket[8] = ((dataLen + 2) and 0xFF).toByte() // Can Data의 길이, Max 8

            sendPacket[9] = ((dataLen + 1) and 0xFF).toByte()   // Data길이 (Single Frame First Byte)
            sendPacket[10] = cmd                           // Command (Single Frame Second Byte)

            // 체크섬 계산을 위한 데이터 준비
            for (i in 0..7) {
                calcChecksum[i] = sendPacket[i + 3]
            }

            // 데이터 복사
            for (i in 0 until dataLen) {
                sendPacket[11 + i] = data[i]
                calcChecksum[8 + i] = data[i]
            }

            // 체크섬 계산 및 추가
            val checksum = checkSum.calculateCRC(calcChecksum)
            sendPacket[dataLen + 11] = ((checksum shr 8) and 0xff).toByte()  // Checksum(상위)
            sendPacket[dataLen + 12] = (checksum and 0xff).toByte()          // Checksum(하위)

            // 데이터 전송
            val result = connection?.bulkTransfer(epOUT, sendPacket, 20, CanUsbConstants.TIMEOUT)

            // 로깅
            val packetHex = sendPacket.joinToString(" ") {
                String.format("%02X", it)
            }
            Log.d(TAG, "전송 패킷: $packetHex (결과: $result)")

            // 약간의 지연 (안정성을 위해)
            delay(2)

            if (result != null && result >= 0) {
                CommunicationResult.Success(Unit)
            } else {
                CommunicationResult.Error(Exception("패킷 전송 실패: $result"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "패킷 전송 오류", e)
            CommunicationResult.Error(e)
        }
    }

    /**
     * 장치 ID, 명령어 및 데이터를 사용하여 CAN 데이터 전송
     */
    override suspend fun sendData(data: CanData): CommunicationResult<Unit> = withContext(ioDispatcher) {
        try {
            // ID를 바이트 배열로 변환
            val idBytes = byteArrayOf(
                ((data.id shr 24) and 0xff).toByte(),
                ((data.id shr 16) and 0xff).toByte(),
                ((data.id shr 8) and 0xff).toByte(),
                (data.id and 0xff).toByte()
            )

            // Command를 바이트로 변환
            val cmdByte = (data.cmd and 0xff).toByte()

            // 패킷 전송
            return@withContext sendPacket(idBytes, cmdByte, data.data.size, data.data)
        } catch (e: Exception) {
            Log.e(TAG, "데이터 전송 오류", e)
            CommunicationResult.Error(e)
        }
    }

    /**
     * 축 제한 값을 바이트 배열로 변환
     */
    fun getAxisLimitBytes(axis: Int, axisMax: Int, axisMin: Int): ByteArray {
        val axisMaxHigh = (axisMax shr 8)
        val axisMaxLow = axisMax and 0xff

        val axisMinHigh = (axisMin shr 8)
        val axisMinLow = axisMin and 0xff

        return byteArrayOf(
            axis.toByte(),
            axisMaxHigh.toByte(),
            axisMaxLow.toByte(),
            0x00, // 고정값
            axisMinHigh.toByte(),
            axisMinLow.toByte()
        )
    }

    /**
     * 수신 중지
     */
    override suspend fun stopReceiving(): CommunicationResult<Unit> = withContext(ioDispatcher) {
        try {
            receiveScope.cancel()
            CommunicationResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "수신 중지 오류", e)
            CommunicationResult.Error(e)
        }
    }

    /**
     * 연결 해제
     */
    override suspend fun disconnect(): CommunicationResult<Unit> = withContext(ioDispatcher) {
        try {
            // 수신 코루틴 취소
            receiveScope.cancel()

            // USB 연결 해제
            connection?.releaseInterface(interface_)
            connection?.close()

            // 필드 초기화
            connection = null
            device = null
            interface_ = null
            epIN = null
            epOUT = null
            isConnected = false

            CommunicationResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "연결 해제 오류", e)
            CommunicationResult.Error(e)
        }
    }

    /**
     * 데이터 수신 Flow 반환
     */
    override suspend fun receiveData(): Flow<CommunicationResult<CanData>> = _canDataFlow

    /**
     * 리소스 정리 (ViewModel onCleared 또는 앱 종료 시 호출)
     */
    fun cleanup() {
        try {
            receiveScope.cancel()
            context.unregisterReceiver(usbReceiver)

            connection?.releaseInterface(interface_)
            connection?.close()

            connection = null
            device = null
            interface_ = null
            epIN = null
            epOUT = null
            isConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "리소스 정리 오류", e)
        }
    }
}