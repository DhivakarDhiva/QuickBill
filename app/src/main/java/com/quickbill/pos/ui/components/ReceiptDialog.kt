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

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.util.PdfReceiptGenerator
import com.quickbill.pos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    billWithDetails: BillWithDetails,
    onDismiss: () -> Unit,
    onNewSale: () -> Unit
) {
    val context = LocalContext.current
    val bill = billWithDetails.bill
    val items = billWithDetails.items
    val payments = billWithDetails.payments

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
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryLight)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Receipt",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )
                        }

                        IconButton(
                            onClick = { sharePdfReceipt(context, billWithDetails) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = TextPrimaryLight
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom Actions: Share Receipt, Print & Complete Order (100% visible above gesture navigation)
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { sharePdfReceipt(context, billWithDetails) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OutlineLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = { sharePdfReceipt(context, billWithDetails) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OutlineLight),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }

                            Button(
                                onClick = onNewSale,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Complete Order / New Sale",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { sharePdfReceipt(context, billWithDetails) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, OutlineLight),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Receipt", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = { sharePdfReceipt(context, billWithDetails) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, OutlineLight),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Print", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Button(
                                onClick = onNewSale,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Order / New Sale",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarmBackgroundLight)
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // =========================================================================
                    // Realistic Thermal Retail Receipt Card (Screen 8 Mockup)
                    // =========================================================================
                    Surface(
                        color = ReceiptPaperWhite,
                        shape = ReceiptShape,
                        border = BorderStroke(1.dp, OutlineLight),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // QuickBill Logo + Brand Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                QuickBillLogoBadge(size = 32.dp, iconSize = 18.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "QuickBill",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ReceiptInkBlack,
                                        fontSize = 20.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "123 Main Road, Madurai",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                            )
                            Text(
                                text = "Ph: 98765 43210",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            ReceiptDottedLine()
                            Spacer(modifier = Modifier.height(14.dp))

                            // Metadata: Bill No, Date, Cashier
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(bill.timestamp))

                            ReceiptMetaRow(label = "Bill No", value = ": ${bill.billNumber}")
                            ReceiptMetaRow(label = "Date", value = ": $dateStr")
                            ReceiptMetaRow(label = "Cashier", value = ": ${bill.cashierName}")
                            if (bill.customerName.isNotBlank()) {
                                ReceiptMetaRow(label = "Customer", value = ": ${bill.customerName}")
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            ReceiptDottedLine()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Table Header: Item | Qty | Price | Total
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Item",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ReceiptInkBlack),
                                    modifier = Modifier.weight(1.8f)
                                )
                                Text(
                                    text = "Qty",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ReceiptInkBlack),
                                    modifier = Modifier.weight(0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Price",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ReceiptInkBlack),
                                    modifier = Modifier.weight(0.8f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ReceiptInkBlack),
                                    modifier = Modifier.weight(0.9f),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Items List
                            items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.bodySmall.copy(color = ReceiptInkBlack),
                                        modifier = Modifier.weight(1.8f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.quantity}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = ReceiptInkBlack),
                                        modifier = Modifier.weight(0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f", item.unitPrice),
                                        style = MaterialTheme.typography.bodySmall.copy(color = ReceiptInkBlack),
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f", item.lineTotal),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = ReceiptInkBlack),
                                        modifier = Modifier.weight(0.9f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            ReceiptDottedLine()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Subtotal, Discount, CGST, SGST
                            ReceiptAmountRow("Subtotal", String.format(Locale.US, "%.2f", bill.subtotal))
                            if (bill.discountAmount > 0) {
                                ReceiptAmountRow("Discount (${bill.discountValue.toInt()}%)", "-${String.format(Locale.US, "%.2f", bill.discountAmount)}", isDiscount = true)
                            }
                            if (bill.cgstAmount > 0) {
                                ReceiptAmountRow("CGST (2.5%)", String.format(Locale.US, "%.2f", bill.cgstAmount))
                            }
                            if (bill.sgstAmount > 0) {
                                ReceiptAmountRow("SGST (2.5%)", String.format(Locale.US, "%.2f", bill.sgstAmount))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Grand Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Grand Total",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ReceiptInkBlack
                                    )
                                )
                                Text(
                                    text = "₹ ${String.format(Locale.US, "%.2f", bill.grandTotal)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ReceiptInkBlack
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Payment breakdown
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Payment: ${bill.paymentMode.name}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF444444)
                                    )
                                )
                            }
                            payments.forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "   • ${p.mode.name}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF666666))
                                    )
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", p.amount)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF666666))
                                    )
                                }
                            }

                            if (bill.cashTendered > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "   • Cash Tendered",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF555555))
                                    )
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", bill.cashTendered)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF555555))
                                    )
                                }
                            }

                            if (bill.changeDue > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "   • Change Due",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    )
                                    Text(
                                        text = "₹ ${String.format(Locale.US, "%.2f", bill.changeDue)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Thank you!",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ReceiptInkBlack
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stylized Barcode Graphic
                            BarcodeVisual(billNumber = bill.billNumber)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

@Composable
private fun ReceiptMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555)),
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = ReceiptInkBlack)
        )
    }
}

@Composable
private fun ReceiptAmountRow(label: String, amount: String, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isDiscount) CoralAccent else ReceiptInkBlack
            )
        )
    }
}

@Composable
private fun ReceiptDottedLine() {
    Text(
        text = "--------------------------------------------------------",
        style = MaterialTheme.typography.bodySmall.copy(
            color = ReceiptDottedDivider,
            letterSpacing = 1.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}

// Stylized barcode illustration for retail receipt aesthetic
@Composable
private fun BarcodeVisual(billNumber: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .width(180.dp)
                .height(34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pattern = listOf(3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 2, 4, 1, 3)
            pattern.forEachIndexed { idx, barWidth ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((barWidth * 1.5).dp)
                        .background(if (idx % 2 == 0) Color.Black else Color.Transparent)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = billNumber,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.DarkGray,
                letterSpacing = 2.sp,
                fontSize = 9.sp
            )
        )
    }
}

private fun sharePdfReceipt(context: Context, billWithDetails: BillWithDetails) {
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
        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
