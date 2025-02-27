package com.androidkotlin.elinandroidkt.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Elin 시스템 기능 관리 클래스
 * 레거시 ElinSystem 클래스
 */
@Singleton
class ElinSystem @Inject constructor() {

    companion object {
        // 상수
        var can_receive_count = 0
        var _canConnect = false
    }

    // 이벤트 Flow
    private val _pmChangedFlow = MutableSharedFlow<PmChangeResult>()
    val pmChangedFlow: SharedFlow<PmChangeResult> = _pmChangedFlow

    // Collimator 처리 객체
    val _colli: CollimatorProcessor = CollimatorProcessor()

    /**
     * CAN 연결 상태 설정
     */
    fun setCanConnectionStatus(isConnected: Boolean) {
        _canConnect = isConnected
    }

    /**
     * 수신된 패킷 처리
     */
    fun processReceivedData(deviceType: DeviceType, cmdType: Int, data: CharArray) {
        // 명령어 타입 매핑
        val cmd = CanCmd.values().find { it.value == cmdType } ?: CanCmd.UNKNOWN

        // 장치 타입에 따라 처리
        when (deviceType) {
            DeviceType.CEILING -> processCeilingData(cmd, data)
            DeviceType.STAND -> processStandData(cmd, data)
            DeviceType.TABLE -> processTableData(cmd, data)
            DeviceType.COLLIMATOR -> _colli.processData(data)
            DeviceType.ZIGBEE -> processZigbeeData(data)
            else -> {} // 처리하지 않음
        }
    }

    /**
     * Ceiling 데이터 처리
     */
    private fun processCeilingData(cmd: CanCmd, data: CharArray) {
        when (cmd) {
            CanCmd.PM_VALUE, CanCmd.STOP_PM_VALUE -> {
                // PM 값 처리 (위치 정보)
                // 이 값은 PM 필터로 이미 처리됨
            }
            CanCmd.KEY_VALUE -> {
                // 키 입력 처리
                processKeyData(DeviceType.CEILING, data)
            }
            CanCmd.MOTOR_STATUS -> {
                // 모터 상태 처리
                processMotorStatus(DeviceType.CEILING, data)
            }
            CanCmd.SENSOR_STATUS -> {
                // 센서 상태 처리
                processSensorStatus(DeviceType.CEILING, data)
            }
            else -> {
                // 기타 명령어 처리
            }
        }
    }

    /**
     * Stand 데이터 처리
     */
    private fun processStandData(cmd: CanCmd, data: CharArray) {
        // 구현 내용 유사
    }

    /**
     * Table 데이터 처리
     */
    private fun processTableData(cmd: CanCmd, data: CharArray) {
        // 구현 내용 유사
    }




    /**
     * Zigbee 데이터 처리
     */
    fun processZigbeeData(data: CharArray) {
        // Zigbee 데이터 처리
    }

    /**
     * 리모컨 키 데이터 처리
     */
    fun onRemoteControlKeyData(data: CharArray) {
        // 리모컨 키 입력 처리
    }

    /**
     * 풋스위치 키 데이터 처리
     */
    fun onRemoteFootKeyData(data: CharArray) {
        // 풋스위치 키 입력 처리
    }

    /**
     * 키 입력 처리
     */
    private fun processKeyData(deviceType: DeviceType, data: CharArray) {
        // 키 입력 처리 로직
    }

    /**
     * 모터 상태 처리
     */
    private fun processMotorStatus(deviceType: DeviceType, data: CharArray) {
        // 모터 상태 처리 로직
    }

    /**
     * 센서 상태 처리
     */
    private fun processSensorStatus(deviceType: DeviceType, data: CharArray) {
        // 센서 상태 처리 로직
    }

    /**
     * 유닛 타입 가져오기
     */
    fun getUnitType(deviceType: DeviceType, data: CharArray, cmdType: Int): Int {
        // 축 정보 추출
        val axis = data[0].code

        // 장치 타입에 따라 유닛 타입 결정
        return when (deviceType) {
            DeviceType.CEILING -> {
                when (axis) {
                    1 -> 1  // X축
                    2 -> 2  // Y축
                    3 -> 3  // Z축
                    4 -> 4  // A축
                    else -> 0
                }
            }
            DeviceType.STAND -> {
                when (axis) {
                    3 -> 5  // Z축
                    4 -> 6  // A축
                    else -> 0
                }
            }
            DeviceType.TABLE -> {
                when (axis) {
                    1 -> 7  // X축
                    3 -> 8  // Z축
                    else -> 0
                }
            }
            else -> 0
        }
    }

    /**
     * CAN 전송 테스트
     */
    fun canTransferTest() {
        // CAN 전송 테스트 로직
    }

    /**
     * Collimator 처리를 위한 내부 클래스
     */
    inner class CollimatorProcessor {
        /**
         * Collimator 데이터 처리
         */
        fun processData(data: CharArray) {
            // Collimator 데이터 처리 로직
        }

        /**
         * Collimator 데이터 처리 (바이트 배열)
         */
        fun processDataColli(data: CharArray) {
            // Collimator 데이터 처리 로직
        }
    }
}

/**
 * CAN 명령어 열거형
 */
enum class CanCmd(val value: Int) {
    UNKNOWN(-1),
    STATUS_DATA(0x00),
    PM_VALUE(0x02),
    STOP_PM_VALUE(0x06),
    KEY_VALUE(0x03),
    MOTOR_STATUS(0x08),
    SENSOR_STATUS(0x09),
    VERSION_INFO(0x3E),
    ZIGBEE_KEY_VALUE(0x0A);

    companion object {
        fun fromValue(value: Int): CanCmd {
            return values().find { it.value == value } ?: UNKNOWN
        }
    }
}