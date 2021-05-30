package com.example.whereami.di

import com.example.whereami.sounds.AudioPlayer
import com.example.whereami.sounds.SoundPoolPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class SoundModule {
    @Singleton
    @Binds
    abstract fun bindAudioPlayer(
            soundPoolPlayer: SoundPoolPlayer
    ): AudioPlayer
}