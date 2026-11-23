package com.mamafua.app.domain.repository

import com.mamafua.app.data.remote.CreateAvailabilitySlotRequest
import com.mamafua.app.data.remote.CreateJobRequest
import com.mamafua.app.data.remote.CreateOrUpdateWorkerRequest
import com.mamafua.app.data.remote.StkPushResponse
import com.mamafua.app.data.remote.UpdateOpenJobRequest
import com.mamafua.app.data.remote.UpdateUserRequest
import com.mamafua.app.domain.model.*
import com.mamafua.app.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): User
    suspend fun register(name: String, email: String, phone: String, password: String, role: String): User
    suspend fun forgotPassword(email: String): Resource<String>
    suspend fun resetPassword(token: String, newPassword: String): Resource<String>
    suspend fun logout()
}

interface CatalogRepository {
    suspend fun getCatalog(forceRefresh: Boolean = false): CatalogData
    suspend fun estimatePrice(
        items: List<SelectedQuantityItem>,
        addons: List<SelectedQuantityItem>,
        workerBringsDetergents: Boolean
    ): PriceEstimate
}

interface UserRepository {
    suspend fun getProfile(): User
    fun getUserFlow(): Flow<User?>
    suspend fun updateProfile(request: UpdateUserRequest): Resource<User>
    suspend fun switchRole(role: String): Resource<User>
    suspend fun getFavorites(): Resource<List<WorkerProfile>>
    suspend fun addFavorite(workerId: Int, note: String? = null): Resource<Unit>
    suspend fun removeFavorite(workerId: Int): Resource<Unit>
    suspend fun reportUser(userId: Int, reason: String, description: String? = null): Resource<Unit>
    suspend fun blockUser(userId: Int): Resource<Unit>
    suspend fun unblockUser(userId: Int): Resource<Unit>
    suspend fun getBlockedUsers(): Resource<List<BlockedUser>>
}

interface WorkerRepository {
    suspend fun getWorkers(
        estate: String? = null,
        service: String? = null,
        badge: String? = null,
        availabilityStatus: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Double? = null
    ): Resource<List<WorkerProfile>>
    suspend fun getWorkerById(workerId: Int): Resource<WorkerProfile>
    suspend fun getWorkerReviews(workerId: Int): Resource<WorkerReviewsSummary>
    suspend fun createOrUpdateWorkerProfile(isCreate: Boolean, request: CreateOrUpdateWorkerRequest): Resource<Unit>
    suspend fun toggleAvailability(status: String): Resource<Unit>
    suspend fun getJobRadar(): Resource<List<RadarOffer>>
    suspend fun respondToRadarOffer(offerId: Int, action: String): Resource<Unit>
    suspend fun getEarnings(): Resource<WorkerEarningsData>
    suspend fun withdrawEarnings(phoneNumber: String): Resource<String>
    suspend fun getWorkerAvailability(workerId: Int): Resource<List<AvailabilitySlot>>
    suspend fun getMyAvailability(): Resource<List<AvailabilitySlot>>
    suspend fun addAvailabilitySlot(request: CreateAvailabilitySlotRequest): Resource<Unit>
    suspend fun deleteAvailabilitySlot(slotId: Int): Resource<Unit>
}

interface JobRepository {
    suspend fun fetchJobs(status: String? = null, estate: String? = null, category: String? = null, search: String? = null): Resource<List<Job>>
    suspend fun fetchMyPostedJobs(): Resource<List<Job>>
    suspend fun fetchMyAssignedJobs(): Resource<List<Job>>
    suspend fun fetchMyApplications(): Resource<List<Job>>
    fun getActiveJobs(): Flow<List<Job>>
    suspend fun getJobById(jobId: Int): Resource<Job>
    suspend fun createJob(title: String, description: String, location: String, budget: Double): Resource<Job>
    suspend fun createCustomJob(request: CreateJobRequest): Resource<Job>
    suspend fun rebookJob(workerId: Int? = null, isInstant: Boolean = true, scheduledAt: String? = null): Resource<Job>
    suspend fun updateOpenJob(jobId: Int, request: UpdateOpenJobRequest): Resource<Unit>
    suspend fun autoDispatchJob(jobId: Int, radiusKm: Double = 5.0, maxBroadcast: Int = 3): Resource<Unit>
    suspend fun applyForJob(jobId: Int): Resource<Unit>
    suspend fun withdrawApplication(jobId: Int): Resource<Unit>
    suspend fun shortlistWorker(jobId: Int, workerId: Int): Resource<Unit>
    suspend fun acceptWorker(jobId: Int, workerId: Int): Resource<Unit>
    suspend fun rejectWorker(jobId: Int, workerId: Int): Resource<Unit>
    suspend fun directAssignWorker(jobId: Int, workerId: Int): Resource<Unit>
    suspend fun getJobProgress(jobId: Int): Resource<JobProgress>
    suspend fun verifyStartPin(jobId: Int, pin: String): Resource<Unit>
    suspend fun updateJobStatus(jobId: Int, status: JobStatus): Resource<Unit>
    suspend fun createScopeAdjustment(
        jobId: Int,
        additionalAmount: Double,
        reason: String,
        extraItems: List<ScopeExtraItem> = emptyList()
    ): Resource<Unit>
    suspend fun getScopeAdjustments(jobId: Int): Resource<List<ScopeAdjustment>>
    suspend fun respondToScopeAdjustment(jobId: Int, adjustmentId: Int, action: String): Resource<Unit>
    suspend fun markJobCompleteByWorker(jobId: Int): Resource<Unit>
    suspend fun confirmCompletion(jobId: Int): Resource<Unit>
    suspend fun cancelJob(jobId: Int): Resource<Unit>
    suspend fun getJobApplicants(jobId: Int): Resource<List<WorkerProfile>>
    suspend fun createReview(jobId: Int, rating: Int, comment: String?): Resource<Unit>
    suspend fun getJobReviews(jobId: Int): Resource<List<Review>>
    suspend fun raiseDispute(jobId: Int, reason: String, description: String): Resource<Unit>
    suspend fun getJobDisputes(jobId: Int): Resource<List<Dispute>>
    suspend fun getAllDisputes(): Resource<List<Dispute>>
    suspend fun resolveDispute(disputeId: Int, status: String, resolutionNotes: String?): Resource<Unit>
}

interface PaymentRepository {
    suspend fun initiateSTKPush(jobId: Int, phone: String, paymentType: String = "full"): Resource<StkPushResponse>
    suspend fun getJobPayments(jobId: Int): Resource<List<Payment>>
    suspend fun checkPaymentStatus(paymentId: Int): Resource<Payment>
    suspend fun releaseJobEscrow(jobId: Int): Resource<Unit>
    suspend fun triggerAutoReleaseEscrow(): Resource<Unit>
}

interface NotificationRepository {
    suspend fun fetchNotifications()
    fun getNotificationsFlow(): Flow<List<Notification>>
    suspend fun getUnreadCount(): Int
    suspend fun markAsRead(notificationId: Int)
}
