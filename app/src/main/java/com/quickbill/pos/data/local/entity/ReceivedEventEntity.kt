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
