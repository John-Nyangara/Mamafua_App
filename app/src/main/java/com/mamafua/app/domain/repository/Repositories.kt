package com.mamafua.app.domain.repository

import com.mamafua.app.domain.model.*
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
    suspend fun fetchJobs(status: String? = null)
    fun getActiveJobs(): Flow<List<Job>>
    suspend fun createJob(title: String, description: String, location: String, budget: Double): Job
    suspend fun applyForJob(jobId: Int)
    suspend fun acceptWorker(jobId: Int, workerId: Int)
    suspend fun updateJobStatus(jobId: Int, status: String)
}

interface PaymentRepository {
    suspend fun initiateSTKPush(jobId: Int, phone: String)
    suspend fun checkPaymentStatus(checkoutRequestId: String): Payment
}

interface NotificationRepository {
    suspend fun fetchNotifications()
    fun getNotificationsFlow(): Flow<List<Notification>>
}
