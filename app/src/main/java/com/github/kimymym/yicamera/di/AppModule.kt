package com.github.kimymym.yicamera.di

import com.github.kimymym.yicamera.data.repository.DefaultYiCameraRepository
import com.github.kimymym.yicamera.data.repository.YiCameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindYiCameraRepository(
        impl: DefaultYiCameraRepository
    ): YiCameraRepository
}
