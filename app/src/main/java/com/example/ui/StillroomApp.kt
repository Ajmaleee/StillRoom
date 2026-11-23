package com.example.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ClockPosition
import com.example.domain.model.DashboardPage
import com.example.domain.model.GlassPreset
import com.example.domain.model.VisualizerPosition
import com.example.domain.model.WidgetSize
import com.example.domain.model.WidgetType
import com.example.ui.ambient.AmbientBackgroundEngine
import com.example.ui.clock.AmbientClockView
import com.example.ui.glass.GlassBackdropHost
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.settings.StillroomSettingsSheet
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomCharcoal
import com.example.ui.theme.StillroomEspresso
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomObsidian
import com.example.ui.theme.StillroomSage
import com.example.ui.visualizer.AudioVisualizerCanvas
import com.example.ui.widgets.CalendarAgendaTile
import com.example.ui.widgets.CountdownsTile
import com.example.ui.widgets.CustomMantraTile
import com.example.ui.widgets.DailyKnowledgeTile
import com.example.ui.widgets.FocusTimerAndStopwatchTile
import com.example.ui.widgets.GitHubTile
import com.example.ui.widgets.HabitsAndRemindersTile
import com.example.ui.widgets.IotHabitatPanel
import com.example.ui.widgets.MediaNowPlayingTile
import com.example.ui.widgets.NasaAndIssTile
import com.example.ui.widgets.QuickNotesTile
import com.example.ui.widgets.RssFeedTile
import com.example.ui.widgets.SystemTelemetryTile
import com.example.ui.widgets.TasksAndGoalsTile
import com.example.ui.widgets.WeatherAmbientTile
import com.example.ui.widgets.WeatherExpandedSheet
import com.example.ui.widgets.WikipediaTile
import com.example.viewmodel.StillroomViewModel
import kotlinx.coroutines.launch

