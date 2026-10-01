package com.quickbill.pos.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.util.PdfReceiptGenerator
import com.quickbill.pos.ui.theme.PrimaryGreen
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Success check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sale Completed!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Thermal-Receipt Style Visual Card
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "QUICKBILL SUPERMARKET",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                        Text(
                            text = "124 Commercial Street, Tech City",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                        )
                        Text(
                            text = "GSTIN: 29AAAAA0000A1Z5 | Ph: 9876543210",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Bill Meta
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bill: ${bill.billNumber}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(bill.timestamp))
                            Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cashier: ${bill.cashierName}", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                            if (bill.customerName.isNotBlank()) {
                                Text("Customer: ${bill.customerName}", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Items Table Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Item", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            Text("Qty", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            Text("Rate", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            Text("Total", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Item Rows
                        items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(item.productName, modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
                                Text("${item.quantity}", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
                                Text("₹${String.format(Locale.US, "%.1f", item.unitPrice)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
                                Text("₹${String.format(Locale.US, "%.2f", item.lineTotal)}", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color.Black))
                            }
                            if (item.taxRate > 0 || item.itemDiscountAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
                                    Text(
                                        text = "GST ${item.taxRate}% (₹${String.format(Locale.US, "%.2f", item.taxAmount)})" + if (item.itemDiscountAmount > 0) " | Disc: -₹${String.format(Locale.US, "%.2f", item.itemDiscountAmount)}" else "",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Totals
                        ReceiptAmountRow("Subtotal", "₹${String.format(Locale.US, "%.2f", bill.subtotal)}")
                        if (bill.discountAmount > 0) {
                            ReceiptAmountRow("Bill Discount", "-₹${String.format(Locale.US, "%.2f", bill.discountAmount)}")
                        }
                        if (bill.cgstAmount > 0) {
                            ReceiptAmountRow("CGST", "₹${String.format(Locale.US, "%.2f", bill.cgstAmount)}")
                        }
                        if (bill.sgstAmount > 0) {
                            ReceiptAmountRow("SGST", "₹${String.format(Locale.US, "%.2f", bill.sgstAmount)}")
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GRAND TOTAL", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            Text("₹${String.format(Locale.US, "%.2f", bill.grandTotal)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20)))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment Details
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Mode:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                            Text(bill.paymentMode.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                        }
                        payments.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  ${p.mode.name}", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                                Text("₹${String.format(Locale.US, "%.2f", p.amount)}", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                            }
                        }
                        if (bill.cashTendered > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  Cash Tendered", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                                Text("₹${String.format(Locale.US, "%.2f", bill.cashTendered)}", style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("  Change Returned", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)))
                                Text("₹${String.format(Locale.US, "%.2f", bill.changeDue)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Thank you for shopping with us!", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Actions: Share PDF & New Sale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sharePdfReceipt(context, billWithDetails)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }

                    Button(
                        onClick = onNewSale,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("New Sale", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptAmountRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
    }
}

@Composable
private fun DottedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray, letterSpacing = 2.sp),
        maxLines = 1
    )
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
