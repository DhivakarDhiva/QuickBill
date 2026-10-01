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
}
