package com.mamafua.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun ClientProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val user by viewModel.user.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Client Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        user?.let { current ->
            Text("Name: ${current.name}", style = MaterialTheme.typography.titleMedium)
            Text("Email: ${current.email}")
            Text("Phone: ${current.phone}")
            Text("Role: ${current.role}")
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
