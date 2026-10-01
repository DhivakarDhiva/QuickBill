package com.quickbill.pos

import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.util.BillingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCalculatorTest {

    private fun createProduct(
        id: Long = 1,
        name: String = "Test Product",
        price: Double = 100.0,
        taxRate: Double = 18.0,
        stock: Int = 10
    ): ProductEntity {
        return ProductEntity(
            id = id,
            name = name,
            sku = "SKU$id",
            category = "General",
            price = price,
            taxRate = taxRate,
            stockQuantity = stock
        )
    }

    @Test
    fun testEmptyCartReturnsZero() {
        val summary = BillingCalculator.calculateCartSummary(emptyList())
        assertEquals(0.0, summary.subtotal, 0.001)
        assertEquals(0.0, summary.totalDiscount, 0.001)
        assertEquals(0.0, summary.taxTotal, 0.001)
        assertEquals(0.0, summary.grandTotal, 0.001)
        assertEquals(0, summary.totalItemCount)
    }

    @Test
    fun testStandardBillingWithGst18() {
        // Product price: 100, Qty: 2 -> Gross: 200, GST 18% -> CGST: 18, SGST: 18 -> Tax: 36 -> Grand Total: 236
        val product = createProduct(price = 100.0, taxRate = 18.0)
        val item = CartItem(product = product, quantity = 2)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(200.0, summary.subtotal, 0.001)
        assertEquals(0.0, summary.totalDiscount, 0.001)
        assertEquals(200.0, summary.taxableSubtotal, 0.001)
        assertEquals(18.0, summary.cgstTotal, 0.001)
        assertEquals(18.0, summary.sgstTotal, 0.001)
        assertEquals(36.0, summary.taxTotal, 0.001)
        assertEquals(236.0, summary.grandTotal, 0.001)
        assertEquals(2, summary.totalItemCount)
    }

    @Test
    fun testPerItemPercentageDiscount() {
        // Product price: 200, Qty: 1, Discount: 10% -> Item discount: 20 -> Taxable: 180
        // GST 12% on 180 -> CGST: 6% of 180 = 10.8, SGST: 6% of 180 = 10.8 -> Total Tax: 21.6
        // Grand total: 180 + 21.6 = 201.6
        val product = createProduct(price = 200.0, taxRate = 12.0)
        val item = CartItem(
            product = product,
            quantity = 1,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 10.0
        )
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(200.0, summary.subtotal, 0.001)
        assertEquals(20.0, summary.itemDiscountTotal, 0.001)
        assertEquals(180.0, summary.taxableSubtotal, 0.001)
        assertEquals(10.80, summary.cgstTotal, 0.001)
        assertEquals(10.80, summary.sgstTotal, 0.001)
        assertEquals(21.60, summary.taxTotal, 0.001)
        assertEquals(201.60, summary.grandTotal, 0.001)
    }

    @Test
    fun testPerItemFlatDiscount() {
        // Product price: 150, Qty: 2 -> Gross: 300, Flat discount: 50 -> Taxable: 250
        // GST 5% on 250 -> CGST: 6.25, SGST: 6.25 -> Total Tax: 12.50 -> Grand Total: 262.50
        val product = createProduct(price = 150.0, taxRate = 5.0)
        val item = CartItem(
            product = product,
            quantity = 2,
            discountType = DiscountType.FLAT,
            discountValue = 50.0
        )
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(300.0, summary.subtotal, 0.001)
        assertEquals(50.0, summary.itemDiscountTotal, 0.001)
        assertEquals(250.0, summary.taxableSubtotal, 0.001)
        assertEquals(6.25, summary.cgstTotal, 0.001)
        assertEquals(6.25, summary.sgstTotal, 0.001)
        assertEquals(12.50, summary.taxTotal, 0.001)
        assertEquals(262.50, summary.grandTotal, 0.001)
    }

    @Test
    fun testWholeBillDiscountPercentage() {
        // Item 1: 100 with 18% GST; Item 2: 100 with 18% GST -> Subtotal: 200
        // Whole bill discount: 10% -> Bill discount: 20 -> Net taxable: 180
        // GST 18% on 180 -> CGST: 16.20, SGST: 16.20 -> Tax: 32.40 -> Grand Total: 212.40
        val item1 = CartItem(product = createProduct(id = 1, price = 100.0, taxRate = 18.0), quantity = 1)
        val item2 = CartItem(product = createProduct(id = 2, price = 100.0, taxRate = 18.0), quantity = 1)

        val summary = BillingCalculator.calculateCartSummary(
            items = listOf(item1, item2),
            billDiscountType = DiscountType.PERCENTAGE,
            billDiscountValue = 10.0
        )

        assertEquals(200.0, summary.subtotal, 0.001)
        assertEquals(20.0, summary.billDiscountAmount, 0.001)
        assertEquals(180.0, summary.taxableSubtotal, 0.001)
        assertEquals(16.20, summary.cgstTotal, 0.001)
        assertEquals(16.20, summary.sgstTotal, 0.001)
        assertEquals(32.40, summary.taxTotal, 0.001)
        assertEquals(212.40, summary.grandTotal, 0.001)
    }

    @Test
    fun testWholeBillDiscountFlat() {
        // Subtotal: 500, Flat bill discount: 100 -> Net taxable: 400
        val item = CartItem(product = createProduct(price = 500.0, taxRate = 0.0), quantity = 1)
        val summary = BillingCalculator.calculateCartSummary(
            items = listOf(item),
            billDiscountType = DiscountType.FLAT,
            billDiscountValue = 100.0
        )

        assertEquals(500.0, summary.subtotal, 0.001)
        assertEquals(100.0, summary.billDiscountAmount, 0.001)
        assertEquals(400.0, summary.taxableSubtotal, 0.001)
        assertEquals(400.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testDiscountCannotExceedSubtotal() {
        val item = CartItem(product = createProduct(price = 100.0, taxRate = 18.0), quantity = 1)
        val summary = BillingCalculator.calculateCartSummary(
            items = listOf(item),
            billDiscountType = DiscountType.FLAT,
            billDiscountValue = 500.0 // Excess discount
        )

        assertEquals(100.0, summary.billDiscountAmount, 0.001)
        assertEquals(0.0, summary.taxableSubtotal, 0.001)
        assertEquals(0.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testCashTenderedAndChange() {
        val change1 = BillingCalculator.calculateCashChange(cashTendered = 500.0, cashDue = 450.0)
        assertEquals(50.0, change1, 0.001)

        val change2 = BillingCalculator.calculateCashChange(cashTendered = 400.0, cashDue = 450.0)
        assertEquals(0.0, change2, 0.001)

        val change3 = BillingCalculator.calculateCashChange(cashTendered = 450.0, cashDue = 450.0)
        assertEquals(0.0, change3, 0.001)
    }
}
