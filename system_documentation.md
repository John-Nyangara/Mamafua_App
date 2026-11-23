# Mama Fua Android App - System Documentation

## 1. System Overview
The **Mama Fua** app is an on-demand service platform connecting clients with domestic workers (Mama Fuas). The application provides a dual-sided marketplace with dedicated experiences for both Clients and Workers, alongside an Admin portal for platform management.

## 2. Architecture
The application follows the **MVVM (Model-View-ViewModel)** design pattern coupled with **Clean Architecture** principles to separate concerns into three distinct layers:
*   **Domain Layer (`domain/`)**: Contains the core business logic, including data models (`User`, `Job`, `WorkerProfile`, `Review`, `Payment`, etc.) and Repository interfaces.
*   **Data Layer (`data/`)**: Handles data fetching and persistence. It acts as the Single Source of Truth (SSOT).
    *   **Remote**: Uses **Ktor Client** (with the OkHttp engine) to communicate with the REST API backend (hosted at `https://mama-fua-backend.onrender.com/`).
    *   **Local**: Uses **Room Database** for offline caching and local persistence (`AppDatabase`, `UserDao`, `JobDao`, `NotificationDao`).
    *   **Repositories**: Implementations that orchestrate data between Ktor and Room (e.g., `JobRepositoryImpl`, `AuthRepositoryImpl`).
*   **Presentation Layer (`presentation/`)**: Contains the UI and state management.
    *   **UI**: Built entirely using **Jetpack Compose**.
    *   **State**: Managed via ViewModels exposing `StateFlow` to the Compose UI.

## 3. Technology Stack
*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose (Material 3)
*   **Dependency Injection**: Koin
*   **Networking**: Ktor Client (OkHttp engine) + Kotlinx Serialization
*   **Local Database**: Room
*   **Asynchronous Programming**: Kotlin Coroutines & Flow
*   **Navigation**: Jetpack Navigation Compose
*   **Image Loading**: Coil
*   **Location Services**: Google Play Services Location & Maps Compose

## 4. Comprehensive Feature Breakdown

The Mama Fua platform is a dual-sided marketplace tailored to two primary user roles: **Clients** (users seeking domestic services) and **Workers / Mama Fuas** (users providing services), governed by an **Admin** system.

### 4.1. Authentication & User Onboarding
*   **Role-Based Registration**: Users choose their account type (Client or Worker) during signup, which dynamically dictates the home dashboard they see upon logging in.
*   **Secure Authentication**: Standard email/password login integrated with backend JWT verification.
*   **Session Persistence**: Seamless auto-login handling using encrypted tokens stored via `TokenManager`.
*   **Password Recovery**: "Forgot Password" capabilities allowing users to securely reset their credentials.

### 4.2. Client-Specific Features
*   **Worker Discovery (Maps/List)**: An interactive home screen utilizing Google Maps and Location Services to display nearby, available Mama Fuas based on the client's current location and a defined search radius.
*   **Job Posting**: A dedicated form to create detailed gig requests. Clients can specify the job title, detailed descriptions, expected budget, and physical location.
*   **Applicant Management**: Clients can view the list of workers who applied for their open jobs, check the worker's profile (including bio, rates, and past ratings), and officially "Accept" a candidate.
*   **Job Tracking & Status Tabs**: A customized dashboard to track jobs categorized by their current lifecycle state:
    *   *Open*: Jobs awaiting workers.
    *   *In Progress*: Jobs currently being executed.
    *   *Completed*: Finished jobs with historical records.
*   **Secure Escrow Payments (M-Pesa)**: Deep integration with M-Pesa STK Push. Clients pay into a secure escrow system when a worker is accepted. Funds are securely held and automatically released to the worker only upon confirmed job completion.

### 4.3. Worker (Mama Fua) Specific Features
*   **Proximity-Based Job Feed**: A personalized feed showing open jobs within the worker's configured service radius, giving them quick access to local opportunities.
*   **Frictionless Job Applications**: Workers can view detailed requirements for available jobs and apply with a single tap.
*   **Active Job Execution**: Workers can manage their assigned jobs, officially mark a job as "Started," and trigger a "Request Completion" prompt when the task is done.
*   **Dynamic Profile Management**:
    *   **Availability Toggle**: Workers can switch their visibility on/off, deciding if they want to appear on the Client's map.
    *   **Preferences**: Set a base hourly rate, maximum service radius in kilometers, and a descriptive bio to attract clients.
*   **Earnings & Wallet Dashboard**: A financial tracker where workers can monitor ledger payouts, view funds currently pending in escrow, and track lifetime earnings.

### 4.4. Shared Platform Features
*   **Real-Time Job Details**: A shared interface (adapting to the user's role) detailing active job statuses, acting as the bridge for handshakes (e.g., Worker requests completion -> Client confirms completion).
*   **Push Notifications System**: In-app notifications alerting users to critical workflow events (e.g., "A worker applied to your job", "Payment received", "Job marked complete"). Backed by a local Room database for historical viewing.
*   **Mutual Rating & Review System**: Post-job feedback loop. Once a job concludes, both parties can leave a 1-to-5 star rating and comments, directly impacting the public aggregate rating of the profiles.
*   **Dispute Resolution Mechanism**: If a job goes wrong, either party can raise a dispute via a dedicated form. This immediately flags the job and freezes the M-Pesa escrow funds until admin mediation resolves the issue.

### 4.5. Admin Features
*   **Platform Oversight**: Internal screens powered by the `AdminPortalViewModel` to moderate users, oversee job lifecycles, and manually resolve escalated disputes/release locked funds.

## 5. Dependency Injection Modules (`di/`)
Koin is used to wire up the application dependencies across the following modules:
1.  **`appModule`**: Provides `TokenManager`, `MamaFuaApiClient`, and Room Database/DAOs.
2.  **`repositoryModule`**: Binds Domain repository interfaces to their Data layer implementations.
3.  **`viewModelModule`**: Injects necessary use cases and repositories into all ViewModels.

## 6. Build and Deployment
*   **Minimum SDK**: 24 (Android 7.0)
*   **Target SDK**: 34 (Android 14)
*   **Build System**: Gradle (Kotlin DSL) with Version Catalogs (`libs.versions.toml`).
