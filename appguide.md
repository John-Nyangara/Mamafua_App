Comprehensive Mama Fua Android App Architecture Guide
This document acts as an exhaustive blueprint for the Mama Fua Android application. It lists every class, model, screen, repository, and component you need to build in Android Studio using Jetpack Compose, Koin, Ktor, and Room.

1. Domain Models (domain/model/)
These are the core Kotlin data classes representing the business entities, mapped directly from the backend.

User: id, name, email, phone, role, profilePictureUrl, isVerified
WorkerProfile: userId, bio, hourlyRate, serviceRadiusKm, locationLat, locationLng, availabilityStatus, averageRating
Job: id, clientId, workerId, title, description, location, budget, status, paymentStatus, createdAt
Review: id, jobId, reviewerId, revieweeId, rating, comments, createdAt
Dispute: id, jobId, raisedBy, againstUser, reason, status, createdAt
Payment: id, jobId, clientId, workerId, amount, status, checkoutRequestId, mpesaReceiptNumber
Notification: id, userId, type, title, body, data, isRead, createdAt
2. Remote Data Layer (Ktor) (data/remote/)
2.1 Ktor Client Configuration
KtorClient: Configured with ContentNegotiation (Kotlinx Serialization), Logging, and a custom AuthPlugin that automatically adds the Bearer Token from local storage and handles 401 logouts.
2.2 API Service Interfaces
AuthApiService:
suspend fun login(req: LoginReq): AuthRes
suspend fun register(req: RegisterReq): AuthRes
UserApiService:
suspend fun getProfile(): User
suspend fun updateProfile(req: UpdateProfileReq): User
WorkerApiService:
suspend fun searchWorkers(lat, lng, radius): List<WorkerProfile>
JobApiService:
suspend fun createJob(req: CreateJobReq): Job
suspend fun getJobs(status: String?): List<Job>
suspend fun applyForJob(jobId: Int)
suspend fun acceptWorker(jobId: Int, workerId: Int)
suspend fun updateJobStatus(jobId: Int, status: String)
PaymentApiService:
suspend fun initiateSTKPush(jobId: Int, phone: String)
suspend fun checkPaymentStatus(checkoutRequestId: String): Payment
NotificationApiService:
suspend fun getNotifications(): List<Notification>
3. Local Data Layer (Room) (data/local/)
3.1 Entities (Room Tables)
@Entity UserEntity
@Entity JobEntity
@Entity NotificationEntity
3.2 DAOs (Data Access Objects)
UserDao: @Query("SELECT * FROM users LIMIT 1"), @Insert(onConflict = REPLACE)
JobDao: @Query("SELECT * FROM jobs"), @Insert, @Query("DELETE FROM jobs")
NotificationDao: @Query("SELECT * FROM notifications ORDER BY createdAt DESC"), @Insert
3.3 DataStore / Preferences
TokenManager: Uses EncryptedSharedPreferences or DataStore to securely store the jwt_token.
4. Repositories (data/repository/ implementing domain/repository/)
Repositories orchestrate data between Room (Single Source of Truth) and Ktor.

AuthRepositoryImpl: Calls Ktor, saves token to TokenManager, saves User to UserDao.
UserRepositoryImpl: Fetches profile from Ktor, updates UserDao. Returns Flow<User>.
JobRepositoryImpl:
Fetches list of jobs from Ktor, saves to JobDao.
Exposes fun getActiveJobs(): Flow<List<Job>> reading directly from JobDao.
PaymentRepositoryImpl: Handles STK push and polling logic.
NotificationRepositoryImpl: Synchronizes notifications from Ktor to NotificationDao.
5. Dependency Injection (Koin Modules) (di/)
NetworkModule.kt: Provides HttpClient, AuthApiService, JobApiService, etc.
DatabaseModule.kt: Provides Room AppDatabase, UserDao, JobDao, TokenManager.
RepositoryModule.kt: Binds interfaces to implementations (e.g., single<JobRepository> { JobRepositoryImpl(get(), get()) }).
ViewModelModule.kt: Provides all ViewModels (e.g., viewModel { HomeViewModel(get()) }).
6. Presentation Layer: ViewModels & State (presentation/)
Each ViewModel exposes a StateFlow representing the UI state.

6.1 Auth ViewModels
LoginViewModel: State = Email, Password, isLoading, errorMessage. Methods = onLoginClick().
RegisterViewModel: State = Name, Email, Phone, RoleSelection.
6.2 Main App ViewModels
MainViewModel: Observes TokenManager. Decides whether to show Auth Graph or Main Graph.
ClientHomeViewModel: Observes Flow<List<WorkerProfile>> nearby.
WorkerHomeViewModel: Observes Flow<List<Job>> available.
JobDetailsViewModel: Fetches specific job by ID. Handles actions (Apply, Accept, Request Completion).
PaymentViewModel: State = Idle, Loading, WaitingForMpesa, Success, Failed. Methods = initiatePayment(), pollStatus().
NotificationViewModel: Observes Flow<List<Notification>> from Room.
7. UI Screens (Jetpack Compose Composables)
Organized by Navigation Graphs.

