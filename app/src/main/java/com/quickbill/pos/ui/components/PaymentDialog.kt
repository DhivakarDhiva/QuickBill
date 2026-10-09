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

package com.quickbill.pos.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quickbill.pos.data.model.CartSummary
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.model.PaymentSplit
import com.quickbill.pos.data.util.BillingCalculator
import com.quickbill.pos.ui.theme.*
import java.util.Locale

// Data class to represent flexible payment line items in the UI
data class PaymentLineItem(
    val id: Int,
    var mode: PaymentMode,
    var amountText: String
)

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
    var isSplitPayment by remember { mutableStateOf(false) }
    var singleMode by remember { mutableStateOf(PaymentMode.CASH) }

    // Customer details
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }

    // Cash single payment: Tendered & Change
    var cashTenderedText by remember { mutableStateOf(String.format(Locale.US, "%.0f", grandTotal)) }

    // Split payment line items & cash tendered
    var splitCashTenderedText by remember { mutableStateOf("") }
    var paymentLines by remember {
        mutableStateOf(
            listOf(
                PaymentLineItem(1, PaymentMode.CASH, String.format(Locale.US, "%.0f", grandTotal * 0.6)),
                PaymentLineItem(2, PaymentMode.UPI, String.format(Locale.US, "%.0f", grandTotal * 0.4))
            )
        )
    }

    // Calculations
    val singleCashTendered = cashTenderedText.toDoubleOrNull() ?: 0.0
    val singleCashChange = if (singleMode == PaymentMode.CASH && singleCashTendered > grandTotal) {
        BillingCalculator.round2(singleCashTendered - grandTotal)
    } else 0.0

    // Split calculations
    val splitTotalAllocated = if (isSplitPayment) {
        paymentLines.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    } else {
        grandTotal
    }
    val splitRemaining = BillingCalculator.round2(maxOf(0.0, grandTotal - splitTotalAllocated))
    val splitCashAllocated = if (isSplitPayment) {
        paymentLines.filter { it.mode == PaymentMode.CASH }.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    } else 0.0
    val splitCashTendered = if (splitCashTenderedText.isBlank()) splitCashAllocated else (splitCashTenderedText.toDoubleOrNull() ?: splitCashAllocated)
    val splitCashChange = if (isSplitPayment && splitCashAllocated > 0.0 && splitCashTendered > splitCashAllocated) {
        BillingCalculator.round2(splitCashTendered - splitCashAllocated)
    } else 0.0

    val isReadyToCheckout = if (!isSplitPayment) {
        if (singleMode == PaymentMode.CASH) singleCashTendered >= (grandTotal - 0.01) else true
    } else {
        splitTotalAllocated >= (grandTotal - 0.01) && (splitCashAllocated <= 0.0 || splitCashTendered >= (splitCashAllocated - 0.01))
    }

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Payment",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                        )
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        QuickBillButton(
                            text = "Complete Order • ₹${String.format(Locale.US, "%.2f", grandTotal)}",
                            onClick = {
                                if (isSplitPayment) {
                                    var cashVal = 0.0
                                    var cardVal = 0.0
                                    var upiVal = 0.0
                                    paymentLines.forEach { line ->
                                        val amt = line.amountText.toDoubleOrNull() ?: 0.0
                                        when (line.mode) {
                                            PaymentMode.CASH -> cashVal += amt
                                            PaymentMode.CARD -> cardVal += amt
                                            PaymentMode.UPI -> upiVal += amt
                                            PaymentMode.SPLIT -> {}
                                        }
                                    }
                                    val splitObj = PaymentSplit(
                                        cashAmount = cashVal,
                                        cardAmount = cardVal,
                                        upiAmount = upiVal,
                                        cashTendered = if (cashVal > 0.0) splitCashTendered else 0.0
                                    )
                                    onCompleteCheckout(customerName, customerPhone, PaymentMode.SPLIT, splitObj)
                                } else {
                                    val splitObj = when (singleMode) {
                                        PaymentMode.CASH -> PaymentSplit(
                                            cashAmount = grandTotal,
                                            cashTendered = singleCashTendered
                                        )
                                        PaymentMode.CARD -> PaymentSplit(cardAmount = grandTotal)
                                        PaymentMode.UPI -> PaymentSplit(upiAmount = grandTotal)
                                        PaymentMode.SPLIT -> PaymentSplit()
                                    }
                                    onCompleteCheckout(customerName, customerPhone, singleMode, splitObj)
                                }
                            },
                            enabled = isReadyToCheckout,
                            containerColor = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarmBackgroundLight)
                    .padding(paddingValues)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // =========================================================================
                    // Grand Total Banner Card (Screen 7 Mockup)
                    // =========================================================================
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, OutlineLight),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Grand Total",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryLight
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "₹ ${String.format(Locale.US, "%.2f", grandTotal)}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimaryLight
                                )
                            )
                        }
                    }

                    // =========================================================================
                    // Payment Method Selector (Cash, Card, UPI)
                    // Matches mockup 7: Cards with clean icon & border
                    // =========================================================================
                    if (!isSplitPayment) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(1),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Payment Method",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PaymentMethodChip(
                                    name = "Cash",
                                    icon = Icons.Default.Payments,
                                    selected = singleMode == PaymentMode.CASH,
                                    onClick = { singleMode = PaymentMode.CASH },
                                    modifier = Modifier.weight(1f)
                                )

                                PaymentMethodChip(
                                    name = "Card",
                                    icon = Icons.Default.CreditCard,
                                    selected = singleMode == PaymentMode.CARD,
                                    onClick = { singleMode = PaymentMode.CARD },
                                    modifier = Modifier.weight(1f)
                                )

                                PaymentMethodChip(
                                    name = "UPI",
                                    icon = Icons.Default.QrCode,
                                    selected = singleMode == PaymentMode.UPI,
                                    onClick = { singleMode = PaymentMode.UPI },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // Split Payment Toggle Switch
                    // =========================================================================
                    QuickBillCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Split Payment",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryLight
                                    )
                                )
                                Text(
                                    text = "Accept multiple modes (Cash + UPI / Card)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondaryLight
                                    )
                                )
                            }

                            Switch(
                                checked = isSplitPayment,
                                onCheckedChange = { isSplitPayment = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }

                        // Split Payment Rows
                        if (isSplitPayment) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Add Payment",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            paymentLines.forEachIndexed { index, line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Mode Selector
                                    var modeDropdownExpanded by remember { mutableStateOf(false) }
                                    Surface(
                                        onClick = { modeDropdownExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = SurfaceMutedLight,
                                        border = BorderStroke(1.dp, OutlineLight),
                                        modifier = Modifier.width(110.dp).height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = line.mode.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryLight)
                                        }

                                        DropdownMenu(
                                            expanded = modeDropdownExpanded,
                                            onDismissRequest = { modeDropdownExpanded = false }
                                        ) {
                                            listOf(PaymentMode.CASH, PaymentMode.CARD, PaymentMode.UPI).forEach { m ->
                                                DropdownMenuItem(
                                                    text = { Text(m.name) },
                                                    onClick = {
                                                        val updated = paymentLines.toMutableList()
                                                        updated[index] = line.copy(mode = m)
                                                        paymentLines = updated
                                                        modeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Amount Field
                                    OutlinedTextField(
                                        value = line.amountText,
                                        onValueChange = { newVal ->
                                            val updated = paymentLines.toMutableList()
                                            updated[index] = line.copy(amountText = newVal)
                                            paymentLines = updated
                                        },
                                        placeholder = { Text("Amount") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    )

                                    // Remove row icon
                                    if (paymentLines.size > 1) {
                                        IconButton(
                                            onClick = {
                                                paymentLines = paymentLines.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CoralAccent)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // "+ Add Another Payment" Button
                            TextButton(
                                onClick = {
                                    val newId = (paymentLines.maxOfOrNull { it.id } ?: 0) + 1
                                    val remainingAmt = String.format(Locale.US, "%.0f", splitRemaining)
                                    paymentLines = paymentLines + PaymentLineItem(newId, PaymentMode.UPI, remainingAmt)
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Add Another Payment",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // Single Cash Amount Tendered & Change Due
                    // =========================================================================
                    if (!isSplitPayment && singleMode == PaymentMode.CASH) {
                        QuickBillCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Amount Tendered",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            QuickBillTextField(
                                value = cashTenderedText,
                                onValueChange = { cashTenderedText = it },
                                placeholder = "e.g. 500",
                                leadingIcon = Icons.Default.CurrencyRupee,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick Presets (Exact, +50, +100, +500)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(grandTotal, grandTotal + 50, grandTotal + 100, 500.0).distinct().forEach { preset ->
                                    Surface(
                                        onClick = { cashTenderedText = String.format(Locale.US, "%.0f", preset) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceMutedLight,
                                        border = BorderStroke(1.dp, OutlineLight),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.0f", preset)}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryLight
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (singleCashChange > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change Due",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", singleCashChange)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Split Cash Amount Tendered & Change Due
                    if (isSplitPayment && splitCashAllocated > 0) {
                        QuickBillCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Cash Tendered (for Cash portion: ₹${String.format(Locale.US, "%.2f", splitCashAllocated)})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            QuickBillTextField(
                                value = splitCashTenderedText,
                                onValueChange = { splitCashTenderedText = it },
                                placeholder = "e.g. ${String.format(Locale.US, "%.0f", splitCashAllocated)}",
                                leadingIcon = Icons.Default.CurrencyRupee,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (splitCashChange > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change Due",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", splitCashChange)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // =========================================================================
                    // Financial Summary Breakdown (Total Paid / Remaining)
                    // Matches mockup 7: Total Paid, Remaining
                    // =========================================================================
                    QuickBillCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Paid",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
                            )
                            Text(
                                text = "₹ ${String.format(Locale.US, "%.2f", if (isSplitPayment) splitTotalAllocated else grandTotal)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
                            )
                            Text(
                                text = "₹ ${String.format(Locale.US, "%.2f", if (isSplitPayment) splitRemaining else 0.0)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSplitPayment && splitRemaining > 0) CoralAccent else EmeraldPrimary
                                )
                            )
                        }
                    }

                    // Optional Customer Details Accordion
                    var showCustomerFields by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.staggeredEntrance(3)) {
                        TextButton(
                            onClick = { showCustomerFields = !showCustomerFields },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (showCustomerFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showCustomerFields) "Hide Customer Info" else "+ Add Customer Info (Optional)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondaryLight
                                )
                            )
                        }

                        if (showCustomerFields) {
                            Spacer(modifier = Modifier.height(6.dp))
                            QuickBillTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                placeholder = "Customer Name",
                                leadingIcon = Icons.Default.PersonOutline,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            QuickBillTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                placeholder = "Phone Number",
                                leadingIcon = Icons.Default.Phone,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

// Payment method selection chip with selected emerald border
@Composable
private fun PaymentMethodChip(
    name: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) EmeraldPrimary else OutlineLight
        ),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.height(68.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (selected) EmeraldPrimary else TextSecondaryLight,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) EmeraldPrimary else TextPrimaryLight
                )
            )
        }
    }
}
