package com.eplaytime.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eplaytime.app.data.dao.ScheduledTaskDao
import com.eplaytime.app.data.database.PlayTimeDatabase
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SchedulerViewModel - Manages scheduled playback tasks
 */
@HiltViewModel
class SchedulerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val taskDao: ScheduledTaskDao = PlayTimeDatabase.getDatabase(application).scheduledTaskDao()
    private val alarmScheduler = AlarmScheduler(application)

    private val _tasks = MutableStateFlow<List<ScheduledTask>>(emptyList())
    val tasks: StateFlow<List<ScheduledTask>> = _tasks.asStateFlow()

    private val _selectedHour = MutableStateFlow(8)
    val selectedHour: StateFlow<Int> = _selectedHour.asStateFlow()

    private val _selectedMinute = MutableStateFlow(0)
    val selectedMinute: StateFlow<Int> = _selectedMinute.asStateFlow()

    init {
        loadTasks()
    }

    /**
     * Load all scheduled tasks
     */
    private fun loadTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { taskList ->
                _tasks.value = taskList
            }
        }
    }

    /**
     * Create a new scheduled task
     */
    fun createTask(songUri: String, songTitle: String, hour: Int, minute: Int, targetVolume: Float, repeatDays: String) {
        viewModelScope.launch {
            val task = ScheduledTask(
                songUri = songUri,
                songTitle = songTitle,
                hour = hour,
                minute = minute,
                isEnabled = true,
                targetVolume = targetVolume,
                repeatDays = repeatDays
            )

            val taskId = taskDao.insertTask(task)
            val insertedTask = task.copy(id = taskId)

            // Schedule the alarm
            alarmScheduler.scheduleAlarm(insertedTask)
        }
    }

    /**
     * Delete a task
     */
    fun deleteTask(task: ScheduledTask) {
        viewModelScope.launch {
            // Cancel alarm first
            alarmScheduler.cancelAlarm(task)
            // Then delete from database
            taskDao.deleteTask(task)
        }
    }

    /**
     * Toggle task enabled/disabled
     */
    fun toggleTask(task: ScheduledTask) {
        viewModelScope.launch {
            val updatedTask = task.copy(isEnabled = !task.isEnabled)
            taskDao.updateTask(updatedTask)

            if (updatedTask.isEnabled) {
                alarmScheduler.scheduleAlarm(updatedTask)
            } else {
                alarmScheduler.cancelAlarm(updatedTask)
            }
        }
    }

    /**
     * Update an existing task
     */
    fun updateTask(task: ScheduledTask, songUri: String, songTitle: String, hour: Int, minute: Int, targetVolume: Float, repeatDays: String) {
        viewModelScope.launch {
            // Cancel old alarm
            alarmScheduler.cancelAlarm(task)
            
            // Create updated task object
            val updatedTask = task.copy(
                songUri = songUri,
                songTitle = songTitle,
                hour = hour,
                minute = minute,
                isEnabled = true, // Re-enable on edit? Usually yes.
                targetVolume = targetVolume,
                repeatDays = repeatDays
            )

            // Update DB
            taskDao.updateTask(updatedTask)

            // Schedule new alarm
            alarmScheduler.scheduleAlarm(updatedTask)
        }
    }

    /**
     * Update selected time
     */
    fun updateTime(hour: Int, minute: Int) {
        _selectedHour.value = hour
        _selectedMinute.value = minute
    }
}