7.1 Auth Nav Graph
SplashScreen: Checks if user is logged in.
LoginScreen: Email/Password fields, Login Button, "Forgot Password" link.
RegisterScreen: Multi-step or long form. Name, Phone, Email, Password, Role Toggle (Client/Worker).
ForgotPasswordScreen: Email input to request reset token.
7.2 Client Nav Graph (Bottom Nav)
ClientHomeScreen: Shows a map or list of nearby workers using LocationServices.
PostJobScreen: Form to enter Job Title, Description, Budget, and expected location.
ClientMyJobsScreen: Tabs for Open, In Progress, Completed.
ClientProfileScreen: Edit profile, view settings, Logout button.
7.3 Worker Nav Graph (Bottom Nav)
WorkerHomeScreen: Feed of nearby open jobs available for application.
WorkerMyJobsScreen: Jobs assigned to the worker. Tabs for Active, Completed.
WorkerProfileScreen: Manage base rate, bio, service radius, and toggle "Availability" status.
EarningsScreen: Displays ledger payouts and released escrow funds.
7.4 Shared Screens (Accessible by both)
JobDetailsScreen:
If Client: Shows applicants. Button to "Accept Worker", "Pay M-Pesa Escrow", "Confirm Completion".
If Worker: Shows Job details. Button to "Apply", "Start Job", "Request Completion".
PaymentStkScreen: Overlay/BottomSheet showing M-Pesa instructions while polling for the callback.
DisputeScreen: Form to raise an issue with a job.
ReviewScreen: BottomSheet to leave a 1-5 star rating and comment after job completion.
NotificationsScreen: List view of all system alerts. Tapping an item uses an Intent/DeepLink to navigate to JobDetailsScreen.
8. Android Core Integrations
8.1 Intents & Deep Links
Navigation Compose Deep Links: Map URIs like mamafua://jobs/{id} directly to the JobDetailsScreen composable.
Maps Intent: A button on the JobDetailsScreen to "Navigate to Location", triggering geo:lat,lng to open Google Maps.
Dialer Intent: A button to "Call Client/Worker" using tel:2547....
8.2 Firebase Cloud Messaging (FCM)
MamaFuaFirebaseMessagingService: Extends FirebaseMessagingService.
onNewToken: Sends the newly generated FCM device token to the Ktor backend.
onMessageReceived: Extracts title/body and builds an Android System Notification using NotificationManagerCompat. Attaches a PendingIntent mapped to the Navigation Compose Deep Links mentioned above.
8.3 Location Services
LocationTracker: An interface implemented using Google Play Services FusedLocationProviderClient. Used in WorkerHomeViewModel and ClientHomeViewModel to fetch the device's exact lat/lng coordinates to pass to the backend search APIs.
8.4 Internationalization (i18n)
res/values/strings.xml (Default: English)
res/values-sw/strings.xml (Swahili)
Compose usage: Text(text = stringResource(id = R.string.login_button))
9. Next Steps for Implementation in Android Studio
Create the Project: Empty Compose Activity.
Add Dependencies: Koin, Ktor, Room, Jetpack Navigation, Kotlinx Serialization, Google Maps/Location.
Setup DI & Network: Create the Ktor client and provide it via Koin. Test a basic login API call.
Build UI Scaffold: Setup the Navigation Host and Bottom Navigation bars for Client vs Worker.
Implement Repositories: Wire the Ktor client to Room DAOs to enable offline caching.
Build Screens: Implement the Compose screens one by one, binding them to their respective ViewModels.
Implement New Project Branch
Proceeded with
Implementation Plan
I have successfully completed Phase 5! The notification system is now fully integrated into the backend application.

Here is a summary of the accomplishments:

Core Service & Background Worker: Implemented the asynchronous store-and-forward pattern. Notifications are written to the database, queued, and then polled non-blockingly via an internal background worker.
Provider Implementation: Included an FCM (Firebase) adapter that is currently configured to run in Mock mode until real credentials are provided.
API Integration: Created GET /api/notifications, GET /api/notifications/unread-count, and PUT /api/notifications/:id/read endpoints, including validation.
Lifecycle Hooks: Weaved background push events directly into the job, payment, and dispute controllers.
Testing: Addressed a minor incompatibility with the Express 5 request query parser and fixed the test 