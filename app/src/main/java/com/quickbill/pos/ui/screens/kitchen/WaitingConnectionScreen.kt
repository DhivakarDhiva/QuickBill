package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WaitingConnectionScreen(
    kitchenName: String = "Main Kitchen",
    ipAddress: String,
    port: Int,
    serviceName: String = "QuickKitchen-KDS",
    connectedClientsCount: Int = 0,
    onConnected: () -> Unit,
    onSkipToDashboard: () -> Unit
) {
    // If a POS client connects, automatically navigate
    LaunchedEffect(connectedClientsCount) {
        if (connectedClientsCount > 0) {
            onConnected()
        }
    }

    // Gentle pulse animation for radar rings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        containerColor = QuickKitchenTheme.Background,
        topBar = {
            QuickKitchenHeader(
                kitchenName = kitchenName,
                isConnected = connectedClientsCount > 0
            )
        },
        bottomBar = {
            Surface(
                color = QuickKitchenTheme.Background,
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Information card matching Reference Screen 4
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Keep this device on the same Wi-Fi network as your POS.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                        }
                    }

                    // Optional skip for testing/solo kitchen usage
                    TextButton(
                        onClick = onSkipToDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip to Kitchen Display",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = QuickKitchenTheme.GreenPrimary
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Concentric Radar Rings matching Reference Screen 4
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale)
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7).copy(alpha = 0.35f))
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7).copy(alpha = 0.65f))
                )

                // Inner circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBBF7D0))
                )

                // Center Chef Hat Badge
                ChefHatBadge(size = 52)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text: Advertising on Network
            Text(
                text = "Advertising on Network",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = QuickKitchenTheme.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Service Name
            Text(
                text = serviceName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = QuickKitchenTheme.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic IP and Port
            Text(
                text = "$ipAddress : $port",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = QuickKitchenTheme.TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Waiting Pill with spinner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = QuickKitchenTheme.Surface,
                border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = QuickKitchenTheme.GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Waiting for POS to Connect...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = QuickKitchenTheme.TextPrimary
                        )
                    )
                }
            }
        }
    }
}
