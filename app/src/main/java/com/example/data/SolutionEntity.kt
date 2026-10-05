package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solution_history")
data class SolutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val stateCount: Int,
    val alphabetString: String,
    val finalRegex: String,
    val compactDfa: String,
    val timestamp: Long = System.currentTimeMillis()
)
