package com.eplaytime.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.eplaytime.app.data.database.PlayTimeDatabase
import com.eplaytime.app.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BootReceiver - Reschedules alarms after device restart.
 * Ensures scheduled playback tasks persist across reboots.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "Device boot completed - rescheduling alarms")

            // Use coroutine to load tasks from database
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = PlayTimeDatabase.getDatabase(context)
                    val taskDao = database.scheduledTaskDao()
                    val alarmScheduler = AlarmScheduler(context)

                    // Get all enabled tasks
                    var tasksRescheduled = 0
                    taskDao.getEnabledTasks().collect { enabledTasks ->
                        enabledTasks.forEach { task ->
                            alarmScheduler.scheduleAlarm(task)
                            tasksRescheduled++
                        }

                        Log.d(TAG, "Rescheduled $tasksRescheduled alarms after boot")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
