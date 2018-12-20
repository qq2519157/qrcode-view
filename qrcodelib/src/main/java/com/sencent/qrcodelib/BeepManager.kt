package com.sencent.qrcodelib

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import java.io.Closeable
import java.io.IOException

/**
 *  Create by Logan at 2018/12/14 0014
 *  提示音管理器
 */
class BeepManager(val captureView: CaptureView) : MediaPlayer.OnErrorListener, Closeable {

    private var mediaPlayer: MediaPlayer? = null
    private var playBeep: Boolean = false
    private var vibrate: Boolean = false
    private var context = captureView.context

    /**
     * 设置是否震动
     */
    fun setVibrate(vibrate: Boolean) {
        this.vibrate = vibrate
    }

    /**
     * 设置是否有提示音
     */
    fun setPlayBeep(playBeep: Boolean) {
        this.playBeep = playBeep
    }

    @Synchronized
    internal fun updatePrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        playBeep = shouldBeep(prefs, context)
        //        vibrate = prefs.getBoolean(Preferences.KEY_VIBRATE, false);
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC)
            mediaPlayer = buildMediaPlayer(context)
        }
    }

    private fun setVolumeControlStream(volumControlStream: Int) {
        captureView.setVolumeControlStream(volumControlStream)
    }

    @Synchronized
    internal fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VIBRATE_DURATION)
        }
    }

    private fun shouldBeep(prefs: SharedPreferences, context: Context): Boolean {
        var shouldPlayBeep = prefs.getBoolean(Preferences.KEY_PLAY_BEEP, true)
        if (shouldPlayBeep) {
            // See if sound settings overrides this
            val audioService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                shouldPlayBeep = false
            }
        }
        return shouldPlayBeep
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun buildMediaPlayer(context: Context): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        try {
            context.resources.openRawResourceFd(R.raw.beep).use { file ->
                mediaPlayer.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                mediaPlayer.setOnErrorListener(this)
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer.isLooping = false
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME)
                mediaPlayer.prepare()
                return mediaPlayer
            }
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
            mediaPlayer.release()
            return null
        }

    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required and finish
            captureView.onBeepManagerError()
        } else {
            // possibly media player error, so release and recreate
            close()
            updatePrefs()
        }
        return true
    }

    override fun close() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    companion object {

        private val TAG = BeepManager::class.java.simpleName
        private const val BEEP_VOLUME = 0.10f
        private const val VIBRATE_DURATION = 200L
    }
}