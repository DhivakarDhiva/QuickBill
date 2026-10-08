package com.quickbill.pos.ui.screens.mode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.kds.DeviceMode
import com.quickbill.pos.ui.screens.kitchen.ChefHatBadge
import com.quickbill.pos.ui.screens.kitchen.QuickKitchenTheme

@Composable
fun DeviceModeSelectionScreen(
    currentMode: DeviceMode? = null,
    onBackClick: (() -> Unit)? = null,
    onModeSelected: (DeviceMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode ?: DeviceMode.KDS) }

    Scaffold(
        containerColor = QuickKitchenTheme.Background,
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = QuickKitchenTheme.TextPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChefHatBadge(size = 32)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QuickBill",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QuickKitchenTheme.TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        bottomBar = {
            Surface(
                color = QuickKitchenTheme.Background,
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = { onModeSelected(selectedMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary)
                ) {
                    Text(
                        text = "Continue",
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen Header matching Reference Screen 2
            Text(
                text = "Choose Device Mode",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = QuickKitchenTheme.TextPrimary
                )
            )

            Text(
                text = "Select how you want to use this device",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = QuickKitchenTheme.TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: POS
            ModeOptionCard(
                title = "POS",
                subtitle = "Billing, Sales & Management",
                icon = Icons.Default.PointOfSale,
                iconBg = QuickKitchenTheme.BluePillBg,
                iconTint = QuickKitchenTheme.BluePillText,
                isSelected = selectedMode == DeviceMode.POS,
                onClick = { selectedMode = DeviceMode.POS }
            )

            // Option 2: Kitchen Display
            ModeOptionCard(
                title = "Kitchen Display",
                subtitle = "Receive and manage kitchen orders",
                icon = Icons.Default.Restaurant,
                iconBg = QuickKitchenTheme.GreenPillBg,
                iconTint = QuickKitchenTheme.GreenAccent,
                isSelected = selectedMode == DeviceMode.KDS,
                onClick = { selectedMode = DeviceMode.KDS }
            )
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = QuickKitchenTheme.Surface,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.BorderSubtle
        ),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = QuickKitchenTheme.TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = QuickKitchenTheme.TextSecondary
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = QuickKitchenTheme.TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
