package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.model.AmbientSceneType
import com.example.domain.model.ClockPosition
import com.example.domain.model.ClockStyle
import com.example.domain.model.GlassPreset
import com.example.domain.model.SceneConditionType
import com.example.domain.model.SceneConfig
import com.example.domain.model.SceneTransitionType
import com.example.domain.model.VisualizerMode

@Database(
    entities = [
        NoteEntity::class,
        TodoTaskEntity::class,
        HabitEntity::class,
        ReminderEntity::class,
        CountdownEntity::class,
        SceneProfileEntity::class,
        RssFeedSourceEntity::class,
        IotDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StillroomDatabase : RoomDatabase() {
    abstract fun stillroomDao(): StillroomDao

    companion object {
        @Volatile
        private var INSTANCE: StillroomDatabase? = null

        fun getInstance(context: Context): StillroomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StillroomDatabase::class.java,
                    "stillroom.db"
                ).build().also { INSTANCE = it }
            }
        }

        fun defaultScenes(): List<SceneProfileEntity> = listOf(
            SceneConfig(
                id = "scene_morning_atelier",
                name = "Morning Atelier",
                condition = SceneConditionType.TIME_MORNING,
                backgroundType = AmbientSceneType.LANDSCAPE_BOTANICAL,
                transitionType = SceneTransitionType.CROSSFADE,
                clockStyle = ClockStyle.EDITORIAL_DIGITAL,
                clockPosition = ClockPosition.TOP_LEFT,
                visualizerMode = VisualizerMode.MINIMAL_PULSE,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.08f,
                motionSpeed = 0.85f
            ),
            SceneConfig(
                id = "scene_afternoon_focus",
                name = "Afternoon Drift",
                condition = SceneConditionType.TIME_AFTERNOON,
                backgroundType = AmbientSceneType.SOFT_ATMOSPHERIC_LIGHT,
                transitionType = SceneTransitionType.LIQUID,
                clockStyle = ClockStyle.EDITORIAL_DIGITAL,
                clockPosition = ClockPosition.TOP_LEFT,
                visualizerMode = VisualizerMode.LIQUID_WAVES,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.12f,
                motionSpeed = 0.9f
            ),
            SceneConfig(
                id = "scene_evening_ridge",
                name = "Evening Dusk",
                condition = SceneConditionType.TIME_EVENING,
                backgroundType = AmbientSceneType.LANDSCAPE_RIDGE,
                transitionType = SceneTransitionType.DISSOLVE,
                clockStyle = ClockStyle.MINIMAL_ANALOG,
                clockPosition = ClockPosition.CENTER,
                visualizerMode = VisualizerMode.FREQUENCY_RINGS,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.18f,
                motionSpeed = 0.75f
            ),
            SceneConfig(
                id = "scene_night_stillness",
                name = "Nocturnal Sky",
                condition = SceneConditionType.TIME_NIGHT,
                backgroundType = AmbientSceneType.SPACE_STARFIELD,
                transitionType = SceneTransitionType.GENTLE_ZOOM,
                clockStyle = ClockStyle.FLIP_CLOCK,
                clockPosition = ClockPosition.CENTER,
                visualizerMode = VisualizerMode.AUDIO_PARTICLES,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CLEAR,
                ambientDimming = 0.32f,
                motionSpeed = 0.65f
            ),
            SceneConfig(
                id = "scene_music_vinyl",
                name = "Sonic Resonance",
                condition = SceneConditionType.MUSIC_PLAYING,
                backgroundType = AmbientSceneType.AURORA_MOTION,
                transitionType = SceneTransitionType.LIQUID,
                clockStyle = ClockStyle.EDITORIAL_DIGITAL,
                clockPosition = ClockPosition.SPLIT_CORNER,
                visualizerMode = VisualizerMode.CIRCULAR_SPECTRUM,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.10f,
                motionSpeed = 1.15f
            ),
            SceneConfig(
                id = "scene_charging_dock",
                name = "Charging Stand",
                condition = SceneConditionType.CHARGING,
                backgroundType = AmbientSceneType.MINIMAL_GEOMETRIC,
                transitionType = SceneTransitionType.CROSSFADE,
                clockStyle = ClockStyle.FLIP_CLOCK,
                clockPosition = ClockPosition.CENTER,
                visualizerMode = VisualizerMode.SPECTRUM_BARS,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.15f,
                motionSpeed = 0.7f
            ),
            SceneConfig(
                id = "scene_rain_window",
                name = "Rainy Window",
                condition = SceneConditionType.MANUAL,
                backgroundType = AmbientSceneType.RAIN_ON_GLASS,
                transitionType = SceneTransitionType.DISSOLVE,
                clockStyle = ClockStyle.EDITORIAL_DIGITAL,
                clockPosition = ClockPosition.BOTTOM_LEFT,
                visualizerMode = VisualizerMode.WAVEFORM,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.16f,
                motionSpeed = 0.9f
            ),
            SceneConfig(
                id = "scene_tidal_calm",
                name = "Tidal Horizon",
                condition = SceneConditionType.MANUAL,
                backgroundType = AmbientSceneType.OCEAN_FLOW,
                transitionType = SceneTransitionType.LIQUID,
                clockStyle = ClockStyle.MINIMAL_ANALOG,
                clockPosition = ClockPosition.TOP_LEFT,
                visualizerMode = VisualizerMode.DUAL_BAND,
                visualizerEnabled = true,
                glassPreset = GlassPreset.CRYSTAL,
                ambientDimming = 0.12f,
                motionSpeed = 0.8f
            )
        ).map { it.toEntity() }

        fun defaultRssFeeds(): List<RssFeedSourceEntity> = listOf(
            RssFeedSourceEntity(
                title = "NASA Breaking News",
                url = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
                enabled = true
            ),
            RssFeedSourceEntity(
                title = "Hacker News",
                url = "https://news.ycombinator.com/rss",
                enabled = true
            )
        )
    }
}

