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

package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.model.PaymentMode

@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["billNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["status"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: String,
    val cashierId: Long,
    val cashierName: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val subtotal: Double,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double,
    val paymentMode: PaymentMode,
    val cashTendered: Double = 0.0,
    val changeDue: Double = 0.0,
    val notes: String = "",
    val status: BillStatus = BillStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis()
)
