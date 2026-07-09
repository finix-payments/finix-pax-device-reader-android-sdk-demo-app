package com.finix.paxdevicereaderapplication.di

import com.finix.paxdevicereaderapplication.data.repository.ConfigRepository
import com.finix.paxdevicereaderapplication.data.repository.DefaultConfigRepository
import com.finix.paxdevicereaderapplication.device.FinixTerminalDeviceFactory
import com.finix.paxdevicereaderapplication.device.TerminalDeviceFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/** Provides concrete dependencies that cannot be constructor-injected directly. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideTerminalDeviceFactory(): TerminalDeviceFactory = FinixTerminalDeviceFactory()
}

/** Binds interface types to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: DefaultConfigRepository): ConfigRepository
}
