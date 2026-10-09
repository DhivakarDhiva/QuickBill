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
