package com.mamafua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mamafua.app.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)

    @Query("DELETE FROM jobs")
    suspend fun clearJobs()

    @Query("UPDATE jobs SET status = :status WHERE id = :jobId")
    suspend fun updateJobStatus(jobId: Int, status: String)
}
