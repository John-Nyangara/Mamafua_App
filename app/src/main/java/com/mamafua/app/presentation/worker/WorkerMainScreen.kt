package com.mamafua.app.presentation.worker

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mamafua.app.presentation.main.NotificationsScreen
import com.mamafua.app.ui.theme.*

sealed class WorkerTab(val route: String, val title: String, val icon: ImageVector) {
    object JobFeed : WorkerTab("worker_home", "Radar", Icons.Default.Radar)
    object MyJobs : WorkerTab("worker_my_jobs", "My Jobs", Icons.Default.CheckCircle)
    object Earnings : WorkerTab("worker_earnings", "Wallet", Icons.Default.AccountBalanceWallet)
    object Profile : WorkerTab("worker_profile", "Profile", Icons.Default.Person)
}

@Composable
fun WorkerMainScreen(
    onRoleSwitched: () -> Unit = {},
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        WorkerTab.JobFeed,
        WorkerTab.MyJobs,
        WorkerTab.Earnings,
        WorkerTab.Profile
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = PremiumOrangeDark.copy(alpha = 0.12f),
                            spotColor = PremiumOrangeDark.copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        items.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.15f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "tab_scale_${tab.route}"
                            )

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                    )
                                },
                                label = {
                                    Text(
                                        tab.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PremiumOrangeDark,
                                    selectedTextColor = PremiumOrangeDark,
                                    indicatorColor = PremiumOrangeSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = WorkerTab.JobFeed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(WorkerTab.JobFeed.route) {
                WorkerHomeScreen(
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToActiveJob = { jobId ->
                        navController.navigate("worker_job_detail/$jobId")
                    }
                )
            }
            composable(WorkerTab.MyJobs.route) {
                WorkerMyJobsScreen(
                    onOpenActiveJob = { jobId ->
                        navController.navigate("worker_job_detail/$jobId")
                    }
                )
            }
            composable(WorkerTab.Earnings.route) {
                EarningsScreen()
            }
            composable(WorkerTab.Profile.route) {
                WorkerProfileScreen(
                    onRoleSwitched = onRoleSwitched,
                    onLoggedOut = onLogout
                )
            }
            composable("worker_job_detail/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: 0
                WorkerJobDetailScreen(
                    jobId = jobId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenJob = { jobId ->
                        navController.navigate("worker_job_detail/$jobId")
                    }
                )
            }
        }
    }
}
