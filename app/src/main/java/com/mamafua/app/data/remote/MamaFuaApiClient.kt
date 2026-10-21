package com.mamafua.app.data.remote

import com.mamafua.app.data.local.TokenManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

/**
 * Data Classes representing the JSON Request and Response bodies for the Mama Fua API
 */

// --- AUTHENTICATION ---
@Serializable
data class RegisterRequest(val name: String, val email: String, val phone: String, val password: String, val role: String)
@Serializable
data class RegisterResponse(val success: Boolean, val message: String, @SerialName("user_id") val userId: Int? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)
@Serializable
data class LoginResponse(val success: Boolean = true, val message: String = "", val token: String? = null)

@Serializable
data class ForgotPasswordRequest(val email: String)
@Serializable
data class ResetPasswordRequest(val token: String, @SerialName("new_password") val newPassword: String)
@Serializable
data class GenericResponse(val success: Boolean = true, val message: String = "")

// --- USERS ---
@Serializable
data class CurrentUserResponse(val success: Boolean = true, val user: com.mamafua.app.domain.model.User)

@Serializable
data class SwitchRoleRequest(val role: String)
@Serializable
data class UpdateUserRequest(val name: String? = null, val email: String? = null, val phone: String? = null, @SerialName("current_password") val currentPassword: String? = null, @SerialName("new_password") val newPassword: String? = null)

// --- WORKERS ---
@Serializable
data class CreateWorkerRequest(val service: String, val location: String, val price: Double, val skills: String? = null, val bio: String? = null)
@Serializable
data class UpdateWorkerRequest(val service: String? = null, val location: String? = null, val price: Double? = null)

// --- JOBS ---
@Serializable
data class CreateJobRequest(val title: String, val description: String, val location: String, val budget: Double)
@Serializable
data class JobWorkerRequest(val workerId: Int)
@Serializable
data class UpdateJobStatusRequest(val status: String)
@Serializable
data class NotificationResponse(val success: Boolean, val data: List<com.mamafua.app.domain.model.Notification>)

@Serializable
data class CreateReviewRequest(val rating: Int, val comment: String? = null)
@Serializable
data class CreateDisputeRequest(val reason: String, val description: String)

// --- PAYMENTS ---
@Serializable
data class StkPushRequest(@SerialName("phone_number") val phoneNumber: String)

@Serializable
data class PaginatedWorkersResponse(val total: Int, val page: Int, val limit: Int, val totalPages: Int, val workers: List<com.mamafua.app.domain.model.WorkerProfile>)

@Serializable
data class PaginatedJobsResponse(val total: Int, val page: Int, val limit: Int, val totalPages: Int, val jobs: List<com.mamafua.app.domain.model.Job>)


/**
 * The MamaFua API Client
 * 
 * Usage:
 * val apiClient = MamaFuaApiClient(TokenManager(context)) // Pass your local token storage
 * val response = apiClient.login(LoginRequest("test@email.com", "pass123"))
 */
class MamaFuaApiClient(private val tokenManager: TokenManager) {

