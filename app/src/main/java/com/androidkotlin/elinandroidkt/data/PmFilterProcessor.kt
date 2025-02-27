package com.androidkotlin.elinandroidkt.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * PM(Potentiometer) 값 필터링 및 처리를 위한 클래스
 * 레거시 코드의 pmFilter 로직을 모던 안드로이드 아키텍처에 맞게 재구현
 */
@Singleton
class PmFilterProcessor @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher,
    private val elinSystem: ElinSystem
) {
    // PM 값 변경 이벤트를 위한 Flow
    private val _pmChangedFlow = MutableSharedFlow<PmChangeResult>()
    val pmChangedFlow: SharedFlow<PmChangeResult> = _pmChangedFlow

    // 이전 PM 값 저장 변수
    private var preTubeXPm = 0
    private var preTubeYPm = 0
    private var preTubeZPm = 0
    private var preTubeAPm = 0
    private var preStandAPm = 0
    private var preStandZPm = 0
    private var preTableXPm = 0
    private var preTableZPm = 0

    /**
     * PM 값 필터링 및 처리 메서드
     *
     * @param deviceType 장치 타입 (Ceiling, Stand, Table 등)
     * @param data PM 데이터가 포함된 바이트 배열
     * @param commandType 명령어 타입 (PmValue, StopPmValue 등)
     */
    suspend fun processPmData(deviceType: DeviceType, data: ByteArray, commandType: Int): PmChangeResult =
        withContext(ioDispatcher) {
            // 축 정보와 값 추출
            val axis = data[0].toInt() and 0xFF
            val valueHigh = data[1].toInt() and 0xFF
            val valueLow = data[2].toInt() and 0xFF
            val value = (valueHigh shl 8) or valueLow

            var processData = false

            // 현재 PM 값 초기화 (기존 값으로)
            var tubeXPm = preTubeXPm
            var tubeYPm = preTubeYPm
            var tubeZPm = preTubeZPm
            var tubeAPm = preTubeAPm

            var standZPm = preStandZPm
            var standAPm = preStandAPm

            var tableXPm = preTableXPm
            var tableZPm = preTableZPm

            // 장치 타입에 따른 분기 처리
            when (deviceType) {
                DeviceType.CEILING -> {
                    when (axis) {
                        1 -> tubeXPm = value
                        2 -> tubeYPm = value
                        3 -> tubeZPm = value
                        4 -> tubeAPm = value
                    }

                    // PM 값 변화 여부 확인
                    processData = isPmVariation(preTubeXPm, tubeXPm) ||
                            isPmVariation(preTubeYPm, tubeYPm) ||
                            isPmVariation(preTubeZPm, tubeZPm) ||
                            isPmVariation(preTubeAPm, tubeAPm)

                    // 변화가 있으면 값 업데이트
                    if (processData) {
                        preTubeXPm = tubeXPm
                        preTubeYPm = tubeYPm
                        preTubeZPm = tubeZPm
                        preTubeAPm = tubeAPm
                    }
                }

                DeviceType.STAND -> {
                    when (axis) {
                        3 -> standZPm = value
                        4 -> standAPm = value
                    }

                    // PM 값 변화 여부 확인
                    processData = isPmVariation(preStandZPm, standZPm) ||
                            isPmVariation(preStandAPm, standAPm)

                    // 값 업데이트 (항상)
                    preStandZPm = standZPm
                    preStandAPm = standAPm
                }

                DeviceType.TABLE -> {
                    when (axis) {
                        1 -> tableXPm = value
                        3 -> tableZPm = value
                    }

                    // PM 값 변화 여부 확인
                    processData = isPmVariation(preTableXPm, tableXPm) ||
                            isPmVariation(preTableZPm, tableZPm)

                    // 값 업데이트 (항상)
                    preTableXPm = tableXPm
                    preTableZPm = tableZPm
                }

                else -> {
                    // 다른 장치 타입은 처리하지 않음
                }
            }

            // 로깅
            val logMessage = "PM Filter - Device: $deviceType, Axis: $axis, Value: $value, Process: $processData"

            // 처리할 데이터가 있는 경우
            if (processData) {
                // CanData 객체로 변환
                val charData = charArrayOf(
                    axis.toChar(),
                    valueHigh.toChar(),
                    valueLow.toChar()
                )

                // ElinSystem에 데이터 전달
                elinSystem.processReceivedData(deviceType, commandType, charData)

                // 유닛 타입 가져오기
                val unitType = elinSystem.getUnitType(deviceType, charData, commandType)

                // 결과 생성
                val result = PmChangeResult.Success(
                    deviceType = deviceType,
                    axis = axis,
                    value = value,
                    unitType = unitType
                )

                // Flow로 결과 전달
                _pmChangedFlow.emit(result)

                return@withContext result
            } else {
                // 처리할 데이터가 없는 경우
                return@withContext PmChangeResult.NoChange
            }
        }

    /**
     * PM 값 변화가 의미있는지 확인
     *
     * @param prevValue 이전 값
     * @param currentValue 현재 값
     * @return 의미있는 변화가 있으면 true
     */
    private fun isPmVariation(prevValue: Int, currentValue: Int): Boolean {
        return abs(prevValue - currentValue) > 1
    }
}

/**
 * PM 값 변경 결과를 나타내는 sealed class
 */
sealed class PmChangeResult {
    data class Success(
        val deviceType: DeviceType,
        val axis: Int,
        val value: Int,
        val unitType: Int
    ) : PmChangeResult()

    object NoChange : PmChangeResult()

    data class Error(val exception: Exception) : PmChangeResult()
}

/**
 * 장치 타입 열거형
 */
enum class DeviceType(val value: Int) {
    UNKNOWN(0),
    CEILING(1),
    STAND(2),
    TABLE(3),
    COLLIMATOR(4),
    ZIGBEE(5);

    companion object {
        fun fromValue(value: Int): DeviceType {
            return values().find { it.value == value } ?: UNKNOWN
        }
    }
}