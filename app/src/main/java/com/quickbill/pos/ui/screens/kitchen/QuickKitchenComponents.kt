package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.kds.ConnectedPosTerminal
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderStatus
import androidx.compose.ui.window.Dialog
import java.util.Locale

// =========================================================================
// Chef Hat Logo Icon matching Reference (Screen 2, 3, 4, 5, etc.)
// =========================================================================
@Composable
fun ChefHatBadge(
    modifier: Modifier = Modifier,
    size: Int = 40,
    backgroundColor: Color = Color(0xFFDCFCE7),
    iconColor: Color = Color(0xFF0F5132)
) {
    Surface(
        shape = RoundedCornerShape((size * 0.3f).dp),
        color = backgroundColor,
        modifier = modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Chef Hat",
                tint = iconColor,
                modifier = Modifier.size((size * 0.58f).dp)
            )
        }
    }
}

// =========================================================================
// Header: QuickKitchen Top Bar matching Reference Screen 5, 6, 7
// =========================================================================
@Composable
fun QuickKitchenHeader(
    kitchenName: String,
    isConnected: Boolean,
    onConnectionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = QuickKitchenTheme.Surface,
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Chef Badge + Title + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChefHatBadge(size = 38)
                Column {
                    Text(
                        text = "QuickKitchen",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QuickKitchenTheme.TextPrimary
                        )
                    )
                    Text(
                        text = kitchenName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = QuickKitchenTheme.TextSecondary
                        )
                    )
                }
            }

            // Right: Connected Pill (Clickable to view connected POS terminal details)
            Surface(
                onClick = onConnectionClick,
                shape = RoundedCornerShape(16.dp),
                color = if (isConnected) QuickKitchenTheme.GreenPillBg else QuickKitchenTheme.RedPillBg,
                border = BorderStroke(1.dp, if (isConnected) Color(0xFFBBF7D0) else Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Connection Status",
                        tint = if (isConnected) QuickKitchenTheme.GreenPillText else QuickKitchenTheme.RedPillText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isConnected) "Connected" else "Waiting...",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isConnected) QuickKitchenTheme.GreenPillText else QuickKitchenTheme.RedPillText
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = if (isConnected) QuickKitchenTheme.GreenPillText.copy(alpha = 0.7f) else QuickKitchenTheme.RedPillText.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// Status Tabs matching Reference Screen 5, 6, 7
// 3 rounded cards: [ 4 New ] [ 3 Preparing ] [ 2 Ready ]
// =========================================================================
@Composable
fun QuickKitchenStatusTabs(
    newCount: Int,
    preparingCount: Int,
    readyCount: Int,
    selectedStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tab 1: NEW
        val isNewSelected = selectedStatus == OrderStatus.NEW
        StatusTabCard(
            modifier = Modifier.weight(1f),
            count = newCount,
            label = "New",
            isSelected = isNewSelected,
            selectedBg = QuickKitchenTheme.GreenPrimary,
            selectedContentColor = Color.White,
            unselectedAccentColor = QuickKitchenTheme.GreenLight,
            onClick = { onStatusSelected(OrderStatus.NEW) }
        )

        // Tab 2: PREPARING
        val isPreparingSelected = selectedStatus == OrderStatus.PREPARING
        StatusTabCard(
            modifier = Modifier.weight(1f),
            count = preparingCount,
            label = "Preparing",
            isSelected = isPreparingSelected,
            selectedBg = QuickKitchenTheme.OrangePrimary,
            selectedContentColor = Color.White,
            unselectedAccentColor = QuickKitchenTheme.OrangePrimary,
            onClick = { onStatusSelected(OrderStatus.PREPARING) }
        )

        // Tab 3: READY
        val isReadySelected = selectedStatus == OrderStatus.READY
        StatusTabCard(
            modifier = Modifier.weight(1f),
            count = readyCount,
            label = "Ready",
            isSelected = isReadySelected,
            selectedBg = QuickKitchenTheme.GreenPrimary,
            selectedContentColor = Color.White,
            unselectedAccentColor = QuickKitchenTheme.RedPrimary,
            onClick = { onStatusSelected(OrderStatus.READY) }
        )
    }
}

@Composable
private fun StatusTabCard(
    count: Int,
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedContentColor: Color,
    unselectedAccentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) selectedBg else QuickKitchenTheme.Surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) selectedBg else QuickKitchenTheme.BorderSubtle
        ),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isSelected) selectedContentColor else unselectedAccentColor
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isSelected) selectedContentColor.copy(alpha = 0.9f) else QuickKitchenTheme.TextSecondary
                )
            )
        }
    }
}

