package com.mamafua.app.domain.use_case

import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.util.Resource
import com.mamafua.app.data.local.TokenManager

class ApplyForJobUseCase(
    private val jobRepository: JobRepository,
    private val tokenManager: TokenManager
) {
    suspend operator fun invoke(jobId: Int): Resource<Unit> {
        // Prevent applying if already applied locally
        val appliedJobs = tokenManager.getAppliedJobs()
        if (appliedJobs.contains(jobId)) {
            return Resource.Error("You have already applied for this job.")
        }

        // Apply via network
        return when (val result = jobRepository.applyForJob(jobId)) {
            is Resource.Success -> {
                tokenManager.saveAppliedJob(jobId)
                Resource.Success(Unit)
            }
            is Resource.Error -> {
                Resource.Error(result.message ?: "Failed to apply for job.")
            }
            is Resource.Loading -> Resource.Loading()
        }
    }
}
