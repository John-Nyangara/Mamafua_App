package com.mamafua.app.presentation.client

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun PostJobScreen(
    viewModel: PostJobViewModel = koinViewModel(),
    onJobPosted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(500))
        contentOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Gradient Header ──
        GradientTopBar(
            title = "Post a Job ",
            subtitle = "Describe the work you need done"
        )

        // ── Form Body ──
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffset.value
                    }
            ) {
            // Job Title
            AnimatedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = "Job Title",
                leadingIcon = Icons.Default.Work
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            AnimatedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = "Description",
                leadingIcon = Icons.Default.Description,
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            AnimatedTextField(
                value = viewModel.location,
                onValueChange = { viewModel.location = it },
                label = "Location",
                leadingIcon = Icons.Default.LocationOn
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Budget
            AnimatedTextField(
                value = viewModel.budget,
                onValueChange = { viewModel.budget = it },
                label = "Budget (KES)",
                leadingIcon = Icons.Default.AttachMoney,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error
            val errorMsg = uiState.errorMessage
            if (errorMsg != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedSurface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorRedDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMsg,
                            color = ErrorRedDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Success message
            val successMsg = uiState.successMessage
            if (successMsg != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessGreenSurface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = successMsg,
                            color = SuccessGreenDark,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Post Job Button
            GradientButton(
                text = "Post Job",
                onClick = { viewModel.postJob(onJobPosted) },
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading,
                icon = Icons.Default.Send
            )
            }
        }
    }
}
