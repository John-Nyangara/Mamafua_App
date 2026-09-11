package com.mamafua.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mamafua.app.presentation.auth.screen.ForgotPasswordScreen
import com.mamafua.app.presentation.auth.screen.LoginScreen
import com.mamafua.app.presentation.auth.screen.RegisterScreen
import com.mamafua.app.presentation.auth.screen.SplashScreen
import com.mamafua.app.presentation.client.ClientHomeScreen
import com.mamafua.app.presentation.job.JobDetailsScreen
import com.mamafua.app.presentation.main.viewmodel.AuthState
import com.mamafua.app.presentation.main.viewmodel.MainViewModel
import com.mamafua.app.presentation.worker.WorkerHomeScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val authState = mainViewModel.authState.collectAsState().value

    NavHost(navController = navController, startDestination = "splash", modifier = modifier) {
        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    when (authState) {
                        is AuthState.AuthenticatedClient -> navController.navigate("client_home") { popUpTo("splash") { inclusive = true } }
                        is AuthState.AuthenticatedWorker -> navController.navigate("worker_home") { popUpTo("splash") { inclusive = true } }
                        is AuthState.Unauthenticated -> navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                        is AuthState.Loading -> { /* Wait for state to resolve */ }
                    }
                }
            )
        }
        
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = { 
                    navController.navigate("splash") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("splash") { popUpTo("register") { inclusive = true } }
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("client_home") {
            ClientHomeScreen()
        }
        composable("worker_home") {
            WorkerHomeScreen(
                onNavigateToJobDetails = { jobId -> navController.navigate("job_details/$jobId") }
            )
        }
        composable(
            route = "job_details/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.IntType })
        ) {
            val isClient = authState is AuthState.AuthenticatedClient
            JobDetailsScreen(
                isClient = isClient,
                onNavigateToPayment = { jobId -> navController.navigate("payment/$jobId") }
            )
        }
    }
}
