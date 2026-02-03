package com.eplaytime.app.data.dao

import androidx.room.*
import com.eplaytime.app.data.model.ScheduledTask
import kotlinx.coroutines.flow.Flow

/**
 * DAO for scheduled tasks
 */
@Dao
interface ScheduledTaskDao {

    @Query("SELECT * FROM scheduled_tasks ORDER BY hour, minute")
    fun getAllTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE isEnabled = 1 ORDER BY hour, minute")
    fun getEnabledTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): ScheduledTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTask): Long

    @Update
    suspend fun updateTask(task: ScheduledTask)

    @Delete
    suspend fun deleteTask(task: ScheduledTask)

    @Query("DELETE FROM scheduled_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE scheduled_tasks SET isEnabled = :enabled WHERE id = :taskId")
    suspend fun toggleTask(taskId: Long, enabled: Boolean)
}
