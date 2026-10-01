package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quickbill.pos.data.model.DiscountType

@Entity(tableName = "held_carts")
data class HeldCartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val note: String, // e.g. "Customer in blue shirt" or "Order #1"
    val itemsJson: String, // Serialized cart items
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0,
    val totalAmount: Double = 0.0,
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
