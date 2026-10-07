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
import androidx.compose.material.icons.automirrored.filled.Notes
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    currentTimeMillis: Long = System.currentTimeMillis(),
    warningThresholdMinutes: Int = 5,
    onBackClick: () -> Unit,
    onAdvanceStatus: () -> Unit,
    onViewBill: () -> Unit = {}
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Details, 1 = Timeline
    var showBillDialog by remember { mutableStateOf(false) }

    val elapsedSec = ((currentTimeMillis - order.createdAt) / 1000L).coerceAtLeast(0L)
    val elapsedMin = elapsedSec / 60
    val isOverdue = elapsedMin >= warningThresholdMinutes && order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED

    if (showBillDialog) {
        KitchenBillReceiptDialog(
            order = order,
            items = items,
            onDismiss = { showBillDialog = false }
        )
    }

    Scaffold(
        containerColor = if (isOverdue) Color(0xFFFFF5F5) else QuickKitchenTheme.Background,
        topBar = {
            Surface(
                color = QuickKitchenTheme.Surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                ) {
                    // Top App Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = QuickKitchenTheme.TextPrimary
                            )
                        }

                        // Order Title (Flexible width with ellipsis so it never squishes status badges)
                        Text(
                            text = "Order #${order.orderNumber}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = QuickKitchenTheme.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        )

                        // Timer badge on right
                        if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                            QuickKitchenTimerBadge(
                                createdAt = order.createdAt,
                                currentTimeMillis = currentTimeMillis,
                                warningThresholdMinutes = warningThresholdMinutes,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        // Status Pill (with intrinsic padding and non-wrapping text)
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
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                                        fontSize = 11.sp,
                                        color = when (order.status) {
                                            OrderStatus.NEW -> QuickKitchenTheme.GreenPillText
                                            OrderStatus.PREPARING -> QuickKitchenTheme.OrangePillText
                                            OrderStatus.READY, OrderStatus.COMPLETED -> QuickKitchenTheme.GreenPillText
                                            OrderStatus.CANCELLED -> QuickKitchenTheme.RedPillText
                                        }
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Details vs Timeline Subtabs with clean structure
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAdvanceStatus,
                                modifier = Modifier
                                    .weight(2f)
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

                            OutlinedButton(
                                onClick = {
                                    showBillDialog = true
                                    onViewBill()
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "View Bill",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }

                    OrderStatus.PREPARING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                            OutlinedButton(
                                onClick = {
                                    showBillDialog = true
                                    onViewBill()
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "View Bill",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }

                    OrderStatus.READY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAdvanceStatus,
                                modifier = Modifier
                                    .weight(2f)
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

                            OutlinedButton(
                                onClick = {
                                    showBillDialog = true
                                    onViewBill()
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "View Bill",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }

                    OrderStatus.COMPLETED -> {
                        Button(
                            onClick = {
                                showBillDialog = true
                                onViewBill()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "View Bill",
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
            if (selectedSubTab == 0) {
                // Details Tab View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Order Metadata block (highlighted when overdue)
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isOverdue) Color(0xFFFFF1F2) else QuickKitchenTheme.Surface,
                            border = BorderStroke(
                                if (isOverdue) 2.dp else 1.dp,
                                if (isOverdue) Color(0xFFE11D48) else QuickKitchenTheme.BorderSubtle
                            ),
                            shadowElevation = if (isOverdue) 3.dp else 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isOverdue) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFE4E6),
                                        border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFE11D48),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "ORDER DELAYED · Exceeded target prep time (${elapsedMin}m elapsed / ${warningThresholdMinutes}m target)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFBE123C)
                                                )
                                            )
                                        }
                                    }
                                }

                                val timeFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
                                MetadataRow(Icons.Default.AccessTime, "Order Time", timeFormatted)
                                val elapsedStr = if (elapsedMin >= 60) {
                                    String.format(Locale.US, "%02d:%02d:%02d", elapsedMin / 60, elapsedMin % 60, elapsedSec % 60)
                                } else {
                                    String.format(Locale.US, "%02d:%02d", elapsedMin, elapsedSec % 60)
                                }
                                MetadataRow(Icons.Default.Timer, "Elapsed Time", elapsedStr)
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
                                            imageVector = Icons.AutoMirrored.Filled.Notes,
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

@Composable
fun KitchenBillReceiptDialog(
    order: KitchenOrder,
    items: List<KitchenOrderItem>,
    onDismiss: () -> Unit
) {
    val totalAmount = remember(items) {
        items.sumOf { it.quantity * it.unitPrice }
    }
    val totalQty = remember(items) {
        items.sumOf { it.quantity }
    }
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Order Bill / KOT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = QuickKitchenTheme.TextPrimary
                                )
                            )
                            Text(
                                text = "Receipt & item breakdown",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = QuickKitchenTheme.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = QuickKitchenTheme.BorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Bill Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bill Metadata Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Order No",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = QuickKitchenTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "#${order.orderNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = QuickKitchenTheme.TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Date & Time",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = QuickKitchenTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = dateFormatter.format(Date(order.createdAt)),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = QuickKitchenTheme.TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Dining Type",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = QuickKitchenTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = order.orderType.displayName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = QuickKitchenTheme.TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Table",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = QuickKitchenTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = if (order.tableNumber.isNotBlank()) order.tableNumber else "Takeaway / Direct",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = QuickKitchenTheme.TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                if (order.customerName.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Customer",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = QuickKitchenTheme.TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = order.customerName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = QuickKitchenTheme.TextPrimary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                if (order.notes.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Special Note",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = QuickKitchenTheme.TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = order.notes,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFDC2626),
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Line Items Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ITEM",
                                modifier = Modifier.weight(2f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Text(
                                text = "QTY",
                                modifier = Modifier.weight(0.7f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Text(
                                text = "RATE",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                            Text(
                                text = "AMOUNT",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = QuickKitchenTheme.TextSecondary
                                )
                            )
                        }
                    }

                    // Items rows
                    items(items) { item ->
                        val itemTotal = item.quantity * item.unitPrice
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(2f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (item.isVeg) Color(0xFF16A34A) else Color(0xFFDC2626),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = QuickKitchenTheme.TextPrimary
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "x${item.quantity}",
                                    modifier = Modifier.weight(0.7f),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = QuickKitchenTheme.TextPrimary
                                    )
                                )
                                Text(
                                    text = if (item.unitPrice > 0.0) "₹${String.format(Locale.getDefault(), "%.0f", item.unitPrice)}" else "-",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = QuickKitchenTheme.TextSecondary
                                    )
                                )
                                Text(
                                    text = if (itemTotal > 0.0) "₹${String.format(Locale.getDefault(), "%.0f", itemTotal)}" else "-",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = QuickKitchenTheme.TextPrimary
                                    )
                                )
                            }
                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = "Note: ${item.notes}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFFD97706)
                                    ),
                                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                                )
                            }
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    // Bill Summary Totals Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total Items",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = QuickKitchenTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "$totalQty items",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = QuickKitchenTheme.TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                if (totalAmount > 0.0) {
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Grand Total",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = QuickKitchenTheme.TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = QuickKitchenTheme.GreenPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuickKitchenTheme.GreenPrimary)
                ) {
                    Text(
                        text = "Close Receipt",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
