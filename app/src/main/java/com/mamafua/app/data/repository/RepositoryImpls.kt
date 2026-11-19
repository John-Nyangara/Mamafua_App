package com.mamafua.app.data.repository

import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.data.local.dao.JobDao
import com.mamafua.app.data.local.dao.NotificationDao
import com.mamafua.app.data.local.dao.UserDao
import com.mamafua.app.data.local.entity.toEntity
import com.mamafua.app.data.remote.api.*
import com.mamafua.app.data.remote.dto.CreateJobReq
import com.mamafua.app.data.remote.dto.LoginReq
import com.mamafua.app.data.remote.dto.RegisterReq
import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val authApi: AuthApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : AuthRepository {
    override suspend fun login(email: String, password: String): User {
        val response = authApi.login(LoginReq(email, password))
        tokenManager.saveToken(response.token)
        userDao.insertUser(response.user.toEntity())
        return response.user
    }

    override suspend fun register(name: String, email: String, phone: String, password: String, role: String): User {
        val response = authApi.register(RegisterReq(name, email, phone, password, role))
        tokenManager.saveToken(response.token)
        userDao.insertUser(response.user.toEntity())
        return response.user
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        userDao.deleteUser()
    }
}

class UserRepositoryImpl(
    private val userApi: UserApiService,
    private val userDao: UserDao
) : UserRepository {
    override suspend fun getProfile(): User {
        val user = userApi.getProfile()
        userDao.insertUser(user.toEntity())
        return user
    }

    override fun getUserFlow(): Flow<User?> {
        return userDao.getUser().map { it?.toDomain() }
    }
}

class JobRepositoryImpl(
    private val jobApi: JobApiService,
    private val jobDao: JobDao
) : JobRepository {
    override suspend fun fetchJobs(status: String?) {
        val jobs = jobApi.getJobs(status)
        jobDao.insertJobs(jobs.map { it.toEntity() })
    }

    override fun getActiveJobs(): Flow<List<Job>> {
        return jobDao.getAllJobs().map { list -> list.map { it.toDomain() } }
    }

    override fun getJob(jobId: Int): Flow<Job?> {
        return jobDao.getJob(jobId).map { it?.toDomain() }
    }

    override suspend fun createJob(title: String, description: String, location: String, budget: Double): Job {
        return jobApi.createJob(CreateJobReq(title, description, location, budget))
    }

    override suspend fun applyForJob(jobId: Int) {
        jobApi.applyForJob(jobId)
    }

    override suspend fun acceptWorker(jobId: Int, workerId: Int) {
        jobApi.acceptWorker(jobId, workerId)
    }

    override suspend fun updateJobStatus(jobId: Int, status: String) {
        jobApi.updateJobStatus(jobId, status)
    }
}

class PaymentRepositoryImpl(
    private val paymentApi: PaymentApiService
) : PaymentRepository {
    override suspend fun initiateSTKPush(jobId: Int, phone: String) {
        paymentApi.initiateSTKPush(jobId, phone)
    }

    override suspend fun checkPaymentStatus(checkoutRequestId: String): Payment {
        return paymentApi.checkPaymentStatus(checkoutRequestId)
    }
}

class NotificationRepositoryImpl(
    private val notificationApi: NotificationApiService,
    private val notificationDao: NotificationDao
) : NotificationRepository {
    override suspend fun fetchNotifications() {
        val notifications = notificationApi.getNotifications()
        notificationDao.insertNotifications(notifications.map { it.toEntity() })
    }

    override fun getNotificationsFlow(): Flow<List<Notification>> {
        return notificationDao.getNotifications().map { list -> list.map { it.toDomain() } }
    }
}
