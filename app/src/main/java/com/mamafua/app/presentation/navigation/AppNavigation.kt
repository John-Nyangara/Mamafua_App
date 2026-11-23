package com.mamafua.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mamafua.app.presentation.admin.AdminMainScreen
import com.mamafua.app.presentation.auth.screen.ForgotPasswordScreen
import com.mamafua.app.presentation.auth.screen.LoginScreen
import com.mamafua.app.presentation.auth.screen.RegisterScreen
import com.mamafua.app.presentation.auth.screen.SplashScreen
import com.mamafua.app.presentation.client.ClientMainScreen
import com.mamafua.app.presentation.main.viewmodel.MainViewModel
import com.mamafua.app.presentation.worker.WorkerMainScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val authState = mainViewModel.authState.collectAsState().value

    val routeBackToSplash: () -> Unit = {
        navController.navigate("splash") {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { it / 4 },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { it / 4 },
                animationSpec = tween(300)
            )
        }
    ) {
        composable(
            "splash",
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            SplashScreen(
                authState = authState,
                onSplashComplete = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable(
            "login",
            enterTransition = {
                fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(400)
                )
            }
        ) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
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

        composable(
            "client_main",
            enterTransition = {
                fadeIn(tween(400)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(400)
                )
            }
        ) {
            ClientMainScreen(
                onRoleSwitched = routeBackToSplash,
                onLogout = routeBackToSplash
            )
        }

        composable(
            "worker_main",
            enterTransition = {
                fadeIn(tween(400)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(400)
                )
            }
        ) {
            WorkerMainScreen(
                onRoleSwitched = routeBackToSplash,
                onLogout = routeBackToSplash
            )
        }

        composable("admin_main") {
            AdminMainScreen(
                onRoleSwitched = routeBackToSplash,
                onLogout = routeBackToSplash
            )
        }
    }
}
