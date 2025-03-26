package com.androidkotlin.elinandroidkt.di

import com.androidkotlin.elinandroidkt.data.CanRepository
import com.androidkotlin.elinandroidkt.domain.communication.CanCommunication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunicationModule {

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideCanCommunication(
        canRepository: CanRepository
    ): CanCommunication = canRepository
}