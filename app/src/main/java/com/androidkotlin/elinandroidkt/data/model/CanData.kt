package com.androidkotlin.elinandroidkt.data.model

data class CanData(
    val id: Int,
    val cmd: Int,
    val dlc: Int,
    val data: ByteArray = ByteArray(6),       // frame data length, cmd, data(1~6)
    val dataColli: ByteArray = ByteArray(8)   // only collimator use data(1~8)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CanData

        if (id != other.id) return false
        if (cmd != other.cmd) return false
        if (dlc != other.dlc) return false
        if (!data.contentEquals(other.data)) return false
        if (!dataColli.contentEquals(other.dataColli)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + cmd
        result = 31 * result + dlc
        result = 31 * result + data.contentHashCode()
        result = 31 * result + dataColli.contentHashCode()
        return result
    }
}

// USB 관련 상수들
object CanUsbConstants {
    const val VID = 0x2542
    const val PID = 0x1020
    const val TIMEOUT = 3000
    const val ACTION_USB_PERMISSION = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
}

// 통신 결과를 래핑하는 sealed class
sealed class CommunicationResult<out T> {
    data class Success<T>(val data: T) : CommunicationResult<T>()
    data class Error(val exception: Exception) : CommunicationResult<Nothing>()
    object Loading : CommunicationResult<Nothing>()
}