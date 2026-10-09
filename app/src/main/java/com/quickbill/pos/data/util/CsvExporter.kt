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

package com.quickbill.pos.data.util

import android.content.Context
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.model.DailyReportData
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportBillsToCsv(context: Context, bills: List<BillEntity>): File {
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "Sales_Report_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.append("Bill Number,Date,Cashier,Customer Name,Customer Phone,Subtotal,Discount,CGST,SGST,Total Tax,Grand Total,Payment Mode,Status\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (bill in bills) {
                val dateStr = dateFormat.format(Date(bill.timestamp))
                writer.append("\"${bill.billNumber}\",")
                writer.append("\"$dateStr\",")
                writer.append("\"${bill.cashierName.replace("\"", "\"\"")}\",")
                writer.append("\"${bill.customerName.replace("\"", "\"\"")}\",")
                writer.append("\"${bill.customerPhone}\",")
                writer.append("${bill.subtotal},")
                writer.append("${bill.discountAmount},")
                writer.append("${bill.cgstAmount},")
                writer.append("${bill.sgstAmount},")
                writer.append("${bill.taxAmount},")
                writer.append("${bill.grandTotal},")
                writer.append("\"${bill.paymentMode.name}\",")
                writer.append("\"${bill.status.name}\"\n")
            }
        }
        return file
    }

    fun exportProductsToCsv(context: Context, products: List<ProductEntity>): File {
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "Inventory_Export_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.append("ID,Product Name,SKU / Barcode,Category,Price (₹),Tax Rate (%),Stock Quantity,Min Stock Alert\n")
            for (p in products) {
                writer.append("${p.id},")
                writer.append("\"${p.name.replace("\"", "\"\"")}\",")
                writer.append("\"${p.sku}\",")
                writer.append("\"${p.category.replace("\"", "\"\"")}\",")
                writer.append("${p.price},")
                writer.append("${p.taxRate},")
                writer.append("${p.stockQuantity},")
                writer.append("${p.minStockAlert}\n")
            }
        }
        return file
    }

    fun exportDailyReportToCsv(context: Context, report: DailyReportData): File {
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "Daily_Summary_$timestamp.csv")

        FileWriter(file).use { writer ->
            writer.append("Metric,Value\n")
            writer.append("Period,\"${report.dateLabel}\"\n")
            writer.append("Total Sales (₹),${report.totalSales}\n")
            writer.append("Total Bills,${report.totalBillsCount}\n")
            writer.append("Completed Bills,${report.completedBillsCount}\n")
            writer.append("Refunded Bills,${report.refundedBillsCount}\n")
            writer.append("Refunded Amount (₹),${report.refundedAmount}\n")
            writer.append("Cash Sales (₹),${report.cashSales}\n")
            writer.append("Card Sales (₹),${report.cardSales}\n")
            writer.append("UPI Sales (₹),${report.upiSales}\n\n")

            writer.append("Top 5 Selling Items\n")
            writer.append("Rank,Product Name,SKU,Quantity Sold,Revenue (₹)\n")
            report.topSellingItems.forEachIndexed { index, item ->
                writer.append("${index + 1},\"${item.productName.replace("\"", "\"\"")}\",\"${item.sku}\",${item.totalQty},${item.totalRevenue}\n")
            }
        }
        return file
    }

    fun exportDailyReportToExcel(context: Context, report: DailyReportData): File {
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "Daily_Report_$timestamp.xls")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Sales Summary</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
                <style>
                    body { font-family: Calibri, Arial, sans-serif; font-size: 11pt; }
                    .title { font-size: 16pt; font-weight: bold; color: #0F5132; padding: 10px 0; }
                    .subtitle { font-size: 11pt; color: #555555; margin-bottom: 12px; }
                    .section-header { font-size: 13pt; font-weight: bold; color: #0F5132; margin-top: 20px; }
                    table { border-collapse: collapse; margin-top: 8px; width: 100%; }
                    th { background-color: #0F5132; color: #FFFFFF; font-weight: bold; border: 1px solid #0B3D26; padding: 8px 12px; text-align: left; }
                    td { border: 1px solid #D0D0D0; padding: 6px 12px; }
                    .metric-name { font-weight: 600; background-color: #F8F9FA; width: 260px; }
                    .num { text-align: right; }
                    .total-row { font-weight: bold; background-color: #E8F5E9; color: #0F5132; }
                    .alt-row { background-color: #F9FAF8; }
                </style>
                </head>
                <body>
                    <div class="title">QuickBill POS - Daily Sales Report</div>
                    <div class="subtitle">Period: <strong>${report.dateLabel}</strong> | Generated on: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}</div>
                    
                    <table>
                        <tr><th colspan="2">Executive Summary</th></tr>
                        <tr><td class="metric-name">Period</td><td>${report.dateLabel}</td></tr>
                        <tr class="total-row"><td class="metric-name">Total Sales (Gross)</td><td class="num">₹ ${String.format(Locale.US, "%.2f", report.totalSales)}</td></tr>
                        <tr><td class="metric-name">Total Bills Issued</td><td class="num">${report.totalBillsCount}</td></tr>
                        <tr><td class="metric-name">Completed Bills</td><td class="num">${report.completedBillsCount}</td></tr>
                        <tr><td class="metric-name">Refunded Bills</td><td class="num">${report.refundedBillsCount}</td></tr>
                        <tr><td class="metric-name">Refunded Amount</td><td class="num">₹ ${String.format(Locale.US, "%.2f", report.refundedAmount)}</td></tr>
                        <tr><td class="metric-name">Cash Sales</td><td class="num">₹ ${String.format(Locale.US, "%.2f", report.cashSales)}</td></tr>
                        <tr><td class="metric-name">Card Sales</td><td class="num">₹ ${String.format(Locale.US, "%.2f", report.cardSales)}</td></tr>
                        <tr><td class="metric-name">UPI / QR Sales</td><td class="num">₹ ${String.format(Locale.US, "%.2f", report.upiSales)}</td></tr>
                    </table>

                    <br/>
                    <div class="section-header">Top Selling Items</div>
                    <table>
                        <tr>
                            <th style="width: 50px;">Rank</th>
                            <th>Product Name</th>
                            <th>SKU</th>
                            <th class="num">Quantity Sold</th>
                            <th class="num">Total Revenue (₹)</th>
                        </tr>
            """.trimIndent())

            report.topSellingItems.forEachIndexed { idx, item ->
                val rowClass = if (idx % 2 == 1) " class=\"alt-row\"" else ""
                w.write("""
                        <tr$rowClass>
                            <td style="text-align: center;">${idx + 1}</td>
                            <td><strong>${item.productName}</strong></td>
                            <td>${item.sku}</td>
                            <td class="num">${item.totalQty}</td>
                            <td class="num">₹ ${String.format(Locale.US, "%.2f", item.totalRevenue)}</td>
                        </tr>
                """.trimIndent())
            }

            w.write("""
                    </table>
                </body>
                </html>
            """.trimIndent())
        }
        return file
    }

    fun exportBillsToExcel(context: Context, bills: List<BillEntity>): File {
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "Sales_Transactions_$timestamp.xls")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Sales History</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
                <style>
                    body { font-family: Calibri, Arial, sans-serif; font-size: 11pt; }
                    .title { font-size: 15pt; font-weight: bold; color: #0F5132; margin-bottom: 8px; }
                    table { border-collapse: collapse; margin-top: 8px; width: 100%; }
                    th { background-color: #0F5132; color: #FFFFFF; font-weight: bold; border: 1px solid #0B3D26; padding: 7px 10px; text-align: left; }
                    td { border: 1px solid #D0D0D0; padding: 6px 10px; }
                    .num { text-align: right; }
                    .alt { background-color: #F8F9FA; }
                </style>
                </head>
                <body>
                    <div class="title">QuickBill POS - Transactions Ledger</div>
                    <table>
                        <tr>
                            <th>Bill #</th>
                            <th>Date & Time</th>
                            <th>Cashier</th>
                            <th>Customer</th>
                            <th>Phone</th>
                            <th class="num">Subtotal (₹)</th>
                            <th class="num">Discount (₹)</th>
                            <th class="num">Tax (₹)</th>
                            <th class="num">Grand Total (₹)</th>
                            <th>Payment Mode</th>
                            <th>Status</th>
                        </tr>
            """.trimIndent())

            bills.forEachIndexed { idx, bill ->
                val alt = if (idx % 2 == 1) " class=\"alt\"" else ""
                w.write("""
                        <tr$alt>
                            <td><strong>${bill.billNumber}</strong></td>
                            <td>${dateFormat.format(Date(bill.timestamp))}</td>
                            <td>${bill.cashierName}</td>
                            <td>${bill.customerName}</td>
                            <td>${bill.customerPhone}</td>
                            <td class="num">${String.format(Locale.US, "%.2f", bill.subtotal)}</td>
                            <td class="num">${String.format(Locale.US, "%.2f", bill.discountAmount)}</td>
                            <td class="num">${String.format(Locale.US, "%.2f", bill.taxAmount)}</td>
                            <td class="num"><strong>${String.format(Locale.US, "%.2f", bill.grandTotal)}</strong></td>
                            <td>${bill.paymentMode.name}</td>
                            <td>${bill.status.name}</td>
                        </tr>
                """.trimIndent())
            }

            w.write("""
                    </table>
                </body>
                </html>
            """.trimIndent())
        }
        return file
    }

    fun exportProductsToExcel(context: Context, products: List<ProductEntity>): File {
        val outputDir = File(context.cacheDir, "exports")
        if (!outputDir.exists()) outputDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "Inventory_Catalog_$timestamp.xls")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("""
                <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Inventory</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
                <style>
                    body { font-family: Calibri, Arial, sans-serif; font-size: 11pt; }
                    .title { font-size: 15pt; font-weight: bold; color: #0F5132; margin-bottom: 8px; }
                    table { border-collapse: collapse; margin-top: 8px; width: 100%; }
                    th { background-color: #0F5132; color: #FFFFFF; font-weight: bold; border: 1px solid #0B3D26; padding: 7px 10px; text-align: left; }
                    td { border: 1px solid #D0D0D0; padding: 6px 10px; }
                    .num { text-align: right; }
                    .alt { background-color: #F8F9FA; }
                    .low-stock { color: #D32F2F; font-weight: bold; }
                </style>
                </head>
                <body>
                    <div class="title">QuickBill POS - Inventory Stock List</div>
                    <table>
                        <tr>
                            <th>ID</th>
                            <th>Product Name</th>
                            <th>SKU / Barcode</th>
                            <th>Category</th>
                            <th class="num">Price (₹)</th>
                            <th class="num">Tax Rate (%)</th>
                            <th class="num">Stock Quantity</th>
                            <th class="num">Min Alert</th>
                        </tr>
            """.trimIndent())

            products.forEachIndexed { idx, p ->
                val alt = if (idx % 2 == 1) " class=\"alt\"" else ""
                val stockClass = if (p.stockQuantity <= p.minStockAlert) " class=\"low-stock num\"" else " class=\"num\""
                w.write("""
                        <tr$alt>
                            <td>${p.id}</td>
                            <td><strong>${p.name}</strong></td>
                            <td>${p.sku}</td>
                            <td>${p.category}</td>
                            <td class="num">₹ ${String.format(Locale.US, "%.2f", p.price)}</td>
                            <td class="num">${p.taxRate}%</td>
                            <td$stockClass>${p.stockQuantity}</td>
                            <td class="num">${p.minStockAlert}</td>
                        </tr>
                """.trimIndent())
            }

            w.write("""
                    </table>
                </body>
                </html>
            """.trimIndent())
        }
        return file
    }
}
