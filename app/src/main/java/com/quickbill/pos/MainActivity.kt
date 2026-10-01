package com.quickbill.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quickbill.pos.ui.components.QuickBillNavDrawerContent
import com.quickbill.pos.ui.components.QuickBillTopBar
import com.quickbill.pos.ui.navigation.Screen
import com.quickbill.pos.ui.screens.auth.LoginScreen
import com.quickbill.pos.ui.screens.auth.LoginViewModel
import com.quickbill.pos.ui.screens.billing.BillingScreen
import com.quickbill.pos.ui.screens.billing.BillingViewModel
import com.quickbill.pos.ui.screens.billing.HeldCartsScreen
import com.quickbill.pos.ui.screens.history.SalesHistoryScreen
import com.quickbill.pos.ui.screens.history.SalesHistoryViewModel
import com.quickbill.pos.ui.screens.products.ProductsScreen
import com.quickbill.pos.ui.screens.products.ProductsViewModel
import com.quickbill.pos.ui.screens.reports.DailyReportScreen
import com.quickbill.pos.ui.screens.reports.ReportsViewModel
import com.quickbill.pos.ui.theme.QuickBillTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QuickBillApp

        setContent {
            QuickBillTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                // State from App repositories
                val currentUser by app.authRepository.currentUser.collectAsState()
                val isOnline by app.networkMonitor.isOnline.collectAsState(initial = true)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Billing.route

                // ViewModels
                val billingViewModel: BillingViewModel = viewModel {
                    BillingViewModel(
                        productRepository = app.productRepository,
                        billingRepository = app.billingRepository,
                        authRepository = app.authRepository
                    )
                }
                val loginViewModel: LoginViewModel = viewModel {
                    LoginViewModel(authRepository = app.authRepository)
                }
                val productsViewModel: ProductsViewModel = viewModel {
                    ProductsViewModel(productRepository = app.productRepository)
                }
                val historyViewModel: SalesHistoryViewModel = viewModel {
                    SalesHistoryViewModel(billingRepository = app.billingRepository)
                }
                val reportsViewModel: ReportsViewModel = viewModel {
                    ReportsViewModel(reportRepository = app.reportRepository)
                }

                val heldCarts by billingViewModel.heldCarts.collectAsState()

                // Determine if topbar and drawer should be shown (hidden on login screen)
                val isAuthScreen = currentRoute == Screen.Login.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isAuthScreen,
                    drawerContent = {
                        QuickBillNavDrawerContent(
                            currentRoute = currentRoute,
                            currentUser = currentUser,
                            heldCartsCount = heldCarts.size,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Billing.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onCloseDrawer = {
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (!isAuthScreen) {
                                QuickBillTopBar(
                                    title = when (currentRoute) {
                                        Screen.Billing.route -> "QuickBill Terminal"
                                        Screen.Products.route -> "Products & Inventory"
                                        Screen.SalesHistory.route -> "Sales & Returns"
                                        Screen.DailyReport.route -> "Daily Analytics"
                                        Screen.HeldCarts.route -> "Parked Orders"
                                        else -> "QuickBill POS"
                                    },
                                    currentUser = currentUser,
                                    isOnline = isOnline,
                                    heldCartCount = heldCarts.size,
                                    onMenuClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onHeldCartsClick = {
                                        navController.navigate(Screen.HeldCarts.route)
                                    },
                                    onLogoutClick = {
                                        app.authRepository.logout()
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (currentUser == null) Screen.Login.route else Screen.Billing.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    viewModel = loginViewModel,
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Billing.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Billing.route) {
                                BillingScreen(
                                    viewModel = billingViewModel,
                                    onNavigateToHeldCarts = {
                                        navController.navigate(Screen.HeldCarts.route)
                                    }
                                )
                            }

                            composable(Screen.Products.route) {
                                ProductsScreen(viewModel = productsViewModel)
                            }

                            composable(Screen.SalesHistory.route) {
                                SalesHistoryScreen(viewModel = historyViewModel)
                            }

                            composable(Screen.DailyReport.route) {
                                DailyReportScreen(viewModel = reportsViewModel)
                            }

                            composable(Screen.HeldCarts.route) {
                                HeldCartsScreen(
                                    viewModel = billingViewModel,
                                    onCartResumed = {
                                        navController.navigate(Screen.Billing.route) {
                                            popUpTo(Screen.Billing.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
