package com.mamafua.app.presentation.job

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun JobDetailsScreen(
    viewModel: JobDetailsViewModel = koinViewModel(),
    isClient: Boolean,
    onNavigateToPayment: (Int) -> Unit
) {
    val job by viewModel.job.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (job == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentJob = job!!

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Job Details", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Title: ${currentJob.title}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Description: ${currentJob.description}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Budget: KES ${currentJob.budget}")
                Text("Status: ${currentJob.status}")
                Text("Payment: ${currentJob.paymentStatus}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            if (isClient) {
                // Client Actions
                when (currentJob.status) {
                    "OPEN" -> {
                        Text("Waiting for workers to apply...")
                        Button(onClick = { viewModel.acceptWorker(workerId = 2) }) { 
                            Text("Accept Dummy Worker")
                        }
                    }
                    "ACCEPTED" -> {
                        Button(onClick = { onNavigateToPayment(currentJob.id) }) {
                            Text("Pay M-Pesa Escrow")
                        }
                    }
                    "IN_PROGRESS" -> {
                        Button(onClick = { viewModel.updateStatus("COMPLETED") }) {
                            Text("Confirm Completion")
                        }
                    }
                }
            } else {
                // Worker Actions
                when (currentJob.status) {
                    "OPEN" -> {
                        Button(onClick = { viewModel.applyForJob() }) {
                            Text("Apply for Job")
                        }
                    }
                    "ACCEPTED" -> {
                        Button(onClick = { viewModel.updateStatus("IN_PROGRESS") }) {
                            Text("Start Job")
                        }
                    }
                    "IN_PROGRESS" -> {
                        Button(onClick = { viewModel.updateStatus("COMPLETION_REQUESTED") }) {
                            Text("Request Completion")
                        }
                    }
                }
            }
        }
    }
}
