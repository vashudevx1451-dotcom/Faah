package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {
    @Query("SELECT * FROM jarvis_commands ORDER BY timestamp DESC LIMIT 100")
    fun getAllCommands(): Flow<List<CommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandEntity): Long

    @Query("DELETE FROM jarvis_commands")
    suspend fun clearHistory()
}
