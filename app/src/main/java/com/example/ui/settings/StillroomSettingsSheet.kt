package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AmbientSceneType
import com.example.domain.model.AppSettings
import com.example.domain.model.ClockPosition
import com.example.domain.model.ClockSize
import com.example.domain.model.ClockStyle
import com.example.domain.model.ClockTypography
import com.example.domain.model.GlassPreset
import com.example.domain.model.SceneConditionType
import com.example.domain.model.SceneConfig
import com.example.domain.model.SceneTransitionType
import com.example.domain.model.VisualizerColorPreset
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPosition
import com.example.domain.model.VisualizerRange
import com.example.domain.model.WidgetSize
import com.example.domain.model.WidgetType
import com.example.domain.model.WorldClockZone
import com.example.ui.ambient.AmbientBackgroundEngine
import com.example.ui.glass.GlassBackdropHost
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomCharcoal
import com.example.ui.theme.StillroomCrimson
import com.example.ui.theme.StillroomEspresso
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomObsidian
import com.example.ui.theme.StillroomSage
import kotlin.math.roundToInt

private enum class SettingsTab(val label: String, val icon: ImageVector) {
    SCENES("Scenes", Icons.Outlined.AutoAwesome),
    GLASS("Liquid Glass", Icons.Outlined.BlurOn),
    AMBIENT("Canvas", Icons.Outlined.Wallpaper),
    CLOCK("Clock", Icons.Outlined.Schedule),
    AUDIO("Visualizer", Icons.Outlined.GraphicEq),
    WIDGETS("Widgets", Icons.Outlined.DashboardCustomize),
    INTEGRATIONS("Integrations", Icons.Outlined.Hub),
    IDLE_POWER("Idle & Power", Icons.Outlined.BatterySaver)
}