// =========================================================================
// Live Order Timer Badge matching Reference
// =========================================================================
@Composable
fun QuickKitchenTimerBadge(
    createdAt: Long,
    currentTimeMillis: Long,
    warningThresholdMinutes: Int = 5,
    modifier: Modifier = Modifier
) {
    val elapsedSeconds = ((currentTimeMillis - createdAt) / 1000L).coerceAtLeast(0L)
    val elapsedMinutes = elapsedSeconds / 60
    val isOverdue = elapsedMinutes >= warningThresholdMinutes
    val isWarning = elapsedMinutes >= (warningThresholdMinutes - 1) && !isOverdue

    val timerText = if (elapsedMinutes >= 60) {
        val hours = elapsedMinutes / 60
        val remainingMin = elapsedMinutes % 60
        String.format(Locale.US, "%02d:%02d:%02d", hours, remainingMin, elapsedSeconds % 60)
    } else {
        String.format(Locale.US, "%02d:%02d", elapsedMinutes, elapsedSeconds % 60)
    }

    val backgroundColor = when {
        isOverdue -> QuickKitchenTheme.RedPillBg
        isWarning -> QuickKitchenTheme.OrangePillBg
        else -> QuickKitchenTheme.SurfaceVariant
    }

    val textColor = when {
        isOverdue -> QuickKitchenTheme.RedPrimary
        isWarning -> QuickKitchenTheme.OrangePrimary
        else -> QuickKitchenTheme.TextSecondary
    }

    val borderColor = when {
        isOverdue -> Color(0xFFFECACA)
        isWarning -> Color(0xFFFED7AA)
        else -> QuickKitchenTheme.BorderSubtle
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = timerText,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = textColor
                )
            )
        }
    }
}

