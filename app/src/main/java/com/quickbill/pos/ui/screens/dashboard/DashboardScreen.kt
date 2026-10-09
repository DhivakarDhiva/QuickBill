/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */

package com.quickbill.pos.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.ui.components.ProductThumbnail
import com.quickbill.pos.ui.components.QuickBillCard
import com.quickbill.pos.ui.components.QuickBillKpiCard
import com.quickbill.pos.ui.theme.*
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToBilling: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLowStock: () -> Unit = onNavigateToProducts
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showItemsSoldDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val userName = currentUser?.fullName?.split(" ")?.firstOrNull() ?: currentUser?.username ?: "Admin"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================================
            // Greeting Header
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(0),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting, $userName 👋",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    )
                    Text(
                        text = "Here's what's happening today.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondaryLight
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Quick Launch Terminal Button
                FilledTonalButton(
                    onClick = onNavigateToBilling,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = EmeraldContainer,
                        contentColor = EmeraldPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Terminal",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // =========================================================================
            // 4 KPI Cards (Responsive: 1x4 in Landscape, 2x2 in Portrait)
            // =========================================================================
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Sales
                    QuickBillKpiCard(
                        title = if (uiState.selectedSlotIndex != null) "Sales (${uiState.chartPoints.getOrNull(uiState.selectedSlotIndex!!)?.label})" else "Total Sales",
                        value = "₹${String.format(Locale.US, "%,.2f", uiState.highlightedSales)}",
                        trend = if (uiState.selectedSlotIndex != null) "Slot Filtered" else "Today",
                        trendPositive = true,
                        icon = Icons.Default.CurrencyRupee,
                        iconColor = EmeraldPrimary,
                        iconBgColor = EmeraldContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSales
                    )

                    // Total Bills
                    QuickBillKpiCard(
                        title = "Bills",
                        value = "${uiState.displayedBillsCount}",
                        trend = if (uiState.selectedSlotIndex != null) "In Slot" else "Today",
                        trendPositive = true,
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconColor = AccentBlue,
                        iconBgColor = AccentBlueContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSales
                    )

                    // Items Sold
                    QuickBillKpiCard(
                        title = "Items Sold",
                        value = "${uiState.displayedItemsCount}",
                        trend = if (uiState.selectedSlotIndex != null) "In Slot" else "Today",
                        trendPositive = true,
                        icon = Icons.Default.Inventory2,
                        iconColor = AccentPurple,
                        iconBgColor = AccentPurpleContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { showItemsSoldDialog = true }
                    )

                    // Low Stock Alert
                    val hasLowStock = uiState.lowStockCount > 0
                    QuickBillKpiCard(
                        title = "Low Stock",
                        value = "${uiState.lowStockCount}",
                        trend = if (hasLowStock) "Needs attention" else "Normal",
                        trendPositive = !hasLowStock,
                        icon = Icons.Default.WarningAmber,
                        iconColor = if (hasLowStock) CoralAccent else SuccessGreen,
                        iconBgColor = if (hasLowStock) CoralContainer else SuccessGreenContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLowStock
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Sales - dynamically highlights selected slot or full day
                        QuickBillKpiCard(
                            title = if (uiState.selectedSlotIndex != null) "Sales (${uiState.chartPoints.getOrNull(uiState.selectedSlotIndex!!)?.label})" else "Total Sales",
                            value = "₹${String.format(Locale.US, "%,.2f", uiState.highlightedSales)}",
                            trend = if (uiState.selectedSlotIndex != null) "Slot Filtered" else "Today",
                            trendPositive = true,
                            icon = Icons.Default.CurrencyRupee,
                            iconColor = EmeraldPrimary,
                            iconBgColor = EmeraldContainer,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSales
                        )

                        // Total Bills
                        QuickBillKpiCard(
                            title = "Bills",
                            value = "${uiState.displayedBillsCount}",
                            trend = if (uiState.selectedSlotIndex != null) "In Slot" else "Today",
                            trendPositive = true,
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconColor = AccentBlue,
                            iconBgColor = AccentBlueContainer,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSales
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Items Sold - Clicking opens the detailed Items Sold Dialog
                        QuickBillKpiCard(
                            title = "Items Sold",
                            value = "${uiState.displayedItemsCount}",
                            trend = if (uiState.selectedSlotIndex != null) "In Slot" else "Today",
                            trendPositive = true,
                            icon = Icons.Default.Inventory2,
                            iconColor = AccentPurple,
                            iconBgColor = AccentPurpleContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { showItemsSoldDialog = true }
                        )

                        // Low Stock Alert - Real Room count, navigates to Products
                        val hasLowStock = uiState.lowStockCount > 0
                        QuickBillKpiCard(
                            title = "Low Stock",
                            value = "${uiState.lowStockCount}",
                            trend = if (hasLowStock) "Needs attention" else "Normal",
                            trendPositive = !hasLowStock,
                            icon = Icons.Default.WarningAmber,
                            iconColor = if (hasLowStock) CoralAccent else SuccessGreen,
                            iconBgColor = if (hasLowStock) CoralContainer else SuccessGreenContainer,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLowStock
                        )
                    }
                }
            }

            // =========================================================================
            // Sales Bar Chart Card (Today Only, Interactive Time Bars, Highlighted Sales)
            // =========================================================================
            QuickBillCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(2),
                containerColor = SurfaceWhite
            ) {
                // Header (No dropdown icon, shows pure Today pill badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Sales Report",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                        )
                        Text(
                            text = "Tap any time bar to filter report",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryLight
                            )
                        )
                    }

                    // Static "Today" badge - dropdown removed per user requirement
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldContainer,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prominently Highlighted Sales Amount Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.selectedSlotIndex != null) EmeraldContainer.copy(alpha = 0.45f) else SurfaceMutedLight,
                    border = BorderStroke(1.dp, if (uiState.selectedSlotIndex != null) EmeraldPrimary else OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (uiState.selectedSlotIndex != null) {
                                        "Sales • ${uiState.selectedSlotLabel}"
                                    } else {
                                        "Total Sales (Today)"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.selectedSlotIndex != null) EmeraldPrimary else TextSecondaryLight
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.2f", uiState.highlightedSales)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldPrimary
                                    )
                                )
                            }

                            if (uiState.selectedSlotIndex != null) {
                                Button(
                                    onClick = { viewModel.selectSlot(null) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "Show All",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Reset filter",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.selectedSlotIndex != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${uiState.displayedBillsCount} bills • ${uiState.displayedItemsCount} items sold during this slot",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryLight
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Interactive Bar Chart (Tapping bar filters report accordingly)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    HourlyBarChart(
                        hourlyPoints = uiState.chartPoints,
                        selectedSlotIndex = uiState.selectedSlotIndex,
                        onSlotSelected = { index -> viewModel.selectSlot(index) }
                    )
                }
            }

            // =========================================================================
            // Top Selling Items (Compact Ranked List with Thumbnails)
            // =========================================================================
            QuickBillCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(3),
                containerColor = SurfaceWhite
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Selling Items",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    )

                    Text(
                        text = "Ranked",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryLight
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.topSellingItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = TextMutedLight,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "No sales recorded yet today",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondaryLight,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Top selling items will appear here once orders are billed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMutedLight
                                )
                            )
                        }
                    }
                } else {
                    val items = uiState.topSellingItems
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items.take(4).forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Rank Number
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) CoralAccent else TextSecondaryLight
                                        ),
                                        modifier = Modifier.width(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Product Thumbnail
                                    ProductThumbnail(
                                        productName = item.productName,
                                        category = "Beverages",
                                        size = 40.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Product Name
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryLight
                                        ),
                                        maxLines = 1
                                    )
                                }

                                // Sold Count Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceMutedLight,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "${item.totalQty}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryLight
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (index < items.take(4).size - 1) {
                                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Items Sold Detail Dialog
    if (showItemsSoldDialog) {
        AlertDialog(
            onDismissRequest = { showItemsSoldDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Items Sold (Today)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (uiState.topSellingItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items sold yet today",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
                        )
                    }
                } else {
                    val items = uiState.topSellingItems
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceMutedLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldContainer,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldPrimary
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Revenue: ₹${String.format(Locale.US, "%.0f", item.totalRevenue)}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldPrimary
                                ) {
                                    Text(
                                        text = "${item.totalQty} sold",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
            confirmButton = {
                Button(
                    onClick = { showItemsSoldDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

// =========================================================================
// Hourly Bar Chart Component
// =========================================================================

@Composable
private fun HourlyBarChart(
    hourlyPoints: List<HourlySalePoint>,
    modifier: Modifier = Modifier,
    selectedSlotIndex: Int? = null,
    onSlotSelected: (Int) -> Unit = {}
) {
    val points = if (hourlyPoints.isNotEmpty()) {
        hourlyPoints
    } else {
        listOf(
            HourlySalePoint("6 AM", "6:00 AM - 9:00 AM", 0.0, 0, 0),
            HourlySalePoint("9 AM", "9:00 AM - 12:00 PM", 0.0, 0, 0),
            HourlySalePoint("12 PM", "12:00 PM - 3:00 PM", 0.0, 0, 0),
            HourlySalePoint("3 PM", "3:00 PM - 6:00 PM", 0.0, 0, 0),
            HourlySalePoint("6 PM", "6:00 PM - 9:00 PM", 0.0, 0, 0),
            HourlySalePoint("9 PM", "9:00 PM - 11:59 PM", 0.0, 0, 0)
        )
    }

    val maxAmount = points.maxOfOrNull { it.amount }?.coerceAtLeast(10.0) ?: 10.0

    Column(modifier = modifier.fillMaxSize()) {
        // Bars Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = selectedSlotIndex == index
                val hasSales = point.amount > 0.0
                val targetFraction = if (hasSales) {
                    (point.amount / maxAmount).toFloat().coerceIn(0.18f, 1f)
                } else {
                    0.08f // Small pill placeholder so users can still click empty slots
                }

                val barFraction by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f
                    ),
                    label = "barHeight"
                )

                val barColor by androidx.compose.animation.animateColorAsState(
                    targetValue = when {
                        isSelected -> EmeraldPrimary
                        hasSales -> EmeraldPrimary.copy(alpha = 0.55f)
                        else -> OutlineLight.copy(alpha = 0.35f)
                    },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f
                    ),
                    label = "barColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSlotSelected(index) }
                        .padding(horizontal = 4.dp)
                ) {
                    if (isSelected && hasSales) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = "₹${point.amount.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isSelected) 0.65f else 0.45f)
                            .fillMaxHeight(barFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time slot labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = selectedSlotIndex == index
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) EmeraldPrimary else TextSecondaryLight,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSlotSelected(index) },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
