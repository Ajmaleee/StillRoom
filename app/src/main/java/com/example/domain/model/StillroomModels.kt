package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class AmbientSceneType(val displayName: String, val subtitle: String) {
    SOFT_ATMOSPHERIC_LIGHT("Atmospheric Glow", "Diffused amber & slate warmth"),
    LANDSCAPE_RIDGE("Twilight Ridge", "Misty mountain horizon"),
    LANDSCAPE_BOTANICAL("Morning Linen", "Dappled sunlit plaster"),
    PARTICLE_FIELD("Drifting Motes", "Suspended dust & soft bokeh"),
    ABSTRACT_SHAPES("Organic Silhouettes", "Slow morphing mineral forms"),
    SPACE_STARFIELD("Deep Starfield", "Nocturnal parallax sky"),
    MATRIX_RAIN("Emerald Cipher", "Muted falling glyph rain"),
    RAIN_ON_GLASS("Rain on Glass", "Condensation trails & bokeh"),
    OCEAN_FLOW("Tidal Current", "Flowing water & moonlight"),
    AURORA_MOTION("Boreal Veil", "Slow atmospheric ribbons"),
    MINIMAL_GEOMETRIC("Architectural Arcs", "Precision orbital hairlines"),
    CUSTOM_IMAGE("Custom Canvas", "Your selected photograph")
}

enum class SceneTransitionType(val displayName: String) {
    CROSSFADE("Crossfade"),
    DISSOLVE("Dissolve"),
    GENTLE_ZOOM("Gentle Zoom"),
    LIQUID("Liquid"),
    SLIDE("Slide")
}

enum class GlassPreset(val displayName: String, val description: String) {
    CRYSTAL("Crystal", "99% clear optical lens with edge refraction"),
    CLEAR("Clear", "Subtle tint with crisp Fresnel rim lighting"),
    BALANCED("Balanced", "Moderate visibility over high-detail backdrops"),
    ACCESSIBILITY("Accessibility", "High-contrast surface for maximum legibility")
}

data class GlassMaterialConfig(
    val preset: GlassPreset = GlassPreset.CRYSTAL,
    /** 0f = opaque, 1f = 100% transparent. Crystal defaults to 0.99f (99% transparency). */
    val transparency: Float = 0.99f,
    val refractionStrength: Float = 0.72f,
    val edgeHighlightStrength: Float = 0.78f,
    val blurRadiusDp: Float = 0f,
    val tintColorArgb: Long = 0xFFFFFFFFL,
    val cornerRadiusDp: Float = 26f,
    val shadowElevationDp: Float = 14f,
    val dispersionStrength: Float = 0.48f,
    val interactionElasticity: Float = 0.85f
) {
    val tintColor: Color get() = Color(tintColorArgb)

    companion object {
        fun fromPreset(preset: GlassPreset): GlassMaterialConfig = when (preset) {
            GlassPreset.CRYSTAL -> GlassMaterialConfig(
                preset = GlassPreset.CRYSTAL,
                transparency = 0.99f,
                refractionStrength = 0.78f,
                edgeHighlightStrength = 0.80f,
                blurRadiusDp = 0f,
                tintColorArgb = 0xFFFFFAF2L,
                cornerRadiusDp = 26f,
                shadowElevationDp = 14f,
                dispersionStrength = 0.55f,
                interactionElasticity = 0.85f
            )
            GlassPreset.CLEAR -> GlassMaterialConfig(
                preset = GlassPreset.CLEAR,
                transparency = 0.94f,
                refractionStrength = 0.62f,
                edgeHighlightStrength = 0.68f,
                blurRadiusDp = 2f,
                tintColorArgb = 0xFFF5EFE6L,
                cornerRadiusDp = 24f,
                shadowElevationDp = 12f,
                dispersionStrength = 0.40f,
                interactionElasticity = 0.80f
            )
            GlassPreset.BALANCED -> GlassMaterialConfig(
                preset = GlassPreset.BALANCED,
                transparency = 0.84f,
                refractionStrength = 0.48f,
                edgeHighlightStrength = 0.55f,
                blurRadiusDp = 8f,
                tintColorArgb = 0xFF1F1C18L,
                cornerRadiusDp = 22f,
                shadowElevationDp = 16f,
                dispersionStrength = 0.25f,
                interactionElasticity = 0.70f
            )
            GlassPreset.ACCESSIBILITY -> GlassMaterialConfig(
                preset = GlassPreset.ACCESSIBILITY,
                transparency = 0.22f,
                refractionStrength = 0.0f,
                edgeHighlightStrength = 0.90f,
                blurRadiusDp = 14f,
                tintColorArgb = 0xFF141210L,
                cornerRadiusDp = 20f,
                shadowElevationDp = 18f,
                dispersionStrength = 0.0f,
                interactionElasticity = 0.50f
            )
        }
    }
}

