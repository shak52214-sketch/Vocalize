package com.vocalize.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.vocalize.app.MainActivity
import com.vocalize.app.R
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.util.AudioPlayerManager
import com.vocalize.app.util.Constants
import com.vocalize.app.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : Service() {

    @Inject lateinit var audioPlayerManager: AudioPlayerManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var memoRepository: MemoRepository

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null
    private var notificationId: Int = NOTIFICATION_ID

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification(false))
        audioPlayerManager.onPositionSave = { memoId, positionMs ->
            scope.launch(Dispatchers.IO) {
                memoRepository.updatePlaybackPosition(memoId, positionMs)
            }
        }
        startPositionUpdates()
    }

    private fun startPositionUpdates() {
        positionUpdateJob = scope.launch {
            while (coroutineContext.isActive) {
                audioPlayerManager.updatePosition()
                delay(500)
            }
        }
    }

    fun playMemo(filePath: String, memoId: String, memoTitle: String, startPositionMs: Long = 0L) {
        audioPlayerManager.prepareAndPlay(filePath, memoId, startPositionMs)
        updateNotification(memoTitle, true)
    }

    fun togglePlayPause(memoTitle: String) {
        audioPlayerManager.togglePlayPause()
        updateNotification(memoTitle, audioPlayerManager.isPlaying())
    }

    fun stop() {
        audioPlayerManager.release()
        stopSelf()
    }

    private fun handlePlayAudio(intent: Intent?) {
        val memoId = intent?.getStringExtra(Constants.EXTRA_MEMO_ID) ?: return
        val newNotificationId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        scope.launch {
            val memo = withContext(Dispatchers.IO) { memoRepository.getMemoById(memoId) }
            memo?.let {
                notificationId = newNotificationId
                startForeground(notificationId, buildNotification(false, it.title))
                playMemo(it.filePath, it.id, it.title, it.lastPlaybackPositionMs)
            }
        }
    }

    private fun handleReplayAudio(intent: Intent?) {
        val memoId = intent?.getStringExtra(Constants.EXTRA_MEMO_ID) ?: return
        scope.launch {
            val memo = withContext(Dispatchers.IO) { memoRepository.getMemoById(memoId) }
            memo?.let {
                playMemo(it.filePath, it.id, it.title, 0L)
            }
        }
    }

    private fun buildNotification(isPlaying: Boolean, title: String = "Vocalize"): android.app.Notification {
        val currentPosition = audioPlayerManager.getCurrentPosition()
        val duration = audioPlayerManager.getDuration()
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )
        val replayIntent = PendingIntent.getService(
            this, 3,
            Intent(this, PlaybackService::class.java).setAction(ACTION_REPLAY),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return notificationHelper.buildPlaybackNotification(
            title,
            isPlaying,
            currentPosition,
            duration,
            audioPlayerManager.getMediaSessionToken(),
            openIntent,
            playPauseIntent,
            stopIntent,
            replayIntent
        )
    }

    private fun updateNotification(title: String, isPlaying: Boolean) {
        val notification = buildNotification(isPlaying, title)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(notificationId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_AUDIO -> handlePlayAudio(intent)
            ACTION_REPLAY -> handleReplayAudio(intent)
            ACTION_TOGGLE -> togglePlayPause("Voice Memo")
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        positionUpdateJob?.cancel()
        scope.cancel()
        audioPlayerManager.release()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_AUDIO = "com.vocalize.app.ACTION_PLAY_AUDIO"
        const val ACTION_REPLAY = "com.vocalize.app.ACTION_REPLAY_AUDIO"
        const val ACTION_TOGGLE = "com.vocalize.app.TOGGLE"
        const val ACTION_STOP = "com.vocalize.app.STOP"
    }
}
