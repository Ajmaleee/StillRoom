package com.example

import android.app.Application
import com.example.data.audio.AudioCaptureEngine
import com.example.data.device.DeviceCalendarProvider
import com.example.data.device.SystemTelemetryProvider
import com.example.data.iot.IotRepository
import com.example.data.local.SettingsDataStore
import com.example.data.local.StillroomDatabase
import com.example.data.media.MediaControlRepository
import com.example.data.network.NetworkRepository

class AppContainer(application: Application) {
    val database: StillroomDatabase = StillroomDatabase.getInstance(application)
    val dao = database.stillroomDao()
    val settingsDataStore = SettingsDataStore(application)
    val networkRepository = NetworkRepository(settingsDataStore)
    val systemTelemetryProvider = SystemTelemetryProvider(application)
    val calendarProvider = DeviceCalendarProvider(application)
    val audioCaptureEngine = AudioCaptureEngine(application)
    val mediaControlRepository = MediaControlRepository(application)
    val iotRepository = IotRepository(dao, networkRepository.okHttpClient)
}

class StillroomApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
