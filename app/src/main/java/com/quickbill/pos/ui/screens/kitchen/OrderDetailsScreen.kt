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
import androidx.compose.material.icons.filled.*
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
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrderDetailsScreen(
    order: KitchenOrder,
    items: List<KitchenOrderItem>,
    onBackClick: () -> Unit,
    onAdvanceStatus: () -> Unit,
    onViewBill: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Details, 1 = Timeline

    Scaffold(
        containerColor = QuickKitchenTheme.Background,
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = QuickKitchenTheme.TextPrimary
                            )
                        }

                        Text(
                            text = "Order #${order.orderNumber}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = QuickKitchenTheme.TextPrimary
                            )
                        )
                    }

                    // Status Pill on top right
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (order.status) {
                            OrderStatus.NEW -> QuickKitchenTheme.GreenPillBg
                            OrderStatus.PREPARING -> QuickKitchenTheme.OrangePillBg
                            OrderStatus.READY, OrderStatus.COMPLETED -> QuickKitchenTheme.GreenPillBg
                            OrderStatus.CANCELLED -> QuickKitchenTheme.RedPillBg
                        },
                        border = BorderStroke(
                            1.dp,
                            when (order.status) {
                                OrderStatus.NEW -> Color(0xFFBBF7D0)
                                OrderStatus.PREPARING -> Color(0xFFFED7AA)
                                OrderStatus.READY, OrderStatus.COMPLETED -> Color(0xFFBBF7D0)
                                OrderStatus.CANCELLED -> Color(0xFFFECACA)
                            }
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
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
                                    fontSize = 12.sp,
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
            }
        },
        bottomBar = {
            Surface(
                color = QuickKitchenTheme.Surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (order.status) {
                    OrderStatus.NEW -> {
                        Button(
                            onClick = onAdvanceStatus,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary)
                        ) {
                            Text(
                                text = "Start Preparing",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    OrderStatus.PREPARING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onAdvanceStatus,
                                modifier = Modifier
                                    .weight(2f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.OrangePrimary)
                            ) {
                                Text(
                                    text = "Move to Ready",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                            }

                            Button(
                                onClick = onViewBill,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                            ) {
                                Text(
                                    text = "View Bill",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }

                    OrderStatus.READY -> {
                        Button(
                            onClick = onAdvanceStatus,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary)
                        ) {
                            Text(
                                text = "Mark as Completed",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Details vs Timeline Subtabs matching Reference Screen 8, 9, 10
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(QuickKitchenTheme.Surface)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TabHeaderItem(
                    title = "Details",
                    isSelected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 }
                )
                TabHeaderItem(
                    title = "Timeline",
                    isSelected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 }
                )
            }

            HorizontalDivider(color = QuickKitchenTheme.BorderSubtle)

            if (selectedSubTab == 0) {
                // Details Tab View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Order Metadata block
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = QuickKitchenTheme.Surface,
                            border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
                                MetadataRow(Icons.Default.AccessTime, "Order Time", timeFormatted)
                                MetadataRow(Icons.Default.Restaurant, "Dining Type", order.orderType.displayName)
                                MetadataRow(Icons.Default.TableBar, "Table", if (order.tableNumber.isNotBlank()) "Table ${order.tableNumber}" else "Takeaway")
                                MetadataRow(Icons.Default.Fastfood, "Items", "${items.sumOf { it.quantity }} items")
                            }
                        }
                    }

                    // Items Header & Rows with Food Thumbnail
                    items(items) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QuickKitchenTheme.Surface,
                            border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Food thumbnail icon
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fastfood,
                                            contentDescription = null,
                                            tint = QuickKitchenTheme.OrangePrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = QuickKitchenTheme.TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "${item.quantity} × ₹${item.unitPrice.toInt()}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = QuickKitchenTheme.TextSecondary
                                        )
                                    )
                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            text = item.notes,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = QuickKitchenTheme.OrangePrimary
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "₹ ${(item.quantity * item.unitPrice).toInt()}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = QuickKitchenTheme.TextPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Notes Section matching Reference
                    if (order.notes.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = QuickKitchenTheme.Surface,
                                border = BorderStroke(1.dp, QuickKitchenTheme.BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = QuickKitchenTheme.TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Notes",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = QuickKitchenTheme.TextPrimary
                                            )
                                        )
                                    }
                                    Text(
                                        text = order.notes,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = QuickKitchenTheme.TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Timeline Tab View matching Reference Screen 18
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val orderCreatedTime = timeFormat.format(Date(order.createdAt))

                    item {
                        TimelineNode(
                            title = "Order Created",
                            description = "Order #${order.orderNumber} sent from POS Counter",
                            time = orderCreatedTime,
                            icon = Icons.Default.Receipt,
                            isPassed = true,
                            isLast = false
                        )
                    }

                    item {
                        TimelineNode(
                            title = "Received in Kitchen",
                            description = "Order displayed on Kitchen Display System",
                            time = orderCreatedTime,
                            icon = Icons.Default.Wifi,
                            isPassed = true,
                            isLast = false
                        )
                    }

                    item {
                        TimelineNode(
                            title = "Preparing",
                            description = "Kitchen staff started preparation",
                            time = if (order.status != OrderStatus.NEW) timeFormat.format(Date(order.updatedAt)) else "--:--",
                            icon = Icons.Default.SoupKitchen,
                            isPassed = order.status == OrderStatus.PREPARING || order.status == OrderStatus.READY || order.status == OrderStatus.COMPLETED,
                            isLast = false
                        )
                    }

                    item {
                        TimelineNode(
                            title = "Ready for Pickup",
                            description = "Cooked & plated, counter notified",
                            time = if (order.status == OrderStatus.READY || order.status == OrderStatus.COMPLETED) timeFormat.format(Date(order.updatedAt)) else "--:--",
                            icon = Icons.Default.CheckCircle,
                            isPassed = order.status == OrderStatus.READY || order.status == OrderStatus.COMPLETED,
                            isLast = false
                        )
                    }

                    item {
                        TimelineNode(
                            title = "Completed",
                            description = "Order served to customer",
                            time = if (order.status == OrderStatus.COMPLETED) timeFormat.format(Date(order.updatedAt)) else "--:--",
                            icon = Icons.Default.DoneAll,
                            isPassed = order.status == OrderStatus.COMPLETED,
                            isLast = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabHeaderItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isSelected) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.TextSecondary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(2.5.dp)
                .width(44.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) QuickKitchenTheme.GreenPrimary else Color.Transparent)
        )
    }
}

@Composable
private fun MetadataRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QuickKitchenTheme.TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = QuickKitchenTheme.TextSecondary
                )
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = QuickKitchenTheme.TextPrimary
            )
        )
    }
}

@Composable
private fun TimelineNode(
    title: String,
    description: String,
    time: String,
    icon: ImageVector,
    isPassed: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vertical line + Node Circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isPassed) QuickKitchenTheme.GreenPillBg else Color(0xFFF1F5F9),
                border = BorderStroke(1.5.dp, if (isPassed) QuickKitchenTheme.GreenPrimary else Color(0xFFCBD5E1)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPassed) QuickKitchenTheme.GreenPrimary else QuickKitchenTheme.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .background(if (isPassed) QuickKitchenTheme.GreenPrimary.copy(alpha = 0.5f) else Color(0xFFE2E8F0))
                )
            }
        }

        // Details Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 20.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isPassed) QuickKitchenTheme.TextPrimary else QuickKitchenTheme.TextMuted
                    )
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = QuickKitchenTheme.TextMuted
                    )
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = QuickKitchenTheme.TextSecondary
                )
            )
        }
    }
}
