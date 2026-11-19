package com.mamafua.app.data.remote.api

import com.mamafua.app.data.remote.dto.*
import com.mamafua.app.domain.model.Job
import com.mamafua.app.domain.model.Notification
import com.mamafua.app.domain.model.Payment
import com.mamafua.app.domain.model.User
import com.mamafua.app.domain.model.WorkerProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApiService(private val client: HttpClient) {
    suspend fun login(req: LoginReq): AuthRes = client.post("/api/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()

    suspend fun register(req: RegisterReq): AuthRes = client.post("/api/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}

class UserApiService(private val client: HttpClient) {
    suspend fun getProfile(): User = client.get("/api/users/profile").body()

    suspend fun updateProfile(req: UpdateProfileReq): User = client.put("/api/users/profile") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()
}

class WorkerApiService(private val client: HttpClient) {
    suspend fun searchWorkers(lat: Double, lng: Double, radius: Double): List<WorkerProfile> =
        client.get("/api/workers/search") {
            parameter("lat", lat)
            parameter("lng", lng)
            parameter("radius", radius)
        }.body()
}

class JobApiService(private val client: HttpClient) {
    suspend fun createJob(req: CreateJobReq): Job = client.post("/api/jobs") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }.body()

    suspend fun getJobs(status: String? = null): List<Job> = client.get("/api/jobs") {
        status?.let { parameter("status", it) }
    }.body()

    suspend fun applyForJob(jobId: Int) {
        client.post("/api/jobs/$jobId/apply")
    }

    suspend fun acceptWorker(jobId: Int, workerId: Int) {
        client.post("/api/jobs/$jobId/accept") {
            parameter("workerId", workerId)
        }
    }

    suspend fun updateJobStatus(jobId: Int, status: String) {
        client.put("/api/jobs/$jobId/status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateJobStatusReq(status))
        }
    }
}

class PaymentApiService(private val client: HttpClient) {
    suspend fun initiateSTKPush(jobId: Int, phone: String) {
        client.post("/api/payments/stkpush") {
            contentType(ContentType.Application.Json)
            setBody(StkPushReq(jobId, phone))
        }
    }

    suspend fun checkPaymentStatus(checkoutRequestId: String): Payment =
        client.get("/api/payments/status/$checkoutRequestId").body()
}

class NotificationApiService(private val client: HttpClient) {
    suspend fun getNotifications(): List<Notification> = client.get("/api/notifications").body()
}
