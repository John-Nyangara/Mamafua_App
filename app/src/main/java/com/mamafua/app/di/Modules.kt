package com.mamafua.app.di

import androidx.room.Room
import com.mamafua.app.data.local.AppDatabase
import com.mamafua.app.data.local.TokenManager
import com.mamafua.app.data.remote.api.*
import com.mamafua.app.data.remote.createHttpClient
import com.mamafua.app.data.repository.*
import com.mamafua.app.domain.repository.*
import com.mamafua.app.presentation.auth.viewmodel.LoginViewModel
import com.mamafua.app.presentation.auth.viewmodel.RegisterViewModel
import com.mamafua.app.presentation.main.viewmodel.MainViewModel
import com.mamafua.app.presentation.client.ClientHomeViewModel
import com.mamafua.app.presentation.worker.WorkerHomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient(get()) }
    single { AuthApiService(get()) }
    single { UserApiService(get()) }
    single { WorkerApiService(get()) }
    single { JobApiService(get()) }
    single { PaymentApiService(get()) }
    single { NotificationApiService(get()) }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "mamafua_db"
        ).build()
    }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().jobDao() }
    single { get<AppDatabase>().notificationDao() }
    single { TokenManager(androidContext()) }
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
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
    viewModel { WorkerHomeViewModel(get()) }
}

val appModules = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
