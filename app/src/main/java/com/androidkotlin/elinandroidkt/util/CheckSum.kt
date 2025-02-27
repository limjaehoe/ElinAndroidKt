package com.androidkotlin.elinandroidkt.util

import javax.inject.Inject

/**
 * CRC-16 Kermit 체크섬 계산을 위한 유틸리티 클래스
 * 레거시 CheckSum 클래스를 코틀린으로 변환한 버전
 */
class CheckSum @Inject constructor()  {

    /**
     * CRC-16 Kermit 알고리즘을 사용하여 체크섬 계산
     *
     * @param data 체크섬을 계산할 데이터 바이트 배열
     * @return 계산된 체크섬 (16비트 값)
     */
    fun calculateCRC(data: ByteArray): Int {
        var crc = 0x0000

        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0..7) {
                if (crc and 0x0001 != 0) {
                    crc = crc shr 1
                    crc = crc xor 0x8408  // 0x8408 = 역순 다항식 0x1021의 표현
                } else {
                    crc = crc shr 1
                }
            }
        }

        return crc
    }

    /**
     * 데이터를 16진수 문자열로 변환
     *
     * @param data 변환할 바이트 배열
     * @return 16진수 문자열 (공백으로 구분됨)
     */
    fun bytesToHexString(data: ByteArray): String {
        return data.joinToString(" ") {
            String.format("%02X", it)
        }
    }
}