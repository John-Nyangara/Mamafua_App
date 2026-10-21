package com.mamafua.app.domain.repository

import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): User
    suspend fun register(name: String, email: String, phone: String, password: String, role: String): User
    suspend fun logout()
}

interface UserRepository {
    suspend fun getProfile(): User
    fun getUserFlow(): Flow<User?>
}

interface JobRepository {
    suspend fun fetchJobs(status: String? = null): Resource<Unit>
    suspend fun fetchMyPostedJobs(): Resource<Unit>
    suspend fun fetchMyAssignedJobs(): Resource<Unit>
    suspend fun fetchMyApplications(): Resource<Unit>
    fun getActiveJobs(): Flow<List<Job>>
    suspend fun createJob(title: String, description: String, location: String, budget: Double): Resource<Job>
    suspend fun applyForJob(jobId: Int): Resource<Unit>
    suspend fun acceptWorker(jobId: Int, workerId: Int): Resource<Unit>
    suspend fun rejectWorker(jobId: Int, workerId: Int): Resource<Unit>
    suspend fun updateJobStatus(jobId: Int, status: com.mamafua.app.domain.model.JobStatus): Resource<Unit>
    suspend fun confirmCompletion(jobId: Int): Resource<Unit>
    suspend fun cancelJob(jobId: Int): Resource<Unit>
    suspend fun getJobApplicants(jobId: Int): Resource<List<WorkerProfile>>
    suspend fun createReview(jobId: Int, rating: Int, comment: String?): Resource<Unit>
    suspend fun getJobReviews(jobId: Int): Resource<List<Review>>
    suspend fun raiseDispute(jobId: Int, reason: String, description: String): Resource<Unit>
    suspend fun getJobDisputes(jobId: Int): Resource<List<Dispute>>
}

interface PaymentRepository {
    suspend fun initiateSTKPush(jobId: Int, phone: String)
    suspend fun checkPaymentStatus(paymentId: Int): Payment
}

interface NotificationRepository {
    suspend fun fetchNotifications()
    fun getNotificationsFlow(): Flow<List<Notification>>
}
