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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.quickbill.pos.data.model.BillWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptGenerator {

    fun generateReceiptPdf(context: Context, billWithDetails: BillWithDetails): File {
        val bill = billWithDetails.bill
        val items = billWithDetails.items
        val payments = billWithDetails.payments

        val pageWidth = 400
        val baseHeight = 450 + (items.size * 32) + (payments.size * 25)
        val pageHeight = maxOf(600, baseHeight)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Paints
        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B5E20") // Emerald
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val centerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val rightTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val rightBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 30f
        val centerX = pageWidth / 2f
        val margin = 20f
        val rightMargin = pageWidth - 20f

        // Header
        canvas.drawText("QUICKBILL SUPERMARKET", centerX, y, titlePaint)
        y += 16f
        canvas.drawText("124 Commercial Street, Tech City", centerX, y, centerPaint)
        y += 14f
        canvas.drawText("GSTIN: 29AAAAA0000A1Z5 | Ph: +91 98765 43210", centerX, y, centerPaint)
        y += 18f

        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 16f

        // Bill Meta
        canvas.drawText("Bill No: ${bill.billNumber}", margin, y, boldPaint)
        val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(bill.timestamp))
        canvas.drawText(dateStr, rightMargin, y, rightTextPaint)
        y += 15f

        canvas.drawText("Cashier: ${bill.cashierName}", margin, y, textPaint)
        if (bill.customerName.isNotBlank()) {
            canvas.drawText("Customer: ${bill.customerName}", rightMargin, y, rightTextPaint)
        }
        y += 18f

        // Status badge if refunded
        if (bill.status.name == "REFUNDED") {
            val voidPaint = Paint().apply {
                color = Color.RED
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** BILL REFUNDED / VOID ***", centerX, y, voidPaint)
            y += 18f
        }

        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 15f

        // Table Header
        canvas.drawText("ITEM", margin, y, boldPaint)
        canvas.drawText("QTY", margin + 180f, y, boldPaint)
        canvas.drawText("PRICE", margin + 240f, y, boldPaint)
        canvas.drawText("TOTAL", rightMargin, y, rightBoldPaint)
        y += 8f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 14f

        // Table Rows
        for (item in items) {
            val itemName = if (item.productName.length > 20) item.productName.take(18) + ".." else item.productName
            canvas.drawText(itemName, margin, y, textPaint)
            canvas.drawText("${item.quantity}", margin + 185f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), margin + 240f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.lineTotal), rightMargin, y, rightTextPaint)
            y += 14f

            // Show tax & discount subline if present
            if (item.taxRate > 0 || item.itemDiscountAmount > 0) {
                var detail = "GST ${item.taxRate}% (₹${String.format(Locale.US, "%.2f", item.taxAmount)})"
                if (item.itemDiscountAmount > 0) {
                    detail += " | Disc: -₹${String.format(Locale.US, "%.2f", item.itemDiscountAmount)}"
                }
                val sublinePaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 8.5f
                }
                canvas.drawText(detail, margin + 8f, y, sublinePaint)
                y += 14f
            }
        }

        y += 5f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 16f

        // Totals
        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false) {
            val paintL = if (isBold) boldPaint else textPaint
            val paintR = if (isBold) rightBoldPaint else rightTextPaint
            canvas.drawText(label, margin + 120f, y, paintL)
            canvas.drawText(value, rightMargin, y, paintR)
            y += 15f
        }

        drawSummaryRow("Subtotal:", "₹" + String.format(Locale.US, "%.2f", bill.subtotal))
        if (bill.discountAmount > 0) {
            drawSummaryRow("Discount:", "-₹" + String.format(Locale.US, "%.2f", bill.discountAmount))
        }
        if (bill.cgstAmount > 0) {
            drawSummaryRow("CGST:", "₹" + String.format(Locale.US, "%.2f", bill.cgstAmount))
        }
        if (bill.sgstAmount > 0) {
            drawSummaryRow("SGST:", "₹" + String.format(Locale.US, "%.2f", bill.sgstAmount))
        }

        y += 4f
        canvas.drawLine(margin + 120f, y, rightMargin, y, linePaint)
        y += 16f

        val grandTotalPaintL = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val grandTotalPaintR = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("GRAND TOTAL:", margin + 120f, y, grandTotalPaintL)
        canvas.drawText("₹" + String.format(Locale.US, "%.2f", bill.grandTotal), rightMargin, y, grandTotalPaintR)
        y += 20f

        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 15f

        // Payment Info
        canvas.drawText("PAYMENT METHOD: ${bill.paymentMode.name}", margin, y, boldPaint)
        y += 14f
        for (payment in payments) {
            val pDesc = "${payment.mode.name}: ₹${String.format(Locale.US, "%.2f", payment.amount)}"
            val pNote = if (payment.referenceNote.isNotBlank()) " (${payment.referenceNote})" else ""
            canvas.drawText(pDesc + pNote, margin + 8f, y, textPaint)
            y += 13f
        }

        if (bill.cashTendered > 0) {
            canvas.drawText("Cash Tendered: ₹${String.format(Locale.US, "%.2f", bill.cashTendered)} | Change Due: ₹${String.format(Locale.US, "%.2f", bill.changeDue)}", margin + 8f, y, textPaint)
            y += 14f
        }

        y += 12f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 20f

        // Footer
        canvas.drawText("Thank you for shopping with us!", centerX, y, centerPaint)
        y += 13f
        canvas.drawText("Please preserve this bill for returns/warranty", centerX, y, centerPaint)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "receipts")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "Receipt_${bill.billNumber}.pdf")

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }
}
