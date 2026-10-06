package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KitchenSetupScreen(
    initialKitchenName: String = "Main Kitchen",
    port: Int = 8080,
    serviceName: String = "QuickKitchen-KDS",
    serviceType: String = "_quickbill._tcp",
    onStartKds: (kitchenName: String) -> Unit
) {
    var kitchenName by remember { mutableStateOf(initialKitchenName) }

    Scaffold(
        containerColor = QuickKitchenTheme.Background,
        bottomBar = {
            Surface(
                color = QuickKitchenTheme.Background,
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = { onStartKds(kitchenName.ifBlank { "Main Kitchen" }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary)
                ) {
                    Text(
                        text = "Start Kitchen Display",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centered Chef Hat Logo
            ChefHatBadge(size = 54)

            // Screen Title
            Text(
                text = "Kitchen Display Setup",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = QuickKitchenTheme.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Kitchen Name Field
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Kitchen Name",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = QuickKitchenTheme.TextPrimary
                    )
                )

                OutlinedTextField(
                    value = kitchenName,
                    onValueChange = { kitchenName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = QuickKitchenTheme.Surface,
                        unfocusedContainerColor = QuickKitchenTheme.Surface,
                        focusedBorderColor = QuickKitchenTheme.GreenPrimary,
                        unfocusedBorderColor = QuickKitchenTheme.BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // "This device will act as:" Header
            Text(
                text = "This device will act as:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = QuickKitchenTheme.TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Info Card 1: Kitchen Display (KDS)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = QuickKitchenTheme.Surface,
                border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QuickKitchenTheme.OrangePillBg,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = QuickKitchenTheme.OrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Kitchen Display (KDS)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = QuickKitchenTheme.TextPrimary
                            )
                        )
                        Text(
                            text = "Receive and manage orders from QuickBill POS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = QuickKitchenTheme.TextSecondary
                            )
                        )
                    }
                }
            }

            // Info Card 2: Advertise on Network
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = QuickKitchenTheme.Surface,
                border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QuickKitchenTheme.GreenPillBg,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = QuickKitchenTheme.GreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Advertise on Network",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = QuickKitchenTheme.TextSecondary
                            )
                        )
                        Text(
                            text = serviceName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = QuickKitchenTheme.TextPrimary
                            )
                        )
                        Text(
                            text = "Port: $port\nService Type: $serviceType",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = QuickKitchenTheme.TextMuted,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
