package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quickbill.pos.data.model.PaymentMode

@Entity(
    tableName = "bill_payments",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["billId"])
    ]
)
data class BillPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val mode: PaymentMode,
    val amount: Double,
    val referenceNote: String = "" // e.g. "UPI Ref #1234", "Card Last 4: 9876", "Cash Tendered: 500"
)
