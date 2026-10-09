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

package com.quickbill.pos.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.DailyReportData
import com.quickbill.pos.ui.components.*
import com.quickbill.pos.ui.theme.*
import java.util.Locale

@Composable
fun DailyReportScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSales: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.reportData

    var showSalesBreakdownDialog by remember { mutableStateOf(false) }
    var showBillsSummaryDialog by remember { mutableStateOf(false) }
    var showItemsSoldDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        viewModel.loadReport(uiState.selectedPeriod)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header matching Screen 12 Mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(0),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Report",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    )
                    Text(
                        text = "Analytics for ${uiState.selectedPeriod.label}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                    )
                }

                // Date selector pill with calendar icon matching mockup 12
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, OutlineLight)
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
                            text = report?.dateLabel ?: "Today",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryLight
                            )
                        )
                    }
                }
            }

            // Period Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(1)
            ) {
                items(ReportPeriod.values()) { period ->
                    QuickBillChip(
                        text = period.label,
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            } else if (report != null) {
                // =========================================================================
                // 4 KPI Cards (2x2 Grid matching Screen 12 Mockup)
                // Total Sales, Total Bills, Items Sold, Average Bill
                // Clicking opens interactive detailed breakdown sheets
                // =========================================================================
                val totalItemsCount = if (report.totalItemsSold > 0) report.totalItemsSold else report.topSellingItems.sumOf { it.totalQty }
                val avgBill = if (report.completedBillsCount > 0) report.totalSales / report.completedBillsCount else 0.0

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickBillKpiCard(
                            title = "Total Sales",
                            value = "₹ ${String.format(Locale.US, "%,.2f", report.totalSales)}",
                            icon = Icons.Default.CurrencyRupee,
                            iconColor = EmeraldPrimary,
                            iconBgColor = EmeraldContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { showSalesBreakdownDialog = true }
                        )

                        QuickBillKpiCard(
                            title = "Total Bills",
                            value = "${report.totalBillsCount}",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconColor = AccentBlue,
                            iconBgColor = AccentBlueContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { showBillsSummaryDialog = true }
                        )

                        QuickBillKpiCard(
                            title = "Items Sold",
                            value = "$totalItemsCount",
                            icon = Icons.Default.Inventory2,
                            iconColor = AccentPurple,
                            iconBgColor = AccentPurpleContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { showItemsSoldDialog = true }
                        )

                        QuickBillKpiCard(
                            title = "Average Bill",
                            value = "₹ ${String.format(Locale.US, "%.2f", avgBill)}",
                            icon = Icons.Default.Calculate,
                            iconColor = SuccessGreen,
                            iconBgColor = SuccessGreenContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { showBillsSummaryDialog = true }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickBillKpiCard(
                                title = "Total Sales",
                                value = "₹ ${String.format(Locale.US, "%,.2f", report.totalSales)}",
                                icon = Icons.Default.CurrencyRupee,
                                iconColor = EmeraldPrimary,
                                iconBgColor = EmeraldContainer,
                                modifier = Modifier.weight(1f),
                                onClick = { showSalesBreakdownDialog = true }
                            )

                            QuickBillKpiCard(
                                title = "Total Bills",
                                value = "${report.totalBillsCount}",
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                iconColor = AccentBlue,
                                iconBgColor = AccentBlueContainer,
                                modifier = Modifier.weight(1f),
                                onClick = { showBillsSummaryDialog = true }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickBillKpiCard(
                                title = "Items Sold",
                                value = "$totalItemsCount",
                                icon = Icons.Default.Inventory2,
                                iconColor = AccentPurple,
                                iconBgColor = AccentPurpleContainer,
                                modifier = Modifier.weight(1f),
                                onClick = { showItemsSoldDialog = true }
                            )

                            QuickBillKpiCard(
                                title = "Average Bill",
                                value = "₹ ${String.format(Locale.US, "%.2f", avgBill)}",
                                icon = Icons.Default.Calculate,
                                iconColor = SuccessGreen,
                                iconBgColor = SuccessGreenContainer,
                                modifier = Modifier.weight(1f),
                                onClick = { showBillsSummaryDialog = true }
                            )
                        }
                    }
                }

                // =========================================================================
                // Payment Mode Breakdown Card (Donut Chart & Legend)
                // Matches Screen 12 Mockup
                // =========================================================================
                QuickBillCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(4)
                ) {
                    Text(
                        text = "Payment Mode Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val totalPayments = report.cashSales + report.cardSales + report.upiSales
                    val cashPct = if (totalPayments > 0) (report.cashSales / totalPayments).toFloat() else 0.45f
                    val upiPct = if (totalPayments > 0) (report.upiSales / totalPayments).toFloat() else 0.35f
                    val cardPct = if (totalPayments > 0) (report.cardSales / totalPayments).toFloat() else 0.20f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart Graphic
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                cashFraction = cashPct,
                                upiFraction = upiPct,
                                cardFraction = cardPct,
                                modifier = Modifier.size(100.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Legend with Values & Percentages
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportPaymentLegendRow(
                                label = "Cash",
                                percentage = "${(cashPct * 100).toInt()}%",
                                amount = report.cashSales,
                                color = Color(0xFF10B981)
                            )
                            ReportPaymentLegendRow(
                                label = "UPI",
                                percentage = "${(upiPct * 100).toInt()}%",
                                amount = report.upiSales,
                                color = Color(0xFF8B5CF6)
                            )
                            ReportPaymentLegendRow(
                                label = "Card",
                                percentage = "${(cardPct * 100).toInt()}%",
                                amount = report.cardSales,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }

                // =========================================================================
                // Top 5 Selling Items Card
                // Matches Screen 12 Mockup: Ranked list with thumbnail, name, count
                // =========================================================================
                QuickBillCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(4)
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

                        TextButton(
                            onClick = { showItemsSoldDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (report.topSellingItems.isEmpty()) {
                        Text(
                            text = "No items sold during this period",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        report.topSellingItems.take(5).forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) CoralAccent else TextSecondaryLight
                                        ),
                                        modifier = Modifier.width(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    ProductThumbnail(
                                        productName = item.productName,
                                        category = "Groceries",
                                        size = 38.dp
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryLight
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", item.totalRevenue)}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight, fontSize = 11.sp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceMutedLight
                                ) {
                                    Text(
                                        text = "${item.totalQty} sold",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryLight
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (index < report.topSellingItems.take(5).size - 1) {
                                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // =========================================================================
                // Bottom Export Buttons (Screen 12 Mockup)
                // [Export CSV] [Export Excel]
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportToCsv(context) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export CSV",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportToExcel(context) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OutlineLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export Excel",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 30.dp else 110.dp))
        }

        // =========================================================================
        // Interactive Dialogs for KPI Clicks (Requirement 7)
        // =========================================================================
        if (showSalesBreakdownDialog && report != null) {
            SalesBreakdownDialog(
                report = report,
                onDismiss = { showSalesBreakdownDialog = false },
                onViewSales = onNavigateToSales
            )
        }

        if (showBillsSummaryDialog && report != null) {
            val avgBill = if (report.completedBillsCount > 0) report.totalSales / report.completedBillsCount else 0.0
            BillsSummaryDialog(
                report = report,
                avgBill = avgBill,
                onDismiss = { showBillsSummaryDialog = false },
                onViewSales = onNavigateToSales
            )
        }

        if (showItemsSoldDialog && report != null) {
            val totalItemsCount = if (report.totalItemsSold > 0) report.totalItemsSold else report.topSellingItems.sumOf { it.totalQty }
            ItemsSoldDialog(
                report = report,
                totalItemsCount = totalItemsCount,
                onDismiss = { showItemsSoldDialog = false }
            )
        }
    }
}

// =========================================================================
// Dialog 1: Sales Breakdown Dialog
// =========================================================================
@Composable
private fun SalesBreakdownDialog(
    report: DailyReportData,
    onDismiss: () -> Unit,
    onViewSales: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Total Sales Breakdown",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = report.dateLabel,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Highlighted Total Revenue Hero
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldContainer,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Net Realized Sales",
                            style = MaterialTheme.typography.labelMedium.copy(color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹ ${String.format(Locale.US, "%,.2f", report.totalSales)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                        )
                        Text(
                            text = "${report.completedBillsCount} completed transactions",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                        )
                    }
                }

                // Payment mode breakdown
                Text(
                    text = "By Payment Mode",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                )
                QuickBillPriceRow(label = "Cash Payments", amount = report.cashSales)
                QuickBillPriceRow(label = "Card Payments", amount = report.cardSales)
                QuickBillPriceRow(label = "UPI Payments", amount = report.upiSales)

                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

                // Revenue and Deductions
                Text(
                    text = "Revenue & Deductions",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                )
                val grossAmt = if (report.grossSales > 0.0) report.grossSales else (report.totalSales + report.totalDiscount - report.totalTax)
                QuickBillPriceRow(label = "Gross Sales (Subtotal)", amount = grossAmt)
                QuickBillPriceRow(
                    label = "Discounts Applied",
                    amount = report.totalDiscount,
                    isNegative = true,
                    valueColor = if (report.totalDiscount > 0) CoralAccent else null
                )
                QuickBillPriceRow(label = "Total GST / Taxes", amount = report.totalTax)

                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                QuickBillPriceRow(
                    label = "Net Realized Sales",
                    amount = report.totalSales,
                    isTotal = true,
                    valueColor = EmeraldPrimary
                )

                if (report.refundedBillsCount > 0) {
                    HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Refunded (${report.refundedBillsCount} bills)", style = MaterialTheme.typography.bodyMedium.copy(color = CoralAccent))
                        Text("- ₹${String.format(Locale.US, "%.2f", report.refundedAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CoralAccent))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onViewSales()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("View Sales History")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// =========================================================================
// Dialog 2: Bills Summary Dialog
// =========================================================================
@Composable
private fun BillsSummaryDialog(
    report: DailyReportData,
    avgBill: Double,
    onDismiss: () -> Unit,
    onViewSales: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Bills Summary",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = report.dateLabel,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentBlueContainer,
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Bills Generated",
                            style = MaterialTheme.typography.labelMedium.copy(color = AccentBlue, fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${report.totalBillsCount}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = AccentBlue)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Completed Bills", style = MaterialTheme.typography.bodyMedium)
                    Text("${report.completedBillsCount}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Refunded Bills", style = MaterialTheme.typography.bodyMedium)
                    Text("${report.refundedBillsCount}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (report.refundedBillsCount > 0) CoralAccent else TextSecondaryLight))
                }

                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Average Ticket Value", style = MaterialTheme.typography.bodyMedium)
                    Text("₹ ${String.format(Locale.US, "%.2f", avgBill)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Net Sales", style = MaterialTheme.typography.bodyMedium)
                    Text("₹ ${String.format(Locale.US, "%,.2f", report.totalSales)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onViewSales()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Open Sales Screen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// =========================================================================
// Dialog 3: Items Sold Breakdown Dialog
// =========================================================================
@Composable
private fun ItemsSoldDialog(
    report: DailyReportData,
    totalItemsCount: Int,
    onDismiss: () -> Unit
) {
    val items = report.allSoldItems.ifEmpty { report.topSellingItems }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Items Sold Breakdown",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$totalItemsCount Total Units Sold • ${report.dateLabel}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = "No items recorded as sold for this period.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    items.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (index < 3) EmeraldContainer else SurfaceMutedLight,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (index < 3) EmeraldPrimary else TextSecondaryLight
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.sku.isNotBlank()) {
                                            Text(
                                                text = "SKU: ${item.sku}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight, fontSize = 10.sp)
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AccentPurpleContainer
                                    ) {
                                        Text(
                                            text = "${item.totalQty} sold",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentPurple),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", item.totalRevenue)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Close")
            }
        }
    )
}

// =========================================================================
// Donut Chart Graphic & Legend
// =========================================================================

@Composable
private fun DonutChart(
    cashFraction: Float,
    upiFraction: Float,
    cardFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(diameter, diameter)

        val total = cashFraction + upiFraction + cardFraction
        val safeTotal = if (total > 0f) total else 1f

        val cashSweep = (cashFraction / safeTotal) * 360f
        val upiSweep = (upiFraction / safeTotal) * 360f
        val cardSweep = (cardFraction / safeTotal) * 360f

        var startAngle = -90f

        // Cash arc (Green)
        drawArc(
            color = Color(0xFF10B981),
            startAngle = startAngle,
            sweepAngle = cashSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += cashSweep

        // UPI arc (Purple)
        drawArc(
            color = Color(0xFF8B5CF6),
            startAngle = startAngle,
            sweepAngle = upiSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += upiSweep

        // Card arc (Blue)
        drawArc(
            color = Color(0xFF3B82F6),
            startAngle = startAngle,
            sweepAngle = cardSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ReportPaymentLegendRow(
    label: String,
    percentage: String,
    amount: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = percentage,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
            )
        }

        Text(
            text = "₹ ${String.format(Locale.US, "%,.0f", amount)}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
        )
    }
}
