package com.androidkotlin.elinandroidkt.domain.communication

import com.androidkotlin.elinandroidkt.data.model.CommunicationResult
import kotlinx.coroutines.flow.Flow

interface TcpCommunication {
    suspend fun startServer(port: Int): CommunicationResult<Unit>
    suspend fun stopServer(): CommunicationResult<Unit>
    suspend fun sendMessage(message: String): CommunicationResult<Unit>
    suspend fun receiveMessage(): Flow<CommunicationResult<String>>
}