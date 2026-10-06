package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quickbill.pos.data.model.kds.OrderEventType

@Entity(
    tableName = "pending_events",
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["createdAt"])
    ]
)
data class PendingEventEntity(
    @PrimaryKey
    val eventId: String,
    val eventType: OrderEventType,
    val orderId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
