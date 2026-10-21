package com.mamafua.app.data.repository

import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.data.local.dao.JobDao
import com.mamafua.app.data.local.dao.NotificationDao
import com.mamafua.app.data.local.dao.UserDao
import com.mamafua.app.data.local.entity.toEntity
import com.mamafua.app.data.remote.*
import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.repository.*
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val userDao: UserDao,
    private val tokenManager: TokenManager,
    private val jobDao: JobDao
) : AuthRepository {
    override suspend fun login(email: String, password: String): User {
        val response = apiClient.login(LoginRequest(email, password)).getOrThrow()
        
        if (!response.success || response.token == null) {
            throw Exception(response.message)
        }

        tokenManager.saveToken(response.token)
        
        val user = apiClient.getCurrentUser().getOrThrow()
        userDao.insertUser(user.toEntity())
        return user
    }

    override suspend fun register(name: String, email: String, phone: String, password: String, role: String): User {
        val response = apiClient.register(RegisterRequest(name, email, phone, password, role)).getOrThrow()
        
        if (!response.success) {
            throw Exception(response.message)
        }
        
        return User(id = response.userId ?: 0, name = name, email = email, phone = phone, role = role)
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        userDao.deleteUser()
        jobDao.clearJobs()
    }
}

class UserRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val userDao: UserDao
) : UserRepository {
    override suspend fun getProfile(): User {
        val user = apiClient.getCurrentUser().getOrThrow()
        userDao.insertUser(user.toEntity())
        return user
    }

    override fun getUserFlow(): Flow<User?> {
        return userDao.getUser().map { it?.toDomain() }
    }
}
class JobRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val jobDao: JobDao
) : JobRepository {
    override suspend fun fetchJobs(status: String?): Resource<Unit> {
        return try {
            val response = apiClient.getOpenJobs().getOrThrow()
            jobDao.insertJobs(response.jobs.map { it.toEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    override suspend fun fetchMyPostedJobs(): Resource<Unit> {
        return try {
            val response = apiClient.getMyPostedJobs().getOrThrow()
            jobDao.insertJobs(response.map { it.toEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch my posted jobs")
        }
    }

    override suspend fun fetchMyAssignedJobs(): Resource<Unit> {
        return try {
            val response = apiClient.getMyAssignedJobs().getOrThrow()
            jobDao.insertJobs(response.map { it.toEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch my assigned jobs")
        }
    }

    override suspend fun fetchMyApplications(): Resource<Unit> {
        return try {
            val response = apiClient.getMyApplications().getOrThrow()
            jobDao.insertJobs(response.map { it.toEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch my applications")
        }
    }

    override fun getActiveJobs(): Flow<List<Job>> {
        return jobDao.getAllJobs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createJob(title: String, description: String, location: String, budget: Double): Resource<Job> {
        return try {
            apiClient.createJob(CreateJobRequest(title, description, location, budget)).getOrThrow()
            val dummyJob = Job(id = 0, clientId = 0, title = title, description = description, location = location, budget = budget, status = com.mamafua.app.domain.model.JobStatus.OPEN, paymentStatus = null, createdAt = null)
            Resource.Success(dummyJob)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create job")
        }
    }

    override suspend fun applyForJob(jobId: Int): Resource<Unit> {
        return try {
            apiClient.applyForJob(jobId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to apply for job")
        }
    }

    override suspend fun acceptWorker(jobId: Int, workerId: Int): Resource<Unit> {
        return try {
            // Tell backend to assign the worker (Backend automatically changes status to 'assigned')
            apiClient.acceptApplicant(jobId, JobWorkerRequest(workerId)).getOrThrow()
            
            jobDao.updateJobStatus(jobId, "assigned")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to accept worker")
        }
    }

    override suspend fun rejectWorker(jobId: Int, workerId: Int): Resource<Unit> {
        return try {
            apiClient.rejectApplicant(jobId, JobWorkerRequest(workerId)).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to reject worker")
        }
    }

    override suspend fun updateJobStatus(jobId: Int, status: com.mamafua.app.domain.model.JobStatus): Resource<Unit> {
        return try {
            apiClient.updateJobStatus(jobId, UpdateJobStatusRequest(status.value)).getOrThrow()
            jobDao.updateJobStatus(jobId, status.value)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update job status")
        }
    }

    override suspend fun confirmCompletion(jobId: Int): Resource<Unit> {
        return try {
            apiClient.confirmCompletion(jobId).getOrThrow()
            jobDao.updateJobStatus(jobId, "completed")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to confirm completion")
        }
    }

    override suspend fun getJobApplicants(jobId: Int): Resource<List<WorkerProfile>> {
        return try {
            val applicants = apiClient.getJobApplicants(jobId).getOrThrow()
            Resource.Success(applicants)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get applicants")
        }
    }

    override suspend fun cancelJob(jobId: Int): Resource<Unit> {
        return try {
            apiClient.cancelJob(jobId).getOrThrow()
            jobDao.updateJobStatus(jobId, "CANCELLED")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to cancel job")
        }
    }

    override suspend fun createReview(jobId: Int, rating: Int, comment: String?): Resource<Unit> {
        return try {
            apiClient.createJobReview(jobId, CreateReviewRequest(rating, comment)).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to submit review")
        }
    }

    override suspend fun getJobReviews(jobId: Int): Resource<List<Review>> {
        return try {
            val reviews = apiClient.getJobReviews(jobId).getOrThrow()
            Resource.Success(reviews)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch reviews")
        }
    }

    override suspend fun raiseDispute(jobId: Int, reason: String, description: String): Resource<Unit> {
        return try {
            apiClient.raiseJobDispute(jobId, CreateDisputeRequest(reason, description)).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to raise dispute")
        }
    }

    override suspend fun getJobDisputes(jobId: Int): Resource<List<Dispute>> {
        return try {
            val disputes = apiClient.getJobDisputes(jobId).getOrThrow()
            Resource.Success(disputes)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch disputes")
        }
    }
}

class PaymentRepositoryImpl(
    private val apiClient: MamaFuaApiClient
) : PaymentRepository {
    override suspend fun initiateSTKPush(jobId: Int, phone: String) {
        apiClient.initiateStkPush(jobId, StkPushRequest(phone)).getOrThrow()
    }

    override suspend fun checkPaymentStatus(paymentId: Int): Payment {
        apiClient.queryPayment(paymentId).getOrThrow()
        return Payment(id = paymentId, jobId = 0, amount = 0.0, status = "pending")
    }
}

class NotificationRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val notificationDao: NotificationDao
) : NotificationRepository {
    override suspend fun fetchNotifications() {
        try {
            val response = apiClient.getNotifications().getOrThrow()
            notificationDao.insertNotifications(response.map { it.toEntity() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getNotificationsFlow(): Flow<List<Notification>> {
        return notificationDao.getNotifications().map { list -> list.map { it.toDomain() } }
    }
}

