package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "todo_tasks")
data class TodoTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isDailyGoal: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val streakCount: Int = 0,
    val lastCompletedDate: String = "" // ISO yyyy-MM-dd
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueTimeLabel: String,
    val isDone: Boolean = false
)

@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // EXAM, BIRTHDAY, CUSTOM
    val targetEpochMillis: Long
)

@Entity(tableName = "scene_profiles")
data class SceneProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val conditionName: String,
    val backgroundTypeName: String,
    val transitionTypeName: String,
    val clockStyleName: String,
    val clockPositionName: String,
    val visualizerModeName: String,
    val visualizerEnabled: Boolean,
    val glassPresetName: String,
    val ambientDimming: Float,
    val motionSpeed: Float,
    val scheduleStartHour: Int,
    val scheduleEndHour: Int
)

@Entity(tableName = "rss_feeds")
data class RssFeedSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val enabled: Boolean = true
)

@Entity(tableName = "iot_devices")
data class IotDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val protocol: String, // MQTT or HTTP
    val hostOrUrl: String,
    val port: Int = 1883,
    val topicOrJsonPath: String,
    val deviceKind: String, // TEMPERATURE, HUMIDITY, SOIL_MOISTURE, SWITCH, GENERIC
    val unit: String = "",
    val username: String = "",
    val password: String = "",
    val publishPayloadOn: String = "ON",
    val publishPayloadOff: String = "OFF",
    val lastReading: String = "",
    val lastUpdatedMillis: Long = 0L
)
