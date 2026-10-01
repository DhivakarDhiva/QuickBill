package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quickbill.pos.data.model.DiscountType

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["billId"]),
        Index(value = ["productId"])
    ]
)
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val productId: Long,
    val productName: String,
    val sku: String,
    val unitPrice: Double,
    val quantity: Int,
    val taxRate: Double, // e.g. 18.0
    val itemDiscountType: DiscountType = DiscountType.NONE,
    val itemDiscountValue: Double = 0.0,
    val itemDiscountAmount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val lineTotal: Double = 0.0
)
