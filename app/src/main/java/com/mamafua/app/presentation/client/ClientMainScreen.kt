package com.mamafua.app.presentation.client

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    object Explore : ClientTab("client_home", "Explore", Icons.Default.Search)
    object PostJob : ClientTab("post_job", "Post Job", Icons.Default.AddCircle)
    object MyJobs : ClientTab("client_my_jobs", "My Jobs", Icons.Default.CheckCircle)
    object Profile : ClientTab("client_profile", "Profile", Icons.Default.Person)
}

@Composable
fun ClientMainScreen(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        ClientTab.Explore,
        ClientTab.PostJob,
        ClientTab.MyJobs,
        ClientTab.Profile
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = PremiumTealDark.copy(alpha = 0.15f),
                            spotColor = PremiumTealDark.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(72.dp)
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
            startDestination = ClientTab.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ClientTab.Explore.route) {
                ClientHomeScreen(
                    onNavigateToNotifications = {
                        navController.navigate("notifications")
                    }
                )
            }
            composable(ClientTab.PostJob.route) {
                PostJobScreen(
                    onJobPosted = {
                        navController.navigate(ClientTab.MyJobs.route) {
                            popUpTo(ClientTab.Explore.route)
                        }
                    }
                )
            }
            composable(ClientTab.MyJobs.route) {
                ClientMyJobsScreen(
                    onNavigateToApplicants = { jobId ->
                        navController.navigate("client_job_applicants/$jobId")
                    }
                )
            }
            composable(ClientTab.Profile.route) {
                ClientProfileScreen(
                    onLoggedOut = onLogout
                )
            }
            composable("client_job_applicants/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId")?.toIntOrNull() ?: 0
                ClientJobApplicantsScreen(
                    jobId = jobId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
