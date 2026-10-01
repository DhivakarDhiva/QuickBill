package com.quickbill.pos.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Cashier Login")
    object Billing : Screen("billing", "QuickBill POS")
    object Products : Screen("products", "Product Catalog")
    object SalesHistory : Screen("history", "Sales & Receipts")
    object BillDetail : Screen("bill_detail/{billId}", "Bill Details") {
        fun createRoute(billId: Long) = "bill_detail/$billId"
    }
    object DailyReport : Screen("reports", "Daily Analytics")
    object HeldCarts : Screen("held_carts", "Parked Orders")
}
