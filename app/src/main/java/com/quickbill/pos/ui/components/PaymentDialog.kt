package com.quickbill.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickbill.pos.data.model.CartSummary
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.model.PaymentSplit
import com.quickbill.pos.data.util.BillingCalculator
import com.quickbill.pos.ui.theme.PrimaryGreen
import com.quickbill.pos.ui.theme.SuccessGreen
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    cartSummary: CartSummary,
    onDismiss: () -> Unit,
    onCompleteCheckout: (
        customerName: String,
        customerPhone: String,
        mode: PaymentMode,
        split: PaymentSplit
    ) -> Unit
) {
    val grandTotal = cartSummary.grandTotal
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }

    // Cash fields
    var cashTenderedText by remember { mutableStateOf(String.format(Locale.US, "%.2f", grandTotal)) }

    // Card fields
    var cardRef by remember { mutableStateOf("") }

    // UPI fields
    var upiRef by remember { mutableStateOf("") }

    // Split fields
    var splitCashText by remember { mutableStateOf("") }
    var splitCashTenderedText by remember { mutableStateOf("") }
    var splitCardText by remember { mutableStateOf("") }
    var splitCardRef by remember { mutableStateOf("") }
    var splitUpiText by remember { mutableStateOf("") }
    var splitUpiRef by remember { mutableStateOf("") }

    // Computations
    val cashTendered = cashTenderedText.toDoubleOrNull() ?: 0.0
    val cashChangeDue = if (cashTendered > grandTotal) BillingCalculator.round2(cashTendered - grandTotal) else 0.0

    val splitCash = splitCashText.toDoubleOrNull() ?: 0.0
    val splitCashTendered = splitCashTenderedText.toDoubleOrNull() ?: 0.0
    val splitCard = splitCardText.toDoubleOrNull() ?: 0.0
    val splitUpi = splitUpiText.toDoubleOrNull() ?: 0.0
    val splitTotalAllocated = BillingCalculator.round2(splitCash + splitCard + splitUpi)
    val splitRemaining = BillingCalculator.round2(maxOf(0.0, grandTotal - splitTotalAllocated))
    val splitCashChange = if (splitCashTendered > splitCash) BillingCalculator.round2(splitCashTendered - splitCash) else 0.0

    val isReadyToCheckout = when (selectedMode) {
        PaymentMode.CASH -> cashTendered >= (grandTotal - 0.01)
        PaymentMode.CARD -> true
        PaymentMode.UPI -> true
        PaymentMode.SPLIT -> splitTotalAllocated >= (grandTotal - 0.01)
    }

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
                            text = "Checkout & Payment",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${cartSummary.totalItemCount} items in cart",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grand Total Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AMOUNT PAYABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", grandTotal)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryGreen
                                )
                            )
                        }

                        // Tax & Discount sub-pills
                        Column(horizontalAlignment = Alignment.End) {
                            if (cartSummary.totalDiscount > 0) {
                                Text(
                                    text = "Savings: -₹${String.format(Locale.US, "%.2f", cartSummary.totalDiscount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Text(
                                text = "Incl. GST: ₹${String.format(Locale.US, "%.2f", cartSummary.taxTotal)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Optional Customer Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Mobile No.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Mode Selection Tabs
                    Text(
                        text = "SELECT PAYMENT MODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentModeTab(
                            title = "Cash",
                            icon = Icons.Default.Payments,
                            isSelected = selectedMode == PaymentMode.CASH,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = PaymentMode.CASH }
                        )
                        PaymentModeTab(
                            title = "Card",
                            icon = Icons.Default.CreditCard,
                            isSelected = selectedMode == PaymentMode.CARD,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = PaymentMode.CARD }
                        )
                        PaymentModeTab(
                            title = "UPI",
                            icon = Icons.Default.QrCode2,
                            isSelected = selectedMode == PaymentMode.UPI,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = PaymentMode.UPI }
                        )
                        PaymentModeTab(
                            title = "Split",
                            icon = Icons.Default.CallSplit,
                            isSelected = selectedMode == PaymentMode.SPLIT,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = PaymentMode.SPLIT }
                        )
                    }

                    // Mode-Specific Body
                    when (selectedMode) {
                        PaymentMode.CASH -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = cashTenderedText,
                                    onValueChange = { cashTenderedText = it },
                                    label = { Text("Cash Tendered (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Fast round tender buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val exact = grandTotal
                                    val next50 = (ceil(grandTotal / 50.0) * 50.0).coerceAtLeast(exact)
                                    val next100 = (ceil(grandTotal / 100.0) * 100.0).coerceAtLeast(exact)
                                    val next500 = (ceil(grandTotal / 500.0) * 500.0).coerceAtLeast(exact)

                                    val suggestions = linkedSetOf(exact, next50, next100, next500).toList()

                                    suggestions.forEach { amount ->
                                        FilledTonalButton(
                                            onClick = { cashTenderedText = String.format(Locale.US, "%.2f", amount) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Text("₹${amount.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }

                                // Change Due Alert Box
                                if (cashTendered >= grandTotal) {
                                    Surface(
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SuccessGreen
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Change to Return:",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", cashChangeDue)}",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = SuccessGreen
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Pending Amount:",
                                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                                            )
                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", grandTotal - cashTendered)}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        PaymentMode.CARD -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Swipe or Tap Card on Terminal",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "Collect payment of ₹${String.format(Locale.US, "%.2f", grandTotal)} on EDC device",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = cardRef,
                                    onValueChange = { cardRef = it },
                                    label = { Text("Transaction Ref / Last 4 Digits (Optional)") },
                                    placeholder = { Text("e.g. Auth #847291 / 4590") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        PaymentMode.UPI -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode2,
                                            contentDescription = "UPI QR Code",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(100.dp)
                                        )
                                        Text(
                                            text = "Scan with any UPI App",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "GPay • PhonePe • Paytm • BHIM",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "UPI ID: quickbill.store@pos",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = upiRef,
                                    onValueChange = { upiRef = it },
                                    label = { Text("UPI Ref / UTR No. (Optional)") },
                                    placeholder = { Text("e.g. 428198739124") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        PaymentMode.SPLIT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Balance tracker
                                Surface(
                                    color = if (splitRemaining <= 0.01) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (splitRemaining <= 0.01) "✓ Full Amount Allocated" else "Remaining to Allocate:",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (splitRemaining <= 0.01) "Ready to Complete" else "₹${String.format(Locale.US, "%.2f", splitRemaining)}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (splitRemaining <= 0.01) SuccessGreen else MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }
                                }

                                // 1. Split Cash
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("1. Cash Portion", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = splitCashText,
                                                onValueChange = { splitCashText = it },
                                                label = { Text("Cash Amount (₹)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = splitCashTenderedText,
                                                onValueChange = { splitCashTenderedText = it },
                                                label = { Text("Tendered (₹)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (splitCashChange > 0) {
                                            Text(
                                                text = "Cash Change to Return: ₹${String.format(Locale.US, "%.2f", splitCashChange)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                                            )
                                        }
                                    }
                                }

                                // 2. Split Card
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("2. Card Portion", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = splitCardText,
                                                onValueChange = { splitCardText = it },
                                                label = { Text("Card Amount (₹)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = splitCardRef,
                                                onValueChange = { splitCardRef = it },
                                                label = { Text("Card Ref") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // 3. Split UPI
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("3. UPI Portion", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = splitUpiText,
                                                onValueChange = { splitUpiText = it },
                                                label = { Text("UPI Amount (₹)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = splitUpiRef,
                                                onValueChange = { splitUpiRef = it },
                                                label = { Text("UPI Ref") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Checkout Button
                Button(
                    onClick = {
                        val split = when (selectedMode) {
                            PaymentMode.CASH -> PaymentSplit(
                                cashAmount = grandTotal,
                                cashTendered = cashTendered
                            )
                            PaymentMode.CARD -> PaymentSplit(
                                cardAmount = grandTotal,
                                cardRef = cardRef
                            )
                            PaymentMode.UPI -> PaymentSplit(
                                upiAmount = grandTotal,
                                upiRef = upiRef
                            )
                            PaymentMode.SPLIT -> PaymentSplit(
                                cashAmount = splitCash,
                                cardAmount = splitCard,
                                upiAmount = splitUpi,
                                cashTendered = if (splitCashTendered > 0) splitCashTendered else splitCash,
                                cardRef = splitCardRef,
                                upiRef = splitUpiRef
                            )
                        }

                        onCompleteCheckout(
                            customerName.trim(),
                            customerPhone.trim(),
                            selectedMode,
                            split
                        )
                    },
                    enabled = isReadyToCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        disabledContainerColor = PrimaryGreen.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReadyToCheckout) "COMPLETE SALE (₹${String.format(Locale.US, "%.2f", grandTotal)})" else "INSUFFICIENT PAYMENT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen) else null,
        modifier = modifier.height(64.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