// =========================================================================
// Order Card matching Reference Screen 5, 6, 7
// =========================================================================
@Composable
fun QuickKitchenOrderCard(
    order: KitchenOrder,
    items: List<KitchenOrderItem>,
    currentTimeMillis: Long,
    warningThresholdMinutes: Int = 5,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elapsedSeconds = ((currentTimeMillis - order.createdAt) / 1000L).coerceAtLeast(0L)
    val elapsedMinutes = elapsedSeconds / 60
    val isOverdue = elapsedMinutes >= warningThresholdMinutes
    val timerText = String.format(Locale.US, "%02d:%02d", elapsedMinutes, elapsedSeconds % 60)

    val itemCount = if (items.isNotEmpty()) items.sumOf { it.quantity } else 1
    val metadataText = buildString {
        append(order.orderType.displayName)
        if (order.tableNumber.isNotBlank()) append(" · Table ${order.tableNumber}")
        append(" · $itemCount items")
    }

    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(14.dp),
        color = QuickKitchenTheme.Surface,
        border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
        shadowElevation = 0.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: #10042 on left, Timer pill on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayOrderNumber = if (order.orderNumber.startsWith("QB-") && order.orderNumber.length > 12) {
                    "#" + order.orderNumber.substringAfterLast("-")
                } else {
                    "#${order.orderNumber}"
                }

                Text(
                    text = displayOrderNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = QuickKitchenTheme.TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                QuickKitchenTimerBadge(
                    createdAt = order.createdAt,
                    currentTimeMillis = currentTimeMillis,
                    warningThresholdMinutes = warningThresholdMinutes
                )
            }

            // Order metadata: Dine In · Table 3 · 4 items
            Text(
                text = metadataText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = QuickKitchenTheme.TextSecondary
                )
            )

            HorizontalDivider(color = QuickKitchenTheme.BorderSubtle.copy(alpha = 0.6f))

            // Items List matching reference
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${item.quantity} ×",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = QuickKitchenTheme.TextPrimary
                                )
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = QuickKitchenTheme.TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.notes.isNotBlank()) {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = QuickKitchenTheme.OrangePrimary
                                ),
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Action Button: Solid Green or Orange
            when (order.status) {
                OrderStatus.NEW -> {
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        Text(
                            text = "Start Preparing",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                OrderStatus.PREPARING -> {
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.OrangePrimary),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        Text(
                            text = "Mark as Ready",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                OrderStatus.READY -> {
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        Text(
                            text = "Mark as Completed",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

// =========================================================================
// Bottom Navigation matching Reference (Orders | History | Settings)
// =========================================================================
@Composable
fun QuickKitchenBottomBar(
    currentTab: Int, // 0 = Orders, 1 = History, 2 = Settings
    activeOrdersCount: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = QuickKitchenTheme.Surface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 1: Orders
            BottomNavItem(
                icon = Icons.Default.RestaurantMenu,
                label = "Orders",
                isSelected = currentTab == 0,
                badgeCount = if (activeOrdersCount > 0) activeOrdersCount else null,
                onClick = { onSelectTab(0) }
            )

            // Tab 2: History
            BottomNavItem(
                icon = Icons.Default.History,
                label = "History",
                isSelected = currentTab == 1,
                badgeCount = null,
                onClick = { onSelectTab(1) }
            )

            // Tab 3: Settings
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == 2,
                badgeCount = null,
                onClick = { onSelectTab(2) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(
            badge = {
                if (badgeCount != null) {
                    Badge(
                        containerColor = QuickKitchenTheme.RedPrimary,
                        contentColor = Color.White
                    ) {
                        Text(text = badgeCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.TextMuted
            )
        )
    }
}

// =========================================================================
// Connected POS Terminals Details Dialog (Requirement 3)
// Triggered by clicking the connected network badge in QuickKitchenHeader
// =========================================================================
@Composable
fun ConnectedPosDetailsDialog(
    kitchenName: String,
    serverIp: String,
    serverPort: Int,
    isConnected: Boolean,
    connectedTerminals: List<ConnectedPosTerminal>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = QuickKitchenTheme.Surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with icon and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isConnected) QuickKitchenTheme.GreenPillBg else QuickKitchenTheme.RedPillBg,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (isConnected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.RedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "POS Connection Info",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = QuickKitchenTheme.TextPrimary
                                )
                            )
                            Text(
                                text = if (isConnected) "${if (connectedTerminals.isNotEmpty()) connectedTerminals.size else 1} POS terminal(s) connected" else "Waiting for POS connection",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = QuickKitchenTheme.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Station Local Server Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuickKitchenTheme.Background,
                    border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kitchen Station",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = QuickKitchenTheme.TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = kitchenName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = QuickKitchenTheme.TextPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KDS Server IP & Port",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = QuickKitchenTheme.TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "$serverIp:$serverPort",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = QuickKitchenTheme.GreenPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Network Discovery",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = QuickKitchenTheme.TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "QuickKitchen-KDS (NSD Active)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = QuickKitchenTheme.TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Connected Terminals Section
                Text(
                    text = "Connected POS Terminals",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = QuickKitchenTheme.TextPrimary
                    )
                )

                if (connectedTerminals.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        connectedTerminals.forEach { terminal ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = QuickKitchenTheme.Background,
                                border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = QuickKitchenTheme.GreenPillBg,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PointOfSale,
                                                    contentDescription = null,
                                                    tint = QuickKitchenTheme.GreenPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = terminal.name.ifBlank { "POS Terminal" },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = QuickKitchenTheme.TextPrimary
                                                )
                                            )
                                            if (terminal.deviceModel.isNotBlank()) {
                                                Text(
                                                    text = "Device: ${terminal.deviceModel}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.sp,
                                                        color = QuickKitchenTheme.TextSecondary
                                                    )
                                                )
                                            }
                                            Text(
                                                text = "IP: ${terminal.ipAddress}:${terminal.port}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    color = QuickKitchenTheme.TextMuted
                                                )
                                            )
                                        }
                                    }

                                    // Synced Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = QuickKitchenTheme.GreenPillBg,
                                        border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(QuickKitchenTheme.GreenLight)
                                            )
                                            Text(
                                                text = "Active",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = QuickKitchenTheme.GreenPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (isConnected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QuickKitchenTheme.GreenPillBg.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = QuickKitchenTheme.GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "1 POS Terminal Connected",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = QuickKitchenTheme.GreenPrimary
                                    )
                                )
                                Text(
                                    text = "Orders are syncing live to this kitchen station.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = QuickKitchenTheme.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QuickKitchenTheme.Background,
                        border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = QuickKitchenTheme.TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "No POS Terminal Connected",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = QuickKitchenTheme.TextPrimary
                                )
                            )
                            Text(
                                text = "Connect your QuickBill POS device on the same Wi-Fi using IP: $serverIp:$serverPort or via automatic Kitchen Discovery.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = QuickKitchenTheme.TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

