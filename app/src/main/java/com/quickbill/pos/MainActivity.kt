package com.quickbill.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.quickbill.pos.data.repository.AppThemeMode
import com.quickbill.pos.ui.components.AppearanceDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.quickbill.pos.data.model.UserRole
import com.quickbill.pos.ui.components.QuickBillBottomBar
import com.quickbill.pos.ui.components.QuickBillNavItem
import com.quickbill.pos.ui.components.QuickBillNavRail
import com.quickbill.pos.ui.components.QuickBillNavDrawerContent
import com.quickbill.pos.ui.components.QuickBillTopBar
import com.quickbill.pos.ui.navigation.Screen
import com.quickbill.pos.ui.screens.auth.LoginScreen
import com.quickbill.pos.ui.screens.auth.LoginViewModel
import com.quickbill.pos.ui.screens.billing.BillingScreen
import com.quickbill.pos.ui.screens.billing.BillingViewModel
import com.quickbill.pos.ui.screens.billing.HeldCartsScreen
import com.quickbill.pos.ui.screens.dashboard.DashboardScreen
import com.quickbill.pos.ui.screens.dashboard.DashboardViewModel
import com.quickbill.pos.ui.screens.history.SalesHistoryScreen
import com.quickbill.pos.ui.screens.history.SalesHistoryViewModel
import com.quickbill.pos.ui.screens.products.ProductsScreen
import com.quickbill.pos.ui.screens.products.ProductsViewModel
import com.quickbill.pos.ui.screens.reports.DailyReportScreen
import com.quickbill.pos.ui.screens.reports.ReportsViewModel
import com.quickbill.pos.ui.theme.QuickBillTheme
import com.quickbill.pos.ui.theme.SwiftUiMotion
import com.quickbill.pos.app.QuickBillApp
import com.quickbill.pos.ui.components.NetworkDisconnectedDialog
import com.quickbill.pos.ui.components.NetworkConnectedDialog
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QuickBillApp

        setContent {
            val themeMode by app.themeRepository.themeMode.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            QuickBillTheme(
                darkTheme = darkTheme,
                themeMode = themeMode,
                onThemeChange = { mode -> app.themeRepository.setThemeMode(mode) }
            ) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                var showAppearanceDialog by remember { mutableStateOf(false) }

                // State from App repositories injected via Koin
                val currentUser by app.authRepository.currentUser.collectAsState()
                val isOnline by app.networkMonitor.isOnline.collectAsState(initial = true)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route 
                    ?: if (currentUser == null) Screen.Login.route else (if (currentUser?.role == UserRole.ADMIN) Screen.Dashboard.route else Screen.Billing.route)

                // Network status check and popup dialog triggers
                var isDisconnectedDialogOpen by remember { mutableStateOf(false) }
                var isConnectedDialogOpen by remember { mutableStateOf(false) }
                var hasCheckedInitialNetwork by remember { mutableStateOf(false) }

                LaunchedEffect(isOnline) {
                    if (!hasCheckedInitialNetwork) {
                        hasCheckedInitialNetwork = true
                        if (!isOnline) {
                            isDisconnectedDialogOpen = true
                        }
                    } else {
                        if (!isOnline) {
                            isDisconnectedDialogOpen = true
                            isConnectedDialogOpen = false
                        } else {
                            isDisconnectedDialogOpen = false
                            isConnectedDialogOpen = true
                        }
                    }
                }

                // ViewModels injected via Koin Dependency Injection
                val dashboardViewModel: DashboardViewModel = koinViewModel()
                val billingViewModel: BillingViewModel = koinViewModel()
                val loginViewModel: LoginViewModel = koinViewModel()
                val productsViewModel: ProductsViewModel = koinViewModel()
                val historyViewModel: SalesHistoryViewModel = koinViewModel()
                val reportsViewModel: ReportsViewModel = koinViewModel()

                val heldCarts by billingViewModel.heldCarts.collectAsState()

                // Logout and navigation helpers
                val performLogout = {
                    coroutineScope.launch { drawerState.close() }
                    val currentRole = currentUser?.role
                    loginViewModel.resetState()
                    app.authRepository.logout()
                    val rootRoute = if (currentRole == UserRole.ADMIN) Screen.Dashboard.route else Screen.Billing.route
                    try {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(rootRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    } catch (_: Exception) {
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                    }
                }

                val navigateToTab: (String) -> Unit = { route ->
                    if (route != currentRoute) {
                        val rootRoute = if (currentUser?.role == UserRole.ADMIN) Screen.Dashboard.route else Screen.Billing.route
                        navController.navigate(route) {
                            popUpTo(rootRoute) {
                                inclusive = false
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    }
                }

                // If user was previously logged in and becomes null (e.g. session invalidated), bounce to login
                var wasLoggedIn by remember { mutableStateOf(currentUser != null) }
                LaunchedEffect(currentUser) {
                    if (wasLoggedIn && currentUser == null) {
                        loginViewModel.resetState()
                        try {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } catch (_: Exception) {
                            navController.navigate(Screen.Login.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                    wasLoggedIn = currentUser != null
                }

                // Determine if topbar and drawer should be shown (hidden on login screen)
                val isAuthScreen = currentRoute == Screen.Login.route

                // Modal state tracking across ViewModels to hide root bars when dialogs/sheets are active
                val billingUiState by billingViewModel.uiState.collectAsState()
                val isAddDialogOpen by productsViewModel.isAddDialogOpen.collectAsState()
                val editingProduct by productsViewModel.editingProduct.collectAsState()
                val selectedBillDetails by historyViewModel.selectedBillDetails.collectAsState()

                val isModalActive = billingUiState.isPaymentDialogOpen ||
                        billingUiState.completedBill != null ||
                        isAddDialogOpen ||
                        editingProduct != null ||
                        selectedBillDetails != null ||
                        showAppearanceDialog

                // Scroll-to-hide bottom navigation bar
                var isBarsVisible by remember { mutableStateOf(true) }

                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                val delta = available.y
                                if (delta < -25f) {
                                    // User is scrolling down -> hide floating bottom bar
                                    if (isBarsVisible) isBarsVisible = false
                                } else if (delta > 25f) {
                                    // User is scrolling up -> show floating bottom bar
                                    if (!isBarsVisible) isBarsVisible = true
                                }
                            }
                            return Offset.Zero
                        }
                    }
                }

                // Restore bars when changing tabs
                LaunchedEffect(currentRoute) {
                    isBarsVisible = true
                }

                val bottomBarOffset by animateDpAsState(
                    targetValue = if (isBarsVisible && !isModalActive && !isAuthScreen) 0.dp else 130.dp,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
                    label = "bottomBarOffset"
                )

                // Bottom bar navigation items (Home, Terminal, Products, Sales, Reports)
                val bottomNavItems = listOf(
                    QuickBillNavItem("Home", Screen.Dashboard.route, Icons.Default.Home),
                    QuickBillNavItem("Terminal", Screen.Billing.route, Icons.Default.PointOfSale),
                    QuickBillNavItem("Products", Screen.Products.route, Icons.Default.Inventory2),
                    QuickBillNavItem("Sales", Screen.SalesHistory.route, Icons.AutoMirrored.Filled.ReceiptLong),
                    QuickBillNavItem("Reports", Screen.DailyReport.route, Icons.Default.Assessment)
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isAuthScreen,
                    drawerContent = {
                        QuickBillNavDrawerContent(
                            currentRoute = currentRoute,
                            currentUser = currentUser,
                            heldCartsCount = heldCarts.size,
                            currentThemeMode = themeMode,
                            onSelectTheme = { mode ->
                                app.themeRepository.setThemeMode(mode)
                            },
                            onOpenAppearanceDialog = {
                                showAppearanceDialog = true
                            },
                            onNavigate = { route ->
                                coroutineScope.launch { drawerState.close() }
                                navigateToTab(route)
                            },
                            onCloseDrawer = {
                                coroutineScope.launch { drawerState.close() }
                            },
                            onLogoutClick = {
                                performLogout()
                            }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            if (!isAuthScreen && !isModalActive) {
                                QuickBillTopBar(
                                    title = when (currentRoute) {
                                        Screen.Dashboard.route -> "QuickBill"
                                        Screen.Billing.route -> "Billing Terminal"
                                        Screen.Products.route -> "Products"
                                        Screen.SalesHistory.route -> "Sales History"
                                        Screen.DailyReport.route -> "Daily Report"
                                        Screen.HeldCarts.route -> "Held Bills"
                                        else -> "QuickBill"
                                    },
                                    currentUser = currentUser,
                                    isOnline = isOnline,
                                    heldCartCount = heldCarts.size,
                                    canNavigateBack = currentRoute == Screen.HeldCarts.route,
                                    onBackClick = {
                                        navController.popBackStack()
                                    },
                                    onMenuClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onHeldCartsClick = {
                                        navController.navigate(Screen.HeldCarts.route)
                                    },
                                    onAppearanceClick = {
                                        showAppearanceDialog = true
                                    },
                                    onLogoutClick = {
                                        performLogout()
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        val topPadding = if (!isAuthScreen && !isModalActive) {
                            innerPadding.calculateTopPadding()
                        } else 0.dp

                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                        val isMainTab = currentRoute in listOf(
                            Screen.Dashboard.route,
                            Screen.Billing.route,
                            Screen.Products.route,
                            Screen.SalesHistory.route,
                            Screen.DailyReport.route
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topPadding)
                        ) {
                            // Landscape Navigation Rail: On phone landscape, sits neatly on the left with 0 vertical blockage
                            if (isLandscape && !isAuthScreen && !isModalActive && isMainTab) {
                                QuickBillNavRail(
                                    currentRoute = currentRoute,
                                    items = bottomNavItems,
                                    onItemClick = { route ->
                                        navigateToTab(route)
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .nestedScroll(nestedScrollConnection)
                            ) {
                                NavHost(
                                    navController = navController,
                                startDestination = if (currentUser == null) {
                                    Screen.Login.route
                                } else if (currentUser?.role == UserRole.ADMIN) {
                                    Screen.Dashboard.route
                                } else {
                                    Screen.Billing.route
                                },
                                enterTransition = { SwiftUiMotion.NavEnterTransition },
                                exitTransition = { SwiftUiMotion.NavExitTransition },
                                popEnterTransition = { SwiftUiMotion.NavPopEnterTransition },
                                popExitTransition = { SwiftUiMotion.NavPopExitTransition },
                                modifier = Modifier.fillMaxSize()
                            ) {
                            composable(Screen.Login.route) {
                                LoginScreen(
                                    viewModel = loginViewModel,
                                    onLoginSuccess = {
                                        val destination = if (currentUser?.role == UserRole.ADMIN) {
                                            Screen.Dashboard.route
                                        } else {
                                            Screen.Billing.route
                                        }
                                        navController.navigate(destination) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToBilling = {
                                        navigateToTab(Screen.Billing.route)
                                    },
                                    onNavigateToProducts = {
                                        productsViewModel.setFilterLowStock(false)
                                        navigateToTab(Screen.Products.route)
                                    },
                                    onNavigateToLowStock = {
                                        productsViewModel.setFilterLowStock(true)
                                        navigateToTab(Screen.Products.route)
                                    },
                                    onNavigateToSales = {
                                        navigateToTab(Screen.SalesHistory.route)
                                    },
                                    onNavigateToReports = {
                                        navigateToTab(Screen.DailyReport.route)
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
                                SalesHistoryScreen(
                                    viewModel = historyViewModel,
                                    isAdmin = currentUser?.role == UserRole.ADMIN
                                )
                            }

                            composable(Screen.DailyReport.route) {
                                DailyReportScreen(
                                    viewModel = reportsViewModel,
                                    onNavigateToSales = {
                                        navigateToTab(Screen.SalesHistory.route)
                                    }
                                )
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

                        // Floating Bottom Navigation Bar (Transparent Container) - only in Portrait on primary tabs
                        if (!isLandscape && !isAuthScreen && !isModalActive && isMainTab) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset { IntOffset(0, bottomBarOffset.roundToPx()) }
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
                            ) {
                                QuickBillBottomBar(
                                    currentRoute = currentRoute,
                                    items = bottomNavItems,
                                    onItemClick = { route ->
                                        navigateToTab(route)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Network check popup dialogs (Offline and Connected)
            if (isDisconnectedDialogOpen) {
                NetworkDisconnectedDialog(
                    onDismiss = { isDisconnectedDialogOpen = false },
                    onRetry = { isDisconnectedDialogOpen = false }
                )
            }

            if (isConnectedDialogOpen) {
                NetworkConnectedDialog(
                    onDismiss = { isConnectedDialogOpen = false }
                )
            }

            // Appearance manual selection dialog
            if (showAppearanceDialog) {
                AppearanceDialog(
                    currentThemeMode = themeMode,
                    onSelectTheme = { mode ->
                        app.themeRepository.setThemeMode(mode)
                    },
                    onDismiss = { showAppearanceDialog = false }
                )
            }
        }
    }
}
}
}
