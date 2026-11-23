package com.mamafua.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkerProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val user by viewModel.user.collectAsState()
    
    var isAvailable by remember { mutableStateOf(true) }
    var hourlyRate by remember { mutableStateOf("500") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Worker Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        user?.let { current ->
            Text("Name: ${current.name}", style = MaterialTheme.typography.titleMedium)
            Text("Email: ${current.email}")
            Text("Phone: ${current.phone}")
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = hourlyRate,
            onValueChange = { hourlyRate = it },
            label = { Text("Hourly Rate (KES)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Available for Jobs")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isAvailable,
                onCheckedChange = { isAvailable = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Logout")
        }
    }
}