enum class ClockStyle(val displayName: String) {
    EDITORIAL_DIGITAL("Editorial Digital"),
    MINIMAL_ANALOG("Minimal Analog"),
    FLIP_CLOCK("Split-Flap Flip"),
    HIDDEN("Hidden")
}

enum class ClockTypography(val displayName: String) {
    SERIF("Cormorant Serif"),
    MONO("JetBrains Mono"),
    SANS("Jakarta Sans")
}

enum class ClockSize(val displayName: String, val scale: Float) {
    COMPACT("Compact", 0.72f),
    MEDIUM("Medium", 0.92f),
    LARGE("Large", 1.15f),
    MONUMENTAL("Monumental", 1.42f)
}

enum class ClockPosition(val displayName: String) {
    TOP_LEFT("Top Left"),
    CENTER("Centered"),
    BOTTOM_LEFT("Bottom Left"),
    SPLIT_CORNER("Editorial Split")
}

data class WorldClockZone(
    val zoneId: String,
    val label: String
)

data class ClockConfig(
    val style: ClockStyle = ClockStyle.EDITORIAL_DIGITAL,
    val is24Hour: Boolean = false,
    val showSeconds: Boolean = true,
    val showDate: Boolean = true,
    val typography: ClockTypography = ClockTypography.SERIF,
    val size: ClockSize = ClockSize.LARGE,
    val position: ClockPosition = ClockPosition.TOP_LEFT,
    val worldZones: List<WorldClockZone> = listOf(
        WorldClockZone("Europe/London", "LDN"),
        WorldClockZone("Asia/Tokyo", "TYO")
    )
)

enum class VisualizerMode(val displayName: String) {
    SPECTRUM_BARS("Spectrum Bars"),
    CIRCULAR_SPECTRUM("Circular Spectrum"),
    WAVEFORM("Waveform"),
    FREQUENCY_RINGS("Frequency Rings"),
    AUDIO_PARTICLES("Audio Particles"),
    MINIMAL_PULSE("Minimal Pulse"),
    LIQUID_WAVES("Liquid Sound Waves"),
    DUAL_BAND("Dual-Band Horizon")
}

enum class VisualizerRange(val displayName: String) {
    FULL("Full Spectrum"),
    BASS("Sub & Bass"),
    MIDS("Vocals & Mids"),
    HIGHS("Air & Treble")
}

enum class VisualizerPosition(val displayName: String) {
    BOTTOM_STAGE("Lower Horizon"),
    CENTER_HALO("Center Aura"),
    TOP_RIBBON("Upper Ribbon")
}

enum class VisualizerColorPreset(val displayName: String, val primaryArgb: Long, val secondaryArgb: Long) {
    WARM_AMBER("Warm Amber", 0xFFD9A66CL, 0xFFC27456L),
    SAGE_MIST("Sage Mist", 0xFF839B8BL, 0xFFD9A66CL),
    NOCTURNE_SLATE("Nocturne Slate", 0xFF8AA4BCL, 0xFF8E7485L),
    IVORY_MONO("Ivory Monochrome", 0xFFF0ECE3L, 0xFF8C857AL)
}

data class VisualizerConfig(
    val enabled: Boolean = true,
    val mode: VisualizerMode = VisualizerMode.LIQUID_WAVES,
    val sensitivity: Float = 1.25f,
    val smoothing: Float = 0.72f,
    val frequencyRange: VisualizerRange = VisualizerRange.FULL,
    val sizeScale: Float = 1.0f,
    val alpha: Float = 0.82f,
    val position: VisualizerPosition = VisualizerPosition.BOTTOM_STAGE,
    val colorPreset: VisualizerColorPreset = VisualizerColorPreset.WARM_AMBER,
    val autoSceneOnMusic: Boolean = true
)

enum class DashboardPage(val title: String, val subtitle: String) {
    AMBIENT_CANVAS("Canvas", "Living display & audio"),
    PLANNER_FOCUS("Atelier", "Calendar, focus & notes"),
    TECH_FEEDS("Signals", "Feeds, space & telemetry"),
    IOT_ENVIRONMENT("Habitat", "MQTT, ESP32 & sensors")
}

