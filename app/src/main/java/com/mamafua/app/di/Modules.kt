package com.mamafua.app.di

import androidx.room.Room
import com.mamafua.app.data.local.AppDatabase
import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.data.remote.MamaFuaApiClient
import com.mamafua.app.data.repository.AuthRepositoryImpl
import com.mamafua.app.data.repository.CatalogRepositoryImpl
import com.mamafua.app.data.repository.JobRepositoryImpl
import com.mamafua.app.data.repository.NotificationRepositoryImpl
import com.mamafua.app.data.repository.PaymentRepositoryImpl
import com.mamafua.app.data.repository.UserRepositoryImpl
import com.mamafua.app.data.repository.WorkerRepositoryImpl
import com.mamafua.app.domain.repository.AuthRepository
import com.mamafua.app.domain.repository.CatalogRepository
import com.mamafua.app.domain.repository.JobRepository
import com.mamafua.app.domain.repository.NotificationRepository
import com.mamafua.app.domain.repository.PaymentRepository
import com.mamafua.app.domain.repository.UserRepository
import com.mamafua.app.domain.repository.WorkerRepository
import com.mamafua.app.presentation.admin.AdminPortalViewModel
import com.mamafua.app.presentation.auth.screen.ForgotPasswordViewModel
import com.mamafua.app.presentation.auth.viewmodel.LoginViewModel
import com.mamafua.app.presentation.auth.viewmodel.RegisterViewModel
import com.mamafua.app.presentation.client.ClientHomeViewModel
import com.mamafua.app.presentation.client.ClientJobApplicantsViewModel
import com.mamafua.app.presentation.client.ClientJobDetailViewModel
import com.mamafua.app.presentation.client.ClientMyJobsViewModel
import com.mamafua.app.presentation.client.ClientProfileViewModel
import com.mamafua.app.presentation.client.PostJobViewModel
import com.mamafua.app.presentation.main.NotificationsViewModel
import com.mamafua.app.presentation.main.viewmodel.MainViewModel
import com.mamafua.app.presentation.worker.EarningsViewModel
import com.mamafua.app.presentation.worker.WorkerHomeViewModel
import com.mamafua.app.presentation.worker.WorkerJobDetailViewModel
import com.mamafua.app.presentation.worker.WorkerMyJobsViewModel
import com.mamafua.app.presentation.worker.WorkerProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { TokenManager(androidContext()) }
    single { MamaFuaApiClient(get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mamafua_db"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().jobDao() }
    single { get<AppDatabase>().notificationDao() }
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get()) }
    single<WorkerRepository> { WorkerRepositoryImpl(get(), get()) }
    single<JobRepository> { JobRepositoryImpl(get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { ClientHomeViewModel(get(), get(), get(), get()) }
    viewModel { PostJobViewModel(get(), get(), get()) }
    viewModel { ClientMyJobsViewModel(get(), get(), get()) }
    viewModel { (jobId: Int) -> ClientJobApplicantsViewModel(jobId, get()) }
    viewModel { (jobId: Int) -> ClientJobDetailViewModel(jobId, get(), get(), get()) }
    viewModel { ClientProfileViewModel(get(), get()) }
    viewModel { WorkerHomeViewModel(get(), get(), get()) }
    viewModel { WorkerMyJobsViewModel(get()) }
    viewModel { (jobId: Int) -> WorkerJobDetailViewModel(jobId, get(), get()) }
    viewModel { EarningsViewModel(get(), get()) }
    viewModel { WorkerProfileViewModel(get(), get(), get()) }
    viewModel { NotificationsViewModel(get()) }
    viewModel { AdminPortalViewModel(get(), get(), get(), get()) }
}

val appModules = listOf(appModule, repositoryModule, viewModelModule)