    // Ktor HTTP Client Setup
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true // Prevents crashes if backend adds new fields
                isLenient = true
            })
        }
        
        install(DefaultRequest) {
            // Set your Render backend URL here
            url("https://mama-fua-backend.onrender.com/")
            contentType(ContentType.Application.Json)
            
            // Automatically inject the JWT token into all requests if available
            tokenManager.getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ============================
    // 1. AUTHENTICATION
    // ============================
    
    suspend fun register(request: RegisterRequest): Result<RegisterResponse> = makeRequest {
        client.post("api/auth/register") { setBody(request) }.body()
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = makeRequest {
        client.post("api/auth/login") { setBody(request) }.body()
    }

    suspend fun logout(): Result<GenericResponse> = makeRequest {
        client.post("api/auth/logout").body()
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): Result<GenericResponse> = makeRequest {
        client.post("api/auth/forgot-password") { setBody(request) }.body()
    }

    suspend fun resetPassword(request: ResetPasswordRequest): Result<GenericResponse> = makeRequest {
        client.post("api/auth/reset-password") { setBody(request) }.body()
    }

    // ============================
    // 2. USERS
    // ============================

    suspend fun switchRole(request: SwitchRoleRequest): Result<GenericResponse> = makeRequest {
        client.put("api/users/switch-role") { setBody(request) }.body()
    }

    suspend fun getCurrentUser(): Result<com.mamafua.app.domain.model.User> = makeRequest {
        val response: CurrentUserResponse = client.get("api/users/me").body()
        response.user
    }

    suspend fun updateCurrentUser(request: UpdateUserRequest): Result<GenericResponse> = makeRequest {
        client.put("api/users/me") { setBody(request) }.body()
    }

    // ============================
    // 3. WORKERS
    // ============================

    suspend fun createWorkerProfile(request: CreateWorkerRequest): Result<GenericResponse> = makeRequest {
        client.post("api/workers/create") { setBody(request) }.body()
    }

    suspend fun getWorkers(page: Int = 1, limit: Int = 20): Result<PaginatedWorkersResponse> = makeRequest {
        client.get("api/workers") {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    suspend fun updateWorkerProfile(request: UpdateWorkerRequest): Result<GenericResponse> = makeRequest {
        client.put("api/workers/profile") { setBody(request) }.body()
    }

    // ============================
    // 4. JOBS
    // ============================

    suspend fun createJob(request: CreateJobRequest): Result<GenericResponse> = makeRequest {
        client.post("api/jobs") { setBody(request) }.body()
    }

    suspend fun getOpenJobs(page: Int = 1, limit: Int = 20): Result<PaginatedJobsResponse> = makeRequest {
        client.get("api/jobs/open") {
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    suspend fun getMyPostedJobs(page: Int = 1, limit: Int = 20): Result<List<com.mamafua.app.domain.model.Job>> = makeRequest {
        val jsonString = client.get("api/jobs/my-posted") {
            parameter("page", page)
            parameter("limit", limit)
        }.bodyAsText()
        parseJobList(jsonString)
    }

    suspend fun getMyAssignedJobs(page: Int = 1, limit: Int = 20): Result<List<com.mamafua.app.domain.model.Job>> = makeRequest {
        val jsonString = client.get("api/jobs/my-assigned") {
            parameter("page", page)
            parameter("limit", limit)
        }.bodyAsText()
        parseJobList(jsonString)
    }

    suspend fun getMyApplications(page: Int = 1, limit: Int = 20): Result<List<com.mamafua.app.domain.model.Job>> = makeRequest {
        val jsonString = client.get("api/jobs/my-applications") {
            parameter("page", page)
            parameter("limit", limit)
        }.bodyAsText()
        parseJobList(jsonString)
    }

    private val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun parseJobList(jsonString: String): List<com.mamafua.app.domain.model.Job> {
        val jsonElement = jsonParser.parseToJsonElement(jsonString)
        val array = if (jsonElement is kotlinx.serialization.json.JsonArray) {
            jsonElement
        } else if (jsonElement is kotlinx.serialization.json.JsonObject) {
            jsonElement["jobs"] as? kotlinx.serialization.json.JsonArray
                ?: jsonElement["data"] as? kotlinx.serialization.json.JsonArray
                ?: jsonElement.values.firstOrNull { it is kotlinx.serialization.json.JsonArray } as? kotlinx.serialization.json.JsonArray
        } else null

        return array?.let { jsonParser.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(com.mamafua.app.domain.model.Job.serializer()), it) } ?: emptyList()
    }

    suspend fun applyForJob(jobId: Int): Result<GenericResponse> = makeRequest {
        client.post("api/jobs/$jobId/apply").body()
    }

    suspend fun getJobApplicants(jobId: Int): Result<List<com.mamafua.app.domain.model.WorkerProfile>> = makeRequest {
        val jsonString = client.get("api/jobs/$jobId/applicants").bodyAsText()
        val jsonElement = Json.parseToJsonElement(jsonString)
        
        val array = if (jsonElement is kotlinx.serialization.json.JsonArray) {
            jsonElement
        } else if (jsonElement is kotlinx.serialization.json.JsonObject) {
            jsonElement["applicants"] as? kotlinx.serialization.json.JsonArray
                ?: jsonElement["workers"] as? kotlinx.serialization.json.JsonArray
                ?: jsonElement["data"] as? kotlinx.serialization.json.JsonArray
        } else null

        if (array != null) {
            val decoder = Json { ignoreUnknownKeys = true; isLenient = true }
            decoder.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(com.mamafua.app.domain.model.WorkerProfile.serializer()), array)
        } else {
            emptyList()
        }
    }

    suspend fun acceptApplicant(jobId: Int, request: JobWorkerRequest): Result<GenericResponse> = makeRequest {
        val httpResponse = client.put("api/jobs/$jobId/accept") { setBody(request) }
        if (httpResponse.status.value !in 200..299) {
            throw Exception("Backend Error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        val response: GenericResponse = httpResponse.body()
        if (!response.success) throw Exception(response.message)
        response
    }

    suspend fun rejectApplicant(jobId: Int, request: JobWorkerRequest): Result<GenericResponse> = makeRequest {
        val httpResponse = client.put("api/jobs/$jobId/reject") { setBody(request) }
        if (httpResponse.status.value !in 200..299) {
            throw Exception("Backend Error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        val response: GenericResponse = httpResponse.body()
        if (!response.success) throw Exception(response.message)
        response
    }

    suspend fun updateJobStatus(jobId: Int, request: UpdateJobStatusRequest): Result<GenericResponse> = makeRequest {
        val httpResponse = client.put("api/jobs/$jobId/status") { setBody(request) }
        if (httpResponse.status.value !in 200..299) {
            throw Exception("Backend Error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        val response: GenericResponse = httpResponse.body()
        if (!response.success) throw Exception(response.message)
        response
    }

    suspend fun confirmCompletion(jobId: Int): Result<GenericResponse> = makeRequest {
        val httpResponse = client.post("api/jobs/$jobId/confirm-completion")
        if (httpResponse.status.value !in 200..299) {
            throw Exception("Backend Error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        val response: GenericResponse = httpResponse.body()
        if (!response.success) throw Exception(response.message)
        response
    }

    suspend fun cancelJob(jobId: Int): Result<GenericResponse> = makeRequest {
        client.delete("api/jobs/$jobId").body()
    }

    // ============================
    // REVIEWS
    // ============================

    suspend fun createJobReview(jobId: Int, request: CreateReviewRequest): Result<GenericResponse> = makeRequest {
        val httpResponse = client.post("api/jobs/$jobId/reviews") { setBody(request) }
        if (httpResponse.status.value !in 200..299) {
            throw Exception("Backend Error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        val response: GenericResponse = httpResponse.body()
        if (!response.success) throw Exception(response.message)
        response
    }

    suspend fun getJobReviews(jobId: Int): Result<List<com.mamafua.app.domain.model.Review>> = makeRequest {
        val jsonArray = client.get("api/jobs/$jobId/reviews").bodyAsText()
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        json.decodeFromString<List<com.mamafua.app.domain.model.Review>>(jsonArray)
    }

    // ============================
    // DISPUTES
    // ============================

    suspend fun raiseJobDispute(jobId: Int, request: CreateDisputeRequest): Result<GenericResponse> = makeRequest {
        val httpResponse = client.post("api/jobs/$jobId/disputes") { setBody(request) }
        if (httpResponse.status.value !in 200..299) {
            throw Exception("Backend Error (${httpResponse.status.value}): ${httpResponse.bodyAsText()}")
        }
        val response: GenericResponse = httpResponse.body()
        if (!response.success) throw Exception(response.message)
        response
    }

    suspend fun getJobDisputes(jobId: Int): Result<List<com.mamafua.app.domain.model.Dispute>> = makeRequest {
        val jsonArray = client.get("api/jobs/$jobId/disputes").bodyAsText()
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        json.decodeFromString<List<com.mamafua.app.domain.model.Dispute>>(jsonArray)
    }

    // ============================
    // 5. PAYMENTS (M-PESA)
    // ============================

    suspend fun getNotifications(): Result<List<com.mamafua.app.domain.model.Notification>> = makeRequest {
        val jsonString = client.get("api/notifications").bodyAsText()
        val jsonElement = jsonParser.parseToJsonElement(jsonString)
        
        val array = if (jsonElement is kotlinx.serialization.json.JsonArray) {
            jsonElement
        } else if (jsonElement is kotlinx.serialization.json.JsonObject) {
            jsonElement["notifications"] as? kotlinx.serialization.json.JsonArray
                ?: jsonElement["data"] as? kotlinx.serialization.json.JsonArray
                ?: jsonElement.values.firstOrNull { it is kotlinx.serialization.json.JsonArray } as? kotlinx.serialization.json.JsonArray
        } else null

        if (array != null) {
            jsonParser.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(com.mamafua.app.domain.model.Notification.serializer()), array)
        } else {
            emptyList()
        }
    }

    suspend fun initiateStkPush(jobId: Int, request: StkPushRequest): Result<String> = makeRequest {
        client.post("api/jobs/$jobId/payments/mpesa/stk-push") { setBody(request) }.bodyAsText()
    }

    suspend fun getJobPayments(jobId: Int): Result<String> = makeRequest {
        client.get("api/jobs/$jobId/payments").bodyAsText()
    }

    suspend fun queryPayment(paymentId: Int): Result<String> = makeRequest {
        client.post("api/payments/$paymentId/query").bodyAsText()
    }

    // ============================
    // UTILS
    // ============================

    // A simple wrapper to catch network errors and return Kotlin's Result class
    private suspend inline fun <T> makeRequest(call: () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