enum class WidgetType(val title: String, val defaultPage: DashboardPage) {
    WEATHER("Atmosphere & Weather", DashboardPage.AMBIENT_CANVAS),
    MEDIA_NOW_PLAYING("Now Playing", DashboardPage.AMBIENT_CANVAS),
    KNOWLEDGE_QUOTE("Daily Reflection", DashboardPage.AMBIENT_CANVAS),
    CALENDAR_AGENDA("Calendar Agenda", DashboardPage.PLANNER_FOCUS),
    COUNTDOWNS("Countdowns", DashboardPage.PLANNER_FOCUS),
    FOCUS_TIMER("Focus & Stopwatch", DashboardPage.PLANNER_FOCUS),
    QUICK_NOTES("Scratchpad", DashboardPage.PLANNER_FOCUS),
    TODOS_GOALS("Tasks & Daily Goals", DashboardPage.PLANNER_FOCUS),
    HABITS_REMINDERS("Habits & Reminders", DashboardPage.PLANNER_FOCUS),
    CUSTOM_MANTRA("Custom Text", DashboardPage.AMBIENT_CANVAS),
    SYSTEM_TELEMETRY("System Telemetry", DashboardPage.TECH_FEEDS),
    RSS_NEWS("RSS & Web Feeds", DashboardPage.TECH_FEEDS),
    GITHUB_ACTIVITY("GitHub Activity", DashboardPage.TECH_FEEDS),
    NASA_APOD("NASA Astronomy", DashboardPage.TECH_FEEDS),
    ISS_TRACKER("ISS Orbit", DashboardPage.TECH_FEEDS),
    WIKIPEDIA_DISCOVERY("Wikipedia Discovery", DashboardPage.TECH_FEEDS),
    IOT_DEVICES("IoT Habitat", DashboardPage.IOT_ENVIRONMENT)
}

enum class WidgetSize(val displayName: String) {
    COMPACT("Compact"),
    STANDARD("Standard"),
    EXPANDED("Expanded")
}

data class WidgetPreference(
    val type: WidgetType,
    val visible: Boolean = true,
    val size: WidgetSize = WidgetSize.STANDARD,
    val orderIndex: Int = 0
)

enum class SceneConditionType(val displayName: String) {
    MANUAL("Manual Selection"),
    TIME_MORNING("Morning (06:00 – 11:59)"),
    TIME_AFTERNOON("Afternoon (12:00 – 16:59)"),
    TIME_EVENING("Evening (17:00 – 20:59)"),
    TIME_NIGHT("Night (21:00 – 05:59)"),
    MUSIC_PLAYING("When Music Plays"),
    CHARGING("While Charging"),
    CUSTOM_SCHEDULE("Custom Schedule")
}

data class SceneConfig(
    val id: String,
    val name: String,
    val condition: SceneConditionType = SceneConditionType.MANUAL,
    val backgroundType: AmbientSceneType = AmbientSceneType.SOFT_ATMOSPHERIC_LIGHT,
    val transitionType: SceneTransitionType = SceneTransitionType.CROSSFADE,
    val clockStyle: ClockStyle = ClockStyle.EDITORIAL_DIGITAL,
    val clockPosition: ClockPosition = ClockPosition.TOP_LEFT,
    val visualizerMode: VisualizerMode = VisualizerMode.LIQUID_WAVES,
    val visualizerEnabled: Boolean = true,
    val glassPreset: GlassPreset = GlassPreset.CRYSTAL,
    val ambientDimming: Float = 0.12f,
    val motionSpeed: Float = 1.0f,
    val scheduleStartHour: Int = 8,
    val scheduleEndHour: Int = 18
)

data class AppSettings(
    val activeSceneId: String = "scene_morning_atelier",
    val autoSceneEngineEnabled: Boolean = true,
    val glassConfig: GlassMaterialConfig = GlassMaterialConfig.fromPreset(GlassPreset.CRYSTAL),
    val clockConfig: ClockConfig = ClockConfig(),
    val visualizerConfig: VisualizerConfig = VisualizerConfig(),
    val backgroundType: AmbientSceneType = AmbientSceneType.SOFT_ATMOSPHERIC_LIGHT,
    val transitionType: SceneTransitionType = SceneTransitionType.CROSSFADE,
    val customBackgroundUri: String? = null,
    val motionSpeed: Float = 1.0f,
    val warmthTint: Float = 0.35f,
    val grainOpacity: Float = 0.08f,
    val idleTimeoutSeconds: Int = 30,
    val autoDimEnabled: Boolean = true,
    val idleBrightness: Float = 0.28f,
    val activeBrightness: Float = 0.78f,
    val burnInShiftEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val lowPowerMode: Boolean = false,
    val nightModeTint: Boolean = false,
    val weatherCityName: String = "San Francisco",
    val weatherLatitude: Double = 37.7749,
    val weatherLongitude: Double = -122.4194,
    val useFahrenheit: Boolean = false,
    val githubUsername: String = "torvalds",
    val nasaApiKey: String = "DEMO_KEY",
    val networkRefreshMinutes: Int = 15,
    val pauseNetworkOnIdle: Boolean = true,
    val wifiOnlyRefresh: Boolean = false,
    val customMantraText: String = "Quiet tools for an unhurried mind.",
    val hiddenWidgets: Set<WidgetType> = emptySet(),
    val widgetSizes: Map<WidgetType, WidgetSize> = emptyMap()
)

sealed class ResourceState<out T> {
    data object Idle : ResourceState<Nothing>()
    data object Loading : ResourceState<Nothing>()
    data class Success<T>(
        val data: T,
        val updatedAtMillis: Long = System.currentTimeMillis(),
        val isCachedOffline: Boolean = false
    ) : ResourceState<T>()
    data class Error(val message: String, val cachedFallback: Boolean = false) : ResourceState<Nothing>()
}
