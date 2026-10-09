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

package com.quickbill.pos.data.repository

import com.quickbill.pos.data.local.QuickBillDatabase
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.DailyReportData
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.util.BillingCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportRepository(
    private val database: QuickBillDatabase
) {
    private val billDao = database.billDao()
    private val billItemDao = database.billItemDao()
    private val billPaymentDao = database.billPaymentDao()

    suspend fun getDailyReport(timeInMillis: Long = System.currentTimeMillis()): DailyReportData = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        val dateLabel = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(startTime))
        getReportForRange(startTime, endTime, dateLabel)
    }

    suspend fun getReportForRange(
        startTime: Long,
        endTime: Long,
        label: String = "Report"
    ): DailyReportData = withContext(Dispatchers.IO) {
        val bills = billDao.getBillsByDateRangeSync(startTime, endTime)

        var totalSales = 0.0
        var totalBillsCount = bills.size
        var completedBillsCount = 0
        var refundedBillsCount = 0
        var refundedAmount = 0.0
        var grossSales = 0.0

        var cashSales = 0.0
        var cardSales = 0.0
        var upiSales = 0.0
        var totalTax = 0.0
        var totalDiscount = 0.0

        for (bill in bills) {
            if (bill.status == BillStatus.COMPLETED) {
                completedBillsCount++
                totalSales += bill.grandTotal
                grossSales += bill.subtotal
                totalTax += bill.taxAmount

                var effectiveDiscount = bill.discountAmount
                val billItems = billItemDao.getItemsForBill(bill.id)
                val itemDiscountSum = billItems.sumOf { it.itemDiscountAmount }
                if (effectiveDiscount <= 0.0 && itemDiscountSum > 0.0) {
                    effectiveDiscount = itemDiscountSum
                } else if (bill.discountType != com.quickbill.pos.data.model.DiscountType.NONE && bill.discountAmount > 0.0 && itemDiscountSum > 0.0) {
                    if (bill.discountAmount < (itemDiscountSum + bill.discountAmount - 0.001)) {
                        effectiveDiscount = bill.discountAmount + itemDiscountSum
                    }
                }
                totalDiscount += effectiveDiscount

                // Tally payment modes
                val payments = billPaymentDao.getPaymentsForBill(bill.id)
                if (payments.isNotEmpty()) {
                    for (payment in payments) {
                        when (payment.mode) {
                            PaymentMode.CASH -> cashSales += payment.amount
                            PaymentMode.CARD -> cardSales += payment.amount
                            PaymentMode.UPI -> upiSales += payment.amount
                            PaymentMode.SPLIT -> {}
                        }
                    }
                } else {
                    // Fallback to bill's primary paymentMode if payments table wasn't populated
                    when (bill.paymentMode) {
                        PaymentMode.CASH -> cashSales += bill.grandTotal
                        PaymentMode.CARD -> cardSales += bill.grandTotal
                        PaymentMode.UPI -> upiSales += bill.grandTotal
                        PaymentMode.SPLIT -> cashSales += bill.grandTotal
                    }
                }
            } else if (bill.status == BillStatus.REFUNDED) {
                refundedBillsCount++
                refundedAmount += bill.grandTotal
            }
        }

        val topSelling = billItemDao.getTopSellingItems(startTime, endTime, 5)
        val allSold = billItemDao.getAllSellingItems(startTime, endTime)
        val itemsSold = billItemDao.getTotalItemsSold(startTime, endTime)

        DailyReportData(
            dateLabel = label,
            grossSales = BillingCalculator.round2(grossSales),
            totalSales = BillingCalculator.round2(totalSales),
            totalBillsCount = totalBillsCount,
            completedBillsCount = completedBillsCount,
            refundedBillsCount = refundedBillsCount,
            refundedAmount = BillingCalculator.round2(refundedAmount),
            cashSales = BillingCalculator.round2(cashSales),
            cardSales = BillingCalculator.round2(cardSales),
            upiSales = BillingCalculator.round2(upiSales),
            topSellingItems = topSelling,
            totalItemsSold = itemsSold,
            allSoldItems = allSold,
            totalTax = BillingCalculator.round2(totalTax),
            totalDiscount = BillingCalculator.round2(totalDiscount)
        )
    }

    suspend fun getTodayBills(timeInMillis: Long = System.currentTimeMillis()): List<com.quickbill.pos.data.local.entity.BillEntity> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis
        billDao.getBillsByDateRangeSync(startTime, endTime)
    }

    suspend fun getItemsForBill(billId: Long): List<com.quickbill.pos.data.local.entity.BillItemEntity> = withContext(Dispatchers.IO) {
        billItemDao.getItemsForBill(billId)
    }
}
