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

package com.quickbill.pos.ui.screens.history

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.util.PdfReceiptGenerator
import com.quickbill.pos.ui.components.*
import com.quickbill.pos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SalesHistoryScreen(
    viewModel: SalesHistoryViewModel,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false
) {
    val context = LocalContext.current
    val bills by viewModel.bills.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val customDateRange by viewModel.customDateRange.collectAsState()
    val paymentFilter by viewModel.paymentFilter.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val selectedBillDetails by viewModel.selectedBillDetails.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var billToRefund by remember { mutableStateOf<BillEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }
    var showCalendarRangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackgroundLight)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = if (isLandscape) 16.dp else 84.dp)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            val totalSalesAmount = remember(bills) { bills.filter { it.status == BillStatus.COMPLETED }.sumOf { it.grandTotal } }
            val completedCount = remember(bills) { bills.count { it.status == BillStatus.COMPLETED } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = if (isLandscape) 30.dp else 100.dp
                )
            ) {
                // Header & Date Range Indicator matching Screen 10
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sales History",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )
                            Text(
                                text = "$completedCount completed (${bills.size} total)",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAdmin && bills.isNotEmpty()) {
                                Surface(
                                    onClick = { showClearConfirmDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = ErrorCoral.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Clear Records",
                                            tint = ErrorCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ErrorCoral
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            val datePillLabel = remember(dateFilter, customDateRange) {
                                if (dateFilter == DateFilter.CUSTOM_RANGE && customDateRange.first != null && customDateRange.second != null) {
                                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                    "${sdf.format(Date(customDateRange.first!!))} - ${sdf.format(Date(customDateRange.second!!))}"
                                } else {
                                    dateFilter.label
                                }
                            }

                            // Date range picker pill
                            Box {
                                Surface(
                                    onClick = { showDateMenu = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (dateFilter == DateFilter.CUSTOM_RANGE) EmeraldContainer else SurfaceWhite,
                                    border = BorderStroke(1.dp, if (dateFilter == DateFilter.CUSTOM_RANGE) EmeraldPrimary else OutlineLight)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = datePillLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (dateFilter == DateFilter.CUSTOM_RANGE) EmeraldPrimary else TextPrimaryLight
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showDateMenu,
                                    onDismissRequest = { showDateMenu = false },
                                    modifier = Modifier.background(SurfaceWhite)
                                ) {
                                    DateFilter.values().filter { it != DateFilter.CUSTOM_RANGE }.forEach { filter ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = filter.label,
                                                    fontWeight = if (dateFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (dateFilter == filter) EmeraldPrimary else TextPrimaryLight
                                                )
                                            },
                                            onClick = {
                                                viewModel.onDateFilterSelected(filter)
                                                showDateMenu = false
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Calendar Date Range...",
                                                fontWeight = if (dateFilter == DateFilter.CUSTOM_RANGE) FontWeight.Bold else FontWeight.Normal,
                                                color = EmeraldPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showDateMenu = false
                                            showCalendarRangeDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar + Payment Mode Filters (Responsive Row in Landscape, Stacked in Portrait)
                item(key = "search_and_payment_filters") {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(1),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(0.55f)) {
                                QuickBillSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.onSearchChanged(it) },
                                    placeholder = "Search by Bill # or customer..."
                                )
                            }

                            Row(
                                modifier = Modifier.weight(0.45f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickBillChip(
                                    text = "All",
                                    selected = paymentFilter == null,
                                    onClick = { viewModel.onPaymentFilterSelected(null) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickBillChip(
                                    text = "Cash",
                                    selected = paymentFilter == PaymentMode.CASH,
                                    onClick = { viewModel.onPaymentFilterSelected(PaymentMode.CASH) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickBillChip(
                                    text = "Card",
                                    selected = paymentFilter == PaymentMode.CARD,
                                    onClick = { viewModel.onPaymentFilterSelected(PaymentMode.CARD) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickBillChip(
                                    text = "UPI",
                                    selected = paymentFilter == PaymentMode.UPI,
                                    onClick = { viewModel.onPaymentFilterSelected(PaymentMode.UPI) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(1),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickBillSearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.onSearchChanged(it) },
                                placeholder = "Search by Bill # or customer..."
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                QuickBillChip(
                                    text = "All",
                                    selected = paymentFilter == null,
                                    onClick = { viewModel.onPaymentFilterSelected(null) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickBillChip(
                                    text = "Cash",
                                    selected = paymentFilter == PaymentMode.CASH,
                                    onClick = { viewModel.onPaymentFilterSelected(PaymentMode.CASH) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickBillChip(
                                    text = "Card",
                                    selected = paymentFilter == PaymentMode.CARD,
                                    onClick = { viewModel.onPaymentFilterSelected(PaymentMode.CARD) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickBillChip(
                                    text = "UPI",
                                    selected = paymentFilter == PaymentMode.UPI,
                                    onClick = { viewModel.onPaymentFilterSelected(PaymentMode.UPI) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Date Filter Chips
                item(key = "date_filter_chips") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2)
                    ) {
                        items(DateFilter.values()) { filter ->
                            QuickBillChip(
                                text = filter.label,
                                selected = dateFilter == filter,
                                onClick = { viewModel.onDateFilterSelected(filter) }
                            )
                        }
                    }
                }

                // Total Sales Revenue Highlighted Banner
                item(key = "total_revenue_banner") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(3)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TOTAL SALES REVENUE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$completedCount completed (${bills.size} total)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondaryLight,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }

                            Text(
                                text = "₹ ${String.format(Locale.US, "%,.2f", totalSalesAmount)}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                // Bills List or Empty State
                if (bills.isEmpty()) {
                    item(key = "empty_state") {
                        QuickBillEmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "No sales records found",
                            description = "Try selecting 'All Time' or adjusting your search",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp)
                        )
                    }
                } else {
                    items(bills, key = { it.id }) { bill ->
                        SalesBillCard(
                            bill = bill,
                            onClick = { viewModel.viewBillDetails(bill.id) }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // Screen 11: Bill Details Dialog (with Refund)
    // Matches Screen 11 Mockup: Status badge, items table, subtotal, discount,
    // CGST/SGST, Grand Total, Payment Details, [Refund], [Print], [Share]
    // =========================================================================
    AnimatedVisibility(
        visible = selectedBillDetails != null,
        enter = SwiftUiMotion.ModalEnterTransition,
        exit = SwiftUiMotion.ModalExitTransition
    ) {
        selectedBillDetails?.let { details ->
            BillDetailsDialogScreen(
                details = details,
                onDismiss = { viewModel.closeBillDetails() },
                onRefund = { billToRefund = details.bill },
                onPrint = { sharePdf(context, details) },
                onShare = { sharePdf(context, details) }
            )
        }
    }

    // Refund Confirmation Dialog
    billToRefund?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToRefund = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = ErrorCoral) },
            title = { Text("Refund Bill #${bill.billNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Refunding this bill of ₹ ${String.format(Locale.US, "%.2f", bill.grandTotal)} will mark it as REFUNDED and automatically restore the inventory stock for all purchased items."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundBill(bill.id)
                        billToRefund = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) {
                    Text("Confirm Refund & Restore Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToRefund = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Records Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorCoral) },
            title = { Text("Clear All Transactions?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently clear all completed sales records and transactions? This will reset the sales history and daily revenue reports to zero.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory(isAdmin)
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) {
                    Text("Clear All Records", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Calendar Date Range Picker Dialog
    if (showCalendarRangeDialog) {
        val cal = Calendar.getInstance()
        var startMillis by remember { mutableLongStateOf(customDateRange.first ?: (System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)) }
        var endMillis by remember { mutableLongStateOf(customDateRange.second ?: System.currentTimeMillis()) }
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        QuickBillDialog(
            onDismissRequest = { showCalendarRangeDialog = false },
            title = "Filter by Date Range"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select start and end dates to filter sales transactions",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )

                // Start Date Picker Row
                Surface(
                    onClick = {
                        cal.timeInMillis = startMillis
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val selected = Calendar.getInstance().apply {
                                    set(y, m, d, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                startMillis = selected.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceMutedLight,
                    border = BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("From Date", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight))
                            Text(displayFormat.format(Date(startMillis)), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight))
                        }
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary)
                    }
                }

                // End Date Picker Row
                Surface(
                    onClick = {
                        cal.timeInMillis = endMillis
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val selected = Calendar.getInstance().apply {
                                    set(y, m, d, 23, 59, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                endMillis = selected.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceMutedLight,
                    border = BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("To Date", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight))
                            Text(displayFormat.format(Date(endMillis)), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight))
                        }
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary)
                    }
                }

                // Quick Range Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickBillOutlinedButton(
                        text = "This Month",
                        onClick = {
                            val mCal = Calendar.getInstance()
                            mCal.set(Calendar.DAY_OF_MONTH, 1)
                            mCal.set(Calendar.HOUR_OF_DAY, 0)
                            mCal.set(Calendar.MINUTE, 0)
                            mCal.set(Calendar.SECOND, 0)
                            mCal.set(Calendar.MILLISECOND, 0)
                            startMillis = mCal.timeInMillis
                            endMillis = System.currentTimeMillis()
                        },
                        modifier = Modifier.weight(1f),
                        height = 36.dp
                    )
                    QuickBillOutlinedButton(
                        text = "Last 30 Days",
                        onClick = {
                            val mCal = Calendar.getInstance()
                            mCal.add(Calendar.DAY_OF_YEAR, -30)
                            mCal.set(Calendar.HOUR_OF_DAY, 0)
                            mCal.set(Calendar.MINUTE, 0)
                            mCal.set(Calendar.SECOND, 0)
                            mCal.set(Calendar.MILLISECOND, 0)
                            startMillis = mCal.timeInMillis
                            endMillis = System.currentTimeMillis()
                        },
                        modifier = Modifier.weight(1f),
                        height = 36.dp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.onDateFilterSelected(DateFilter.ALL)
                            showCalendarRangeDialog = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Reset")
                    }

                    QuickBillButton(
                        text = "Apply Filter",
                        onClick = {
                            val s = minOf(startMillis, endMillis)
                            val e = maxOf(startMillis, endMillis)
                            viewModel.setCustomDateRange(s, e)
                            showCalendarRangeDialog = false
                        },
                        modifier = Modifier.weight(1.5f),
                        height = 44.dp
                    )
                }
            }
        }
    }
}

// =========================================================================
// Sales List Card (Screen 10 Mockup)
// QB-000123
// 3 items · 01 Oct 2026, 04:28 PM
// ₹ 315.00  [Completed]
// =========================================================================

@Composable
private fun SalesBillCard(
    bill: BillEntity,
    onClick: () -> Unit
) {
    val isRefunded = bill.status == BillStatus.REFUNDED
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(bill.timestamp))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, OutlineLight),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.billNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$dateStr • ${bill.paymentMode.name}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${String.format(Locale.US, "%.2f", bill.grandTotal)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryLight
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Badge matching mockup 10
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isRefunded) CoralContainer else EmeraldContainer
                    ) {
                        Text(
                            text = if (isRefunded) "Voided" else "Completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isRefunded) CoralAccent else EmeraldPrimary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Click affordance chevron
                Surface(
                    shape = CircleShape,
                    color = SurfaceMutedLight,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "View Details",
                            tint = TextSecondaryLight,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// Screen 11: Bill Details Full Dialog (with Refund)
// =========================================================================

@Composable
fun BillDetailsDialogScreen(
    details: BillWithDetails,
    onDismiss: () -> Unit,
    onRefund: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit
) {
    val bill = details.bill
    val items = details.items
    val payments = details.payments
    val isRefunded = bill.status == BillStatus.REFUNDED
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(bill.timestamp))

    BackHandler(onBack = onDismiss)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bill Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isRefunded) CoralContainer else EmeraldContainer
                    ) {
                        Text(
                            text = if (isRefunded) "Refunded" else "Completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isRefunded) CoralAccent else EmeraldPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Actions: Refund (Admin), Print, Share
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, OutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        if (!isRefunded) {
                            OutlinedButton(
                                onClick = onRefund,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CoralAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(QuickBillDimens.buttonHeight)
                            ) {
                                Text("Refund", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onPrint,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier
                                .weight(1f)
                                .height(QuickBillDimens.buttonHeight),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                        ) {
                            Text("Print", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onShare,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(QuickBillDimens.buttonHeight)
                        ) {
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarmBackgroundLight)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Bill Summary Card
                    QuickBillCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = bill.billNumber,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = TextPrimaryLight
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$dateStr • Cashier: ${bill.cashierName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Items Table Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Items", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.8f))
                            Text("Qty", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                            Text("Price", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                            Text("Total", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.productName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.8f))
                                Text("${item.quantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                Text(String.format(Locale.US, "%.2f", item.unitPrice), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                                Text(String.format(Locale.US, "%.2f", item.lineTotal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Subtotal, Discount, Taxes
                        QuickBillPriceRow(label = "Subtotal", amount = bill.subtotal)
                        if (bill.discountAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            QuickBillPriceRow(label = "Discount (${bill.discountValue.toInt()}%)", amount = bill.discountAmount, isNegative = true)
                        }
                        if (bill.cgstAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            QuickBillPriceRow(label = "CGST (2.5%)", amount = bill.cgstAmount)
                        }
                        if (bill.sgstAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            QuickBillPriceRow(label = "SGST (2.5%)", amount = bill.sgstAmount)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        QuickBillPriceRow(label = "Grand Total", amount = bill.grandTotal, isTotal = true)

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Payment Details",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        payments.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(p.mode.name, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight))
                                Text("₹ ${String.format(Locale.US, "%.2f", p.amount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

private fun sharePdf(context: Context, billWithDetails: BillWithDetails) {
    try {
        val pdfFile = PdfReceiptGenerator.generateReceiptPdf(context, billWithDetails)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Receipt #${billWithDetails.bill.billNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
