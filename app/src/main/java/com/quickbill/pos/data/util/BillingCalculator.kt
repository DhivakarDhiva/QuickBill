package com.quickbill.pos.data.util

import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.CartSummary
import com.quickbill.pos.data.model.DiscountType
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

object BillingCalculator {

    fun round2(value: Double): Double {
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun calculateItemDiscount(
        unitPrice: Double,
        quantity: Int,
        discountType: DiscountType,
        discountValue: Double
    ): Double {
        if (discountType == DiscountType.NONE || discountValue <= 0.0 || quantity <= 0) return 0.0
        val gross = unitPrice * quantity
        val rawDiscount = when (discountType) {
            DiscountType.PERCENTAGE -> {
                val clampedPercent = min(100.0, max(0.0, discountValue))
                gross * (clampedPercent / 100.0)
            }
            DiscountType.FLAT -> {
                min(gross, max(0.0, discountValue))
            }
            DiscountType.NONE -> 0.0
        }
        return round2(rawDiscount)
    }

    fun calculateTaxPart(taxableAmount: Double, ratePercent: Double): Double {
        if (taxableAmount <= 0.0 || ratePercent <= 0.0) return 0.0
        return round2(taxableAmount * (ratePercent / 100.0))
    }

    fun calculateCartSummary(
        items: List<CartItem>,
        billDiscountType: DiscountType = DiscountType.NONE,
        billDiscountValue: Double = 0.0
    ): CartSummary {
        if (items.isEmpty()) {
            return CartSummary()
        }

        var grossSubtotal = 0.0
        var itemDiscountTotal = 0.0
        var baseTaxableSubtotal = 0.0
        var totalItemCount = 0

        for (item in items) {
            grossSubtotal += item.grossAmount
            itemDiscountTotal += item.itemDiscountAmount
            baseTaxableSubtotal += item.taxableAmount
            totalItemCount += item.quantity
        }

        grossSubtotal = round2(grossSubtotal)
        itemDiscountTotal = round2(itemDiscountTotal)
        baseTaxableSubtotal = round2(baseTaxableSubtotal)

        // Calculate whole-bill discount
        val billDiscountAmount = when (billDiscountType) {
            DiscountType.PERCENTAGE -> {
                val clampedPercent = min(100.0, max(0.0, billDiscountValue))
                round2(baseTaxableSubtotal * (clampedPercent / 100.0))
            }
            DiscountType.FLAT -> {
                round2(min(baseTaxableSubtotal, max(0.0, billDiscountValue)))
            }
            DiscountType.NONE -> 0.0
        }

        val totalDiscount = round2(itemDiscountTotal + billDiscountAmount)
        val netTaxableSubtotal = round2(max(0.0, baseTaxableSubtotal - billDiscountAmount))

        // Allocate bill discount proportionally across items for accurate GST breakdown
        var cgstTotal = 0.0
        var sgstTotal = 0.0

        val discountRatio = if (baseTaxableSubtotal > 0.0) {
            (baseTaxableSubtotal - billDiscountAmount) / baseTaxableSubtotal
        } else {
            1.0
        }

        for (item in items) {
            val netItemTaxable = round2(item.taxableAmount * discountRatio)
            val cgst = calculateTaxPart(netItemTaxable, item.taxRate / 2.0)
            val sgst = calculateTaxPart(netItemTaxable, item.taxRate / 2.0)
            cgstTotal += cgst
            sgstTotal += sgst
        }

        cgstTotal = round2(cgstTotal)
        sgstTotal = round2(sgstTotal)
        val taxTotal = round2(cgstTotal + sgstTotal)
        val grandTotal = round2(netTaxableSubtotal + taxTotal)

        return CartSummary(
            items = items,
            billDiscountType = billDiscountType,
            billDiscountValue = billDiscountValue,
            subtotal = grossSubtotal,
            itemDiscountTotal = itemDiscountTotal,
            billDiscountAmount = billDiscountAmount,
            totalDiscount = totalDiscount,
            taxableSubtotal = netTaxableSubtotal,
            cgstTotal = cgstTotal,
            sgstTotal = sgstTotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal,
            totalItemCount = totalItemCount
        )
    }

    fun calculateCashChange(cashTendered: Double, cashDue: Double): Double {
        return if (cashTendered > cashDue) {
            round2(cashTendered - cashDue)
        } else {
            0.0
        }
    }

    fun formatCurrency(amount: Double): String {
        return "₹" + String.format(java.util.Locale.US, "%.2f", amount)
    }
}
