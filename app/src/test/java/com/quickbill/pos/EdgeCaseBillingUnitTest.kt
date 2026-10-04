package com.quickbill.pos

import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.util.BillingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeCaseBillingUnitTest {

    private fun createProduct(
        id: Long,
        name: String,
        price: Double,
        taxRate: Double
    ): ProductEntity = ProductEntity(
        id = id,
        name = name,
        sku = "SKU-$id",
        category = "Test",
        price = price,
        taxRate = taxRate,
        stockQuantity = 100
    )

    @Test
    fun test100PercentWholeBillDiscount_GrandTotalZero() {
        val item1 = CartItem(product = createProduct(1, "Item 1", 100.0, 18.0), quantity = 2)
        val item2 = CartItem(product = createProduct(2, "Item 2", 50.0, 12.0), quantity = 1)

        val summary = BillingCalculator.calculateCartSummary(
            items = listOf(item1, item2),
            billDiscountType = DiscountType.PERCENTAGE,
            billDiscountValue = 100.0
        )

        assertEquals(250.0, summary.subtotal, 0.001)
        assertEquals(250.0, summary.totalDiscount, 0.001)
        assertEquals(0.0, summary.taxableSubtotal, 0.001)
        assertEquals(0.0, summary.taxTotal, 0.001)
        assertEquals(0.0, summary.grandTotal, 0.001)
    }

    @Test
    fun test100PercentItemDiscount_LineTotalZero() {
        val freeItem = CartItem(
            product = createProduct(1, "Free Promotional Item", 150.0, 18.0),
            quantity = 1,
            discountType = DiscountType.PERCENTAGE,
            discountValue = 100.0
        )
        val regularItem = CartItem(
            product = createProduct(2, "Regular Item", 200.0, 0.0),
            quantity = 1
        )

        val summary = BillingCalculator.calculateCartSummary(listOf(freeItem, regularItem))

        assertEquals(350.0, summary.subtotal, 0.001)
        assertEquals(150.0, summary.itemDiscountTotal, 0.001)
        assertEquals(200.0, summary.taxableSubtotal, 0.001)
        assertEquals(200.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testFlatDiscountExceedingSubtotal_CappedAtSubtotal() {
        // Subtotal: 100, Flat Discount attempted: 150 -> Discount amount should be capped at 100, grand total 0.
        val item = CartItem(product = createProduct(1, "Item", 100.0, 0.0), quantity = 1)
        val summary = BillingCalculator.calculateCartSummary(
            items = listOf(item),
            billDiscountType = DiscountType.FLAT,
            billDiscountValue = 150.0
        )

        assertEquals(100.0, summary.subtotal, 0.001)
        assertEquals(100.0, summary.totalDiscount, 0.001)
        assertEquals(0.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testZeroPriceProduct() {
        val freeSample = CartItem(product = createProduct(1, "Free Sample", 0.0, 18.0), quantity = 5)
        val summary = BillingCalculator.calculateCartSummary(listOf(freeSample))

        assertEquals(0.0, summary.subtotal, 0.001)
        assertEquals(0.0, summary.taxTotal, 0.001)
        assertEquals(0.0, summary.grandTotal, 0.001)
        assertEquals(5, summary.totalItemCount)
    }

    @Test
    fun testDecimalCentsTaxRounding() {
        // Price: 19.99, Qty: 3 -> Subtotal: 59.97. GST 18% -> 10.7946 -> CGST: 5.40, SGST: 5.40, Tax: 10.80
        val item = CartItem(product = createProduct(1, "Odd Price Item", 19.99, 18.0), quantity = 3)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(59.97, summary.subtotal, 0.001)
        assertEquals(10.80, summary.taxTotal, 0.01)
        assertEquals(70.77, summary.grandTotal, 0.01)
    }

    @Test
    fun testLargeCartPerformance() {
        val largeCart = (1..100).map { id ->
            CartItem(
                product = createProduct(id.toLong(), "Item $id", 10.0 + id, if (id % 2 == 0) 18.0 else 5.0),
                quantity = (id % 5) + 1
            )
        }

        val start = System.currentTimeMillis()
        val summary = BillingCalculator.calculateCartSummary(largeCart)
        val duration = System.currentTimeMillis() - start

        assertTrue("Calculation should be instantaneous (<100ms)", duration < 100)
        assertTrue("Subtotal must be positive", summary.subtotal > 0)
        assertTrue("Grand total must exceed subtotal due to taxes", summary.grandTotal > summary.subtotal)
    }
}
