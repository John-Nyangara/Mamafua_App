@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mamafua.app.data.remote

import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

// --- AUTHENTICATION ---
@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String
)

@Serializable
data class RegisterResponse(
    val success: Boolean = true,
    val message: String = "",
    @JsonNames("user_id", "userId") val userId: Int? = null,
    val token: String? = null
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val success: Boolean = true,
    val message: String = "",
    val token: String? = null
)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    @SerialName("new_password") val newPassword: String
)

@Serializable
data class GenericResponse(
    val success: Boolean = true,
    val message: String = ""
)

// --- CATALOG & ESTIMATOR ---
@Serializable
data class EstimatePriceRequest(
    val items: List<SelectedQuantityItem> = emptyList(),
    val addons: List<SelectedQuantityItem> = emptyList(),
    val workerBringsDetergents: Boolean = false
)

// --- USERS, FAVORITES, REPORT & BLOCK ---
@Serializable
data class CurrentUserResponse(
    val success: Boolean = true,
    val user: User
)

@Serializable
data class SwitchRoleRequest(val role: String)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("current_password") val currentPassword: String? = null,
    @SerialName("new_password") val newPassword: String? = null
)

@Serializable
data class AddFavoriteRequest(val note: String? = null)

@Serializable
data class ReportUserRequest(
    val reason: String,
    val description: String? = null
)

// --- WORKERS, RADAR, AVAILABILITY & WALLET ---
@Serializable
data class CreateOrUpdateWorkerRequest(
    val service: String? = null,
    val location: String? = null,
    val estate: String? = null,
    val price: Double? = null,
    @SerialName("hourly_rate") val hourlyRate: Double? = null,
    @Serializable(with = FlexibleStringListSerializer::class)
    val skills: List<String> = emptyList(),
    @Serializable(with = FlexibleStringListSerializer::class)
    val badges: List<String> = emptyList(),
    @SerialName("id_number") val idNumber: String? = null,
    val bio: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("availability_status") val availabilityStatus: String? = null,
    @SerialName("is_id_verified") val isIdVerified: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class AvailabilityToggleRequest(val status: String)

@Serializable
data class RadarRespondRequest(val action: String) // "accept" or "decline"

@Serializable
data class WithdrawEarningsRequest(val phoneNumber: String)

@Serializable
data class CreateAvailabilitySlotRequest(
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean = true
)

// --- JOBS, MATCHING, HANDSHAKE & SCOPE ---
@Serializable
data class CreateJobRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val location: String,
    val estate: String? = null,
    val budget: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isInstant: Boolean = true,
    val scheduledAt: String? = null,
    val workerBringsDetergents: Boolean = false,
    val matchingMode: String? = null,
    val preferredWorkerId: Int? = null,
    val items: List<SelectedQuantityItem>? = null,
    val addons: List<SelectedQuantityItem>? = null
)

@Serializable
data class UpdateOpenJobRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val estate: String? = null,
    val budget: Double? = null
)

@Serializable
data class RebookJobRequest(
    val workerId: Int? = null,
    val isInstant: Boolean = true,
    val scheduledAt: String? = null
)

@Serializable
data class AutoDispatchRequest(
    val radiusKm: Double = 5.0,
    val maxBroadcast: Int = 3
)

@Serializable
data class JobWorkerRequest(val workerId: Int)

@Serializable
data class UpdateJobStatusRequest(val status: String)

@Serializable
data class VerifyStartPinRequest(val pin: String)

@Serializable
data class CreateScopeAdjustmentRequest(
    val additionalAmount: Double,
    val reason: String,
    val extraItems: List<ScopeExtraItem> = emptyList()
)

@Serializable
data class RespondScopeAdjustmentRequest(val action: String) // "approve" or "reject"

// --- REVIEWS & DISPUTES ---
@Serializable
data class CreateReviewRequest(val rating: Int, val comment: String? = null)

@Serializable
data class CreateDisputeRequest(val reason: String, val description: String)

@Serializable
data class ResolveDisputeRequest(
    val status: String, // "resolved" or "dismissed"
    @SerialName("resolution_notes") val resolutionNotes: String? = null,
    val action: String? = null
)

