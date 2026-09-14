package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jarvis_commands")
data class CommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userCommand: String,
    val jarvisResponse: String,
    val actionType: String, // "CALL", "SMS", "WHATSAPP", "APP_LAUNCH", "TORCH", "VOLUME", "BATTERY", "ALARM", "QUERY", "GENERAL"
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)
