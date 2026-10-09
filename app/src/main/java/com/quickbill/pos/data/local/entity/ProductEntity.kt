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

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String, // Barcode or SKU
    val category: String,
    val price: Double, // Base price
    val taxRate: Double = 0.0, // GST percentage (e.g. 0, 5, 12, 18, 28)
    val stockQuantity: Int = 0,
    val minStockAlert: Int = 5,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean get() = stockQuantity <= 0
    val isLowStock: Boolean get() = stockQuantity in 1..minStockAlert
}
