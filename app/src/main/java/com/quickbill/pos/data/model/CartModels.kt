package com.quickbill.pos.data.model

import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.local.entity.BillItemEntity
import com.quickbill.pos.data.local.entity.BillPaymentEntity
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.util.BillingCalculator

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0
) {
    val unitPrice: Double get() = product.price
    val grossAmount: Double get() = BillingCalculator.round2(unitPrice * quantity)
    val itemDiscountAmount: Double get() = BillingCalculator.calculateItemDiscount(unitPrice, quantity, discountType, discountValue)
    val taxableAmount: Double get() = BillingCalculator.round2(grossAmount - itemDiscountAmount)
    val taxRate: Double get() = product.taxRate
    val cgstAmount: Double get() = BillingCalculator.calculateTaxPart(taxableAmount, taxRate / 2.0)
    val sgstAmount: Double get() = BillingCalculator.calculateTaxPart(taxableAmount, taxRate / 2.0)
    val taxAmount: Double get() = BillingCalculator.round2(cgstAmount + sgstAmount)
    val lineTotal: Double get() = BillingCalculator.round2(taxableAmount + taxAmount)
}

data class CartSummary(
    val items: List<CartItem> = emptyList(),
    val billDiscountType: DiscountType = DiscountType.NONE,
    val billDiscountValue: Double = 0.0,
    val subtotal: Double = 0.0,
    val itemDiscountTotal: Double = 0.0,
    val billDiscountAmount: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val taxableSubtotal: Double = 0.0,
    val cgstTotal: Double = 0.0,
    val sgstTotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalItemCount: Int = 0
)

data class PaymentSplit(
    val cashAmount: Double = 0.0,
    val cardAmount: Double = 0.0,
    val upiAmount: Double = 0.0,
    val cashTendered: Double = 0.0,
    val cardRef: String = "",
    val upiRef: String = ""
) {
    val totalPaid: Double get() = BillingCalculator.round2(cashAmount + cardAmount + upiAmount)
    val changeDue: Double get() = if (cashTendered > cashAmount) BillingCalculator.round2(cashTendered - cashAmount) else 0.0
    fun isComplete(targetTotal: Double): Boolean = totalPaid >= (targetTotal - 0.01)
    fun remaining(targetTotal: Double): Double = BillingCalculator.round2(maxOf(0.0, targetTotal - totalPaid))
}

data class BillWithDetails(
    val bill: BillEntity,
    val items: List<BillItemEntity>,
    val payments: List<BillPaymentEntity>
)

data class DailyReportData(
    val dateLabel: String,
    val grossSales: Double = 0.0,
    val totalSales: Double,
    val totalBillsCount: Int,
    val completedBillsCount: Int,
    val refundedBillsCount: Int,
    val refundedAmount: Double,
    val cashSales: Double,
    val cardSales: Double,
    val upiSales: Double,
    val topSellingItems: List<com.quickbill.pos.data.local.dao.TopSellingItemResult>,
    val totalItemsSold: Int = 0,
    val allSoldItems: List<com.quickbill.pos.data.local.dao.TopSellingItemResult> = emptyList(),
    val totalTax: Double = 0.0,
    val totalDiscount: Double = 0.0
)
