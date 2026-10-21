package com.mamafua.app.presentation.worker

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mamafua.app.ui.components.*
import com.mamafua.app.ui.theme.*

@Composable
fun EarningsScreen() {
    val context = LocalContext.current
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(600))
        contentOffset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Gradient Header ──
        GradientTopBar(
            title = "Earnings ",
            subtitle = "Track your income and payouts",
            gradient = Brush.verticalGradient(
                colors = listOf(PremiumOrangeDark, PremiumOrange)
            )
        )

        // ── Main Content ──
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffset.value
                    }
            ) {
            // ── Balance Card with Glass Effect ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                PremiumOrange.copy(alpha = 0.5f),
                                PremiumOrangeDark.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Available Balance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedCounter(
                        targetValue = "KES 0.00",
                        style = MaterialTheme.typography.displayMedium,
                        color = PremiumOrangeDark
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Withdraw button
                    GradientButton(
                        text = "Withdraw to M-Pesa",
                        onClick = { Toast.makeText(context, "Withdrawal coming soon!", Toast.LENGTH_SHORT).show() },
                        gradient = mpesaGradient(),
                        icon = Icons.Default.AccountBalanceWallet
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Stats ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total Jobs",
                    value = "0",
                    icon = Icons.Default.Work,
                    accentColor = PremiumTealDark,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "This Month",
                    value = "KES 0",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = PremiumOrangeDark,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Avg Rating",
                    value = "—",
                    icon = Icons.Default.Star,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Recent Transactions ──
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PremiumCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your payment history will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