// --- PAYMENTS ---
@Serializable
data class StkPushRequest(
    val phoneNumber: String,
    val paymentType: String = "full" // "full", "deposit_50", or "scope_topup"
)

@Serializable
data class StkPushResponse(
    val success: Boolean = true,
    val message: String = "",
    @JsonNames("payment_id", "paymentId", "id") val paymentId: Int? = null,
    @JsonNames("checkout_request_id", "checkoutRequestId") val checkoutRequestId: String? = null
)

@Serializable
data class PaginatedWorkersResponse(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val totalPages: Int = 1,
    val workers: List<WorkerProfile> = emptyList()
)

@Serializable
data class PaginatedJobsResponse(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val totalPages: Int = 1,
    val jobs: List<Job> = emptyList()
)

class MamaFuaApiClient(private val tokenManager: TokenManager) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000L // 60 seconds
            connectTimeoutMillis = 60000L
            socketTimeoutMillis = 60000L
        }

        install(ContentNegotiation) {
            json(jsonParser)
        }

        install(DefaultRequest) {
            url("https://mamafua-backend-4oig.onrender.com/")
            contentType(ContentType.Application.Json)
            tokenManager.getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ============================
    // 1. AUTHENTICATION (/api/auth)
    // ============================

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> = makeRequest {
        val resp = client.post("api/auth/register") { setBody(request) }
        ensureSuccess(resp)
        resp.body()
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = makeRequest {
        val resp = client.post("api/auth/login") { setBody(request) }
        ensureSuccess(resp)
        resp.body()
    }

    suspend fun logout(): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/auth/logout")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/auth/forgot-password") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun resetPassword(request: ResetPasswordRequest): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/auth/reset-password") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    // ============================
    // 2. ELDORET CATALOG & ESTIMATOR (/api/catalog)
    // ============================

    suspend fun getCatalog(): Result<CatalogData> = makeRequest {
        val resp = client.get("api/catalog")
        ensureSuccess(resp)
        parseCatalogData(resp.bodyAsText())
    }

    suspend fun estimatePrice(request: EstimatePriceRequest): Result<PriceEstimate> = makeRequest {
        val resp = client.post("api/catalog/estimate") { setBody(request) }
        ensureSuccess(resp)
        val root = jsonParser.parseToJsonElement(resp.bodyAsText())
        val target = (root as? JsonObject)?.get("estimate")
            ?: (root as? JsonObject)?.get("data")
            ?: root
        jsonParser.decodeFromJsonElement(PriceEstimate.serializer(), target)
    }

    // ============================
    // 3. USERS, FAVORITES, REPORT & BLOCK (/api/users)
    // ============================

    suspend fun getCurrentUser(): Result<User> = makeRequest {
        val resp = client.get("api/users/me")
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        val root = jsonParser.parseToJsonElement(text)
        val userEl = (root as? JsonObject)?.get("user")
            ?: (root as? JsonObject)?.get("data")
            ?: root
        jsonParser.decodeFromJsonElement(User.serializer(), userEl)
    }

    suspend fun updateCurrentUser(request: UpdateUserRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/users/me") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun switchRole(role: String): Result<String?> = makeRequest {
        val resp = client.put("api/users/switch-role") { setBody(SwitchRoleRequest(role)) }
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        val root = runCatching { jsonParser.parseToJsonElement(text) as? JsonObject }.getOrNull()
        val newToken = root?.get("token")?.jsonPrimitive?.contentOrNull
        newToken
    }

    suspend fun getFavoriteWorkers(): Result<List<WorkerProfile>> = makeRequest {
        val resp = client.get("api/users/favorites")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            WorkerProfile.serializer(),
            listOf("favorites", "workers", "data")
        )
    }

    suspend fun addFavoriteWorker(workerId: Int, note: String? = null): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/users/favorites/$workerId") {
            setBody(AddFavoriteRequest(note))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun removeFavoriteWorker(workerId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.delete("api/users/favorites/$workerId")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun reportUser(userId: Int, reason: String, description: String? = null): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/users/$userId/report") {
            setBody(ReportUserRequest(reason, description))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun blockUser(userId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/users/$userId/block")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun unblockUser(userId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.delete("api/users/$userId/block")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getBlockedUsers(): Result<List<BlockedUser>> = makeRequest {
        val resp = client.get("api/users/blocked")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            BlockedUser.serializer(),
            listOf("blocked", "blockedUsers", "users", "data")
        )
    }

    // ============================
    // 4. WORKERS, JOB RADAR & WALLET (/api/workers)
    // ============================

    suspend fun getWorkers(
        estate: String? = null,
        service: String? = null,
        badge: String? = null,
        availabilityStatus: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Double? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<WorkerProfile>> = makeRequest {
        val resp = client.get("api/workers") {
            estate?.takeIf { it.isNotBlank() }?.let { parameter("estate", it) }
            service?.takeIf { it.isNotBlank() }?.let { parameter("service", it) }
            badge?.takeIf { it.isNotBlank() }?.let { parameter("badge", it) }
            availabilityStatus?.takeIf { it.isNotBlank() }?.let { parameter("availability_status", it) }
            minPrice?.let { parameter("minPrice", it) }
            maxPrice?.let { parameter("maxPrice", it) }
            lat?.let { parameter("lat", it) }
            lng?.let { parameter("lng", it) }
            radiusKm?.let { parameter("radiusKm", it) }
            parameter("page", page)
            parameter("limit", limit)
        }
        ensureSuccess(resp)
        parseListFromJson(resp.bodyAsText(), WorkerProfile.serializer(), listOf("workers", "data"))
    }

    suspend fun getWorkerById(workerId: Int): Result<WorkerProfile> = makeRequest {
        val resp = client.get("api/workers/$workerId")
        ensureSuccess(resp)
        val root = jsonParser.parseToJsonElement(resp.bodyAsText())
        val workerEl = (root as? JsonObject)?.get("worker")
            ?: (root as? JsonObject)?.get("data")
            ?: root
        jsonParser.decodeFromJsonElement(WorkerProfile.serializer(), workerEl)
    }

    suspend fun getWorkerReviews(workerId: Int): Result<WorkerReviewsSummary> = makeRequest {
        val resp = client.get("api/workers/$workerId/reviews")
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        val root = jsonParser.parseToJsonElement(text)
        if (root is JsonArray) {
            val list = jsonParser.decodeFromJsonElement(ListSerializer(Review.serializer()), root)
            val avg = if (list.isNotEmpty()) list.map { it.rating }.average() else 0.0
            WorkerReviewsSummary(averageRating = avg, totalReviews = list.size, reviews = list)
        } else {
            val obj = root as? JsonObject
            val reviews = parseListFromJson(text, Review.serializer(), listOf("reviews", "data"))
            val avg = obj?.get("averageRating")?.jsonPrimitive?.doubleOrNull
                ?: obj?.get("average_rating")?.jsonPrimitive?.doubleOrNull
                ?: (obj?.get("summary") as? JsonObject)?.get("averageRating")?.jsonPrimitive?.doubleOrNull
                ?: if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
            WorkerReviewsSummary(averageRating = avg, totalReviews = reviews.size, reviews = reviews)
        }
    }

    suspend fun createWorkerProfile(request: CreateOrUpdateWorkerRequest): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/workers/create") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun updateWorkerProfile(request: CreateOrUpdateWorkerRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/workers/profile") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun toggleWorkerAvailability(status: String): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/workers/me/availability-toggle") {
            setBody(AvailabilityToggleRequest(status))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getJobRadar(): Result<List<RadarOffer>> = makeRequest {
        val resp = client.get("api/workers/me/job-radar")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            RadarOffer.serializer(),
            listOf("incomingRequests", "offers", "radar", "data")
        )
    }

    suspend fun respondToRadarOffer(offerId: Int, action: String): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/workers/me/job-radar/$offerId/respond") {
            setBody(RadarRespondRequest(action))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getWorkerEarnings(): Result<WorkerEarningsData> = makeRequest {
        val resp = client.get("api/workers/me/earnings")
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        val root = jsonParser.parseToJsonElement(text)
        val target = (root as? JsonObject)?.get("data") ?: root
        jsonParser.decodeFromJsonElement(WorkerEarningsData.serializer(), target)
    }

    suspend fun withdrawEarnings(phoneNumber: String): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/workers/me/withdraw") {
            setBody(WithdrawEarningsRequest(phoneNumber))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getWorkerAvailability(workerId: Int): Result<List<AvailabilitySlot>> = makeRequest {
        val resp = client.get("api/workers/$workerId/availability")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            AvailabilitySlot.serializer(),
            listOf("availability", "slots", "schedule", "data")
        )
    }

    suspend fun getMyAvailability(): Result<List<AvailabilitySlot>> = makeRequest {
        val resp = client.get("api/workers/me/availability")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            AvailabilitySlot.serializer(),
            listOf("availability", "slots", "schedule", "data")
        )
    }

    suspend fun addAvailabilitySlot(request: CreateAvailabilitySlotRequest): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/workers/availability") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun deleteAvailabilitySlot(slotId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.delete("api/workers/availability/$slotId")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    // ============================
    // 5. JOBS, MATCHING, HANDSHAKE & SCOPE (/api/jobs)
    // ============================

    suspend fun getOpenJobs(
        estate: String? = null,
        category: String? = null,
        search: String? = null,
        minBudget: Double? = null,
        maxBudget: Double? = null,
        page: Int = 1,
        limit: Int = 30
    ): Result<List<Job>> = makeRequest {
        val resp = client.get("api/jobs/open") {
            estate?.takeIf { it.isNotBlank() }?.let { parameter("estate", it) }
            category?.takeIf { it.isNotBlank() }?.let { parameter("category", it) }
            search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
            minBudget?.let { parameter("minBudget", it) }
            maxBudget?.let { parameter("maxBudget", it) }
            parameter("page", page)
            parameter("limit", limit)
        }
        ensureSuccess(resp)
        parseJobList(resp.bodyAsText())
    }

    suspend fun getJobById(jobId: Int): Result<Job> = makeRequest {
        val resp = client.get("api/jobs/$jobId")
        ensureSuccess(resp)
        parseSingleJob(resp.bodyAsText())
    }

    suspend fun createJob(request: CreateJobRequest): Result<Job> = makeRequest {
        val resp = client.post("api/jobs") { setBody(request) }
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        runCatching { parseSingleJob(text) }.getOrElse {
            val root = runCatching { jsonParser.parseToJsonElement(text) as? JsonObject }.getOrNull()
            val id = root?.get("job_id")?.jsonPrimitive?.intOrNull
                ?: root?.get("jobId")?.jsonPrimitive?.intOrNull
                ?: root?.get("id")?.jsonPrimitive?.intOrNull
                ?: 0
            Job(
                id = id,
                title = request.title ?: "Eldoret Cleaning Booking",
                description = request.description ?: "",
                location = request.location,
                estate = request.estate,
                budget = request.budget ?: 0.0,
                status = JobStatus.OPEN,
                matchingMode = request.matchingMode,
                isInstant = request.isInstant,
                scheduledAt = request.scheduledAt,
                workerBringsDetergents = request.workerBringsDetergents
            )
        }
    }

    suspend fun rebookJob(request: RebookJobRequest): Result<Job> = makeRequest {
        val resp = client.post("api/jobs/rebook") { setBody(request) }
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        runCatching { parseSingleJob(text) }.getOrElse {
            val root = runCatching { jsonParser.parseToJsonElement(text) as? JsonObject }.getOrNull()
            val id = root?.get("job_id")?.jsonPrimitive?.intOrNull
                ?: root?.get("jobId")?.jsonPrimitive?.intOrNull
                ?: 0
            Job(id = id, title = "Rebooked Mama Fua Service", status = JobStatus.OPEN)
        }
    }

    suspend fun updateOpenJob(jobId: Int, request: UpdateOpenJobRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun cancelJob(jobId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.delete("api/jobs/$jobId")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getMyPostedJobs(page: Int = 1, limit: Int = 50): Result<List<Job>> = makeRequest {
        val resp = client.get("api/jobs/my-posted") {
            parameter("page", page)
            parameter("limit", limit)
        }
        ensureSuccess(resp)
        parseJobList(resp.bodyAsText())
    }

    suspend fun getMyAssignedJobs(page: Int = 1, limit: Int = 50): Result<List<Job>> = makeRequest {
        val resp = client.get("api/jobs/my-assigned") {
            parameter("page", page)
            parameter("limit", limit)
        }
        ensureSuccess(resp)
        parseJobList(resp.bodyAsText())
    }

    suspend fun getMyApplications(page: Int = 1, limit: Int = 50): Result<List<Job>> = makeRequest {
        val resp = client.get("api/jobs/my-applications") {
            parameter("page", page)
            parameter("limit", limit)
        }
        ensureSuccess(resp)
        parseJobList(resp.bodyAsText())
    }

    suspend fun getJobProgress(jobId: Int): Result<JobProgress> = makeRequest {
        val resp = client.get("api/jobs/$jobId/progress")
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        val root = jsonParser.parseToJsonElement(text)
        val target = (root as? JsonObject)?.get("progress")
            ?: (root as? JsonObject)?.get("data")
            ?: root
        jsonParser.decodeFromJsonElement(JobProgress.serializer(), target)
    }

    suspend fun autoDispatchJob(jobId: Int, radiusKm: Double = 5.0, maxBroadcast: Int = 3): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/auto-dispatch") {
            setBody(AutoDispatchRequest(radiusKm = radiusKm, maxBroadcast = maxBroadcast))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun applyForJob(jobId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/apply")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun withdrawApplication(jobId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.delete("api/jobs/$jobId/apply")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getJobApplicants(jobId: Int): Result<List<WorkerProfile>> = makeRequest {
        val resp = client.get("api/jobs/$jobId/applicants")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            WorkerProfile.serializer(),
            listOf("applicants", "workers", "data")
        )
    }

    suspend fun shortlistApplicant(jobId: Int, workerId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId/shortlist") { setBody(JobWorkerRequest(workerId)) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun acceptApplicant(jobId: Int, request: JobWorkerRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId/accept") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun rejectApplicant(jobId: Int, request: JobWorkerRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId/reject") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun directAssignWorker(jobId: Int, workerId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId/assign") { setBody(JobWorkerRequest(workerId)) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun verifyStartPin(jobId: Int, pin: String): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/verify-start-pin") {
            setBody(VerifyStartPinRequest(pin.trim()))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun updateJobStatus(jobId: Int, request: UpdateJobStatusRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId/status") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun createScopeAdjustment(
        jobId: Int,
        request: CreateScopeAdjustmentRequest
    ): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/scope-adjustments") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getScopeAdjustments(jobId: Int): Result<List<ScopeAdjustment>> = makeRequest {
        val resp = client.get("api/jobs/$jobId/scope-adjustments")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            ScopeAdjustment.serializer(),
            listOf("adjustments", "scopeAdjustments", "scope_adjustments", "data")
        )
    }

    suspend fun respondToScopeAdjustment(
        jobId: Int,
        adjustmentId: Int,
        action: String
    ): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/jobs/$jobId/scope-adjustments/$adjustmentId/respond") {
            setBody(RespondScopeAdjustmentRequest(action))
        }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun markJobCompleteByWorker(jobId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/complete")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun confirmCompletion(jobId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/confirm-completion")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    // ============================
    // 6. REVIEWS (/api/jobs/:id/reviews & /api/reviews)
    // ============================

    suspend fun createJobReview(jobId: Int, request: CreateReviewRequest): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/reviews") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getJobReviews(jobId: Int): Result<List<Review>> = makeRequest {
        val resp = client.get("api/jobs/$jobId/reviews")
        ensureSuccess(resp)
        parseListFromJson(resp.bodyAsText(), Review.serializer(), listOf("reviews", "data"))
    }

    // ============================
    // 7. DISPUTES (/api/jobs/:id/disputes & /api/disputes)
    // ============================

    suspend fun raiseJobDispute(jobId: Int, request: CreateDisputeRequest): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/disputes") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun getJobDisputes(jobId: Int): Result<List<Dispute>> = makeRequest {
        val resp = client.get("api/jobs/$jobId/disputes")
        ensureSuccess(resp)
        parseListFromJson(resp.bodyAsText(), Dispute.serializer(), listOf("disputes", "data"))
    }

    suspend fun getAllDisputes(): Result<List<Dispute>> = makeRequest {
        val resp = client.get("api/disputes")
        ensureSuccess(resp)
        parseListFromJson(resp.bodyAsText(), Dispute.serializer(), listOf("disputes", "data"))
    }

    suspend fun resolveDispute(disputeId: Int, request: ResolveDisputeRequest): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/disputes/$disputeId/resolve") { setBody(request) }
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    // ============================
    // 8. PAYMENTS & ESCROW (/api/jobs/:id/payments & /api/payments)
    // ============================

    suspend fun initiateStkPush(jobId: Int, request: StkPushRequest): Result<StkPushResponse> = makeRequest {
        val resp = client.post("api/jobs/$jobId/payments/mpesa/stk-push") { setBody(request) }
        ensureSuccess(resp)
        val text = resp.bodyAsText()
        runCatching { jsonParser.decodeFromString(StkPushResponse.serializer(), text) }
            .getOrDefault(StkPushResponse(success = true, message = "STK Push sent to ${request.phoneNumber}"))
    }

    suspend fun getJobPayments(jobId: Int): Result<List<Payment>> = makeRequest {
        val resp = client.get("api/jobs/$jobId/payments")
        ensureSuccess(resp)
        parseListFromJson(resp.bodyAsText(), Payment.serializer(), listOf("payments", "data"))
    }

    suspend fun queryPayment(paymentId: Int): Result<Payment> = makeRequest {
        val resp = client.post("api/payments/$paymentId/query")
        ensureSuccess(resp)
        val root = jsonParser.parseToJsonElement(resp.bodyAsText())
        val target = (root as? JsonObject)?.get("payment")
            ?: (root as? JsonObject)?.get("data")
            ?: root
        jsonParser.decodeFromJsonElement(Payment.serializer(), target)
    }

    suspend fun releaseJobEscrow(jobId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/payments/jobs/$jobId/release")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    suspend fun triggerAutoReleaseEscrow(): Result<GenericResponse> = makeRequest {
        val resp = client.post("api/payments/auto-release")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    // ============================
    // 9. NOTIFICATIONS (/api/notifications)
    // ============================

    suspend fun getNotifications(): Result<List<Notification>> = makeRequest {
        val resp = client.get("api/notifications")
        ensureSuccess(resp)
        parseListFromJson(
            resp.bodyAsText(),
            Notification.serializer(),
            listOf("notifications", "data")
        )
    }

    suspend fun getUnreadNotificationCount(): Result<Int> = makeRequest {
        val resp = client.get("api/notifications/unread-count")
        ensureSuccess(resp)
        val root = jsonParser.parseToJsonElement(resp.bodyAsText()) as? JsonObject
        root?.get("unreadCount")?.jsonPrimitive?.intOrNull
            ?: root?.get("unread_count")?.jsonPrimitive?.intOrNull
            ?: root?.get("count")?.jsonPrimitive?.intOrNull
            ?: 0
    }

    suspend fun markNotificationRead(notificationId: Int): Result<GenericResponse> = makeRequest {
        val resp = client.put("api/notifications/$notificationId/read")
        ensureSuccess(resp)
        parseGenericResponse(resp.bodyAsText())
    }

    // ============================
    // PARSING & ERROR HELPERS
    // ============================

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.value !in 200..299) {
            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            val extractedMessage = runCatching {
                val obj = jsonParser.parseToJsonElement(raw) as? JsonObject
                obj?.get("message")?.jsonPrimitive?.contentOrNull
                    ?: obj?.get("error")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            throw Exception(extractedMessage ?: "Request failed (${response.status.value})")
        }
    }

    private fun parseGenericResponse(jsonString: String): GenericResponse {
        if (jsonString.isBlank()) return GenericResponse(true, "Success")
        return runCatching {
            val res = jsonParser.decodeFromString(GenericResponse.serializer(), jsonString)
            if (!res.success && res.message.isNotBlank()) throw Exception(res.message)
            res
        }.getOrElse { err ->
            if (err.message != null && !jsonString.trimStart().startsWith("{")) {
                GenericResponse(true, jsonString)
            } else if (err is kotlinx.serialization.SerializationException) {
                GenericResponse(true, "Success")
            } else {
                throw err
            }
        }
    }

    private fun parseSingleJob(jsonString: String): Job {
        val root = jsonParser.parseToJsonElement(jsonString)
        val target = (root as? JsonObject)?.get("job")
            ?: (root as? JsonObject)?.get("data")
            ?: root
        return jsonParser.decodeFromJsonElement(Job.serializer(), target)
    }

    private fun parseJobList(jsonString: String): List<Job> {
        return parseListFromJson(jsonString, Job.serializer(), listOf("jobs", "applications", "data"))
    }

    private fun <T> parseListFromJson(
        jsonString: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        candidateKeys: List<String>
    ): List<T> {
        if (jsonString.isBlank()) return emptyList()
        val root = jsonParser.parseToJsonElement(jsonString)
        val array: JsonArray? = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                candidateKeys.firstNotNullOfOrNull { key -> root[key] as? JsonArray }
                    ?: (root["data"] as? JsonObject)?.let { dataObj ->
                        candidateKeys.firstNotNullOfOrNull { key -> dataObj[key] as? JsonArray }
                    }
                    ?: root.values.firstOrNull { it is JsonArray } as? JsonArray
            }
            else -> null
        }
        return array?.let {
            jsonParser.decodeFromJsonElement(ListSerializer(serializer), it)
        } ?: emptyList()
    }

    private fun parseCatalogData(jsonString: String): CatalogData {
        val defaults = EldoretCatalogDefaults.defaultCatalog()
        if (jsonString.isBlank()) return defaults
        val root = jsonParser.parseToJsonElement(jsonString)
        val obj = ((root as? JsonObject)?.get("catalog") as? JsonObject)
            ?: ((root as? JsonObject)?.get("data") as? JsonObject)
            ?: (root as? JsonObject)
            ?: return defaults

        val categories = (obj["categories"] as? JsonArray)?.mapNotNull { el ->
            when (el) {
                is JsonPrimitive -> el.contentOrNull
                is JsonObject -> el["name"]?.jsonPrimitive?.contentOrNull
                    ?: el["title"]?.jsonPrimitive?.contentOrNull
                else -> null
            }
        }?.takeIf { it.isNotEmpty() } ?: defaults.categories

        val services = (obj["services"] as? JsonArray)?.let {
            runCatching {
                jsonParser.decodeFromJsonElement(ListSerializer(CatalogServiceItem.serializer()), it)
            }.getOrNull()
        }?.takeIf { it.isNotEmpty() } ?: defaults.services

        val addons = (obj["addons"] as? JsonArray)?.let {
            runCatching {
                jsonParser.decodeFromJsonElement(ListSerializer(CatalogAddonItem.serializer()), it)
            }.getOrNull()
        }?.takeIf { it.isNotEmpty() } ?: defaults.addons

        val detergentToggle = (obj["detergentToggle"] ?: obj["detergent_toggle"])?.let {
            runCatching {
                jsonParser.decodeFromJsonElement(DetergentToggleConfig.serializer(), it)
            }.getOrNull()
        } ?: defaults.detergentToggle

        val estates = (obj["estates"] as? JsonArray)?.mapNotNull { el ->
            when (el) {
                is JsonPrimitive -> el.contentOrNull?.let { name ->
                    defaults.estates.find { it.name.equals(name, ignoreCase = true) }
                        ?: EldoretEstate(name = name)
                }
                is JsonObject -> runCatching {
                    jsonParser.decodeFromJsonElement(EldoretEstate.serializer(), el)
                }.getOrNull()
                else -> null
            }
        }?.takeIf { it.isNotEmpty() } ?: defaults.estates

        return CatalogData(
            categories = categories,
            services = services,
            addons = addons,
            detergentToggle = detergentToggle,
            estates = estates
        )
    }

    private suspend inline fun <T> makeRequest(call: () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
