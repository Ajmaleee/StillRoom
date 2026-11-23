package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StillroomDao {
    // Notes
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // Tasks & Daily Goals
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TodoTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TodoTaskEntity): Long

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    // Habits
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, id DESC")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    // Countdowns
    @Query("SELECT * FROM countdowns ORDER BY targetEpochMillis ASC")
    fun observeCountdowns(): Flow<List<CountdownEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCountdown(countdown: CountdownEntity): Long

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun deleteCountdown(id: Long)

    // Scene Profiles
    @Query("SELECT * FROM scene_profiles")
    fun observeScenes(): Flow<List<SceneProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScene(scene: SceneProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScenesIfMissing(scenes: List<SceneProfileEntity>)

    @Query("DELETE FROM scene_profiles WHERE id = :id")
    suspend fun deleteScene(id: String)

    // RSS Feeds
    @Query("SELECT * FROM rss_feeds ORDER BY id ASC")
    fun observeRssFeeds(): Flow<List<RssFeedSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRssFeed(feed: RssFeedSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRssFeedsIfMissing(feeds: List<RssFeedSourceEntity>)

    @Query("DELETE FROM rss_feeds WHERE id = :id")
    suspend fun deleteRssFeed(id: Long)

    // IoT Devices
    @Query("SELECT * FROM iot_devices ORDER BY id ASC")
    fun observeIotDevices(): Flow<List<IotDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIotDevice(device: IotDeviceEntity): Long

    @Query("UPDATE iot_devices SET lastReading = :reading, lastUpdatedMillis = :timestamp WHERE id = :id")
    suspend fun updateIotReading(id: Long, reading: String, timestamp: Long)

    @Query("DELETE FROM iot_devices WHERE id = :id")
    suspend fun deleteIotDevice(id: Long)
}
