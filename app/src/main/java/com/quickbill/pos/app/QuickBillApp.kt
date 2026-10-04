package com.quickbill.pos.app

import android.app.Application
import com.quickbill.pos.data.local.QuickBillDatabase
import com.quickbill.pos.data.repository.AuthRepository
import com.quickbill.pos.data.repository.BillingRepository
import com.quickbill.pos.data.repository.ProductRepository
import com.quickbill.pos.data.repository.ReportRepository
import com.quickbill.pos.data.repository.ThemeRepository
import com.quickbill.pos.data.seed.SampleDataSeeder
import com.quickbill.pos.data.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import com.quickbill.pos.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class QuickBillApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database: QuickBillDatabase by inject()
    val authRepository: AuthRepository by inject()
    val productRepository: ProductRepository by inject()
    val billingRepository: BillingRepository by inject()
    val reportRepository: ReportRepository by inject()
    val themeRepository: ThemeRepository by inject()
    val networkMonitor: NetworkMonitor by inject()

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@QuickBillApp)
            modules(appModule)
        }

        // Restore logged in user session immediately on app launch
        runBlocking(Dispatchers.IO) {
            authRepository.restoreSession()
        }

        // Seed initial products, cashiers and sample transactions
        applicationScope.launch(Dispatchers.IO) {
            SampleDataSeeder.seedInitialDataIfEmpty(
                productDao = database.productDao(),
                userDao = database.userDao()
            )
        }
    }

    companion object {
        lateinit var instance: QuickBillApp
            private set
    }
}