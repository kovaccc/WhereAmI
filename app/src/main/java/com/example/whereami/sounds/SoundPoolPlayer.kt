package com.example.whereami.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.example.whereami.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SoundPoolPlayer @Inject constructor(@ApplicationContext context: Context) : AudioPlayer {

    private val priority: Int = 1
    private val maxStreams: Int = 1
    private val srcQuality: Int = 1
    private val leftVolume = 1f
    private val rightVolume = 1f
    private val shouldLoop = 0
    private val playbackRate = 1f
    private val soundPool: SoundPool

    private val addingMarkerSoundId: Int

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()
            soundPool = SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(maxStreams)
                .build()
        } else {
            soundPool = SoundPool(maxStreams, AudioManager.USE_DEFAULT_STREAM_TYPE, srcQuality)
        }

        addingMarkerSoundId = soundPool.load(context, R.raw.hitmarker2 , priority)
    }


    override fun playAddMarkerSound() {
        soundPool.play(addingMarkerSoundId, leftVolume, rightVolume, priority, shouldLoop, playbackRate)
    }
}