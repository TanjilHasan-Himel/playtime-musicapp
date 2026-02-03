package com.eplaytime.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ScheduledTask - Represents a scheduled music playback alarm
 */
@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songUri: String,
    val songTitle: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val targetVolume: Float = 0.5f, // 0.0 to 1.0 (50% default)
    val repeatDays: String = "", // Comma-separated: "MON,TUE,WED" or empty for one-time
    val label: String = "",
    val snoozeEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
