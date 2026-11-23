package com.mamafua.app.presentation.client

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

sealed class ClientTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : ClientTab("client_home", "Home", Icons.Default.Home)
    object PostJob : ClientTab("client_post_job", "Book", Icons.Default.AddCircle)
    object MyJobs : ClientTab("client_my_jobs", "My Jobs", Icons.Default.List)
    object Profile : ClientTab("client_profile", "Profile", Icons.Default.Person)
}

@Composable
fun ClientMainScreen(
    onRoleSwitched: () -> Unit = {},
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        ClientTab.Home,
        ClientTab.PostJob,
        ClientTab.MyJobs,
        ClientTab.Profile
    )

    var selectedCategoryForBooking by remember { mutableStateOf<String?>(null) }
    var preselectedWorkerIdForBooking by remember { mutableStateOf<Int?>(null) }
    var preselectedWorkerNameForBooking by remember { mutableStateOf<String?>(null) }

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
                            ambientColor = PremiumTealDark.copy(alpha = 0.12f),
                            spotColor = PremiumTealDark.copy(alpha = 0.12f)
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
                                    selectedIconColor = PremiumTealDark,
                                    selectedTextColor = PremiumTealDark,
                                    indicatorColor = PremiumTealSurface,
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
            startDestination = ClientTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ClientTab.Home.route) {
                ClientHomeScreen(
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    },
                    onNavigateToCustomizer = { category, workerId, workerName ->
                        selectedCategoryForBooking = category
                        preselectedWorkerIdForBooking = workerId
                        preselectedWorkerNameForBooking = workerName
                        navController.navigate(ClientTab.PostJob.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToJobDetail = { jobId ->
                        navController.navigate("client_job_detail/$jobId")
                    }
                )
            }
            composable(ClientTab.PostJob.route) {
                PostJobScreen(
                    initialCategory = selectedCategoryForBooking,
                    preselectedWorkerId = preselectedWorkerIdForBooking,
                    preselectedWorkerName = preselectedWorkerNameForBooking,
                    onJobPosted = { jobId ->
                        selectedCategoryForBooking = null
                        preselectedWorkerIdForBooking = null
                        preselectedWorkerNameForBooking = null
                        if (jobId > 0) {
                            navController.navigate("client_job_detail/$jobId")
                        } else {
                            navController.navigate(ClientTab.MyJobs.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(ClientTab.MyJobs.route) {
                ClientMyJobsScreen(
                    onViewApplicants = { jobId ->
                        navController.navigate("applicants/$jobId")
                    },
                    onOpenJobDetail = { jobId ->
                        navController.navigate("client_job_detail/$jobId")
                    }
                )
            }
            composable(ClientTab.Profile.route) {
                ClientProfileScreen(
                    onRoleSwitched = onRoleSwitched,
                    onLoggedOut = onLogout
                )
            }
            composable("client_job_detail/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: 0
                ClientJobDetailScreen(
                    jobId = jobId,
                    onBack = { navController.popBackStack() },
                    onNavigateToApplicants = { id ->
                        navController.navigate("applicants/$id")
                    }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenJob = { jobId ->
                        navController.navigate("client_job_detail/$jobId")
                    }
                )
            }
            composable("applicants/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: 0
                ClientJobApplicantsScreen(
                    jobId = jobId,
                    onBack = { navController.popBackStack() },
                    onAccepted = { acceptedJobId ->
                        navController.navigate("client_job_detail/$acceptedJobId") {
                            popUpTo("applicants/$jobId") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