@Composable
fun StillroomApp(
    viewModel: StillroomViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val isIdle by viewModel.isIdle.collectAsStateWithLifecycle()
    val controlsVisible by viewModel.controlsVisible.collectAsStateWithLifecycle()
    val burnInOffset by viewModel.burnInOffsetDp.collectAsStateWithLifecycle()

    val audioFrame by viewModel.audioFrame.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val cityResults by viewModel.cityResults.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val hasCalendarPerm by viewModel.hasCalendarPermission.collectAsStateWithLifecycle()

    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val countdowns by viewModel.countdowns.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()

    val rssFeeds by viewModel.rssFeeds.collectAsStateWithLifecycle()
    val rssArticles by viewModel.rssArticles.collectAsStateWithLifecycle()
    val githubState by viewModel.githubState.collectAsStateWithLifecycle()
    val nasaState by viewModel.nasaState.collectAsStateWithLifecycle()
    val issState by viewModel.issState.collectAsStateWithLifecycle()
    val wikiState by viewModel.wikiState.collectAsStateWithLifecycle()
    val knowledge by viewModel.currentKnowledge.collectAsStateWithLifecycle()

    val iotDevices by viewModel.iotDevices.collectAsStateWithLifecycle()
    val iotDeviceStatus by viewModel.iotDeviceStatus.collectAsStateWithLifecycle()
    val iotBrokerStates by viewModel.iotBrokerStates.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    var editingMantra by remember { mutableStateOf(false) }
    var customizingWidget by remember { mutableStateOf<WidgetType?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> viewModel.onAudioPermissionResult(granted) }
    )
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) viewModel.refreshCalendar() }
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) viewModel.useDeviceCoarseLocation() }
    )

    val openNotificationSettings: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    val pagerState = rememberPagerState(pageCount = { DashboardPage.entries.size })
    val activeScene = scenes.firstOrNull { it.id == settings.activeSceneId }

    val burnInX by animateDpAsState(targetValue = burnInOffset.first.dp, animationSpec = tween(900), label = "burn_x")
    val burnInY by animateDpAsState(targetValue = burnInOffset.second.dp, animationSpec = tween(900), label = "burn_y")
    val idleContentAlpha by animateFloatAsState(
        targetValue = if (isIdle) 0.84f else 1f,
        animationSpec = tween(900),
        label = "idle_alpha"
    )

    val dimmingLevel = if (isIdle) (activeScene?.ambientDimming ?: 0.14f) + 0.18f else (activeScene?.ambientDimming ?: 0.10f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StillroomObsidian)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            viewModel.registerInteraction(revealControls = false)
                        }
                    }
                }
            }
    ) {
        GlassBackdropHost(
            modifier = Modifier.fillMaxSize(),
            glassConfig = settings.glassConfig,
            reducedMotion = settings.reducedMotion,
            lowPowerMode = settings.lowPowerMode,
            backdrop = {
                AmbientBackgroundEngine(
                    sceneType = settings.backgroundType,
                    transitionType = settings.transitionType,
                    customImageUri = settings.customBackgroundUri,
                    motionSpeed = settings.motionSpeed,
                    warmthTint = settings.warmthTint,
                    grainOpacity = settings.grainOpacity,
                    dimmingOverlay = dimmingLevel,
                    nightModeTint = settings.nightModeTint,
                    reducedMotion = settings.reducedMotion,
                    lowPowerMode = settings.lowPowerMode,
                    isIdle = isIdle,
                    audioEnergy = audioFrame.overallRms
                )
            },
            overlay = {
                // Main Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(idleContentAlpha)
                ) { pageIndex ->
                    val page = DashboardPage.entries[pageIndex]
                    when (page) {
                        DashboardPage.AMBIENT_CANVAS -> {
                            AmbientCanvasPage(
                                viewModel = viewModel,
                                settings = settings,
                                isIdle = isIdle,
                                controlsVisible = controlsVisible,
                                burnInX = burnInX,
                                burnInY = burnInY,
                                audioFrame = audioFrame,
                                mediaState = mediaState,
                                weatherState = weatherState,
                                knowledge = knowledge,
                                onToggleControls = { viewModel.toggleControlsVisibility() },
                                onSwipeScene = { dir -> viewModel.cycleNextScene(dir) },
                                onRequestAudioPerm = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                onOpenNotificationAccess = openNotificationSettings,
                                onExpandWeather = { showWeatherSheet = true },
                                onEditMantra = { editingMantra = true },
                                onLongPressWidget = { customizingWidget = it }
                            )
                        }

                        DashboardPage.PLANNER_FOCUS -> {
                            AtelierPlannerPage(
                                settings = settings,
                                hasCalendarPerm = hasCalendarPerm,
                                calendarEvents = calendarEvents,
                                countdowns = countdowns,
                                timerState = timerState,
                                notes = notes,
                                tasks = tasks,
                                habits = habits,
                                reminders = reminders,
                                viewModel = viewModel,
                                onRequestCalendarPerm = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                                onLongPressWidget = { customizingWidget = it }
                            )
                        }

                        DashboardPage.TECH_FEEDS -> {
                            TechSignalsPage(
                                settings = settings,
                                telemetry = telemetry,
                                rssArticles = rssArticles,
                                rssFeeds = rssFeeds,
                                githubState = githubState,
                                nasaState = nasaState,
                                issState = issState,
                                wikiState = wikiState,
                                viewModel = viewModel,
                                onLongPressWidget = { customizingWidget = it }
                            )
                        }

                        DashboardPage.IOT_ENVIRONMENT -> {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(horizontal = 24.dp, vertical = 72.dp)
                            ) {
                                val maxContentW = if (maxWidth > 900.dp) 900.dp else maxWidth
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = maxContentW)
                                        .align(Alignment.TopCenter)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    IotHabitatPanel(
                                        devices = iotDevices,
                                        deviceStatus = iotDeviceStatus,
                                        brokerStates = iotBrokerStates,
                                        onSaveDevice = { viewModel.saveIotDevice(it) },
                                        onDeleteDevice = { viewModel.deleteIotDevice(it) },
                                        onToggleSwitch = { viewModel.toggleIotSwitch(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Top Control Bar (Revealed on tap, fades away in ambient mode)
                AnimatedVisibility(
                    visible = controlsVisible && !showSettings,
                    enter = fadeIn(tween(260)) + slideInVertically(tween(280)) { -it / 2 },
                    exit = fadeOut(tween(340)) + slideOutVertically(tween(320)) { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scene Selector Glass Pill
                        LiquidGlassSurface(cornerRadius = 24.dp) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.cycleNextScene(-1) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("prev_scene_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous scene", tint = StillroomLinen)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { showSettings = true }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = activeScene?.name ?: settings.backgroundType.displayName,
                                        fontFamily = PlusJakartaFontFamily,
                                        fontSize = 13.sp,
                                        color = StillroomLinen
                                    )
                                    Text(
                                        text = if (settings.autoSceneEngineEnabled) "AUTO ENGINE" else "MANUAL SCENE",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = StillroomAmber,
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.cycleNextScene(1) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("next_scene_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next scene", tint = StillroomLinen)
                                }
                            }
                        }

                        // Right System Micro-Chip + Idle + Settings
                        LiquidGlassSurface(cornerRadius = 24.dp) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${telemetry.batteryPercent}% · ${telemetry.networkType}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = StillroomBone,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.enterAmbientIdleNow() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("enter_ambient_idle_btn")
                                ) {
                                    Icon(Icons.Outlined.Bedtime, contentDescription = "Enter ambient idle mode", tint = StillroomSage)
                                }
                                IconButton(
                                    onClick = { showSettings = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_settings_btn")
                                ) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "Open Stillroom Studio settings", tint = StillroomAmber)
                                }
                            }
                        }
                    }
                }

                // Floating Bottom Liquid Glass Dock
                AnimatedVisibility(
                    visible = controlsVisible && !showSettings,
                    enter = fadeIn(tween(260)) + slideInVertically(tween(280)) { it / 2 },
                    exit = fadeOut(tween(340)) + slideOutVertically(tween(320)) { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    LiquidGlassSurface(cornerRadius = 28.dp) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DashboardPage.entries.forEachIndexed { idx, page ->
                                val selected = pagerState.currentPage == idx
                                val icon = when (page) {
                                    DashboardPage.AMBIENT_CANVAS -> Icons.Outlined.AutoAwesome
                                    DashboardPage.PLANNER_FOCUS -> Icons.Outlined.EventNote
                                    DashboardPage.TECH_FEEDS -> Icons.Outlined.Public
                                    DashboardPage.IOT_ENVIRONMENT -> Icons.Outlined.Hub
                                }
                                Row(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("nav_page_${page.name.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = page.title,
                                        tint = if (selected) StillroomAmber else StillroomBone,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = page.title,
                                        fontFamily = PlusJakartaFontFamily,
                                        fontSize = 13.sp,
                                        color = if (selected) StillroomLinen else StillroomBone
                                    )
                                }
                            }
                        }
                    }
                }

                // Weather Expanded Glass Sheet
                if (showWeatherSheet) {
                    BackHandler { showWeatherSheet = false }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(StillroomObsidian.copy(alpha = 0.78f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showWeatherSheet = false }
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassSurface(
                            cornerRadius = 28.dp,
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* consume */ }
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(StillroomEspresso.copy(alpha = 0.78f))
                                    .padding(22.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { showWeatherSheet = false }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Close weather details", tint = StillroomLinen)
                                    }
                                }
                                WeatherExpandedSheet(
                                    state = weatherState,
                                    cityName = settings.weatherCityName,
                                    useFahrenheit = settings.useFahrenheit,
                                    cityResults = cityResults,
                                    onSearchCity = { viewModel.searchCity(it) },
                                    onSelectCity = { viewModel.selectCity(it) },
                                    onUseCoarseLocation = {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    },
                                    onToggleUnit = {
                                        viewModel.updateSettings { it.copy(useFahrenheit = !it.useFahrenheit) }
                                        viewModel.refreshWeather()
                                    },
                                    onRefresh = { viewModel.refreshWeather() }
                                )
                            }
                        }
                    }
                }

                // Studio Settings Overlay
                if (showSettings) {
                    StillroomSettingsSheet(
                        settings = settings,
                        scenes = scenes,
                        onClose = { showSettings = false },
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onApplyGlassPreset = { viewModel.applyGlassPreset(it) },
                        onApplyScene = { viewModel.applyScene(it, keepAutoEngine = false) },
                        onSaveScene = { viewModel.saveSceneProfile(it) },
                        onDeleteScene = { viewModel.deleteSceneProfile(it) },
                        onSelectCustomImage = { viewModel.persistCustomBackgroundUri(it) },
                        onToggleWidget = { viewModel.toggleWidgetVisibility(it) },
                        onSetWidgetSize = { wt, sz -> viewModel.setWidgetSize(wt, sz) },
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onOpenNotificationAccess = openNotificationSettings,
                        onResetAll = { viewModel.resetAllSettings() }
                    )
                }
            }
        )
    }

    // Long-press Widget Customization Dialog
    customizingWidget?.let { wt ->
        val currentSize = settings.widgetSizes[wt] ?: WidgetSize.STANDARD
        AlertDialog(
            onDismissRequest = { customizingWidget = null },
            containerColor = StillroomCharcoal,
            title = { Text(wt.title, fontFamily = CormorantFontFamily, fontSize = 24.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Widget Size", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomBone)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetSize.entries.forEach { sz ->
                            FilterChip(
                                selected = currentSize == sz,
                                onClick = { viewModel.setWidgetSize(wt, sz) },
                                label = { Text(sz.displayName) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleWidgetVisibility(wt)
                    customizingWidget = null
                }) { Text("Hide Widget", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { customizingWidget = null }) { Text("Done", color = StillroomLinen) }
            }
        )
    }

    // Custom Mantra Editor
    if (editingMantra) {
        var draft by remember(settings.customMantraText) { mutableStateOf(settings.customMantraText) }
        AlertDialog(
            onDismissRequest = { editingMantra = false },
            containerColor = StillroomCharcoal,
            title = { Text("Custom Ambient Text", fontFamily = CormorantFontFamily, fontSize = 24.sp, color = StillroomLinen) },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Mantra or note") }, minLines = 2)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSettings { it.copy(customMantraText = draft) }
                    editingMantra = false
                }) { Text("Save", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { editingMantra = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }
}

@Composable
private fun AmbientCanvasPage(
    viewModel: StillroomViewModel,
    settings: com.example.domain.model.AppSettings,
    isIdle: Boolean,
    controlsVisible: Boolean,
    burnInX: androidx.compose.ui.unit.Dp,
    burnInY: androidx.compose.ui.unit.Dp,
    audioFrame: com.example.data.audio.AudioFrameData,
    mediaState: com.example.data.media.MediaTrackState,
    weatherState: com.example.domain.model.ResourceState<com.example.data.network.WeatherSnapshot>,
    knowledge: com.example.data.device.KnowledgeEntry,
    onToggleControls: () -> Unit,
    onSwipeScene: (Int) -> Unit,
    onRequestAudioPerm: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onExpandWeather: () -> Unit,
    onEditMantra: () -> Unit,
    onLongPressWidget: (WidgetType) -> Unit
) {
    var verticalDragAccum by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (verticalDragAccum < -80f) onSwipeScene(1)
                        else if (verticalDragAccum > 80f) onSwipeScene(-1)
                        verticalDragAccum = 0f
                    },
                    onVerticalDrag = { _, dragAmount -> verticalDragAccum += dragAmount }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleControls
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 22.dp)
            .offset(x = burnInX, y = burnInY)
    ) {
        val isLandscapeTablet = maxWidth >= 720.dp

        // Audio Visualizer layered organically according to position preference
        if (settings.visualizerConfig.enabled) {
            val visAlignment = when (settings.visualizerConfig.position) {
                VisualizerPosition.BOTTOM_STAGE -> Alignment.BottomCenter
                VisualizerPosition.CENTER_HALO -> Alignment.Center
                VisualizerPosition.TOP_RIBBON -> Alignment.TopCenter
            }
            val visHeight = if (settings.visualizerConfig.position == VisualizerPosition.CENTER_HALO) 260.dp else 170.dp
            AudioVisualizerCanvas(
                config = settings.visualizerConfig,
                frame = audioFrame,
                controlsVisible = controlsVisible,
                onRequestAudioPermission = onRequestAudioPerm,
                modifier = Modifier
                    .align(visAlignment)
                    .fillMaxWidth()
                    .height(visHeight)
                    .padding(bottom = if (controlsVisible) 54.dp else 8.dp)
            )
        }

        if (isLandscapeTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, bottom = 60.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hero Clock Zone
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .padding(end = 24.dp),
                    contentAlignment = when (settings.clockConfig.position) {
                        ClockPosition.CENTER -> Alignment.CenterStart
                        ClockPosition.BOTTOM_LEFT -> Alignment.BottomStart
                        else -> Alignment.TopStart
                    }
                ) {
                    AmbientClockView(
                        config = settings.clockConfig,
                        isIdle = isIdle,
                        reducedMotion = settings.reducedMotion
                    )
                }

                // Curated Right Ambient Rail
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .widthIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (WidgetType.WEATHER !in settings.hiddenWidgets) {
                        WeatherAmbientTile(
                            state = weatherState,
                            cityName = settings.weatherCityName,
                            useFahrenheit = settings.useFahrenheit,
                            size = settings.widgetSizes[WidgetType.WEATHER] ?: WidgetSize.STANDARD,
                            onExpand = onExpandWeather,
                            onLongPress = { onLongPressWidget(WidgetType.WEATHER) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (WidgetType.MEDIA_NOW_PLAYING !in settings.hiddenWidgets) {
                        MediaNowPlayingTile(
                            mediaState = mediaState,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.skipNextTrack() },
                            onPrev = { viewModel.skipPrevTrack() },
                            onOpenNotificationAccess = onOpenNotificationAccess,
                            onLongPress = { onLongPressWidget(WidgetType.MEDIA_NOW_PLAYING) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!isIdle && WidgetType.KNOWLEDGE_QUOTE !in settings.hiddenWidgets) {
                        DailyKnowledgeTile(
                            entry = knowledge,
                            onNext = { viewModel.nextKnowledgeEntry() },
                            onLongPress = { onLongPressWidget(WidgetType.KNOWLEDGE_QUOTE) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (WidgetType.CUSTOM_MANTRA !in settings.hiddenWidgets) {
                        CustomMantraTile(
                            text = settings.customMantraText,
                            onEditClick = onEditMantra,
                            onLongPress = { onLongPressWidget(WidgetType.CUSTOM_MANTRA) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Portrait Tablet / Phone Responsive Stack
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp, bottom = 68.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AmbientClockView(
                    config = settings.clockConfig,
                    isIdle = isIdle,
                    reducedMotion = settings.reducedMotion
                )
                if (WidgetType.WEATHER !in settings.hiddenWidgets) {
                    WeatherAmbientTile(
                        state = weatherState,
                        cityName = settings.weatherCityName,
                        useFahrenheit = settings.useFahrenheit,
                        size = settings.widgetSizes[WidgetType.WEATHER] ?: WidgetSize.STANDARD,
                        onExpand = onExpandWeather,
                        onLongPress = { onLongPressWidget(WidgetType.WEATHER) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (WidgetType.MEDIA_NOW_PLAYING !in settings.hiddenWidgets) {
                    MediaNowPlayingTile(
                        mediaState = mediaState,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.skipNextTrack() },
                        onPrev = { viewModel.skipPrevTrack() },
                        onOpenNotificationAccess = onOpenNotificationAccess,
                        onLongPress = { onLongPressWidget(WidgetType.MEDIA_NOW_PLAYING) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!isIdle && WidgetType.KNOWLEDGE_QUOTE !in settings.hiddenWidgets) {
                    DailyKnowledgeTile(
                        entry = knowledge,
                        onNext = { viewModel.nextKnowledgeEntry() },
                        onLongPress = { onLongPressWidget(WidgetType.KNOWLEDGE_QUOTE) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AtelierPlannerPage(
    settings: com.example.domain.model.AppSettings,
    hasCalendarPerm: Boolean,
    calendarEvents: List<com.example.data.device.CalendarAgendaEvent>,
    countdowns: List<com.example.data.local.CountdownEntity>,
    timerState: com.example.viewmodel.TimerUiState,
    notes: List<com.example.data.local.NoteEntity>,
    tasks: List<com.example.data.local.TodoTaskEntity>,
    habits: List<com.example.data.local.HabitEntity>,
    reminders: List<com.example.data.local.ReminderEntity>,
    viewModel: StillroomViewModel,
    onRequestCalendarPerm: () -> Unit,
    onLongPressWidget: (WidgetType) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 74.dp)
    ) {
        val isWide = maxWidth >= 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = DashboardPage.PLANNER_FOCUS.title,
                fontFamily = CormorantFontFamily,
                fontSize = 34.sp,
                color = StillroomLinen
            )
            if (isWide) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (WidgetType.CALENDAR_AGENDA !in settings.hiddenWidgets) {
                            CalendarAgendaTile(
                                hasPermission = hasCalendarPerm,
                                events = calendarEvents,
                                onRequestPermission = onRequestCalendarPerm,
                                onRefresh = { viewModel.refreshCalendar() },
                                onLongPress = { onLongPressWidget(WidgetType.CALENDAR_AGENDA) }
                            )
                        }
                        if (WidgetType.COUNTDOWNS !in settings.hiddenWidgets) {
                            CountdownsTile(
                                countdowns = countdowns,
                                onAddCountdown = { t, c, ms -> viewModel.addCountdown(t, c, ms) },
                                onDeleteCountdown = { viewModel.deleteCountdown(it) },
                                onLongPress = { onLongPressWidget(WidgetType.COUNTDOWNS) }
                            )
                        }
                        if (WidgetType.HABITS_REMINDERS !in settings.hiddenWidgets) {
                            HabitsAndRemindersTile(
                                habits = habits,
                                reminders = reminders,
                                onAddHabit = { viewModel.addHabit(it) },
                                onCheckHabit = { viewModel.checkHabitToday(it) },
                                onDeleteHabit = { viewModel.deleteHabit(it) },
                                onAddReminder = { t, d -> viewModel.addReminder(t, d) },
                                onToggleReminder = { viewModel.toggleReminder(it) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                onLongPress = { onLongPressWidget(WidgetType.HABITS_REMINDERS) }
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (WidgetType.FOCUS_TIMER !in settings.hiddenWidgets) {
                            FocusTimerAndStopwatchTile(
                                timerState = timerState,
                                onSelectPhase = { viewModel.selectPomodoroPhase(it) },
                                onToggleTimer = { viewModel.toggleTimerRunning() },
                                onResetTimer = { viewModel.resetTimer() },
                                onToggleStopwatch = { viewModel.toggleStopwatch() },
                                onLapStopwatch = { viewModel.lapStopwatch() },
                                onResetStopwatch = { viewModel.resetStopwatch() },
                                onLongPress = { onLongPressWidget(WidgetType.FOCUS_TIMER) }
                            )
                        }
                        if (WidgetType.TODOS_GOALS !in settings.hiddenWidgets) {
                            TasksAndGoalsTile(
                                tasks = tasks,
                                onAddTask = { t, g -> viewModel.addTask(t, g) },
                                onToggleTask = { viewModel.toggleTask(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onLongPress = { onLongPressWidget(WidgetType.TODOS_GOALS) }
                            )
                        }
                        if (WidgetType.QUICK_NOTES !in settings.hiddenWidgets) {
                            QuickNotesTile(
                                notes = notes,
                                onSaveNote = { id, t, b -> viewModel.saveNote(id, t, b) },
                                onDeleteNote = { viewModel.deleteNote(it) },
                                onLongPress = { onLongPressWidget(WidgetType.QUICK_NOTES) }
                            )
                        }
                    }
                }
            } else {
                if (WidgetType.FOCUS_TIMER !in settings.hiddenWidgets) {
                    FocusTimerAndStopwatchTile(
                        timerState = timerState,
                        onSelectPhase = { viewModel.selectPomodoroPhase(it) },
                        onToggleTimer = { viewModel.toggleTimerRunning() },
                        onResetTimer = { viewModel.resetTimer() },
                        onToggleStopwatch = { viewModel.toggleStopwatch() },
                        onLapStopwatch = { viewModel.lapStopwatch() },
                        onResetStopwatch = { viewModel.resetStopwatch() },
                        onLongPress = { onLongPressWidget(WidgetType.FOCUS_TIMER) }
                    )
                }
                if (WidgetType.CALENDAR_AGENDA !in settings.hiddenWidgets) {
                    CalendarAgendaTile(
                        hasPermission = hasCalendarPerm,
                        events = calendarEvents,
                        onRequestPermission = onRequestCalendarPerm,
                        onRefresh = { viewModel.refreshCalendar() },
                        onLongPress = { onLongPressWidget(WidgetType.CALENDAR_AGENDA) }
                    )
                }
                if (WidgetType.TODOS_GOALS !in settings.hiddenWidgets) {
                    TasksAndGoalsTile(
                        tasks = tasks,
                        onAddTask = { t, g -> viewModel.addTask(t, g) },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onLongPress = { onLongPressWidget(WidgetType.TODOS_GOALS) }
                    )
                }
                if (WidgetType.QUICK_NOTES !in settings.hiddenWidgets) {
                    QuickNotesTile(
                        notes = notes,
                        onSaveNote = { id, t, b -> viewModel.saveNote(id, t, b) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onLongPress = { onLongPressWidget(WidgetType.QUICK_NOTES) }
                    )
                }
                if (WidgetType.COUNTDOWNS !in settings.hiddenWidgets) {
                    CountdownsTile(
                        countdowns = countdowns,
                        onAddCountdown = { t, c, ms -> viewModel.addCountdown(t, c, ms) },
                        onDeleteCountdown = { viewModel.deleteCountdown(it) },
                        onLongPress = { onLongPressWidget(WidgetType.COUNTDOWNS) }
                    )
                }
                if (WidgetType.HABITS_REMINDERS !in settings.hiddenWidgets) {
                    HabitsAndRemindersTile(
                        habits = habits,
                        reminders = reminders,
                        onAddHabit = { viewModel.addHabit(it) },
                        onCheckHabit = { viewModel.checkHabitToday(it) },
                        onDeleteHabit = { viewModel.deleteHabit(it) },
                        onAddReminder = { t, d -> viewModel.addReminder(t, d) },
                        onToggleReminder = { viewModel.toggleReminder(it) },
                        onDeleteReminder = { viewModel.deleteReminder(it) },
                        onLongPress = { onLongPressWidget(WidgetType.HABITS_REMINDERS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TechSignalsPage(
    settings: com.example.domain.model.AppSettings,
    telemetry: com.example.data.device.SystemTelemetry,
    rssArticles: com.example.domain.model.ResourceState<List<com.example.data.network.RssArticle>>,
    rssFeeds: List<com.example.data.local.RssFeedSourceEntity>,
    githubState: com.example.domain.model.ResourceState<com.example.data.network.GitHubSnapshot>,
    nasaState: com.example.domain.model.ResourceState<com.example.data.network.NasaApodDto>,
    issState: com.example.domain.model.ResourceState<com.example.data.network.IssPositionDto>,
    wikiState: com.example.domain.model.ResourceState<com.example.data.network.WikiSummaryDto>,
    viewModel: StillroomViewModel,
    onLongPressWidget: (WidgetType) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 74.dp)
    ) {
        val isWide = maxWidth >= 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = DashboardPage.TECH_FEEDS.title,
                fontFamily = CormorantFontFamily,
                fontSize = 34.sp,
                color = StillroomLinen
            )
            if (isWide) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (WidgetType.SYSTEM_TELEMETRY !in settings.hiddenWidgets) {
                            SystemTelemetryTile(
                                telemetry = telemetry,
                                onLongPress = { onLongPressWidget(WidgetType.SYSTEM_TELEMETRY) }
                            )
                        }
                        if (WidgetType.RSS_NEWS !in settings.hiddenWidgets) {
                            RssFeedTile(
                                state = rssArticles,
                                feeds = rssFeeds,
                                onRefresh = { viewModel.refreshRss() },
                                onAddFeed = { t, u -> viewModel.addRssFeed(t, u) },
                                onDeleteFeed = { viewModel.deleteRssFeed(it) },
                                onLongPress = { onLongPressWidget(WidgetType.RSS_NEWS) }
                            )
                        }
                        if (WidgetType.WIKIPEDIA_DISCOVERY !in settings.hiddenWidgets) {
                            WikipediaTile(
                                state = wikiState,
                                onNextRandom = { viewModel.refreshWikipedia() },
                                onLongPress = { onLongPressWidget(WidgetType.WIKIPEDIA_DISCOVERY) }
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (WidgetType.NASA_APOD !in settings.hiddenWidgets || WidgetType.ISS_TRACKER !in settings.hiddenWidgets) {
                            NasaAndIssTile(
                                nasaState = nasaState,
                                issState = issState,
                                onRefreshIss = { viewModel.refreshIssPosition() },
                                onRefreshNasa = { viewModel.refreshNasaApod() },
                                onLongPress = { onLongPressWidget(WidgetType.NASA_APOD) }
                            )
                        }
                        if (WidgetType.GITHUB_ACTIVITY !in settings.hiddenWidgets) {
                            GitHubTile(
                                state = githubState,
                                username = settings.githubUsername,
                                onRefresh = { viewModel.refreshGitHub() },
                                onLongPress = { onLongPressWidget(WidgetType.GITHUB_ACTIVITY) }
                            )
                        }
                    }
                }
            } else {
                if (WidgetType.SYSTEM_TELEMETRY !in settings.hiddenWidgets) {
                    SystemTelemetryTile(telemetry = telemetry, onLongPress = { onLongPressWidget(WidgetType.SYSTEM_TELEMETRY) })
                }
                if (WidgetType.NASA_APOD !in settings.hiddenWidgets) {
                    NasaAndIssTile(
                        nasaState = nasaState,
                        issState = issState,
                        onRefreshIss = { viewModel.refreshIssPosition() },
                        onRefreshNasa = { viewModel.refreshNasaApod() },
                        onLongPress = { onLongPressWidget(WidgetType.NASA_APOD) }
                    )
                }
                if (WidgetType.RSS_NEWS !in settings.hiddenWidgets) {
                    RssFeedTile(
                        state = rssArticles,
                        feeds = rssFeeds,
                        onRefresh = { viewModel.refreshRss() },
                        onAddFeed = { t, u -> viewModel.addRssFeed(t, u) },
                        onDeleteFeed = { viewModel.deleteRssFeed(it) },
                        onLongPress = { onLongPressWidget(WidgetType.RSS_NEWS) }
                    )
                }
                if (WidgetType.GITHUB_ACTIVITY !in settings.hiddenWidgets) {
                    GitHubTile(
                        state = githubState,
                        username = settings.githubUsername,
                        onRefresh = { viewModel.refreshGitHub() },
                        onLongPress = { onLongPressWidget(WidgetType.GITHUB_ACTIVITY) }
                    )
                }
                if (WidgetType.WIKIPEDIA_DISCOVERY !in settings.hiddenWidgets) {
                    WikipediaTile(
                        state = wikiState,
                        onNextRandom = { viewModel.refreshWikipedia() },
                        onLongPress = { onLongPressWidget(WidgetType.WIKIPEDIA_DISCOVERY) }
                    )
                }
            }
        }
    }
}
