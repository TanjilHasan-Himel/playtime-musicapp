package com.eplaytime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import com.eplaytime.app.ui.viewmodel.SchedulerViewModel
import java.util.*

/**
 * SchedulerScreen - UI for creating and managing scheduled playback alarms
 * USP Feature: Auto-Play Scheduler
 * Rich & Soft aesthetic with Outfit font
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    onNavigateBack: () -> Unit,
    schedulerViewModel: SchedulerViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel
) {
    val tasks by schedulerViewModel.tasks.collectAsState()
    val songList by musicViewModel.songList.collectAsState()
    val selectedHour by schedulerViewModel.selectedHour.collectAsState()
    val selectedMinute by schedulerViewModel.selectedMinute.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<ScheduledTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Schedule Playback",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BlackBackground,
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SoftGold
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Alarm",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (tasks.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Scheduled Alarms",
                        fontFamily = OswaldFontFamily,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to create your first alarm",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            } else {
                // Task list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { schedulerViewModel.toggleTask(task) },
                            onDelete = { schedulerViewModel.deleteTask(task) },
                            onEdit = { 
                                alarmToEdit = task
                                showCreateDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Create/Edit alarm dialog
    if (showCreateDialog) {
        com.eplaytime.app.ui.components.AlarmDialog(
            songList = songList,
            alarmToEdit = alarmToEdit, // Pass the task to edit
            onConfirm = { selectedSong, volume, repeatDays, hour, minute ->
                if (alarmToEdit != null) {
                    // Update existing
                    schedulerViewModel.updateTask(
                        task = alarmToEdit!!,
                        songUri = selectedSong.uri,
                        songTitle = selectedSong.title,
                        hour = hour,
                        minute = minute,
                        targetVolume = volume,
                        repeatDays = repeatDays
                    )
                } else {
                    // Create new
                    schedulerViewModel.createTask(
                        songUri = selectedSong.uri,
                        songTitle = selectedSong.title,
                        hour = hour,
                        minute = minute,
                        targetVolume = volume,
                        repeatDays = repeatDays
                    )
                }
                showCreateDialog = false
                alarmToEdit = null // Reset
            },
            onDismiss = { 
                showCreateDialog = false 
                alarmToEdit = null // Reset
            }
        )
    }
}

/**
 * Task card showing scheduled alarm details
 */
@Composable
private fun TaskCard(
    task: ScheduledTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }, // Make card clickable to edit
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time display
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTime(task.hour, task.minute),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = if (task.isEnabled) Color.White else Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${task.songTitle} • Vol: ${(task.targetVolume * 100).toInt()}%${if (task.repeatDays.isNotEmpty()) " • ${task.repeatDays}" else ""}",
                    fontSize = 14.sp,
                    color = if (task.isEnabled) Color.White.copy(alpha = 0.7f)
                           else Color.White.copy(alpha = 0.3f),
                    maxLines = 1
                )
            }

            // Toggle switch
            Switch(
                checked = task.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF1DB954),
                    checkedTrackColor = Color(0xFF1DB954).copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Edit button
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Format time as HH:MM
 */
private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
}
