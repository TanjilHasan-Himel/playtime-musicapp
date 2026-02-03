package com.eplaytime.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.receiver.AlarmReceiver
import java.util.*

/**
 * AlarmScheduler - Utility to schedule and cancel alarms using AlarmManager
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    /**
     * Schedule an exact alarm for a task
     * Uses setAlarmClock (the TRUE alarm clock API) for rock-solid reliability
     * This API guarantees the device WILL wake up, just like a system alarm clock
     */
    fun scheduleAlarm(task: ScheduledTask) {
        // Check if we have permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "❌ Cannot schedule exact alarms - permission not granted on Android 12+")
                return
            }
        }

        val alarmIntent = createAlarmIntent(task)
        val triggerTime = calculateTriggerTime(task.hour, task.minute)

        try {
            Log.d(TAG, "📍 Scheduling ALARM_CLOCK for task ${task.id} at ${formatTime(task.hour, task.minute)}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+: Use setAlarmClock (most reliable)
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, alarmIntent)
                alarmManager.setAlarmClock(alarmClockInfo, alarmIntent)
            } else {
                // Fallback for older Android: setExactAndAllowWhileIdle
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    alarmIntent
                )
            }

            Log.d(TAG, "✓ Alarm scheduled successfully for task ${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling alarm for task ${task.id}", e)
        }
    }

    /**
     * Cancel a scheduled alarm
     */
    fun cancelAlarm(task: ScheduledTask) {
        val alarmIntent = createAlarmIntent(task)
        alarmManager.cancel(alarmIntent)
        alarmIntent.cancel()

        Log.d(TAG, "Alarm cancelled for task ${task.id}")
    }

    /**
     * Reschedule all enabled tasks (called after boot)
     */
    suspend fun rescheduleAllTasks(tasks: List<ScheduledTask>) {
        tasks.filter { it.isEnabled }.forEach { task ->
            scheduleAlarm(task)
        }
        Log.d(TAG, "Rescheduled ${tasks.size} tasks after boot")
    }

    /**
     * Create PendingIntent for alarm
     */
    private fun createAlarmIntent(task: ScheduledTask): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SCHEDULED_PLAYBACK
            putExtra(AlarmReceiver.EXTRA_SONG_URI, task.songUri)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, task.id)
        }

        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(), // Use task ID as request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Calculate trigger time for alarm
     * If time has passed today, schedule for tomorrow
     */
    private fun calculateTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * Format time for logging
     */
    private fun formatTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }
}
