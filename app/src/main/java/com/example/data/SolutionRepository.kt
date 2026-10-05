package com.example.data

import kotlinx.coroutines.flow.Flow

class SolutionRepository(private val solutionDao: SolutionDao) {

    val allSolutions: Flow<List<SolutionEntity>> = solutionDao.getAllSolutions()

    suspend fun saveSolution(solution: SolutionEntity): Long {
        return solutionDao.insertSolution(solution)
    }

    suspend fun deleteSolution(id: Long) {
        solutionDao.deleteSolutionById(id)
    }

    suspend fun clearHistory() {
        solutionDao.clearAllSolutions()
    }
}
