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

import com.quickbill.pos.data.model.PaymentSplit
import com.quickbill.pos.data.util.BillingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentUnitTest {

    @Test
    fun testExactCashPayment_ChangeDueIsZero() {
        val total = 250.0
        val tendered = 250.0
        val changeDue = if (tendered > total) BillingCalculator.round2(tendered - total) else 0.0

        assertEquals(0.0, changeDue, 0.001)
    }

    @Test
    fun testCashOverpayment_CalculatesChangeAccurately() {
        val total = 345.50
        val tendered = 500.0
        val changeDue = if (tendered > total) BillingCalculator.round2(tendered - total) else 0.0

        assertEquals(154.50, changeDue, 0.001)
    }

    @Test
    fun testSplitPaymentThreeWays_TotalPaidMatches() {
        val split = PaymentSplit(
            cashAmount = 100.0,
            cardAmount = 150.0,
            upiAmount = 250.0
        )
        val grandTotal = 500.0

        assertEquals(500.0, split.totalPaid, 0.001)
        assertTrue(split.isComplete(grandTotal))
        assertEquals(0.0, split.remaining(grandTotal), 0.001)
    }

    @Test
    fun testSplitPayment_WithCashTenderedAndChangeDue() {
        // Grand Total: 1000. Customer pays 600 by CARD and 400 in CASH.
        // For the 400 cash part, customer gives a 500 note (tendered = 500).
        val split = PaymentSplit(
            cashAmount = 400.0,
            cardAmount = 600.0,
            cashTendered = 500.0
        )
        val grandTotal = 1000.0

        assertEquals(1000.0, split.totalPaid, 0.001)
        assertTrue(split.isComplete(grandTotal))
        assertEquals(100.0, split.changeDue, 0.001)
    }

    @Test
    fun testSplitPayment_IncompleteState() {
        val split = PaymentSplit(
            cashAmount = 100.0,
            cardAmount = 200.0
        )
        val grandTotal = 500.0

        assertEquals(300.0, split.totalPaid, 0.001)
        assertFalse(split.isComplete(grandTotal))
        assertEquals(200.0, split.remaining(grandTotal), 0.001)
    }

    @Test
    fun testSplitPayment_RoundingPrecision() {
        val split = PaymentSplit(
            cashAmount = 33.33,
            cardAmount = 33.33,
            upiAmount = 33.34
        )
        val grandTotal = 100.0

        assertEquals(100.0, split.totalPaid, 0.001)
        assertTrue(split.isComplete(grandTotal))
    }
}
