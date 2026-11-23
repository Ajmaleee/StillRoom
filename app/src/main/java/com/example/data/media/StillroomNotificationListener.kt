package com.example.data.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StillroomNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _listenerConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _listenerConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        _pulseCounter.value += 1
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        _pulseCounter.value += 1
    }

    companion object {
        private val _listenerConnected = MutableStateFlow(false)
        val listenerConnected = _listenerConnected.asStateFlow()

        private val _pulseCounter = MutableStateFlow(0L)
        val pulseCounter = _pulseCounter.asStateFlow()
    }
}
