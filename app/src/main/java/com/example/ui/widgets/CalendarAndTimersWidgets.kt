package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.CalendarAgendaEvent
import com.example.data.local.CountdownEntity
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.theme.CormorantFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.StillroomAmber
import com.example.ui.theme.StillroomAsh
import com.example.ui.theme.StillroomBone
import com.example.ui.theme.StillroomCharcoal
import com.example.ui.theme.StillroomLinen
import com.example.ui.theme.StillroomSage
import com.example.viewmodel.PomodoroPhase
import com.example.viewmodel.TimerUiState
import java.util.concurrent.TimeUnit

@Composable
fun CalendarAgendaTile(
    hasPermission: Boolean,
    events: List<CalendarAgendaEvent>,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("calendar_agenda_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AGENDA & WEEK", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                if (hasPermission) {
                    IconButton(onClick = onRefresh, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh calendar", tint = StillroomBone, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (!hasPermission) {
                Text("Connect your local Android calendar to view today's events and weekly agenda.", fontSize = 13.sp, color = StillroomBone)
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("grant_calendar_permission_btn")
                ) {
                    Text("Connect Calendar", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily)
                }
            } else if (events.isEmpty()) {
                Text("No scheduled events over the next 7 days.", fontSize = 13.sp, color = StillroomAsh)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    events.take(5).forEach { ev ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.title, color = StillroomLinen, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = listOfNotNull(ev.dayLabel, ev.location).joinToString(" · ")
                                Text(sub, color = StillroomAsh, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(ev.timeLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAmber)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownsTile(
    countdowns: List<CountdownEntity>,
    onAddCountdown: (title: String, category: String, targetEpochMillis: Long) -> Unit,
    onDeleteCountdown: (Long) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("countdowns_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = StillroomSage, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COUNTDOWNS", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_countdown_btn")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add countdown", tint = StillroomAmber)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (countdowns.isEmpty()) {
                Text("Track exams, birthdays, or milestone dates.", fontSize = 13.sp, color = StillroomAsh)
            } else {
                val now = System.currentTimeMillis()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    countdowns.forEach { item ->
                        val diffMs = (item.targetEpochMillis - now).coerceAtLeast(0L)
                        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                        val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = StillroomLinen, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.category, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = StillroomSage)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (diffMs == 0L) "Today" else "${days}d ${hours}h",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 14.sp,
                                    color = StillroomAmber
                                )
                                IconButton(onClick = { onDeleteCountdown(item.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete countdown", tint = StillroomAsh, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("EXAM") }
        var daysAhead by remember { mutableStateOf("14") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = StillroomCharcoal,
            title = { Text("New Countdown", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event name") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EXAM", "BIRTHDAY", "CUSTOM").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = daysAhead,
                        onValueChange = { daysAhead = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days from today") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val d = daysAhead.toLongOrNull()?.coerceIn(0L, 3650L) ?: 7L
                    val target = System.currentTimeMillis() + d * 86_400_000L
                    onAddCountdown(title, category, target)
                    showAddDialog = false
                }) {
                    Text("Save", color = StillroomAmber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }
}

@Composable
fun FocusTimerAndStopwatchTile(
    timerState: TimerUiState,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStopwatch by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("focus_timer_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (showStopwatch) "STOPWATCH" else "FOCUS & POMODORO",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = StillroomBone
                    )
                }
                TextButton(
                    onClick = { showStopwatch = !showStopwatch },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_timer_stopwatch_btn")
                ) {
                    Text(if (showStopwatch) "Focus Timer" else "Stopwatch", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            if (!showStopwatch) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PomodoroPhase.FOCUS, PomodoroPhase.STUDY, PomodoroPhase.BREAK).forEach { ph ->
                        FilterChip(
                            selected = timerState.pomodoroPhase == ph,
                            onClick = { onSelectPhase(ph) },
                            label = { Text("${ph.label.split(" ").first()} ${ph.defaultMinutes}m", fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val min = timerState.timerRemainingSec / 60
                val sec = timerState.timerRemainingSec % 60
                val progress = if (timerState.timerTotalSec > 0) {
                    timerState.timerRemainingSec.toFloat() / timerState.timerTotalSec.toFloat()
                } else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(84.dp)) {
                            drawArc(
                                color = StillroomAsh.copy(alpha = 0.22f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = StillroomAmber,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "%02d:%02d".format(min, sec),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 16.sp,
                            color = StillroomLinen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${timerState.completedFocusSessions} sessions today",
                            fontFamily = PlusJakartaFontFamily,
                            fontSize = 12.sp,
                            color = StillroomBone
                        )
                        Row {
                            IconButton(
                                onClick = onToggleTimer,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("timer_start_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (timerState.timerRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = if (timerState.timerRunning) "Pause timer" else "Start timer",
                                    tint = StillroomAmber
                                )
                            }
                            IconButton(onClick = onResetTimer, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset timer", tint = StillroomBone)
                            }
                        }
                    }
                }
            } else {
                val totalSec = timerState.stopwatchElapsedMs / 1000L
                val min = totalSec / 60L
                val sec = totalSec % 60L
                val tenths = (timerState.stopwatchElapsedMs % 1000L) / 100L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d:%02d.%d".format(min, sec, tenths),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 34.sp,
                        color = StillroomLinen
                    )
                    Row {
                        IconButton(onClick = onToggleStopwatch, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(
                                imageVector = if (timerState.stopwatchRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = "Start/pause stopwatch",
                                tint = StillroomAmber
                            )
                        }
                        TextButton(onClick = onLapStopwatch, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Text("Lap", color = StillroomSage, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        }
                        IconButton(onClick = onResetStopwatch, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reset stopwatch", tint = StillroomBone)
                        }
                    }
                }
                if (timerState.stopwatchLaps.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    timerState.stopwatchLaps.take(3).forEachIndexed { idx, lapMs ->
                        val s = lapMs / 1000L
                        Text(
                            text = "Lap ${timerState.stopwatchLaps.size - idx}: %02d:%02d.%d".format(s / 60, s % 60, (lapMs % 1000) / 100),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = StillroomBone
                        )
                    }
                }
            }
        }
    }
}
