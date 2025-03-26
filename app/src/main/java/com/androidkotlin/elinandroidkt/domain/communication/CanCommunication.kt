package com.androidkotlin.elinandroidkt.domain.communication

import com.androidkotlin.elinandroidkt.data.model.CanData
import com.androidkotlin.elinandroidkt.data.model.CommunicationResult
import kotlinx.coroutines.flow.Flow

interface CanCommunication {
    suspend fun connect(): CommunicationResult<Unit>
    suspend fun disconnect(): CommunicationResult<Unit>
    suspend fun sendData(data: CanData): CommunicationResult<Unit>
    suspend fun receiveData(): Flow<CommunicationResult<CanData>>

    suspend fun startReceiving(): CommunicationResult<Unit>
    suspend fun sendPacket(
        id: ByteArray,
        cmd: Byte,
        dataLen: Int,
        data: ByteArray
    ): CommunicationResult<Unit>
    suspend fun stopReceiving(): CommunicationResult<Unit>
}
