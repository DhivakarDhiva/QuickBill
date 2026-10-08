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
    p2pState: com.quickbill.pos.network.kds.P2pConnectionState = com.quickbill.pos.network.kds.P2pConnectionState(),
    onStartP2pGroup: () -> Unit = {},
    onStopP2pGroup: () -> Unit = {},
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
                    // Information card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QuickKitchenTheme.SurfaceVariant,
                        border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = QuickKitchenTheme.BluePillBg,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = QuickKitchenTheme.BluePillText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (p2pState.isConnected) {
                                    "Wi-Fi Direct Active! Connect POS to this device directly via P2P."
                                } else {
                                    "Connect both devices to the same Wi-Fi, or use Wi-Fi Direct below."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                        }
                    }

                    // P2P Direct Group Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (p2pState.isConnected) QuickKitchenTheme.GreenPillBg else QuickKitchenTheme.SurfaceVariant,
                        border = BorderStroke(1.dp, if (p2pState.isConnected) QuickKitchenTheme.GreenPrimary.copy(alpha = 0.5f) else QuickKitchenTheme.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (p2pState.isConnected) "Wi-Fi Direct Group Active" else "No Router? Use Wi-Fi Direct (P2P)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (p2pState.isConnected) QuickKitchenTheme.GreenAccent else QuickKitchenTheme.TextPrimary
                                    )
                                )
                                Text(
                                    text = if (p2pState.isConnected) "Direct IP: 192.168.49.1:$port" else "Create a direct hotspot without any router",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = QuickKitchenTheme.TextSecondary
                                    )
                                )
                            }
                            if (p2pState.isConnected) {
                                OutlinedButton(
                                    onClick = onStopP2pGroup,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QuickKitchenTheme.RedAccent),
                                    border = BorderStroke(1.dp, QuickKitchenTheme.RedPrimary.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Stop P2P", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = onStartP2pGroup,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Start P2P", fontSize = 11.sp)
                                }
                            }
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
                                color = QuickKitchenTheme.GreenAccent
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
                        .background(QuickKitchenTheme.GreenPillBg.copy(alpha = 0.45f))
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(QuickKitchenTheme.GreenPillBg.copy(alpha = 0.75f))
                )

                // Inner circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(QuickKitchenTheme.GreenPillBg)
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
                        color = QuickKitchenTheme.GreenAccent,
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
