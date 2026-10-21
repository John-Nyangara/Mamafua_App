package com.mamafua.app.di

import androidx.room.Room
import com.mamafua.app.data.local.AppDatabase
import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.data.remote.MamaFuaApiClient
import com.mamafua.app.data.remote.createHttpClient
import com.mamafua.app.data.repository.*
import com.mamafua.app.domain.repository.*
import com.mamafua.app.presentation.auth.viewmodel.LoginViewModel
import com.mamafua.app.presentation.auth.viewmodel.RegisterViewModel
import com.mamafua.app.presentation.main.viewmodel.MainViewModel
import com.mamafua.app.presentation.client.ClientHomeViewModel
import com.mamafua.app.presentation.client.ClientMyJobsViewModel
import com.mamafua.app.presentation.client.PostJobViewModel
import com.mamafua.app.presentation.client.ClientProfileViewModel
import com.mamafua.app.presentation.client.ClientJobApplicantsViewModel
import com.mamafua.app.presentation.worker.WorkerHomeViewModel
import com.mamafua.app.presentation.worker.WorkerMyJobsViewModel
import com.mamafua.app.presentation.worker.WorkerProfileViewModel
import com.mamafua.app.presentation.main.NotificationsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient(get()) }
    single { MamaFuaApiClient(get()) }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mamafua_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().jobDao() }
    single { get<AppDatabase>().notificationDao() }
    single { TokenManager(androidContext()) }
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<JobRepository> { JobRepositoryImpl(get(), get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
}

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { ClientHomeViewModel(get()) }
    viewModel { ClientMyJobsViewModel(get(), get(), get()) }
    viewModel { PostJobViewModel(get()) }
    viewModel { ClientProfileViewModel(get(), get()) }
    viewModel { (jobId: Int) -> ClientJobApplicantsViewModel(jobId, get()) }
    viewModel { WorkerHomeViewModel(get(), get()) }
    viewModel { WorkerMyJobsViewModel(get()) }
    viewModel { WorkerProfileViewModel(get(), get()) }
    viewModel { NotificationsViewModel(get()) }
}

val appModules = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
