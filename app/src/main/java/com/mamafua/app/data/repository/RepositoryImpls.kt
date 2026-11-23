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
    private val jobDao: JobDao,
    private val notificationDao: NotificationDao
) : AuthRepository {
    override suspend fun login(email: String, password: String): User {
        val response = apiClient.login(LoginRequest(email.trim(), password)).getOrThrow()
        if (!response.success || response.token == null) {
            throw Exception(response.message.ifBlank { "Invalid credentials" })
        }
        tokenManager.saveToken(response.token)
        jobDao.clearJobs()
        val user = apiClient.getCurrentUser().getOrThrow()
        userDao.insertUser(user.toEntity())
        return user
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): User {
        val response = apiClient.register(
            RegisterRequest(name.trim(), email.trim(), phone.trim(), password, role)
        ).getOrThrow()
        if (!response.success) {
            throw Exception(response.message.ifBlank { "Registration failed" })
        }
        // If backend returned token directly, save it; otherwise auto-login immediately
        return if (!response.token.isNullOrBlank()) {
            tokenManager.saveToken(response.token)
            val user = runCatching { apiClient.getCurrentUser().getOrThrow() }
                .getOrDefault(User(id = response.userId ?: 0, name = name, email = email, phone = phone, role = role))
            userDao.insertUser(user.toEntity())
            user
        } else {
            runCatching { login(email, password) }.getOrDefault(
                User(id = response.userId ?: 0, name = name, email = email, phone = phone, role = role)
            )
        }
    }

    override suspend fun forgotPassword(email: String): Resource<String> {
        return try {
            val res = apiClient.forgotPassword(ForgotPasswordRequest(email.trim())).getOrThrow()
            Resource.Success(res.message.ifBlank { "Password reset instructions sent to $email" })
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send password reset token")
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Resource<String> {
        return try {
            val res = apiClient.resetPassword(ResetPasswordRequest(token.trim(), newPassword)).getOrThrow()
            Resource.Success(res.message.ifBlank { "Password reset successfully! You can now sign in." })
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to reset password")
        }
    }

    override suspend fun logout() {
        runCatching { apiClient.logout() }
        tokenManager.clearToken()
        userDao.deleteUser()
        jobDao.clearJobs()
        notificationDao.clearNotifications()
    }
}

class CatalogRepositoryImpl(
    private val apiClient: MamaFuaApiClient
) : CatalogRepository {
    private var cachedCatalog: CatalogData? = null

    override suspend fun getCatalog(forceRefresh: Boolean): CatalogData {
        if (!forceRefresh && cachedCatalog != null) {
            return cachedCatalog!!
        }
        val fetched = apiClient.getCatalog().getOrElse {
            cachedCatalog ?: EldoretCatalogDefaults.defaultCatalog()
        }
        cachedCatalog = fetched
        return fetched
    }

    override suspend fun estimatePrice(
        items: List<SelectedQuantityItem>,
        addons: List<SelectedQuantityItem>,
        workerBringsDetergents: Boolean
    ): PriceEstimate {
        val catalog = cachedCatalog ?: EldoretCatalogDefaults.defaultCatalog()
        val activeItems = items.filter { it.quantity > 0 }
        val activeAddons = addons.filter { it.quantity > 0 }
        if (activeItems.isEmpty() && activeAddons.isEmpty()) {
            return PriceEstimate(workerBringsDetergents = workerBringsDetergents)
        }
        return apiClient.estimatePrice(
            EstimatePriceRequest(
                items = activeItems,
                addons = activeAddons,
                workerBringsDetergents = workerBringsDetergents
            )
        ).getOrElse {
            EldoretCatalogDefaults.computeLocalEstimate(
                catalog = catalog,
                items = activeItems,
                addons = activeAddons,
                workerBringsDetergents = workerBringsDetergents
            )
        }
    }
}

class UserRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val userDao: UserDao,
    private val tokenManager: TokenManager,
    private val jobDao: JobDao
) : UserRepository {
    override suspend fun getProfile(): User {
        val user = apiClient.getCurrentUser().getOrThrow()
        userDao.insertUser(user.toEntity())
        return user
    }

    override fun getUserFlow(): Flow<User?> {
        return userDao.getUser().map { it?.toDomain() }
    }

    override suspend fun updateProfile(request: UpdateUserRequest): Resource<User> {
        return try {
            apiClient.updateCurrentUser(request).getOrThrow()
            val updated = getProfile()
            Resource.Success(updated)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    override suspend fun switchRole(role: String): Resource<User> {
        return try {
            val maybeNewToken = apiClient.switchRole(role).getOrThrow()
            if (!maybeNewToken.isNullOrBlank()) {
                tokenManager.saveToken(maybeNewToken)
            }
            jobDao.clearJobs()
            val updated = runCatching { getProfile() }.getOrElse {
                val existing = userDao.getUser()
                User(role = role)
            }
            val finalUser = if (updated.role.equals(role, ignoreCase = true)) {
                updated
            } else {
                val patched = updated.copy(role = role)
                userDao.insertUser(patched.toEntity())
                patched
            }
            Resource.Success(finalUser)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to switch role")
        }
    }

    override suspend fun getFavorites(): Resource<List<WorkerProfile>> {
        return try {
            Resource.Success(apiClient.getFavoriteWorkers().getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load favorite Mama Fuas")
        }
    }

    override suspend fun addFavorite(workerId: Int, note: String?): Resource<Unit> {
        return try {
            apiClient.addFavoriteWorker(workerId, note).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add to favorites")
        }
    }

    override suspend fun removeFavorite(workerId: Int): Resource<Unit> {
        return try {
            apiClient.removeFavoriteWorker(workerId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove from favorites")
        }
    }

    override suspend fun reportUser(userId: Int, reason: String, description: String?): Resource<Unit> {
        return try {
            apiClient.reportUser(userId, reason, description).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to submit report")
        }
    }

    override suspend fun blockUser(userId: Int): Resource<Unit> {
        return try {
            apiClient.blockUser(userId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to block user")
        }
    }

    override suspend fun unblockUser(userId: Int): Resource<Unit> {
        return try {
            apiClient.unblockUser(userId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unblock user")
        }
    }

    override suspend fun getBlockedUsers(): Resource<List<BlockedUser>> {
        return try {
            Resource.Success(apiClient.getBlockedUsers().getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load blocked users")
        }
    }
}

class WorkerRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val tokenManager: TokenManager
) : WorkerRepository {
    override suspend fun getWorkers(
        estate: String?,
        service: String?,
        badge: String?,
        availabilityStatus: String?,
        lat: Double?,
        lng: Double?,
        radiusKm: Double?
    ): Resource<List<WorkerProfile>> {
        return try {
            val workers = apiClient.getWorkers(
                estate = estate,
                service = service,
                badge = badge,
                availabilityStatus = availabilityStatus,
                lat = lat,
                lng = lng,
                radiusKm = radiusKm
            ).getOrThrow()
            Resource.Success(workers)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load Mama Fuas")
        }
    }

    override suspend fun getWorkerById(workerId: Int): Resource<WorkerProfile> {
        return try {
            Resource.Success(apiClient.getWorkerById(workerId).getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load worker profile")
        }
    }

    override suspend fun getWorkerReviews(workerId: Int): Resource<WorkerReviewsSummary> {
        return try {
            Resource.Success(apiClient.getWorkerReviews(workerId).getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load worker reviews")
        }
    }

    override suspend fun createOrUpdateWorkerProfile(
        isCreate: Boolean,
        request: CreateOrUpdateWorkerRequest
    ): Resource<Unit> {
        return try {
            if (isCreate) {
                runCatching { apiClient.createWorkerProfile(request).getOrThrow() }
                    .recoverCatching { apiClient.updateWorkerProfile(request).getOrThrow() }
                    .getOrThrow()
            } else {
                runCatching { apiClient.updateWorkerProfile(request).getOrThrow() }
                    .recoverCatching { apiClient.createWorkerProfile(request).getOrThrow() }
                    .getOrThrow()
            }
            request.availabilityStatus?.let { tokenManager.saveAvailabilityStatus(it) }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save worker profile")
        }
    }

    override suspend fun toggleAvailability(status: String): Resource<Unit> {
        return try {
            apiClient.toggleWorkerAvailability(status).getOrThrow()
            tokenManager.saveAvailabilityStatus(status)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update availability status")
        }
    }

    override suspend fun getJobRadar(): Resource<List<RadarOffer>> {
        return try {
            Resource.Success(apiClient.getJobRadar().getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load Job Radar")
        }
    }

    override suspend fun respondToRadarOffer(offerId: Int, action: String): Resource<Unit> {
        return try {
            apiClient.respondToRadarOffer(offerId, action).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to respond to offer")
        }
    }

    override suspend fun getEarnings(): Resource<WorkerEarningsData> {
        return try {
            Resource.Success(apiClient.getWorkerEarnings().getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load earnings wallet")
        }
    }

    override suspend fun withdrawEarnings(phoneNumber: String): Resource<String> {
        return try {
            val res = apiClient.withdrawEarnings(phoneNumber).getOrThrow()
            Resource.Success(res.message.ifBlank { "Instant M-Pesa withdrawal initiated!" })
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to withdraw earnings")
        }
    }

    override suspend fun getWorkerAvailability(workerId: Int): Resource<List<AvailabilitySlot>> {
        return try {
            Resource.Success(apiClient.getWorkerAvailability(workerId).getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load availability schedule")
        }
    }

    override suspend fun getMyAvailability(): Resource<List<AvailabilitySlot>> {
        return try {
            Resource.Success(apiClient.getMyAvailability().getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load schedule")
        }
    }

    override suspend fun addAvailabilitySlot(request: CreateAvailabilitySlotRequest): Resource<Unit> {
        return try {
            apiClient.addAvailabilitySlot(request).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add availability slot")
        }
    }

    override suspend fun deleteAvailabilitySlot(slotId: Int): Resource<Unit> {
        return try {
            apiClient.deleteAvailabilitySlot(slotId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete slot")
        }
    }
}

class JobRepositoryImpl(
    private val apiClient: MamaFuaApiClient,
    private val jobDao: JobDao
) : JobRepository {
    override suspend fun fetchJobs(
        status: String?,
        estate: String?,
        category: String?,
        search: String?
    ): Resource<List<Job>> {
        return try {
            val jobs = apiClient.getOpenJobs(
                estate = estate,
                category = category,
                search = search
            ).getOrThrow()
            jobDao.insertJobs(jobs.map { it.toEntity() })
            Resource.Success(jobs)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load open jobs")
        }
    }

    override suspend fun fetchMyPostedJobs(): Resource<List<Job>> {
        return try {
            val response = apiClient.getMyPostedJobs().getOrThrow()
            jobDao.clearJobs()
            jobDao.insertJobs(response.map { it.toEntity() })
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch posted jobs")
        }
    }

    override suspend fun fetchMyAssignedJobs(): Resource<List<Job>> {
        return try {
            val response = apiClient.getMyAssignedJobs().getOrThrow()
            jobDao.insertJobs(response.map { it.toEntity() })
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch assigned jobs")
        }
    }

    override suspend fun fetchMyApplications(): Resource<List<Job>> {
        return try {
            val response = apiClient.getMyApplications().getOrThrow()
            jobDao.insertJobs(response.map { it.toEntity() })
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch applications")
        }
    }

    override fun getActiveJobs(): Flow<List<Job>> {
        return jobDao.getAllJobs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getJobById(jobId: Int): Resource<Job> {
        return try {
            val job = apiClient.getJobById(jobId).getOrThrow()
            jobDao.insertJob(job.toEntity())
            Resource.Success(job)
        } catch (e: Exception) {
            val cached = jobDao.getJobById(jobId)?.toDomain()
            if (cached != null) Resource.Success(cached)
            else Resource.Error(e.message ?: "Failed to load job details")
        }
    }

    override suspend fun createJob(
        title: String,
        description: String,
        location: String,
        budget: Double
    ): Resource<Job> {
        return createCustomJob(
            CreateJobRequest(
                title = title,
                description = description,
                location = location,
                budget = budget,
                matchingMode = "marketplace"
            )
        )
    }

    override suspend fun createCustomJob(request: CreateJobRequest): Resource<Job> {
        return try {
            val created = apiClient.createJob(request).getOrThrow()
            if (created.id != 0) {
                jobDao.insertJob(created.toEntity())
            }
            Resource.Success(created)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create booking")
        }
    }

    override suspend fun rebookJob(
        workerId: Int?,
        isInstant: Boolean,
        scheduledAt: String?
    ): Resource<Job> {
        return try {
            val created = apiClient.rebookJob(
                RebookJobRequest(
                    workerId = workerId,
                    isInstant = isInstant,
                    scheduledAt = scheduledAt
                )
            ).getOrThrow()
            if (created.id != 0) {
                jobDao.insertJob(created.toEntity())
            }
            Resource.Success(created)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to rebook Mama Fua")
        }
    }

    override suspend fun updateOpenJob(jobId: Int, request: UpdateOpenJobRequest): Resource<Unit> {
        return try {
            apiClient.updateOpenJob(jobId, request).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update job")
        }
    }

    override suspend fun autoDispatchJob(jobId: Int, radiusKm: Double, maxBroadcast: Int): Resource<Unit> {
        return try {
            apiClient.autoDispatchJob(jobId, radiusKm, maxBroadcast).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to trigger auto-dispatch")
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

    override suspend fun withdrawApplication(jobId: Int): Resource<Unit> {
        return try {
            apiClient.withdrawApplication(jobId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to withdraw application")
        }
    }

    override suspend fun shortlistWorker(jobId: Int, workerId: Int): Resource<Unit> {
        return try {
            apiClient.shortlistApplicant(jobId, workerId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to shortlist applicant")
        }
    }

    override suspend fun acceptWorker(jobId: Int, workerId: Int): Resource<Unit> {
        return try {
            apiClient.acceptApplicant(jobId, JobWorkerRequest(workerId)).getOrThrow()
            jobDao.updateJobStatus(jobId, JobStatus.ASSIGNED.value)
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

    override suspend fun directAssignWorker(jobId: Int, workerId: Int): Resource<Unit> {
        return try {
            apiClient.directAssignWorker(jobId, workerId).getOrThrow()
            jobDao.updateJobStatus(jobId, JobStatus.ASSIGNED.value)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to assign worker")
        }
    }

    override suspend fun getJobProgress(jobId: Int): Resource<JobProgress> {
        return try {
            val progress = apiClient.getJobProgress(jobId).getOrThrow()
            jobDao.updateJobStatus(jobId, progress.status.value)
            Resource.Success(progress)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load job progress")
        }
    }

    override suspend fun verifyStartPin(jobId: Int, pin: String): Resource<Unit> {
        return try {
            apiClient.verifyStartPin(jobId, pin).getOrThrow()
            jobDao.updateJobStatus(jobId, JobStatus.IN_PROGRESS.value)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Invalid Start PIN")
        }
    }

    override suspend fun updateJobStatus(jobId: Int, status: JobStatus): Resource<Unit> {
        return try {
            apiClient.updateJobStatus(jobId, UpdateJobStatusRequest(status.value)).getOrThrow()
            jobDao.updateJobStatus(jobId, status.value)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update job status")
        }
    }

    override suspend fun createScopeAdjustment(
        jobId: Int,
        additionalAmount: Double,
        reason: String,
        extraItems: List<ScopeExtraItem>
    ): Resource<Unit> {
        return try {
            apiClient.createScopeAdjustment(
                jobId,
                CreateScopeAdjustmentRequest(
                    additionalAmount = additionalAmount,
                    reason = reason,
                    extraItems = extraItems
                )
            ).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to request scope adjustment")
        }
    }

    override suspend fun getScopeAdjustments(jobId: Int): Resource<List<ScopeAdjustment>> {
        return try {
            Resource.Success(apiClient.getScopeAdjustments(jobId).getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load scope adjustments")
        }
    }

    override suspend fun respondToScopeAdjustment(
        jobId: Int,
        adjustmentId: Int,
        action: String
    ): Resource<Unit> {
        return try {
            apiClient.respondToScopeAdjustment(jobId, adjustmentId, action).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to respond to scope adjustment")
        }
    }

    override suspend fun markJobCompleteByWorker(jobId: Int): Resource<Unit> {
        return try {
            apiClient.markJobCompleteByWorker(jobId).getOrThrow()
            jobDao.updateJobStatus(jobId, JobStatus.COMPLETION_REQUESTED.value)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark job complete")
        }
    }

    override suspend fun confirmCompletion(jobId: Int): Resource<Unit> {
        return try {
            apiClient.confirmCompletion(jobId).getOrThrow()
            jobDao.updateJobStatus(jobId, JobStatus.COMPLETED.value)
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
            jobDao.updateJobStatus(jobId, JobStatus.CANCELLED.value)
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

    override suspend fun getAllDisputes(): Resource<List<Dispute>> {
        return try {
            Resource.Success(apiClient.getAllDisputes().getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load disputes")
        }
    }

    override suspend fun resolveDispute(
        disputeId: Int,
        status: String,
        resolutionNotes: String?
    ): Resource<Unit> {
        return try {
            apiClient.resolveDispute(
                disputeId,
                ResolveDisputeRequest(status = status, resolutionNotes = resolutionNotes, action = status)
            ).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to resolve dispute")
        }
    }
}

class PaymentRepositoryImpl(
    private val apiClient: MamaFuaApiClient
) : PaymentRepository {
    override suspend fun initiateSTKPush(
        jobId: Int,
        phone: String,
        paymentType: String
    ): Resource<StkPushResponse> {
        return try {
            val res = apiClient.initiateStkPush(
                jobId,
                StkPushRequest(phoneNumber = phone.trim(), paymentType = paymentType)
            ).getOrThrow()
            Resource.Success(res)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to initiate M-Pesa STK Push")
        }
    }

    override suspend fun getJobPayments(jobId: Int): Resource<List<Payment>> {
        return try {
            Resource.Success(apiClient.getJobPayments(jobId).getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch payment records")
        }
    }

    override suspend fun checkPaymentStatus(paymentId: Int): Resource<Payment> {
        return try {
            Resource.Success(apiClient.queryPayment(paymentId).getOrThrow())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to query payment status")
        }
    }

    override suspend fun releaseJobEscrow(jobId: Int): Resource<Unit> {
        return try {
            apiClient.releaseJobEscrow(jobId).getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to release escrow")
        }
    }

    override suspend fun triggerAutoReleaseEscrow(): Resource<Unit> {
        return try {
            apiClient.triggerAutoReleaseEscrow().getOrThrow()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to run auto-release")
        }
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
        } catch (_: Exception) {
        }
    }

    override fun getNotificationsFlow(): Flow<List<Notification>> {
        return notificationDao.getNotifications().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getUnreadCount(): Int {
        return apiClient.getUnreadNotificationCount().getOrDefault(0)
    }

    override suspend fun markAsRead(notificationId: Int) {
        notificationDao.markAsRead(notificationId)
        runCatching { apiClient.markNotificationRead(notificationId) }
    }
}
