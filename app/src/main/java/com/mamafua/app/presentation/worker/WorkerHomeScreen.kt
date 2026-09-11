package com.mamafua.app.presentation.worker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkerHomeScreen(
    viewModel: WorkerHomeViewModel = koinViewModel(),
    onNavigateToJobDetails: (Int) -> Unit = {}
) {
    val jobs by viewModel.availableJobs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Available Jobs", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(jobs) { job ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onNavigateToJobDetails(job.id) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(job.title, style = MaterialTheme.typography.titleMedium)
                        Text("Budget: KES ${job.budget}")
                        Text("Location: ${job.location}")
                    }
                }
            }
        }
    }
}
