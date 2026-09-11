package com.mamafua.app.presentation.payment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun PaymentStkScreen(
    viewModel: PaymentViewModel = koinViewModel(),
    onPaymentComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("M-Pesa Payment", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("To pay the worker, enter your M-Pesa registered phone number below. You will receive an STK prompt on your phone.")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number (e.g. 254712345678)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (state.message != null) {
            Text(state.message!!, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.initiatePayment(phone) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && phone.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Send STK Push")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        if (state.isSuccess) {
            Button(
                onClick = onPaymentComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("I have completed the payment")
            }
        }
    }
}
