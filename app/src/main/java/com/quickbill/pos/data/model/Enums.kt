package com.quickbill.pos.data.model

enum class DiscountType {
    NONE,
    PERCENTAGE,
    FLAT
}

enum class PaymentMode {
    CASH,
    CARD,
    UPI,
    SPLIT
}

enum class BillStatus {
    COMPLETED,
    REFUNDED,
    VOID
}

enum class UserRole {
    ADMIN,
    CASHIER
}
