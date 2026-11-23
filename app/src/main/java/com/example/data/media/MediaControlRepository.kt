package com.example.data.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaTrackState(
    val hasNotificationAccess: Boolean = false,
    val hasActiveSession: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val sourcePackage: String = ""
)

class MediaControlRepository(private val context: Context) {

    private val _mediaState = MutableStateFlow(MediaTrackState())
    val mediaState: StateFlow<MediaTrackState> = _mediaState.asStateFlow()

    private val componentName = ComponentName(context, StillroomNotificationListener::class.java)
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishFromController(activeController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishFromController(activeController)
        }

        override fun onSessionDestroyed() {
            refreshSessions()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindControllers(controllers)
        }

    fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabled.contains(context.packageName)
    }

    fun startObserving() {
        refreshSessions()
        if (!isNotificationAccessEnabled()) return
        runCatching {
            val mgr = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            mediaSessionManager = mgr
            mgr?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            mgr?.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
        }
    }

    fun stopObserving() {
        runCatching {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            activeController?.unregisterCallback(controllerCallback)
        }
        activeController = null
    }

    fun refreshSessions() {
        val hasAccess = isNotificationAccessEnabled()
        if (!hasAccess) {
            _mediaState.value = MediaTrackState(hasNotificationAccess = false)
            return
        }
        runCatching {
            val mgr = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            mediaSessionManager = mgr
            val controllers = mgr?.getActiveSessions(componentName).orEmpty()
            bindControllers(controllers)
        }.onFailure {
            _mediaState.value = MediaTrackState(hasNotificationAccess = hasAccess)
        }
    }

    private fun bindControllers(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        val chosen = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        activeController = chosen
        chosen?.registerCallback(controllerCallback)
        publishFromController(chosen)
    }

    private fun publishFromController(controller: MediaController?) {
        val hasAccess = isNotificationAccessEnabled()
        if (controller == null) {
            _mediaState.value = MediaTrackState(hasNotificationAccess = hasAccess, hasActiveSession = false)
            return
        }
        val meta = controller.metadata
        val playback = controller.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val playing = playback?.state == PlaybackState.STATE_PLAYING

        _mediaState.value = MediaTrackState(
            hasNotificationAccess = hasAccess,
            hasActiveSession = true,
            title = title,
            artist = artist,
            album = album,
            artwork = art,
            isPlaying = playing,
            sourcePackage = controller.packageName
        )
    }

    fun togglePlayPause() {
        val ctrl = activeController ?: return
        val isPlaying = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
        if (isPlaying) ctrl.transportControls.pause() else ctrl.transportControls.play()
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }
}