@Composable
fun StillroomSettingsSheet(
    settings: AppSettings,
    scenes: List<SceneConfig>,
    onClose: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onApplyGlassPreset: (GlassPreset) -> Unit,
    onApplyScene: (SceneConfig) -> Unit,
    onSaveScene: (SceneConfig) -> Unit,
    onDeleteScene: (String) -> Unit,
    onSelectCustomImage: (Uri) -> Unit,
    onToggleWidget: (WidgetType) -> Unit,
    onSetWidgetSize: (WidgetType, WidgetSize) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    var activeTab by remember { mutableStateOf(SettingsTab.SCENES) }
    var showCreateScene by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onSelectCustomImage(uri) }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StillroomObsidian.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        LiquidGlassSurface(
            cornerRadius = 28.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StillroomEspresso.copy(alpha = 0.72f))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Stillroom Studio", fontFamily = CormorantFontFamily, fontSize = 30.sp, color = StillroomLinen)
                        Text("Live optical, ambient, and automation controls", fontSize = 12.sp, color = StillroomBone)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_settings_button")
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close settings", tint = StillroomLinen)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Live miniature preview of Scene + Liquid Glass
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(20.dp))
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
                                dimmingOverlay = 0.1f,
                                nightModeTint = settings.nightModeTint,
                                reducedMotion = settings.reducedMotion,
                                lowPowerMode = settings.lowPowerMode,
                                isIdle = false,
                                audioEnergy = 0.15f
                            )
                        },
                        overlay = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LIVE PREVIEW", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomLinen)
                                LiquidGlassSurface(
                                    config = settings.glassConfig,
                                    modifier = Modifier.width(240.dp).height(76.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "${settings.glassConfig.preset.displayName} · ${(settings.glassConfig.transparency * 100).roundToInt()}% Clear",
                                            fontFamily = PlusJakartaFontFamily,
                                            fontSize = 13.sp,
                                            color = StillroomLinen
                                        )
                                        Text(
                                            "Refraction ${(settings.glassConfig.refractionStrength * 100).roundToInt()}% · ${settings.backgroundType.displayName}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = StillroomAmber
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val isWide = maxWidth >= 700.dp
                    if (isWide) {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(
                                modifier = Modifier
                                    .width(190.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SettingsTab.entries.forEach { tab ->
                                    SettingsNavItem(tab, activeTab == tab) { activeTab = tab }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SettingsTabContent(
                                    tab = activeTab,
                                    settings = settings,
                                    scenes = scenes,
                                    onUpdateSettings = onUpdateSettings,
                                    onApplyGlassPreset = onApplyGlassPreset,
                                    onApplyScene = onApplyScene,
                                    onOpenCreateScene = { showCreateScene = true },
                                    onDeleteScene = onDeleteScene,
                                    onLaunchPhotoPicker = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onToggleWidget = onToggleWidget,
                                    onSetWidgetSize = onSetWidgetSize,
                                    onRequestAudioPermission = onRequestAudioPermission,
                                    onOpenNotificationAccess = onOpenNotificationAccess,
                                    onResetAll = onResetAll
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SettingsTab.entries.forEach { tab ->
                                    FilterChip(
                                        selected = activeTab == tab,
                                        onClick = { activeTab = tab },
                                        label = { Text(tab.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SettingsTabContent(
                                    tab = activeTab,
                                    settings = settings,
                                    scenes = scenes,
                                    onUpdateSettings = onUpdateSettings,
                                    onApplyGlassPreset = onApplyGlassPreset,
                                    onApplyScene = onApplyScene,
                                    onOpenCreateScene = { showCreateScene = true },
                                    onDeleteScene = onDeleteScene,
                                    onLaunchPhotoPicker = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onToggleWidget = onToggleWidget,
                                    onSetWidgetSize = onSetWidgetSize,
                                    onRequestAudioPermission = onRequestAudioPermission,
                                    onOpenNotificationAccess = onOpenNotificationAccess,
                                    onResetAll = onResetAll
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateScene) {
        var name by remember { mutableStateOf("Custom Scene") }
        var condition by remember { mutableStateOf(SceneConditionType.MANUAL) }
        AlertDialog(
            onDismissRequest = { showCreateScene = false },
            containerColor = StillroomCharcoal,
            title = { Text("Save Current Scene Preset", fontFamily = CormorantFontFamily, fontSize = 24.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scene name") }, singleLine = true)
                    Text("Automation Trigger", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomBone)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SceneConditionType.entries.forEach { cond ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { condition = cond }
                                    .padding(vertical = 4.dp)
                            ) {
                                FilterChip(selected = condition == cond, onClick = { condition = cond }, label = { Text(cond.displayName, fontSize = 11.sp) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newScene = SceneConfig(
                        id = "scene_custom_${System.currentTimeMillis()}",
                        name = name.ifBlank { "Custom Scene" },
                        condition = condition,
                        backgroundType = settings.backgroundType,
                        transitionType = settings.transitionType,
                        clockStyle = settings.clockConfig.style,
                        clockPosition = settings.clockConfig.position,
                        visualizerMode = settings.visualizerConfig.mode,
                        visualizerEnabled = settings.visualizerConfig.enabled,
                        glassPreset = settings.glassConfig.preset,
                        ambientDimming = 0.14f,
                        motionSpeed = settings.motionSpeed
                    )
                    onSaveScene(newScene)
                    showCreateScene = false
                }) { Text("Save Preset", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateScene = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }
}

@Composable
private fun SettingsNavItem(tab: SettingsTab, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) StillroomAmber.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tab.icon, contentDescription = null, tint = if (selected) StillroomAmber else StillroomBone, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(tab.label, color = if (selected) StillroomLinen else StillroomBone, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsTabContent(
    tab: SettingsTab,
    settings: AppSettings,
    scenes: List<SceneConfig>,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onApplyGlassPreset: (GlassPreset) -> Unit,
    onApplyScene: (SceneConfig) -> Unit,
    onOpenCreateScene: () -> Unit,
    onDeleteScene: (String) -> Unit,
    onLaunchPhotoPicker: () -> Unit,
    onToggleWidget: (WidgetType) -> Unit,
    onSetWidgetSize: (WidgetType, WidgetSize) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onResetAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        when (tab) {
            SettingsTab.SCENES -> {
                SettingToggleRow(
                    title = "Automatic Scene Engine",
                    subtitle = "Switch scenes by time of day, music playback, or charging",
                    checked = settings.autoSceneEngineEnabled,
                    onCheckedChange = { enabled -> onUpdateSettings { it.copy(autoSceneEngineEnabled = enabled) } }
                )
                Button(
                    onClick = onOpenCreateScene,
                    colors = ButtonDefaults.buttonColors(containerColor = StillroomAmber, contentColor = StillroomObsidian),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Save Current Dashboard as Scene")
                }
                scenes.forEach { scene ->
                    val isActive = scene.id == settings.activeSceneId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isActive) StillroomAmber.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                            .clickable { onApplyScene(scene) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(scene.name, color = StillroomLinen, fontSize = 15.sp)
                            Text(
                                "${scene.condition.displayName} · ${scene.backgroundType.displayName} · ${scene.glassPreset.displayName}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (isActive) StillroomAmber else StillroomAsh
                            )
                        }
                        if (scene.id.startsWith("scene_custom_")) {
                            IconButton(onClick = { onDeleteScene(scene.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete scene", tint = StillroomAsh)
                            }
                        }
                    }
                }
            }

            SettingsTab.GLASS -> {
                Text("GLASS MATERIAL PRESETS", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.glassConfig.preset == preset,
                            onClick = { onApplyGlassPreset(preset) },
                            label = { Text(preset.displayName) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transparency: ${(settings.glassConfig.transparency * 100).roundToInt()}%", color = StillroomLinen, fontSize = 14.sp)
                    TextButton(onClick = {
                        onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(transparency = 0.99f)) }
                    }) {
                        Text("Snap to 99% Crystal", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    }
                }
                SettingSlider("Transparency (0% – 100%)", settings.glassConfig.transparency, 0f..1f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(transparency = v)) }
                }
                SettingSlider("Optical Refraction", settings.glassConfig.refractionStrength, 0f..1.4f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(refractionStrength = v)) }
                }
                SettingSlider("Specular Edge Highlight", settings.glassConfig.edgeHighlightStrength, 0f..1.4f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(edgeHighlightStrength = v)) }
                }
                SettingSlider("Chromatic Dispersion", settings.glassConfig.dispersionStrength, 0f..1.4f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(dispersionStrength = v)) }
                }
                SettingSlider("Supplementary Blur (dp)", settings.glassConfig.blurRadiusDp, 0f..24f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(blurRadiusDp = v)) }
                }
                SettingSlider("Corner Radius (dp)", settings.glassConfig.cornerRadiusDp, 8f..40f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(cornerRadiusDp = v)) }
                }
                SettingSlider("Shadow Depth", settings.glassConfig.shadowElevationDp, 0f..28f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(shadowElevationDp = v)) }
                }
                SettingSlider("Interaction Elasticity", settings.glassConfig.interactionElasticity, 0f..1.3f) { v ->
                    onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(interactionElasticity = v)) }
                }
            }

            SettingsTab.AMBIENT -> {
                Text("BACKGROUND SCENE", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                AmbientSceneType.entries.forEach { bg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (settings.backgroundType == bg) StillroomAmber.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.03f))
                            .clickable { onUpdateSettings { it.copy(backgroundType = bg) } }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bg.displayName, color = StillroomLinen, fontSize = 14.sp)
                            Text(bg.subtitle, fontSize = 11.sp, color = StillroomBone)
                        }
                        if (bg == AmbientSceneType.CUSTOM_IMAGE) {
                            OutlinedButton(onClick = onLaunchPhotoPicker) {
                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pick Photo", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Text("SCENE TRANSITION", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SceneTransitionType.entries.forEach { tr ->
                        FilterChip(
                            selected = settings.transitionType == tr,
                            onClick = { onUpdateSettings { it.copy(transitionType = tr) } },
                            label = { Text(tr.displayName) }
                        )
                    }
                }
                SettingSlider("Animation Speed", settings.motionSpeed, 0.2f..2.0f) { v ->
                    onUpdateSettings { it.copy(motionSpeed = v) }
                }
                SettingSlider("Warmth Atmosphere", settings.warmthTint, 0f..1f) { v ->
                    onUpdateSettings { it.copy(warmthTint = v) }
                }
                SettingSlider("Film Grain Texture", settings.grainOpacity, 0f..0.25f) { v ->
                    onUpdateSettings { it.copy(grainOpacity = v) }
                }
            }

            SettingsTab.CLOCK -> {
                Text("CLOCK STYLE", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockStyle.entries.forEach { st ->
                        FilterChip(
                            selected = settings.clockConfig.style == st,
                            onClick = { onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(style = st)) } },
                            label = { Text(st.displayName) }
                        )
                    }
                }
                Text("TYPOGRAPHY", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockTypography.entries.forEach { tp ->
                        FilterChip(
                            selected = settings.clockConfig.typography == tp,
                            onClick = { onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(typography = tp)) } },
                            label = { Text(tp.displayName) }
                        )
                    }
                }
                Text("SIZE & POSITION", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockSize.entries.forEach { sz ->
                        FilterChip(
                            selected = settings.clockConfig.size == sz,
                            onClick = { onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(size = sz)) } },
                            label = { Text(sz.displayName) }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = settings.clockConfig.position == pos,
                            onClick = { onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(position = pos)) } },
                            label = { Text(pos.displayName) }
                        )
                    }
                }
                SettingToggleRow("24-Hour Clock", "Use 24-hour military time", settings.clockConfig.is24Hour) { v ->
                    onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(is24Hour = v)) }
                }
                SettingToggleRow("Show Seconds", "Display seconds ticker", settings.clockConfig.showSeconds) { v ->
                    onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(showSeconds = v)) }
                }
                SettingToggleRow("Show Date", "Display weekday, month & day", settings.clockConfig.showDate) { v ->
                    onUpdateSettings { it.copy(clockConfig = it.clockConfig.copy(showDate = v)) }
                }

                // World clock quick toggle
                Text("WORLD TIME ZONES", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                val presets = listOf(
                    WorldClockZone("America/New_York", "NYC"),
                    WorldClockZone("Europe/London", "LDN"),
                    WorldClockZone("Asia/Tokyo", "TYO"),
                    WorldClockZone("Europe/Berlin", "BER"),
                    WorldClockZone("Australia/Sydney", "SYD")
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { z ->
                        val active = settings.clockConfig.worldZones.any { it.zoneId == z.zoneId }
                        FilterChip(
                            selected = active,
                            onClick = {
                                onUpdateSettings { cur ->
                                    val next = cur.clockConfig.worldZones.toMutableList()
                                    if (active) next.removeAll { it.zoneId == z.zoneId } else next.add(z)
                                    cur.copy(clockConfig = cur.clockConfig.copy(worldZones = next))
                                }
                            },
                            label = { Text(z.label) }
                        )
                    }
                }
            }

            SettingsTab.AUDIO -> {
                SettingToggleRow("Real-Time Audio Visualizer", "Respond to live system output or microphone audio", settings.visualizerConfig.enabled) { v ->
                    onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(enabled = v)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRequestAudioPermission) {
                        Text("Audio Permission", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onOpenNotificationAccess) {
                        Text("Media Session Access", fontSize = 12.sp)
                    }
                }
                Text("VISUALIZER MODE", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerMode.entries.forEach { m ->
                        FilterChip(
                            selected = settings.visualizerConfig.mode == m,
                            onClick = { onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(mode = m)) } },
                            label = { Text(m.displayName) }
                        )
                    }
                }
                Text("FREQUENCY RANGE & POSITION", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerRange.entries.forEach { r ->
                        FilterChip(
                            selected = settings.visualizerConfig.frequencyRange == r,
                            onClick = { onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(frequencyRange = r)) } },
                            label = { Text(r.displayName) }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerPosition.entries.forEach { p ->
                        FilterChip(
                            selected = settings.visualizerConfig.position == p,
                            onClick = { onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(position = p)) } },
                            label = { Text(p.displayName) }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerColorPreset.entries.forEach { cp ->
                        FilterChip(
                            selected = settings.visualizerConfig.colorPreset == cp,
                            onClick = { onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(colorPreset = cp)) } },
                            label = { Text(cp.displayName) }
                        )
                    }
                }
                SettingSlider("Sensitivity", settings.visualizerConfig.sensitivity, 0.4f..2.8f) { v ->
                    onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(sensitivity = v)) }
                }
                SettingSlider("Smoothing", settings.visualizerConfig.smoothing, 0.1f..0.92f) { v ->
                    onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(smoothing = v)) }
                }
                SettingSlider("Visualizer Size", settings.visualizerConfig.sizeScale, 0.5f..1.5f) { v ->
                    onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(sizeScale = v)) }
                }
                SettingSlider("Visualizer Opacity", settings.visualizerConfig.alpha, 0.2f..1.0f) { v ->
                    onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(alpha = v)) }
                }
                SettingToggleRow("Auto-Scene on Music", "Switch to Sonic Resonance scene when playback starts", settings.visualizerConfig.autoSceneOnMusic) { v ->
                    onUpdateSettings { it.copy(visualizerConfig = it.visualizerConfig.copy(autoSceneOnMusic = v)) }
                }
            }

            SettingsTab.WIDGETS -> {
                var mantraDraft by remember(settings.customMantraText) { mutableStateOf(settings.customMantraText) }
                OutlinedTextField(
                    value = mantraDraft,
                    onValueChange = {
                        mantraDraft = it
                        onUpdateSettings { s -> s.copy(customMantraText = it) }
                    },
                    label = { Text("Custom Mantra / Ambient Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("VISIBILITY & SIZE", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                WidgetType.entries.forEach { wt ->
                    val visible = wt !in settings.hiddenWidgets
                    val size = settings.widgetSizes[wt] ?: WidgetSize.STANDARD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wt.title, color = StillroomLinen, fontSize = 14.sp)
                            Text(wt.defaultPage.title, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomAsh)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val next = WidgetSize.entries[(size.ordinal + 1) % WidgetSize.entries.size]
                                onSetWidgetSize(wt, next)
                            }) {
                                Text(size.displayName, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAmber)
                            }
                            Switch(
                                checked = visible,
                                onCheckedChange = { onToggleWidget(wt) },
                                colors = SwitchDefaults.colors(checkedThumbColor = StillroomAmber)
                            )
                        }
                    }
                }
            }

            SettingsTab.INTEGRATIONS -> {
                var ghUser by remember(settings.githubUsername) { mutableStateOf(settings.githubUsername) }
                var nasaKey by remember(settings.nasaApiKey) { mutableStateOf(settings.nasaApiKey) }
                OutlinedTextField(
                    value = ghUser,
                    onValueChange = {
                        ghUser = it
                        onUpdateSettings { s -> s.copy(githubUsername = it.trim()) }
                    },
                    label = { Text("GitHub Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nasaKey,
                    onValueChange = {
                        nasaKey = it
                        onUpdateSettings { s -> s.copy(nasaApiKey = it.trim().ifBlank { "DEMO_KEY" }) }
                    },
                    label = { Text("NASA APOD API Key (default DEMO_KEY)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SettingToggleRow("Use Fahrenheit (°F)", "Switch Open-Meteo temperature scale", settings.useFahrenheit) { v ->
                    onUpdateSettings { it.copy(useFahrenheit = v) }
                }
                Text("DATA REFRESH INTERVAL", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = settings.networkRefreshMinutes == mins,
                            onClick = { onUpdateSettings { it.copy(networkRefreshMinutes = mins) } },
                            label = { Text("${mins}m") }
                        )
                    }
                }
                SettingToggleRow("Pause Network on Idle", "Avoid background refreshes while tablet is idle", settings.pauseNetworkOnIdle) { v ->
                    onUpdateSettings { it.copy(pauseNetworkOnIdle = v) }
                }
                SettingToggleRow("Wi-Fi Only Refresh", "Skip automatic API refreshes on metered cellular links", settings.wifiOnlyRefresh) { v ->
                    onUpdateSettings { it.copy(wifiOnlyRefresh = v) }
                }
            }

            SettingsTab.IDLE_POWER -> {
                Text("AMBIENT IDLE TIMEOUT", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAsh)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Never", 15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.idleTimeoutSeconds == sec,
                            onClick = { onUpdateSettings { it.copy(idleTimeoutSeconds = sec) } },
                            label = { Text(label) }
                        )
                    }
                }
                SettingToggleRow("Auto-Dim on Idle", "Gently dim window brightness when ambient idle engages", settings.autoDimEnabled) { v ->
                    onUpdateSettings { it.copy(autoDimEnabled = v) }
                }
                SettingSlider("Active Brightness", settings.activeBrightness, 0.2f..1.0f) { v ->
                    onUpdateSettings { it.copy(activeBrightness = v) }
                }
                SettingSlider("Idle Dim Brightness", settings.idleBrightness, 0.05f..0.65f) { v ->
                    onUpdateSettings { it.copy(idleBrightness = v) }
                }
                SettingToggleRow("Burn-In Pixel Shift", "Subtly drift ambient layout while idle", settings.burnInShiftEnabled) { v ->
                    onUpdateSettings { it.copy(burnInShiftEnabled = v) }
                }
                SettingToggleRow("Night Mode Warm Filter", "Apply low-blue-light ember tint", settings.nightModeTint) { v ->
                    onUpdateSettings { it.copy(nightModeTint = v) }
                }
                SettingToggleRow("Reduced Motion", "Minimize transitions and continuous motion", settings.reducedMotion) { v ->
                    onUpdateSettings { it.copy(reducedMotion = v) }
                }
                SettingToggleRow("Low-Power Mode", "Disable optical shader refraction & lower frame updates", settings.lowPowerMode) { v ->
                    onUpdateSettings { it.copy(lowPowerMode = v) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onResetAll,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Reset Stillroom Configuration", color = StillroomCrimson)
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = StillroomLinen, fontSize = 14.sp)
            Text(subtitle, color = StillroomBone, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = StillroomAmber)
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = StillroomBone, fontSize = 13.sp)
            Text("%.2f".format(value), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAmber)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = StillroomAmber, activeTrackColor = StillroomAmber)
        )
    }
}
