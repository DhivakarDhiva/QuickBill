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

    // Repositories & Utilities
    single { AuthRepository(get(), androidContext()) }
    single { ProductRepository(get()) }
    single { BillingRepository(get()) }
    single { ReportRepository(get()) }
    single { ThemeRepository(androidContext()) }
    single { NetworkMonitor(androidContext()) }

    // ViewModels
    viewModel {
        DashboardViewModel(
            reportRepository = get(),
            productRepository = get(),
            billingRepository = get(),
            authRepository = get()
        )
    }
    viewModel {
        BillingViewModel(
            productRepository = get(),
            billingRepository = get(),
            authRepository = get()
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
}
