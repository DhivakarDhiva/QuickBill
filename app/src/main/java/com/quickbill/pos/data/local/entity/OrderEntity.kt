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
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.model.kds.OrderType

@Entity(
    tableName = "kitchen_orders",
    indices = [
        Index(value = ["orderNumber"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val orderNumber: String,
    val customerName: String = "",
    val tableNumber: String = "",
    val notes: String = "",
    val orderType: OrderType = OrderType.DINE_IN,
    val status: OrderStatus = OrderStatus.NEW,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
