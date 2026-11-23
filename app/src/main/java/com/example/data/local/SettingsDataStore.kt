package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AmbientSceneType
import com.example.domain.model.AppSettings
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockPosition
import com.example.domain.model.ClockSize
import com.example.domain.model.ClockStyle
import com.example.domain.model.ClockTypography
import com.example.domain.model.GlassMaterialConfig
import com.example.domain.model.GlassPreset
import com.example.domain.model.SceneTransitionType
import com.example.domain.model.VisualizerColorPreset
import com.example.domain.model.VisualizerConfig
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPosition
import com.example.domain.model.VisualizerRange
import com.example.domain.model.WidgetSize
import com.example.domain.model.WidgetType
import com.example.domain.model.WorldClockZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stillroomDataStore: DataStore<Preferences> by preferencesDataStore(name = "stillroom_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ACTIVE_SCENE_ID = stringPreferencesKey("active_scene_id")
        val AUTO_SCENE_ENGINE = booleanPreferencesKey("auto_scene_engine")

        // Glass
        val GLASS_PRESET = stringPreferencesKey("glass_preset")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val GLASS_REFRACTION = floatPreferencesKey("glass_refraction")
        val GLASS_HIGHLIGHT = floatPreferencesKey("glass_highlight")
        val GLASS_BLUR = floatPreferencesKey("glass_blur")
        val GLASS_TINT = longPreferencesKey("glass_tint")
        val GLASS_CORNER = floatPreferencesKey("glass_corner")
        val GLASS_SHADOW = floatPreferencesKey("glass_shadow")
        val GLASS_DISPERSION = floatPreferencesKey("glass_dispersion")
        val GLASS_ELASTICITY = floatPreferencesKey("glass_elasticity")

        // Clock
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val CLOCK_SECONDS = booleanPreferencesKey("clock_seconds")
        val CLOCK_DATE = booleanPreferencesKey("clock_date")
        val CLOCK_TYPOGRAPHY = stringPreferencesKey("clock_typography")
        val CLOCK_SIZE = stringPreferencesKey("clock_size")
        val CLOCK_POSITION = stringPreferencesKey("clock_position")
        val WORLD_ZONES = stringPreferencesKey("world_zones")

        // Visualizer
        val VIS_ENABLED = booleanPreferencesKey("vis_enabled")
        val VIS_MODE = stringPreferencesKey("vis_mode")
        val VIS_SENSITIVITY = floatPreferencesKey("vis_sensitivity")
        val VIS_SMOOTHING = floatPreferencesKey("vis_smoothing")
        val VIS_RANGE = stringPreferencesKey("vis_range")
        val VIS_SIZE = floatPreferencesKey("vis_size")
        val VIS_ALPHA = floatPreferencesKey("vis_alpha")
        val VIS_POSITION = stringPreferencesKey("vis_position")
        val VIS_COLOR = stringPreferencesKey("vis_color")
        val VIS_AUTO_SCENE = booleanPreferencesKey("vis_auto_scene")

        // Ambient & Idle
        val BG_TYPE = stringPreferencesKey("bg_type")
        val TRANSITION_TYPE = stringPreferencesKey("transition_type")
        val CUSTOM_BG_URI = stringPreferencesKey("custom_bg_uri")
        val MOTION_SPEED = floatPreferencesKey("motion_speed")
        val WARMTH_TINT = floatPreferencesKey("warmth_tint")
        val GRAIN_OPACITY = floatPreferencesKey("grain_opacity")
        val IDLE_TIMEOUT_SEC = intPreferencesKey("idle_timeout_sec")
        val AUTO_DIM = booleanPreferencesKey("auto_dim")
        val IDLE_BRIGHTNESS = floatPreferencesKey("idle_brightness")
        val ACTIVE_BRIGHTNESS = floatPreferencesKey("active_brightness")
        val BURN_IN_SHIFT = booleanPreferencesKey("burn_in_shift")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
        val NIGHT_MODE_TINT = booleanPreferencesKey("night_mode_tint")

        // Integrations
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = doublePreferencesKey("weather_lat")
        val WEATHER_LON = doublePreferencesKey("weather_lon")
        val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val GITHUB_USER = stringPreferencesKey("github_user")
        val NASA_KEY = stringPreferencesKey("nasa_key")
        val REFRESH_MINUTES = intPreferencesKey("refresh_minutes")
        val PAUSE_NET_IDLE = booleanPreferencesKey("pause_net_idle")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CUSTOM_MANTRA = stringPreferencesKey("custom_mantra")
        val HIDDEN_WIDGETS = stringPreferencesKey("hidden_widgets")
        val WIDGET_SIZES = stringPreferencesKey("widget_sizes")

        // Cached weather JSON
        val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val CACHED_WEATHER_TIME = longPreferencesKey("cached_weather_time")
    }

    val settingsFlow: Flow<AppSettings> = context.stillroomDataStore.data.map { prefs ->
        val defaultGlass = GlassMaterialConfig.fromPreset(
            prefs[Keys.GLASS_PRESET]?.let { runCatching { GlassPreset.valueOf(it) }.getOrNull() }
                ?: GlassPreset.CRYSTAL
        )
        val glass = GlassMaterialConfig(
            preset = defaultGlass.preset,
            transparency = prefs[Keys.GLASS_TRANSPARENCY] ?: defaultGlass.transparency,
            refractionStrength = prefs[Keys.GLASS_REFRACTION] ?: defaultGlass.refractionStrength,
            edgeHighlightStrength = prefs[Keys.GLASS_HIGHLIGHT] ?: defaultGlass.edgeHighlightStrength,
            blurRadiusDp = prefs[Keys.GLASS_BLUR] ?: defaultGlass.blurRadiusDp,
            tintColorArgb = prefs[Keys.GLASS_TINT] ?: defaultGlass.tintColorArgb,
            cornerRadiusDp = prefs[Keys.GLASS_CORNER] ?: defaultGlass.cornerRadiusDp,
            shadowElevationDp = prefs[Keys.GLASS_SHADOW] ?: defaultGlass.shadowElevationDp,
            dispersionStrength = prefs[Keys.GLASS_DISPERSION] ?: defaultGlass.dispersionStrength,
            interactionElasticity = prefs[Keys.GLASS_ELASTICITY] ?: defaultGlass.interactionElasticity
        )

        val zonesRaw = prefs[Keys.WORLD_ZONES]
        val zones = if (zonesRaw.isNullOrBlank()) {
            ClockConfig().worldZones
        } else {
            zonesRaw.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) WorldClockZone(parts[0], parts[1]) else null
            }
        }

        val clock = ClockConfig(
            style = prefs[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: ClockStyle.EDITORIAL_DIGITAL,
            is24Hour = prefs[Keys.CLOCK_24H] ?: false,
            showSeconds = prefs[Keys.CLOCK_SECONDS] ?: true,
            showDate = prefs[Keys.CLOCK_DATE] ?: true,
            typography = prefs[Keys.CLOCK_TYPOGRAPHY]?.let { runCatching { ClockTypography.valueOf(it) }.getOrNull() } ?: ClockTypography.SERIF,
            size = prefs[Keys.CLOCK_SIZE]?.let { runCatching { ClockSize.valueOf(it) }.getOrNull() } ?: ClockSize.LARGE,
            position = prefs[Keys.CLOCK_POSITION]?.let { runCatching { ClockPosition.valueOf(it) }.getOrNull() } ?: ClockPosition.TOP_LEFT,
            worldZones = zones
        )

        val visualizer = VisualizerConfig(
            enabled = prefs[Keys.VIS_ENABLED] ?: true,
            mode = prefs[Keys.VIS_MODE]?.let { runCatching { VisualizerMode.valueOf(it) }.getOrNull() } ?: VisualizerMode.LIQUID_WAVES,
            sensitivity = prefs[Keys.VIS_SENSITIVITY] ?: 1.25f,
            smoothing = prefs[Keys.VIS_SMOOTHING] ?: 0.72f,
            frequencyRange = prefs[Keys.VIS_RANGE]?.let { runCatching { VisualizerRange.valueOf(it) }.getOrNull() } ?: VisualizerRange.FULL,
            sizeScale = prefs[Keys.VIS_SIZE] ?: 1.0f,
            alpha = prefs[Keys.VIS_ALPHA] ?: 0.82f,
            position = prefs[Keys.VIS_POSITION]?.let { runCatching { VisualizerPosition.valueOf(it) }.getOrNull() } ?: VisualizerPosition.BOTTOM_STAGE,
            colorPreset = prefs[Keys.VIS_COLOR]?.let { runCatching { VisualizerColorPreset.valueOf(it) }.getOrNull() } ?: VisualizerColorPreset.WARM_AMBER,
            autoSceneOnMusic = prefs[Keys.VIS_AUTO_SCENE] ?: true
        )

        val hiddenSet = prefs[Keys.HIDDEN_WIDGETS]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { WidgetType.valueOf(name) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

        val sizeMap = prefs[Keys.WIDGET_SIZES]
            ?.split(";")
            ?.mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val wt = runCatching { WidgetType.valueOf(parts[0]) }.getOrNull()
                    val sz = runCatching { WidgetSize.valueOf(parts[1]) }.getOrNull()
                    if (wt != null && sz != null) wt to sz else null
                } else null
            }
            ?.toMap()
            ?: emptyMap()

        AppSettings(
            activeSceneId = prefs[Keys.ACTIVE_SCENE_ID] ?: "scene_morning_atelier",
            autoSceneEngineEnabled = prefs[Keys.AUTO_SCENE_ENGINE] ?: true,
            glassConfig = glass,
            clockConfig = clock,
            visualizerConfig = visualizer,
            backgroundType = prefs[Keys.BG_TYPE]?.let { runCatching { AmbientSceneType.valueOf(it) }.getOrNull() } ?: AmbientSceneType.SOFT_ATMOSPHERIC_LIGHT,
            transitionType = prefs[Keys.TRANSITION_TYPE]?.let { runCatching { SceneTransitionType.valueOf(it) }.getOrNull() } ?: SceneTransitionType.CROSSFADE,
            customBackgroundUri = prefs[Keys.CUSTOM_BG_URI],
            motionSpeed = prefs[Keys.MOTION_SPEED] ?: 1.0f,
            warmthTint = prefs[Keys.WARMTH_TINT] ?: 0.35f,
            grainOpacity = prefs[Keys.GRAIN_OPACITY] ?: 0.08f,
            idleTimeoutSeconds = prefs[Keys.IDLE_TIMEOUT_SEC] ?: 30,
            autoDimEnabled = prefs[Keys.AUTO_DIM] ?: true,
            idleBrightness = prefs[Keys.IDLE_BRIGHTNESS] ?: 0.28f,
            activeBrightness = prefs[Keys.ACTIVE_BRIGHTNESS] ?: 0.78f,
            burnInShiftEnabled = prefs[Keys.BURN_IN_SHIFT] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            lowPowerMode = prefs[Keys.LOW_POWER_MODE] ?: false,
            nightModeTint = prefs[Keys.NIGHT_MODE_TINT] ?: false,
            weatherCityName = prefs[Keys.WEATHER_CITY] ?: "San Francisco",
            weatherLatitude = prefs[Keys.WEATHER_LAT] ?: 37.7749,
            weatherLongitude = prefs[Keys.WEATHER_LON] ?: -122.4194,
            useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
            githubUsername = prefs[Keys.GITHUB_USER] ?: "torvalds",
            nasaApiKey = prefs[Keys.NASA_KEY] ?: "DEMO_KEY",
            networkRefreshMinutes = prefs[Keys.REFRESH_MINUTES] ?: 15,
            pauseNetworkOnIdle = prefs[Keys.PAUSE_NET_IDLE] ?: true,
            wifiOnlyRefresh = prefs[Keys.WIFI_ONLY] ?: false,
            customMantraText = prefs[Keys.CUSTOM_MANTRA] ?: "Quiet tools for an unhurried mind.",
            hiddenWidgets = hiddenSet,
            widgetSizes = sizeMap
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.stillroomDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SCENE_ID] = settings.activeSceneId
            prefs[Keys.AUTO_SCENE_ENGINE] = settings.autoSceneEngineEnabled

            val g = settings.glassConfig
            prefs[Keys.GLASS_PRESET] = g.preset.name
            prefs[Keys.GLASS_TRANSPARENCY] = g.transparency
            prefs[Keys.GLASS_REFRACTION] = g.refractionStrength
            prefs[Keys.GLASS_HIGHLIGHT] = g.edgeHighlightStrength
            prefs[Keys.GLASS_BLUR] = g.blurRadiusDp
            prefs[Keys.GLASS_TINT] = g.tintColorArgb
            prefs[Keys.GLASS_CORNER] = g.cornerRadiusDp
            prefs[Keys.GLASS_SHADOW] = g.shadowElevationDp
            prefs[Keys.GLASS_DISPERSION] = g.dispersionStrength
            prefs[Keys.GLASS_ELASTICITY] = g.interactionElasticity

            val c = settings.clockConfig
            prefs[Keys.CLOCK_STYLE] = c.style.name
            prefs[Keys.CLOCK_24H] = c.is24Hour
            prefs[Keys.CLOCK_SECONDS] = c.showSeconds
            prefs[Keys.CLOCK_DATE] = c.showDate
            prefs[Keys.CLOCK_TYPOGRAPHY] = c.typography.name
            prefs[Keys.CLOCK_SIZE] = c.size.name
            prefs[Keys.CLOCK_POSITION] = c.position.name
            prefs[Keys.WORLD_ZONES] = c.worldZones.joinToString(";") { "${it.zoneId}|${it.label}" }

            val v = settings.visualizerConfig
            prefs[Keys.VIS_ENABLED] = v.enabled
            prefs[Keys.VIS_MODE] = v.mode.name
            prefs[Keys.VIS_SENSITIVITY] = v.sensitivity
            prefs[Keys.VIS_SMOOTHING] = v.smoothing
            prefs[Keys.VIS_RANGE] = v.frequencyRange.name
            prefs[Keys.VIS_SIZE] = v.sizeScale
            prefs[Keys.VIS_ALPHA] = v.alpha
            prefs[Keys.VIS_POSITION] = v.position.name
            prefs[Keys.VIS_COLOR] = v.colorPreset.name
            prefs[Keys.VIS_AUTO_SCENE] = v.autoSceneOnMusic

            prefs[Keys.BG_TYPE] = settings.backgroundType.name
            prefs[Keys.TRANSITION_TYPE] = settings.transitionType.name
            if (settings.customBackgroundUri != null) {
                prefs[Keys.CUSTOM_BG_URI] = settings.customBackgroundUri
            } else {
                prefs.remove(Keys.CUSTOM_BG_URI)
            }
            prefs[Keys.MOTION_SPEED] = settings.motionSpeed
            prefs[Keys.WARMTH_TINT] = settings.warmthTint
            prefs[Keys.GRAIN_OPACITY] = settings.grainOpacity
            prefs[Keys.IDLE_TIMEOUT_SEC] = settings.idleTimeoutSeconds
            prefs[Keys.AUTO_DIM] = settings.autoDimEnabled
            prefs[Keys.IDLE_BRIGHTNESS] = settings.idleBrightness
            prefs[Keys.ACTIVE_BRIGHTNESS] = settings.activeBrightness
            prefs[Keys.BURN_IN_SHIFT] = settings.burnInShiftEnabled
            prefs[Keys.REDUCED_MOTION] = settings.reducedMotion
            prefs[Keys.LOW_POWER_MODE] = settings.lowPowerMode
            prefs[Keys.NIGHT_MODE_TINT] = settings.nightModeTint

            prefs[Keys.WEATHER_CITY] = settings.weatherCityName
            prefs[Keys.WEATHER_LAT] = settings.weatherLatitude
            prefs[Keys.WEATHER_LON] = settings.weatherLongitude
            prefs[Keys.USE_FAHRENHEIT] = settings.useFahrenheit
            prefs[Keys.GITHUB_USER] = settings.githubUsername
            prefs[Keys.NASA_KEY] = settings.nasaApiKey
            prefs[Keys.REFRESH_MINUTES] = settings.networkRefreshMinutes
            prefs[Keys.PAUSE_NET_IDLE] = settings.pauseNetworkOnIdle
            prefs[Keys.WIFI_ONLY] = settings.wifiOnlyRefresh
            prefs[Keys.CUSTOM_MANTRA] = settings.customMantraText
            prefs[Keys.HIDDEN_WIDGETS] = settings.hiddenWidgets.joinToString(",") { it.name }
            prefs[Keys.WIDGET_SIZES] = settings.widgetSizes.entries.joinToString(";") { "${it.key.name}=${it.value.name}" }
        }
    }

    suspend fun resetAll() {
        context.stillroomDataStore.edit { it.clear() }
    }

    suspend fun cacheWeatherJson(json: String) {
        context.stillroomDataStore.edit {
            it[Keys.CACHED_WEATHER_JSON] = json
            it[Keys.CACHED_WEATHER_TIME] = System.currentTimeMillis()
        }
    }

    val cachedWeatherFlow: Flow<Pair<String?, Long>> = context.stillroomDataStore.data.map {
        it[Keys.CACHED_WEATHER_JSON] to (it[Keys.CACHED_WEATHER_TIME] ?: 0L)
    }
}
