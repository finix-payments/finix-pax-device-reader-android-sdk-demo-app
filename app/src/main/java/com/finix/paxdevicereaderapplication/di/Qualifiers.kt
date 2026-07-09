package com.finix.paxdevicereaderapplication.di

import javax.inject.Qualifier

/** Qualifies the IO [kotlinx.coroutines.CoroutineDispatcher] used for disk and network work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
