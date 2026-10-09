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

package com.quickbill.pos.data.local

import androidx.room.TypeConverter
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromDiscountType(value: DiscountType): String = value.name

    @TypeConverter
    fun toDiscountType(value: String): DiscountType = try {
        DiscountType.valueOf(value)
    } catch (_: Exception) {
        DiscountType.NONE
    }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = try {
        PaymentMode.valueOf(value)
    } catch (_: Exception) {
        PaymentMode.CASH
    }

    @TypeConverter
    fun fromBillStatus(value: BillStatus): String = value.name

    @TypeConverter
    fun toBillStatus(value: String): BillStatus = try {
        BillStatus.valueOf(value)
    } catch (_: Exception) {
        BillStatus.COMPLETED
    }

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (_: Exception) {
        UserRole.CASHIER
    }

    @TypeConverter
    fun fromOrderStatus(value: com.quickbill.pos.data.model.kds.OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): com.quickbill.pos.data.model.kds.OrderStatus = try {
        com.quickbill.pos.data.model.kds.OrderStatus.valueOf(value)
    } catch (_: Exception) {
        com.quickbill.pos.data.model.kds.OrderStatus.NEW
    }

    @TypeConverter
    fun fromOrderEventType(value: com.quickbill.pos.data.model.kds.OrderEventType): String = value.name

    @TypeConverter
    fun toOrderEventType(value: String): com.quickbill.pos.data.model.kds.OrderEventType = try {
        com.quickbill.pos.data.model.kds.OrderEventType.valueOf(value)
    } catch (_: Exception) {
        com.quickbill.pos.data.model.kds.OrderEventType.ORDER_CREATED
    }

    @TypeConverter
    fun fromOrderType(value: com.quickbill.pos.data.model.kds.OrderType): String = value.name

    @TypeConverter
    fun toOrderType(value: String): com.quickbill.pos.data.model.kds.OrderType = try {
        com.quickbill.pos.data.model.kds.OrderType.valueOf(value)
    } catch (_: Exception) {
        com.quickbill.pos.data.model.kds.OrderType.DINE_IN
    }
}

