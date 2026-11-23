package com.example.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.HabitEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TodoTaskEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickNotesTile(
    notes: List<NoteEntity>,
    onSaveNote: (id: Long, title: String, content: String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("quick_notes_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SCRATCHPAD & NOTES", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                IconButton(
                    onClick = { showCreate = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_note_btn")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add note", tint = StillroomAmber)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Text("Tap + to jot down quick thoughts or studio notes.", fontSize = 13.sp, color = StillroomAsh)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    notes.take(4).forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingNote = note }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, color = StillroomLinen, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (note.content.isNotBlank()) {
                                    Text(note.content, color = StillroomBone, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete note", tint = StillroomAsh, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editingNote != null) {
        val initial = editingNote
        var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
        var body by remember(initial) { mutableStateOf(initial?.content ?: "") }
        AlertDialog(
            onDismissRequest = { showCreate = false; editingNote = null },
            containerColor = StillroomCharcoal,
            title = { Text(if (initial == null) "New Note" else "Edit Note", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Scratchpad") }, minLines = 4)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveNote(initial?.id ?: 0L, title, body)
                    showCreate = false
                    editingNote = null
                }) {
                    Text("Save", color = StillroomAmber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false; editingNote = null }) {
                    Text("Cancel", color = StillroomBone)
                }
            }
        )
    }
}

@Composable
fun TasksAndGoalsTile(
    tasks: List<TodoTaskEntity>,
    onAddTask: (title: String, isDailyGoal: Boolean) -> Unit,
    onToggleTask: (TodoTaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdd by remember { mutableStateOf(false) }
    val completedCount = tasks.count { it.isCompleted }
    val progress = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size.toFloat()

    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("tasks_goals_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = StillroomSage, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TASKS & DAILY GOALS", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                IconButton(
                    onClick = { showAdd = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_task_btn")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add task", tint = StillroomAmber)
                }
            }

            if (tasks.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = StillroomSage,
                    trackColor = StillroomAsh.copy(alpha = 0.22f),
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$completedCount of ${tasks.size} completed",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = StillroomAsh
                )
            }

            Spacer(Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Text("Add daily goals or tasks to track completion.", fontSize = 13.sp, color = StillroomAsh)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.take(6).forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTask(task) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = if (task.isCompleted) "Completed" else "Incomplete",
                                    tint = if (task.isCompleted) StillroomSage else StillroomAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontSize = 14.sp,
                                        color = if (task.isCompleted) StillroomAsh else StillroomLinen,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (task.isDailyGoal) {
                                        Text("DAILY GOAL", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = StillroomAmber)
                                    }
                                }
                            }
                            IconButton(onClick = { onDeleteTask(task.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete task", tint = StillroomAsh, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var isGoal by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = StillroomCharcoal,
            title = { Text("New Task", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task description") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isGoal, onCheckedChange = { isGoal = it })
                        Text("Mark as Daily Goal", color = StillroomLinen, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddTask(title, isGoal)
                    showAdd = false
                }) { Text("Add", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }
}

@Composable
fun HabitsAndRemindersTile(
    habits: List<HabitEntity>,
    reminders: List<ReminderEntity>,
    onAddHabit: (String) -> Unit,
    onCheckHabit: (HabitEntity) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onAddReminder: (title: String, dueTime: String) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddHabit by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LiquidGlassSurface(
        onLongClick = onLongPress,
        modifier = modifier.testTag("habits_reminders_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("HABITS & REMINDERS", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.4.sp, color = StillroomBone)
                }
                Row {
                    TextButton(onClick = { showAddHabit = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Text("+ Habit", color = StillroomSage, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    }
                    TextButton(onClick = { showAddReminder = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Text("+ Remind", color = StillroomAmber, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (habits.isEmpty() && reminders.isEmpty()) {
                Text("Build daily habit streaks and lightweight reminders.", fontSize = 13.sp, color = StillroomAsh)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    habits.take(4).forEach { habit ->
                        val checkedToday = habit.lastCompletedDate == today
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCheckHabit(habit) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (checkedToday) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (checkedToday) StillroomSage else StillroomBone,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(habit.name, color = StillroomLinen, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${habit.streakCount}d streak", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomAmber)
                                IconButton(onClick = { onDeleteHabit(habit.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete habit", tint = StillroomAsh, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    reminders.take(4).forEach { rem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleReminder(rem) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = StillroomAmber, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = rem.title,
                                    color = if (rem.isDone) StillroomAsh else StillroomLinen,
                                    fontSize = 14.sp,
                                    textDecoration = if (rem.isDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(rem.dueTimeLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = StillroomBone)
                                IconButton(onClick = { onDeleteReminder(rem.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete reminder", tint = StillroomAsh, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddHabit) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddHabit = false },
            containerColor = StillroomCharcoal,
            title = { Text("New Habit", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Habit name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { onAddHabit(name); showAddHabit = false }) { Text("Save", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabit = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }

    if (showAddReminder) {
        var title by remember { mutableStateOf("") }
        var due by remember { mutableStateOf("18:00") }
        AlertDialog(
            onDismissRequest = { showAddReminder = false },
            containerColor = StillroomCharcoal,
            title = { Text("New Reminder", fontFamily = CormorantFontFamily, fontSize = 26.sp, color = StillroomLinen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reminder") }, singleLine = true)
                    OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("Time / label") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { onAddReminder(title, due); showAddReminder = false }) { Text("Save", color = StillroomAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showAddReminder = false }) { Text("Cancel", color = StillroomBone) }
            }
        )
    }
}
