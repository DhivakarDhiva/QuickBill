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

package com.quickbill.pos.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Cashier Login")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Billing : Screen("billing", "QuickBill POS")
    object Products : Screen("products", "Product Catalog")
    object SalesHistory : Screen("history", "Sales & Receipts")
    object DailyReport : Screen("reports", "Daily Analytics")
    object HeldCarts : Screen("held_carts", "Parked Orders")
}
