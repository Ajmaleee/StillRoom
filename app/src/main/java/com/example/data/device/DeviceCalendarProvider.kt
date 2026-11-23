package com.example.data.device

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarAgendaEvent(
    val id: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val location: String?,
    val timeLabel: String,
    val dayLabel: String
)

class DeviceCalendarProvider(private val context: Context) {

    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun loadUpcomingEvents(daysAhead: Int = 7): List<CalendarAgendaEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val events = mutableListOf<CalendarAgendaEvent>()
        val calStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = calStart.timeInMillis
        val endMillis = startMillis + daysAhead * 86_400_000L

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startMillis)
        ContentUris.appendId(uriBuilder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val todayStr = dayFmt.format(Date())

        runCatching {
            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idxId = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val idxTitle = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val idxBegin = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val idxEnd = cursor.getColumnIndex(CalendarContract.Instances.END)
                val idxAllDay = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val idxLoc = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

                while (cursor.moveToNext() && events.size < 25) {
                    val begin = cursor.getLong(idxBegin)
                    val end = cursor.getLong(idxEnd)
                    val allDay = cursor.getInt(idxAllDay) == 1
                    val title = cursor.getString(idxTitle) ?: "Untitled Event"
                    val loc = cursor.getString(idxLoc)?.takeIf { it.isNotBlank() }
                    val dayText = dayFmt.format(Date(begin))
                    val timeText = if (allDay) "All day" else "${timeFmt.format(Date(begin))} – ${timeFmt.format(Date(end))}"
                    events.add(
                        CalendarAgendaEvent(
                            id = cursor.getLong(idxId),
                            title = title,
                            beginMillis = begin,
                            endMillis = end,
                            allDay = allDay,
                            location = loc,
                            timeLabel = timeText,
                            dayLabel = if (dayText == todayStr) "Today" else dayText
                        )
                    )
                }
            }
        }
        events
    }
}
