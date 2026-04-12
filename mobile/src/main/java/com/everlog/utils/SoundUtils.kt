package com.everlog.utils

import android.content.Context
import android.media.MediaPlayer
import com.everlog.R

class SoundUtils {

    companion object {

        private const val MAX_VOLUME = 100.0
        private const val TARGET_VOLUME = 70.0

        private var mMediaPlayer: MediaPlayer? = null

        fun playTimerSound(context: Context) {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(context, R.raw.timer)
                val volume = (1 - (Math.log(MAX_VOLUME - TARGET_VOLUME) / Math.log(MAX_VOLUME))).toFloat()
                mMediaPlayer?.setVolume(volume, volume);
            }
            mMediaPlayer?.start()
        }
    }
}