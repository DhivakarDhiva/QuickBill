package com.quickbill.pos

import android.app.Application
import com.quickbill.pos.data.local.QuickBillDatabase
import com.quickbill.pos.data.repository.AuthRepository
import com.quickbill.pos.data.repository.BillingRepository
import com.quickbill.pos.data.repository.ProductRepository
import com.quickbill.pos.data.repository.ReportRepository
import com.quickbill.pos.data.seed.SampleDataSeeder
import com.quickbill.pos.data.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickBillApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: QuickBillDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var productRepository: ProductRepository
        private set

    lateinit var billingRepository: BillingRepository
        private set

    lateinit var reportRepository: ReportRepository
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = QuickBillDatabase.getDatabase(this)
        authRepository = AuthRepository(database.userDao())
        productRepository = ProductRepository(database.productDao())
        billingRepository = BillingRepository(database)
        reportRepository = ReportRepository(database)
        networkMonitor = NetworkMonitor(this)

        // Seed initial products, cashiers and sample transactions
        applicationScope.launch(Dispatchers.IO) {
            SampleDataSeeder.seedInitialDataIfEmpty(
                productDao = database.productDao(),
                userDao = database.userDao(),
                billDao = database.billDao(),
                billItemDao = database.billItemDao(),
                billPaymentDao = database.billPaymentDao()
            )
        }
    }

    companion object {
        lateinit var instance: QuickBillApp
            private set
    }
}
