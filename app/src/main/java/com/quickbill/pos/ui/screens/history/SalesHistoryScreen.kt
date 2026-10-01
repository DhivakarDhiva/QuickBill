package com.quickbill.pos.ui.screens.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.util.PdfReceiptGenerator
import com.quickbill.pos.ui.theme.ErrorRed
import com.quickbill.pos.ui.theme.PrimaryGreen
import com.quickbill.pos.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: SalesHistoryViewModel
) {
    val context = LocalContext.current
    val bills by viewModel.bills.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val selectedBillDetails by viewModel.selectedBillDetails.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var billToRefund by remember { mutableStateOf<BillEntity?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sales History & Bills",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${bills.size} transactions found",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchChanged(it) },
                placeholder = { Text("Search by Bill # or customer name/phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Date Filters Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DateFilter.values()) { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { viewModel.onDateFilterSelected(filter) },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Status Filters Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.onStatusFilterSelected(null) },
                    label = { Text("All Status") }
                )
                FilterChip(
                    selected = statusFilter == BillStatus.COMPLETED,
                    onClick = { viewModel.onStatusFilterSelected(BillStatus.COMPLETED) },
                    label = { Text("Completed") }
                )
                FilterChip(
                    selected = statusFilter == BillStatus.REFUNDED,
                    onClick = { viewModel.onStatusFilterSelected(BillStatus.REFUNDED) },
                    label = { Text("Refunded") }
                )
            }

            // Bills List
            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bills found for the selected filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bills, key = { it.id }) { bill ->
                        BillCard(
                            bill = bill,
                            onClick = { viewModel.viewBillDetails(bill.id) }
                        )
                    }
                }
            }
        }
    }

    // Bill Details Dialog
    selectedBillDetails?.let { details ->
        BillDetailsDialog(
            details = details,
            onDismiss = { viewModel.closeBillDetails() },
            onRefund = {
                billToRefund = details.bill
            },
            onSharePdf = {
                sharePdf(context, details)
            }
        )
    }

    // Refund Confirmation Dialog
    billToRefund?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToRefund = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = ErrorRed) },
            title = { Text("Refund Bill #${bill.billNumber}?") },
            text = {
                Text(
                    "Refunding this bill of ₹${String.format(Locale.US, "%.2f", bill.grandTotal)} will mark it as REFUNDED and automatically restore the inventory stock for all purchased items. Are you sure?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundBill(bill.id)
                        billToRefund = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
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
}

@Composable
private fun BillCard(
    bill: BillEntity,
    onClick: () -> Unit
) {
    val isRefunded = bill.status == BillStatus.REFUNDED
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(bill.timestamp))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRefunded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bill.billNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Status chip
                Surface(
                    color = if (isRefunded) ErrorRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = bill.status.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isRefunded) ErrorRed else SuccessGreen
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Cashier: ${bill.cashierName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (bill.customerName.isNotBlank()) {
                        Text(
                            text = "Customer: ${bill.customerName}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Text(
                        text = "Mode: ${bill.paymentMode.name}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Text(
                    text = "₹${String.format(Locale.US, "%.2f", bill.grandTotal)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRefunded) Color.Gray else PrimaryGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun BillDetailsDialog(
    details: BillWithDetails,
    onDismiss: () -> Unit,
    onRefund: () -> Unit,
    onSharePdf: () -> Unit
) {
    val bill = details.bill
    val items = details.items
    val payments = details.payments
    val isRefunded = bill.status == BillStatus.REFUNDED

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bill Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = bill.billNumber,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status Alert Banner
                    if (isRefunded) {
                        Surface(
                            color = ErrorRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ErrorRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This bill has been VOIDED / REFUNDED. Inventory stock was restored.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ErrorRed)
                                )
                            }
                        }
                    }

                    // Metadata
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(bill.timestamp))
                            DetailRow("Date & Time", dateStr)
                            DetailRow("Cashier", bill.cashierName)
                            if (bill.customerName.isNotBlank()) DetailRow("Customer", bill.customerName)
                            if (bill.customerPhone.isNotBlank()) DetailRow("Phone", bill.customerPhone)
                            DetailRow("Payment Mode", bill.paymentMode.name)
                        }
                    }

                    // Items Table
                    Text(
                        text = "PURCHASED ITEMS (${items.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.productName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            text = "${item.quantity} x ₹${String.format(Locale.US, "%.2f", item.unitPrice)} (${item.taxRate.toInt()}% GST)",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // Tax & Totals Breakdown
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailRow("Subtotal", "₹${String.format(Locale.US, "%.2f", bill.subtotal)}")
                            if (bill.discountAmount > 0) DetailRow("Discount", "-₹${String.format(Locale.US, "%.2f", bill.discountAmount)}")
                            if (bill.cgstAmount > 0) DetailRow("CGST", "₹${String.format(Locale.US, "%.2f", bill.cgstAmount)}")
                            if (bill.sgstAmount > 0) DetailRow("SGST", "₹${String.format(Locale.US, "%.2f", bill.sgstAmount)}")
                            DetailRow("Total Tax", "₹${String.format(Locale.US, "%.2f", bill.taxAmount)}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GRAND TOTAL", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("₹${String.format(Locale.US, "%.2f", bill.grandTotal)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryGreen))
                            }
                        }
                    }

                    // Payment Breakdown
                    Text(
                        text = "PAYMENT DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            payments.forEach { p ->
                                DetailRow("${p.mode.name} Paid", "₹${String.format(Locale.US, "%.2f", p.amount)}")
                                if (p.referenceNote.isNotBlank()) {
                                    Text(
                                        text = "Note: ${p.referenceNote}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                            if (bill.changeDue > 0) {
                                DetailRow("Change Returned", "₹${String.format(Locale.US, "%.2f", bill.changeDue)}")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSharePdf,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reprint / Share")
                    }

                    if (!isRefunded) {
                        Button(
                            onClick = onRefund,
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refund / Void")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

private fun sharePdf(context: Context, details: BillWithDetails) {
    try {
        val file = PdfReceiptGenerator.generateReceiptPdf(context, details)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Receipt #${details.bill.billNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
