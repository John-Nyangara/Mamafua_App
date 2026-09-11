package com.mamafua.app.presentation.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun ClientHomeScreen(
    viewModel: ClientHomeViewModel = koinViewModel()
) {
    val workers by viewModel.workers.collectAsState()

    LaunchedEffect(Unit) {
        // Dummy coordinates for search
        viewModel.searchWorkers(-1.2921, 36.8219)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nearby Workers", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(workers) { worker ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Worker ID: ${worker.userId}", style = MaterialTheme.typography.titleMedium)
                        Text("Rate: KES ${worker.hourlyRate}/hr")
                        Text("Rating: ${worker.averageRating} Stars")
                    }
                }
            }
        }
    }
}
