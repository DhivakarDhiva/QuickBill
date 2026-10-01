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
}
