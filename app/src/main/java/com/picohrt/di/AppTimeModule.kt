package com.picohrt.di

import com.picohrt.util.AppTimeSource
import com.picohrt.util.DefaultAppTimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppTimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock {
        return Clock.systemUTC()
    }

    @Provides
    @Singleton
    fun provideAppTimeSource(
        clock: Clock,
        @AppScope appScope: CoroutineScope
    ): AppTimeSource {
        return DefaultAppTimeSource(
            clock = clock,
            appScope = appScope
        )
    }
}
