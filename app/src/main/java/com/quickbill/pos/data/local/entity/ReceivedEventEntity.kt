package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "received_events",
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["receivedAt"])
    ]
)
data class ReceivedEventEntity(
    @PrimaryKey
    val eventId: String,
    val eventType: String,
    val orderId: String,
    val receivedAt: Long = System.currentTimeMillis()
)
