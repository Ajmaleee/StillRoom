package com.example.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StillroomApplication
import com.example.data.audio.AudioFrameData
import com.example.data.device.CalendarAgendaEvent
import com.example.data.device.CuratedKnowledgeBank
import com.example.data.device.KnowledgeEntry
import com.example.data.device.SystemTelemetry
import com.example.data.iot.BrokerConnectionState
import com.example.data.local.CountdownEntity
import com.example.data.local.HabitEntity
import com.example.data.local.IotDeviceEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.RssFeedSourceEntity
import com.example.data.local.SceneProfileEntity
import com.example.data.local.StillroomDatabase
import com.example.data.local.TodoTaskEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.media.MediaTrackState
import com.example.data.network.GeoCityDto
import com.example.data.network.GitHubSnapshot
import com.example.data.network.IssPositionDto
import com.example.data.network.NasaApodDto
import com.example.data.network.RssArticle
import com.example.data.network.WeatherSnapshot
import com.example.data.network.WikiSummaryDto
import com.example.domain.model.AppSettings
import com.example.domain.model.GlassMaterialConfig
import com.example.domain.model.GlassPreset
import com.example.domain.model.ResourceState
import com.example.domain.model.SceneConditionType
import com.example.domain.model.SceneConfig
import com.example.domain.model.WidgetSize
import com.example.domain.model.WidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class PomodoroPhase(val label: String, val defaultMinutes: Int) {
    FOCUS("Deep Focus", 25),
    STUDY("Study Block", 50),
    BREAK("Rest Interval", 5),
    CUSTOM("Custom Timer", 10)
}

data class TimerUiState(
    val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val timerRemainingSec: Int = 25 * 60,
    val timerTotalSec: Int = 25 * 60,
    val timerRunning: Boolean = false,
    val completedFocusSessions: Int = 0,
    val stopwatchElapsedMs: Long = 0L,
    val stopwatchRunning: Boolean = false,
    val stopwatchLaps: List<Long> = emptyList()
)

class StillroomViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StillroomApplication
    private val c = app.container
    private val dao = c.dao
    private val settingsStore = c.settingsDataStore
    private val networkRepo = c.networkRepository

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val scenes: StateFlow<List<SceneConfig>> = dao.observeScenes()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notes: StateFlow<List<NoteEntity>> = dao.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TodoTaskEntity>> = dao.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = dao.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = dao.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countdowns: StateFlow<List<CountdownEntity>> = dao.observeCountdowns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rssFeeds: StateFlow<List<RssFeedSourceEntity>> = dao.observeRssFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val iotDevices: StateFlow<List<IotDeviceEntity>> = dao.observeIotDevices()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val iotDeviceStatus: StateFlow<Map<Long, String>> = c.iotRepository.deviceStatus
    val iotBrokerStates: StateFlow<Map<String, BrokerConnectionState>> = c.iotRepository.brokerStates

    val audioFrame: StateFlow<AudioFrameData> = c.audioCaptureEngine.frame
    val mediaState: StateFlow<MediaTrackState> = c.mediaControlRepository.mediaState

    val telemetry: StateFlow<SystemTelemetry> = c.systemTelemetryProvider.observeTelemetry(2000L)
        .stateIn(viewModelScope, SharingStarted.Eagerly, c.systemTelemetryProvider.snapshot())

    private val _calendarEvents = MutableStateFlow<List<CalendarAgendaEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarAgendaEvent>> = _calendarEvents.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(c.calendarProvider.hasCalendarPermission())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    private val _weatherState = MutableStateFlow<ResourceState<WeatherSnapshot>>(ResourceState.Idle)
    val weatherState: StateFlow<ResourceState<WeatherSnapshot>> = _weatherState.asStateFlow()

    private val _cityResults = MutableStateFlow<List<GeoCityDto>>(emptyList())
    val cityResults: StateFlow<List<GeoCityDto>> = _cityResults.asStateFlow()

    private val _rssArticles = MutableStateFlow<ResourceState<List<RssArticle>>>(ResourceState.Idle)
    val rssArticles: StateFlow<ResourceState<List<RssArticle>>> = _rssArticles.asStateFlow()

    private val _githubState = MutableStateFlow<ResourceState<GitHubSnapshot>>(ResourceState.Idle)
    val githubState: StateFlow<ResourceState<GitHubSnapshot>> = _githubState.asStateFlow()

    private val _nasaState = MutableStateFlow<ResourceState<NasaApodDto>>(ResourceState.Idle)
    val nasaState: StateFlow<ResourceState<NasaApodDto>> = _nasaState.asStateFlow()

    private val _issState = MutableStateFlow<ResourceState<IssPositionDto>>(ResourceState.Idle)
    val issState: StateFlow<ResourceState<IssPositionDto>> = _issState.asStateFlow()

    private val _wikiState = MutableStateFlow<ResourceState<WikiSummaryDto>>(ResourceState.Idle)
    val wikiState: StateFlow<ResourceState<WikiSummaryDto>> = _wikiState.asStateFlow()

    private val _knowledgeIndex = MutableStateFlow(0)
    val currentKnowledge: StateFlow<KnowledgeEntry> = _knowledgeIndex
        .map { CuratedKnowledgeBank.pick(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CuratedKnowledgeBank.pick(0))

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private val _isIdle = MutableStateFlow(false)
    val isIdle: StateFlow<Boolean> = _isIdle.asStateFlow()

    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    private val _burnInOffsetDp = MutableStateFlow(0f to 0f)
    val burnInOffsetDp: StateFlow<Pair<Float, Float>> = _burnInOffsetDp.asStateFlow()

    private var lastInteractionElapsedMs = SystemClock.elapsedRealtime()
    private var stopwatchStartMs = 0L
    private var stopwatchAccumMs = 0L
    private var lastAutoEvaluatedSceneId: String? = null

    init {
        viewModelScope.launch {
            dao.insertScenesIfMissing(StillroomDatabase.defaultScenes())
            dao.insertRssFeedsIfMissing(StillroomDatabase.defaultRssFeeds())
        }

        viewModelScope.launch {
            iotDevices.collect { devices ->
                c.iotRepository.syncDevices(devices)
            }
        }

        viewModelScope.launch {
            settings.collect { s ->
                c.audioCaptureEngine.updateConfig(s.visualizerConfig)
            }
        }

        startIdleAndBurnInLoop()
        startTimerAndStopwatchLoop()
        startSceneEngineLoop()
        startNetworkRefreshLoop()
        refreshCalendar()
        c.mediaControlRepository.startObserving()
        if (c.audioCaptureEngine.hasAudioPermission()) {
            c.audioCaptureEngine.start()
        }
    }

    fun registerInteraction(revealControls: Boolean = true) {
        lastInteractionElapsedMs = SystemClock.elapsedRealtime()
        _isIdle.value = false
        if (revealControls) {
            _controlsVisible.value = true
        }
    }

    fun toggleControlsVisibility() {
        lastInteractionElapsedMs = SystemClock.elapsedRealtime()
        _isIdle.value = false
        _controlsVisible.value = !_controlsVisible.value
    }

    fun enterAmbientIdleNow() {
        _controlsVisible.value = false
        _isIdle.value = true
    }

    private fun startIdleAndBurnInLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val s = settings.value
                val elapsedSec = (SystemClock.elapsedRealtime() - lastInteractionElapsedMs) / 1000L
                if (s.idleTimeoutSeconds > 0 && elapsedSec >= s.idleTimeoutSeconds) {
                    if (!_isIdle.value) {
                        _isIdle.value = true
                        _controlsVisible.value = false
                    }
                }
                if (_isIdle.value && s.burnInShiftEnabled) {
                    val t = (System.currentTimeMillis() / 1000.0) * 0.04
                    _burnInOffsetDp.value = (sin(t) * 10.0).toFloat() to (cos(t * 0.7) * 7.0).toFloat()
                } else {
                    _burnInOffsetDp.value = 0f to 0f
                }
            }
        }
    }

    private fun startTimerAndStopwatchLoop() {
        viewModelScope.launch {
            while (isActive) {
                val st = _timerState.value
                var updated = st
                if (st.timerRunning) {
                    if (st.timerRemainingSec > 0) {
                        updated = updated.copy(timerRemainingSec = st.timerRemainingSec - 1)
                    } else {
                        val completed = if (st.pomodoroPhase == PomodoroPhase.FOCUS || st.pomodoroPhase == PomodoroPhase.STUDY) {
                            st.completedFocusSessions + 1
                        } else st.completedFocusSessions
                        updated = updated.copy(timerRunning = false, completedFocusSessions = completed)
                    }
                }
                if (st.stopwatchRunning) {
                    val now = SystemClock.elapsedRealtime()
                    updated = updated.copy(stopwatchElapsedMs = stopwatchAccumMs + (now - stopwatchStartMs))
                }
                if (updated != st) _timerState.value = updated
                delay(if (st.stopwatchRunning) 100L else 1000L)
            }
        }
    }

    private fun startSceneEngineLoop() {
        viewModelScope.launch {
            while (isActive) {
                evaluateSceneEngine()
                delay(15_000L)
            }
        }
    }

    fun evaluateSceneEngine() {
        val s = settings.value
        if (!s.autoSceneEngineEnabled) return
        val allScenes = scenes.value
        if (allScenes.isEmpty()) return

        val isPlayingMusic = mediaState.value.isPlaying || audioFrame.value.isSignalPresent
        val isCharging = telemetry.value.isCharging
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val matched = when {
            s.visualizerConfig.autoSceneOnMusic && isPlayingMusic ->
                allScenes.firstOrNull { it.condition == SceneConditionType.MUSIC_PLAYING }
            allScenes.any { it.condition == SceneConditionType.CUSTOM_SCHEDULE && isHourInWindow(hour, it.scheduleStartHour, it.scheduleEndHour) } ->
                allScenes.firstOrNull { it.condition == SceneConditionType.CUSTOM_SCHEDULE && isHourInWindow(hour, it.scheduleStartHour, it.scheduleEndHour) }
            isCharging && allScenes.any { it.condition == SceneConditionType.CHARGING } ->
                allScenes.firstOrNull { it.condition == SceneConditionType.CHARGING }
            hour in 6..11 -> allScenes.firstOrNull { it.condition == SceneConditionType.TIME_MORNING }
            hour in 12..16 -> allScenes.firstOrNull { it.condition == SceneConditionType.TIME_AFTERNOON }
            hour in 17..20 -> allScenes.firstOrNull { it.condition == SceneConditionType.TIME_EVENING }
            else -> allScenes.firstOrNull { it.condition == SceneConditionType.TIME_NIGHT }
        }

        if (matched != null && matched.id != lastAutoEvaluatedSceneId && matched.id != s.activeSceneId) {
            lastAutoEvaluatedSceneId = matched.id
            applyScene(matched, keepAutoEngine = true)
        }
    }

    private fun isHourInWindow(hour: Int, start: Int, end: Int): Boolean {
        return if (start <= end) hour in start until end else (hour >= start || hour < end)
    }

    fun applyScene(scene: SceneConfig, keepAutoEngine: Boolean = false) {
        updateSettings { cur ->
            cur.copy(
                activeSceneId = scene.id,
                autoSceneEngineEnabled = keepAutoEngine,
                backgroundType = scene.backgroundType,
                transitionType = scene.transitionType,
                motionSpeed = scene.motionSpeed,
                clockConfig = cur.clockConfig.copy(
                    style = scene.clockStyle,
                    position = scene.clockPosition
                ),
                visualizerConfig = cur.visualizerConfig.copy(
                    mode = scene.visualizerMode,
                    enabled = scene.visualizerEnabled
                ),
                glassConfig = GlassMaterialConfig.fromPreset(scene.glassPreset)
            )
        }
    }

    fun cycleNextScene(direction: Int) {
        val list = scenes.value
        if (list.isEmpty()) return
        val curIdx = list.indexOfFirst { it.id == settings.value.activeSceneId }.coerceAtLeast(0)
        val nextIdx = (curIdx + direction).mod(list.size)
        applyScene(list[nextIdx], keepAutoEngine = false)
    }

    private fun startNetworkRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                val s = settings.value
                val shouldSkip = (s.pauseNetworkOnIdle && _isIdle.value) ||
                    (s.wifiOnlyRefresh && !c.systemTelemetryProvider.isWifiConnected())
                if (!shouldSkip) {
                    refreshAllNetworkWidgets()
                }
                delay((s.networkRefreshMinutes.coerceAtLeast(5)) * 60_000L)
            }
        }
    }

    fun refreshAllNetworkWidgets() {
        refreshWeather()
        refreshRss()
        refreshGitHub()
        refreshNasaApod()
        refreshIssPosition()
        refreshWikipedia()
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val s = settings.value
            _weatherState.value = ResourceState.Loading
            _weatherState.value = networkRepo.fetchWeather(
                cityName = s.weatherCityName,
                lat = s.weatherLatitude,
                lon = s.weatherLongitude,
                useFahrenheit = s.useFahrenheit
            )
        }
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            _cityResults.value = networkRepo.searchCities(query)
        }
    }

    fun selectCity(city: GeoCityDto) {
        val label = listOfNotNull(city.name, city.admin1, city.country).distinct().take(2).joinToString(", ")
        updateSettings {
            it.copy(
                weatherCityName = label.ifBlank { city.name },
                weatherLatitude = city.latitude,
                weatherLongitude = city.longitude
            )
        }
        _cityResults.value = emptyList()
        refreshWeather()
    }

    @SuppressLint("MissingPermission")
    fun useDeviceCoarseLocation() {
        val ctx = getApplication<Application>()
        val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return

        viewModelScope.launch(Dispatchers.IO) {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@launch
            val loc = runCatching {
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }.getOrNull() ?: return@launch

            val cityName = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(ctx, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                    ?.firstOrNull()?.locality ?: "Local Coordinates"
            }.getOrDefault("Local Coordinates")

            updateSettings {
                it.copy(
                    weatherCityName = cityName,
                    weatherLatitude = loc.latitude,
                    weatherLongitude = loc.longitude
                )
            }
            refreshWeather()
        }
    }

    fun refreshRss() {
        viewModelScope.launch {
            val feeds = rssFeeds.value.ifEmpty { dao.observeRssFeeds().first() }
            _rssArticles.value = ResourceState.Loading
            _rssArticles.value = networkRepo.fetchRssFeeds(feeds)
        }
    }

    fun refreshGitHub() {
        viewModelScope.launch {
            _githubState.value = ResourceState.Loading
            _githubState.value = networkRepo.fetchGitHub(settings.value.githubUsername)
        }
    }

    fun refreshNasaApod() {
        viewModelScope.launch {
            _nasaState.value = ResourceState.Loading
            _nasaState.value = networkRepo.fetchNasaApod(settings.value.nasaApiKey)
        }
    }

    fun refreshIssPosition() {
        viewModelScope.launch {
            _issState.value = ResourceState.Loading
            _issState.value = networkRepo.fetchIssPosition()
        }
    }

    fun refreshWikipedia() {
        viewModelScope.launch {
            _wikiState.value = ResourceState.Loading
            _wikiState.value = networkRepo.fetchRandomWikipedia()
        }
    }

    fun nextKnowledgeEntry() {
        _knowledgeIndex.value += 1
    }

    // --- Audio & Media ---
    fun onAudioPermissionResult(granted: Boolean) {
        if (granted) {
            c.audioCaptureEngine.start()
        }
    }

    fun refreshMediaSessions() = c.mediaControlRepository.refreshSessions()
    fun togglePlayPause() = c.mediaControlRepository.togglePlayPause()
    fun skipNextTrack() = c.mediaControlRepository.skipNext()
    fun skipPrevTrack() = c.mediaControlRepository.skipPrevious()

    // --- Calendar ---
    fun refreshCalendar() {
        _hasCalendarPermission.value = c.calendarProvider.hasCalendarPermission()
        viewModelScope.launch {
            _calendarEvents.value = c.calendarProvider.loadUpcomingEvents(7)
        }
    }

    // --- Productivity CRUD ---
    fun saveNote(id: Long = 0L, title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            dao.upsertNote(NoteEntity(id = id, title = title.ifBlank { "Note" }, content = content))
        }
    }

    fun deleteNote(id: Long) = viewModelScope.launch { dao.deleteNote(id) }

    fun addTask(title: String, isDailyGoal: Boolean) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsertTask(TodoTaskEntity(title = title.trim(), isDailyGoal = isDailyGoal))
        }
    }

    fun toggleTask(task: TodoTaskEntity) = viewModelScope.launch {
        dao.upsertTask(task.copy(isCompleted = !task.isCompleted))
    }

    fun deleteTask(id: Long) = viewModelScope.launch { dao.deleteTask(id) }

    fun addHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { dao.upsertHabit(HabitEntity(name = name.trim())) }
    }

    fun checkHabitToday(habit: HabitEntity) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            val next = if (habit.lastCompletedDate == today) {
                habit.copy(streakCount = (habit.streakCount - 1).coerceAtLeast(0), lastCompletedDate = "")
            } else {
                habit.copy(streakCount = habit.streakCount + 1, lastCompletedDate = today)
            }
            dao.upsertHabit(next)
        }
    }

    fun deleteHabit(id: Long) = viewModelScope.launch { dao.deleteHabit(id) }

    fun addReminder(title: String, timeLabel: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsertReminder(ReminderEntity(title = title.trim(), dueTimeLabel = timeLabel.ifBlank { "Today" }))
        }
    }

    fun toggleReminder(item: ReminderEntity) = viewModelScope.launch {
        dao.upsertReminder(item.copy(isDone = !item.isDone))
    }

    fun deleteReminder(id: Long) = viewModelScope.launch { dao.deleteReminder(id) }

    fun addCountdown(title: String, category: String, targetEpochMillis: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsertCountdown(CountdownEntity(title = title.trim(), category = category, targetEpochMillis = targetEpochMillis))
        }
    }

    fun deleteCountdown(id: Long) = viewModelScope.launch { dao.deleteCountdown(id) }

    // --- Timers & Stopwatch ---
    fun selectPomodoroPhase(phase: PomodoroPhase, customMinutes: Int = phase.defaultMinutes) {
        val sec = (customMinutes.coerceIn(1, 180)) * 60
        _timerState.value = _timerState.value.copy(
            pomodoroPhase = phase,
            timerRemainingSec = sec,
            timerTotalSec = sec,
            timerRunning = false
        )
    }

    fun toggleTimerRunning() {
        val cur = _timerState.value
        _timerState.value = cur.copy(timerRunning = !cur.timerRunning)
    }

    fun resetTimer() {
        val cur = _timerState.value
        _timerState.value = cur.copy(timerRemainingSec = cur.timerTotalSec, timerRunning = false)
    }

    fun toggleStopwatch() {
        val cur = _timerState.value
        if (cur.stopwatchRunning) {
            stopwatchAccumMs = cur.stopwatchElapsedMs
            _timerState.value = cur.copy(stopwatchRunning = false)
        } else {
            stopwatchStartMs = SystemClock.elapsedRealtime()
            _timerState.value = cur.copy(stopwatchRunning = true)
        }
    }

    fun lapStopwatch() {
        val cur = _timerState.value
        if (cur.stopwatchElapsedMs > 0L) {
            _timerState.value = cur.copy(stopwatchLaps = listOf(cur.stopwatchElapsedMs) + cur.stopwatchLaps.take(9))
        }
    }

    fun resetStopwatch() {
        stopwatchAccumMs = 0L
        _timerState.value = _timerState.value.copy(
            stopwatchElapsedMs = 0L,
            stopwatchRunning = false,
            stopwatchLaps = emptyList()
        )
    }

    // --- RSS & IoT & Scenes ---
    fun addRssFeed(title: String, url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            dao.upsertRssFeed(RssFeedSourceEntity(title = title.ifBlank { url }, url = url.trim()))
            refreshRss()
        }
    }

    fun deleteRssFeed(id: Long) = viewModelScope.launch {
        dao.deleteRssFeed(id)
        refreshRss()
    }

    fun saveIotDevice(device: IotDeviceEntity) = viewModelScope.launch {
        dao.upsertIotDevice(device)
    }

    fun deleteIotDevice(id: Long) = viewModelScope.launch {
        dao.deleteIotDevice(id)
    }

    fun toggleIotSwitch(device: IotDeviceEntity) = viewModelScope.launch {
        val currentlyOn = device.lastReading.equals(device.publishPayloadOn, ignoreCase = true) ||
            device.lastReading.equals("1") || device.lastReading.equals("true", ignoreCase = true)
        c.iotRepository.toggleSwitchDevice(device, turnOn = !currentlyOn)
    }

    fun saveSceneProfile(scene: SceneConfig) = viewModelScope.launch {
        dao.upsertScene(scene.toEntity())
    }

    fun deleteSceneProfile(id: String) = viewModelScope.launch {
        dao.deleteScene(id)
    }

    // --- Settings & Custom Background ---
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val next = transform(settings.value)
            settingsStore.saveSettings(next)
        }
    }

    fun applyGlassPreset(preset: GlassPreset) {
        updateSettings { it.copy(glassConfig = GlassMaterialConfig.fromPreset(preset)) }
    }

    fun toggleWidgetVisibility(type: WidgetType) {
        updateSettings { cur ->
            val next = cur.hiddenWidgets.toMutableSet()
            if (type in next) next.remove(type) else next.add(type)
            cur.copy(hiddenWidgets = next)
        }
    }

    fun setWidgetSize(type: WidgetType, size: WidgetSize) {
        updateSettings { cur ->
            cur.copy(widgetSizes = cur.widgetSizes + (type to size))
        }
    }

    fun persistCustomBackgroundUri(sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val dest = File(ctx.filesDir, "stillroom_custom_bg.jpg")
            runCatching {
                ctx.contentResolver.openInputStream(sourceUri)?.use { inp ->
                    dest.outputStream().use { out -> inp.copyTo(out) }
                }
            }.onSuccess {
                updateSettings {
                    it.copy(
                        customBackgroundUri = Uri.fromFile(dest).toString(),
                        backgroundType = com.example.domain.model.AmbientSceneType.CUSTOM_IMAGE
                    )
                }
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsStore.resetAll()
            dao.insertScenesIfMissing(StillroomDatabase.defaultScenes())
        }
    }

    override fun onCleared() {
        super.onCleared()
        c.audioCaptureEngine.stop()
        c.mediaControlRepository.stopObserving()
        c.iotRepository.shutdown()
    }
}
