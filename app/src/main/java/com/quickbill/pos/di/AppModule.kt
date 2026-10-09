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

package com.quickbill.pos.di

import com.quickbill.pos.data.local.QuickBillDatabase
import com.quickbill.pos.data.repository.AuthRepository
import com.quickbill.pos.data.repository.BillingRepository
import com.quickbill.pos.data.repository.ProductRepository
import com.quickbill.pos.data.repository.ReportRepository
import com.quickbill.pos.data.repository.ThemeRepository
import com.quickbill.pos.data.util.NetworkMonitor
import com.quickbill.pos.ui.screens.auth.LoginViewModel
import com.quickbill.pos.ui.screens.billing.BillingViewModel
import com.quickbill.pos.ui.screens.dashboard.DashboardViewModel
import com.quickbill.pos.ui.screens.history.SalesHistoryViewModel
import com.quickbill.pos.ui.screens.products.ProductsViewModel
import com.quickbill.pos.ui.screens.reports.ReportsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database & Room DAOs
    single { QuickBillDatabase.getDatabase(androidContext()) }
    single { get<QuickBillDatabase>().userDao() }
    single { get<QuickBillDatabase>().productDao() }
    single { get<QuickBillDatabase>().billDao() }
    single { get<QuickBillDatabase>().billItemDao() }
    single { get<QuickBillDatabase>().billPaymentDao() }
    single { get<QuickBillDatabase>().heldCartDao() }
    single { get<QuickBillDatabase>().orderDao() }
    single { get<QuickBillDatabase>().pendingEventDao() }
    single { get<QuickBillDatabase>().receivedEventDao() }

    // Repositories & Utilities
    single { AuthRepository(get(), androidContext()) }
    single { ProductRepository(get()) }
    single { BillingRepository(get()) }
    single { ReportRepository(get()) }
    single { ThemeRepository(androidContext()) }
    single { NetworkMonitor(androidContext()) }
    single { com.quickbill.pos.data.repository.DeviceModeRepository(androidContext()) }
    single { com.quickbill.pos.data.repository.KdsSettingsRepository(androidContext()) }
    single { com.quickbill.pos.network.kds.WifiP2pConnectionManager(androidContext()) }
    single { com.quickbill.pos.network.kds.NsdDiscoveryManager(androidContext()) }
    single { com.quickbill.pos.network.kds.ConnectionManager() }
    single { com.quickbill.pos.network.kds.OutboxManager(get()) }
    single {
        com.quickbill.pos.network.kds.OrderSyncManager(
            context = androidContext(),
            orderDao = get(),
            receivedEventDao = get(),
            outboxManager = get(),
            connectionManager = get(),
            discoveryManager = get(),
            p2pManager = get(),
            kdsSettingsRepository = get()
        )
    }

    // ViewModels
    viewModel {
        DashboardViewModel(
            reportRepository = get(),
            productRepository = get(),
            authRepository = get()
        )
    }
    viewModel {
        BillingViewModel(
            productRepository = get(),
            billingRepository = get(),
            authRepository = get(),
            orderSyncManager = get()
        )
    }
    viewModel {
        LoginViewModel(authRepository = get())
    }
    viewModel {
        ProductsViewModel(productRepository = get())
    }
    viewModel {
        SalesHistoryViewModel(
            billingRepository = get(),
            authRepository = get()
        )
    }
    viewModel {
        ReportsViewModel(reportRepository = get())
    }
    viewModel {
        com.quickbill.pos.ui.screens.kitchen.QuickKitchenViewModel(
            orderDao = get(),
            orderSyncManager = get(),
            connectionManager = get(),
            discoveryManager = get(),
            settingsRepository = get(),
            p2pManager = get<com.quickbill.pos.network.kds.WifiP2pConnectionManager>()
        )
    }
}

