package com.example.mavlinkapplication.di

import com.example.mavlinkapplication.domain.ArmRepository
import com.example.mavlinkapplication.domain.ModeRepository
import com.example.mavlinkapplication.domain.ParamRepository
import com.example.mavlinkapplication.domain.TelemetryRepository
import com.example.mavlinkapplication.mavsdk.MavsdkArmRepository
import com.example.mavlinkapplication.mavsdk.MavsdkModeRepository
import com.example.mavlinkapplication.mavsdk.MavsdkParamRepository
import com.example.mavlinkapplication.mavsdk.MavsdkTelemetryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindTelemetry(impl: MavsdkTelemetryRepository): TelemetryRepository

    @Binds @Singleton
    abstract fun bindParams(impl: MavsdkParamRepository): ParamRepository

    @Binds @Singleton
    abstract fun bindMode(impl: MavsdkModeRepository): ModeRepository

    @Binds @Singleton
    abstract fun bindArm(impl: MavsdkArmRepository): ArmRepository
}