fun SceneConfig.toEntity(): SceneProfileEntity = SceneProfileEntity(
    id = id,
    name = name,
    conditionName = condition.name,
    backgroundTypeName = backgroundType.name,
    transitionTypeName = transitionType.name,
    clockStyleName = clockStyle.name,
    clockPositionName = clockPosition.name,
    visualizerModeName = visualizerMode.name,
    visualizerEnabled = visualizerEnabled,
    glassPresetName = glassPreset.name,
    ambientDimming = ambientDimming,
    motionSpeed = motionSpeed,
    scheduleStartHour = scheduleStartHour,
    scheduleEndHour = scheduleEndHour
)

fun SceneProfileEntity.toDomain(): SceneConfig = SceneConfig(
    id = id,
    name = name,
    condition = runCatching { SceneConditionType.valueOf(conditionName) }.getOrDefault(SceneConditionType.MANUAL),
    backgroundType = runCatching { AmbientSceneType.valueOf(backgroundTypeName) }.getOrDefault(AmbientSceneType.SOFT_ATMOSPHERIC_LIGHT),
    transitionType = runCatching { SceneTransitionType.valueOf(transitionTypeName) }.getOrDefault(SceneTransitionType.CROSSFADE),
    clockStyle = runCatching { ClockStyle.valueOf(clockStyleName) }.getOrDefault(ClockStyle.EDITORIAL_DIGITAL),
    clockPosition = runCatching { ClockPosition.valueOf(clockPositionName) }.getOrDefault(ClockPosition.TOP_LEFT),
    visualizerMode = runCatching { VisualizerMode.valueOf(visualizerModeName) }.getOrDefault(VisualizerMode.LIQUID_WAVES),
    visualizerEnabled = visualizerEnabled,
    glassPreset = runCatching { GlassPreset.valueOf(glassPresetName) }.getOrDefault(GlassPreset.CRYSTAL),
    ambientDimming = ambientDimming,
    motionSpeed = motionSpeed,
    scheduleStartHour = scheduleStartHour,
    scheduleEndHour = scheduleEndHour
)
