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

package com.quickbill.pos

import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.util.BillingCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class TaxUnitTest {

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
        stockQuantity = 50
    )

    @Test
    fun testZeroTaxSlab_0Percent() {
        val item = CartItem(product = createProduct(1, "Salt", 50.0, 0.0), quantity = 2)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(100.0, summary.subtotal, 0.001)
        assertEquals(0.0, summary.cgstTotal, 0.001)
        assertEquals(0.0, summary.sgstTotal, 0.001)
        assertEquals(0.0, summary.taxTotal, 0.001)
        assertEquals(100.0, summary.grandTotal, 0.001)
    }

    @Test
    fun testGst5Percent_Slab() {
        // Price: 100, Qty: 1 -> Gross: 100, GST 5% -> CGST: 2.5, SGST: 2.5 -> Tax: 5.0 -> Total: 105.0
        val item = CartItem(product = createProduct(2, "Tea", 100.0, 5.0), quantity = 1)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(100.0, summary.subtotal, 0.001)
        assertEquals(2.50, summary.cgstTotal, 0.001)
        assertEquals(2.50, summary.sgstTotal, 0.001)
        assertEquals(5.00, summary.taxTotal, 0.001)
        assertEquals(105.00, summary.grandTotal, 0.001)
    }

    @Test
    fun testGst12Percent_Slab() {
        // Price: 200, Qty: 2 -> Gross: 400, GST 12% -> CGST: 24.0, SGST: 24.0 -> Tax: 48.0 -> Total: 448.0
        val item = CartItem(product = createProduct(3, "Butter", 200.0, 12.0), quantity = 2)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(400.0, summary.subtotal, 0.001)
        assertEquals(24.00, summary.cgstTotal, 0.001)
        assertEquals(24.00, summary.sgstTotal, 0.001)
        assertEquals(48.00, summary.taxTotal, 0.001)
        assertEquals(448.00, summary.grandTotal, 0.001)
    }

    @Test
    fun testGst18Percent_Slab() {
        // Price: 500, Qty: 1 -> Gross: 500, GST 18% -> CGST: 45.0, SGST: 45.0 -> Tax: 90.0 -> Total: 590.0
        val item = CartItem(product = createProduct(4, "Electronics", 500.0, 18.0), quantity = 1)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(500.0, summary.subtotal, 0.001)
        assertEquals(45.00, summary.cgstTotal, 0.001)
        assertEquals(45.00, summary.sgstTotal, 0.001)
        assertEquals(90.00, summary.taxTotal, 0.001)
        assertEquals(590.00, summary.grandTotal, 0.001)
    }

    @Test
    fun testGst28Percent_Slab() {
        // Price: 1000, Qty: 1 -> Gross: 1000, GST 28% -> CGST: 140.0, SGST: 140.0 -> Tax: 280.0 -> Total: 1280.0
        val item = CartItem(product = createProduct(5, "Luxury Good", 1000.0, 28.0), quantity = 1)
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(1000.0, summary.subtotal, 0.001)
        assertEquals(140.00, summary.cgstTotal, 0.001)
        assertEquals(140.00, summary.sgstTotal, 0.001)
        assertEquals(280.00, summary.taxTotal, 0.001)
        assertEquals(1280.00, summary.grandTotal, 0.001)
    }

    @Test
    fun testMultiItemDifferentGstSlabs() {
        val item1 = CartItem(product = createProduct(1, "Bread", 40.0, 0.0), quantity = 1)      // 40.0 + 0 = 40.0
        val item2 = CartItem(product = createProduct(2, "Oil", 150.0, 5.0), quantity = 2)        // 300.0 + 15.0 = 315.0
        val item3 = CartItem(product = createProduct(3, "Biscuits", 100.0, 18.0), quantity = 1)  // 100.0 + 18.0 = 118.0

        val summary = BillingCalculator.calculateCartSummary(listOf(item1, item2, item3))

        assertEquals(440.0, summary.subtotal, 0.001)
        // Item 2 GST 5% on 300: CGST 7.5, SGST 7.5 = 15.0
        // Item 3 GST 18% on 100: CGST 9.0, SGST 9.0 = 18.0
        assertEquals(16.50, summary.cgstTotal, 0.001)
        assertEquals(16.50, summary.sgstTotal, 0.001)
        assertEquals(33.00, summary.taxTotal, 0.001)
        assertEquals(473.00, summary.grandTotal, 0.001)
    }

    @Test
    fun testTaxCalculationAfterDiscount() {
        // Product 200, Qty 1, Flat Discount 50 -> Taxable: 150. GST 18% on 150 -> CGST: 13.50, SGST: 13.50 -> Tax: 27.0
        // Grand total: 150 + 27 = 177.0
        val item = CartItem(
            product = createProduct(1, "Item", 200.0, 18.0),
            quantity = 1,
            discountType = DiscountType.FLAT,
            discountValue = 50.0
        )
        val summary = BillingCalculator.calculateCartSummary(listOf(item))

        assertEquals(200.0, summary.subtotal, 0.001)
        assertEquals(50.0, summary.itemDiscountTotal, 0.001)
        assertEquals(150.0, summary.taxableSubtotal, 0.001)
        assertEquals(13.50, summary.cgstTotal, 0.001)
        assertEquals(13.50, summary.sgstTotal, 0.001)
        assertEquals(27.00, summary.taxTotal, 0.001)
        assertEquals(177.00, summary.grandTotal, 0.001)
    }
}
