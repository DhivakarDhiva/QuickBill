package com.quickbill.pos.ui.screens.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KitchenHistoryScreen(
    orders: List<KitchenOrder>,
    activeCount: Int,
    completedCount: Int,
    onBackClick: () -> Unit,
    onOrderClick: (KitchenOrder) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0 = Active, 1 = Completed

    val filteredOrders = remember(orders, selectedFilter) {
        if (selectedFilter == 0) {
            orders.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
        } else {
            orders.filter { it.status == OrderStatus.COMPLETED || it.status == OrderStatus.CANCELLED }
        }
    }

    Scaffold(
        containerColor = QuickKitchenTheme.Background,
        bottomBar = bottomBar,
        topBar = {
            Surface(
                color = QuickKitchenTheme.Surface,
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = QuickKitchenTheme.TextPrimary
                        )
                    }

                    Text(
                        text = "Order History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QuickKitchenTheme.TextPrimary
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
        ) {
            // Segmented Pills matching Reference Screen 11: Active (9) | Completed (12)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterPillButton(
                    title = "Active ($activeCount)",
                    isSelected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    modifier = Modifier.weight(1f)
                )

                FilterPillButton(
                    title = "Completed ($completedCount)",
                    isSelected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            // Orders list
            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedFilter == 0) "No active orders" else "No completed orders yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuickKitchenTheme.TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders, key = { it.orderId }) { order ->
                        HistoryOrderRow(
                            order = order,
                            onClick = { onOrderClick(order) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPillButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.Surface,
        border = BorderStroke(1.dp, if (isSelected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.BorderSubtle),
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else QuickKitchenTheme.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun HistoryOrderRow(
    order: KitchenOrder,
    onClick: () -> Unit
) {
    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.createdAt))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = QuickKitchenTheme.Surface,
        border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: #10042 on left, Status Pill on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.orderNumber}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = QuickKitchenTheme.TextPrimary
                    )
                )

                // Status pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (order.status) {
                        OrderStatus.NEW -> QuickKitchenTheme.GreenPillBg
                        OrderStatus.PREPARING -> QuickKitchenTheme.OrangePillBg
                        OrderStatus.READY, OrderStatus.COMPLETED -> QuickKitchenTheme.GreenPillBg
                        OrderStatus.CANCELLED -> QuickKitchenTheme.RedPillBg
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when (order.status) {
                                        OrderStatus.NEW -> QuickKitchenTheme.GreenLight
                                        OrderStatus.PREPARING -> QuickKitchenTheme.OrangePrimary
                                        OrderStatus.READY, OrderStatus.COMPLETED -> QuickKitchenTheme.GreenLight
                                        OrderStatus.CANCELLED -> QuickKitchenTheme.RedPrimary
                                    }
                                )
                        )
                        Text(
                            text = order.status.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = when (order.status) {
                                    OrderStatus.NEW -> QuickKitchenTheme.GreenPillText
                                    OrderStatus.PREPARING -> QuickKitchenTheme.OrangePillText
                                    OrderStatus.READY, OrderStatus.COMPLETED -> QuickKitchenTheme.GreenPillText
                                    OrderStatus.CANCELLED -> QuickKitchenTheme.RedPillText
                                }
                            )
                        )
                    }
                }
            }

            // Row 2: Dine In · Table 3 · 4 items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val metadata = buildString {
                    append(order.orderType.displayName)
                    if (order.tableNumber.isNotBlank()) append(" · Table ${order.tableNumber}")
                }
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = QuickKitchenTheme.TextSecondary
                    )
                )
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = QuickKitchenTheme.TextMuted
                    )
                )
            }
        }
    }
}
