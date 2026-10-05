package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SolutionDao {

    @Query("SELECT * FROM solution_history ORDER BY timestamp DESC")
    fun getAllSolutions(): Flow<List<SolutionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolution(solution: SolutionEntity): Long

    @Query("DELETE FROM solution_history WHERE id = :id")
    suspend fun deleteSolutionById(id: Long)

    @Query("DELETE FROM solution_history")
    suspend fun clearAllSolutions()
}
