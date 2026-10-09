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

package com.quickbill.pos.ui.screens.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.ui.components.*
import com.quickbill.pos.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: BillingViewModel,
    onNavigateToHeldCarts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape

    // Mobile sheet or screen state for Cart (Screen 6)
    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isCartSheetOpen by remember { mutableStateOf(false) }
    var showHoldCartDialog by remember { mutableStateOf(false) }
    var holdNote by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (isWideScreen || isLandscape) 20.dp else 120.dp)
            )
        },
        bottomBar = {
            // Screen 5 Mobile Bottom Quick-Cart Bar: Item count, Grand Total, and "View Cart / Proceed"
            if (!isWideScreen && uiState.cartItems.isNotEmpty()) {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OutlineLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 76.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${uiState.cartSummary.totalItemCount}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Current Bill",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
                                )
                            }
                            Text(
                                text = "₹ ${String.format(Locale.US, "%.2f", uiState.cartSummary.grandTotal)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimaryLight
                                )
                            )
                        }

                        Button(
                            onClick = { isCartSheetOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text(
                                text = "View Cart",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(WarmBackgroundLight)
                .padding(paddingValues)
        ) {
            if (isWideScreen) {
                // Dual Pane Layout (Catalog on Left 60%, Current Bill on Right 40%)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Catalog
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CatalogSearchAndFilters(
                            searchQuery = uiState.searchQuery,
                            onSearchChange = { viewModel.onSearchQueryChanged(it) },
                            onScanClick = { viewModel.openBarcodeScanner() },
                            categories = categories,
                            selectedCategory = uiState.selectedCategory,
                            onSelectCategory = { viewModel.onCategorySelected(it) }
                        )

                        ProductGrid(
                            products = products,
                            cartItems = uiState.cartItems,
                            onProductClick = { viewModel.addToCart(it) },
                            onIncrement = { viewModel.addToCart(it) },
                            onDecrement = { product ->
                                viewModel.updateItemQuantity(product.id, -1)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    VerticalDivider(color = OutlineLight)

                    // Right Pane: Screen 6 Cart with Discounts
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .background(SurfaceWhite)
                            .padding(14.dp)
                    ) {
                        CurrentBillHeader(
                            onHoldClick = { showHoldCartDialog = true },
                            onClearCart = { viewModel.clearCart() },
                            hasItems = uiState.cartItems.isNotEmpty()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CartItemsList(
                            cartItems = uiState.cartItems,
                            onIncrement = { viewModel.updateItemQuantity(it, 1) },
                            onDecrement = { viewModel.updateItemQuantity(it, -1) },
                            onRemove = { viewModel.removeItem(it) },
                            onDiscountClick = { viewModel.openItemDiscountDialog(it) },
                            modifier = Modifier.weight(1f)
                        )

                        CurrentBillSummaryCard(
                            summary = uiState.cartSummary,
                            onAddDiscountClick = { viewModel.openWholeBillDiscountDialog() },
                            onProceedToPay = { viewModel.openPaymentDialog() }
                        )
                    }
                }
            } else {
                // Mobile Layout: Screen 5 Product Catalog with Instant Add
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CatalogSearchAndFilters(
                        searchQuery = uiState.searchQuery,
                        onSearchChange = { viewModel.onSearchQueryChanged(it) },
                        onScanClick = { viewModel.openBarcodeScanner() },
                        categories = categories,
                        selectedCategory = uiState.selectedCategory,
                        onSelectCategory = { viewModel.onCategorySelected(it) }
                    )

                    ProductGrid(
                        products = products,
                        cartItems = uiState.cartItems,
                        onProductClick = { viewModel.addToCart(it) },
                        onIncrement = { viewModel.addToCart(it) },
                        onDecrement = { product ->
                            viewModel.updateItemQuantity(product.id, -1)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // =========================================================================
    // Screen 6: Mobile Bottom Sheet for "Current Bill & Cart with Discounts"
    // Matches mockup 6 perfectly: Items list with [- 2 +], Add Discount
    // selector, Subtotal, Discount, Tax, Grand Total, and "Proceed to Pay"
    // =========================================================================
    if (isCartSheetOpen && !isWideScreen) {
        ModalBottomSheet(
            onDismissRequest = { isCartSheetOpen = false },
            sheetState = cartSheetState,
            containerColor = SurfaceWhite,
            shape = BottomSheetShape,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(OutlineLight)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                CurrentBillHeader(
                    onHoldClick = {
                        isCartSheetOpen = false
                        showHoldCartDialog = true
                    },
                    onClearCart = { viewModel.clearCart() },
                    hasItems = uiState.cartItems.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(10.dp))

                CartItemsList(
                    cartItems = uiState.cartItems,
                    onIncrement = { viewModel.updateItemQuantity(it, 1) },
                    onDecrement = { viewModel.updateItemQuantity(it, -1) },
                    onRemove = { viewModel.removeItem(it) },
                    onDiscountClick = { viewModel.openItemDiscountDialog(it) },
                    modifier = Modifier.weight(1f)
                )

                CurrentBillSummaryCard(
                    summary = uiState.cartSummary,
                    onAddDiscountClick = { viewModel.openWholeBillDiscountDialog() },
                    onProceedToPay = {
                        isCartSheetOpen = false
                        viewModel.openPaymentDialog()
                    }
                )
            }
        }
    }

    // Hold Cart Dialog
    if (showHoldCartDialog) {
        QuickBillDialog(
            onDismissRequest = { showHoldCartDialog = false },
            title = "Hold Current Bill"
        ) {
            Text(
                text = "Park this bill to serve another customer quickly. You can resume it anytime.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickBillTextField(
                value = holdNote,
                onValueChange = { holdNote = it },
                label = "Customer or Order Note",
                placeholder = "e.g. Table 4 / Red Shirt",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showHoldCartDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        viewModel.holdCart(holdNote.ifBlank { "Held Order" })
                        holdNote = ""
                        showHoldCartDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("Hold Bill", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Barcode Scanner Modal
    if (uiState.isBarcodeScannerOpen) {
        BarcodeScannerModal(
            onDismiss = { viewModel.closeBarcodeScanner() },
            onBarcodeScanned = { barcode ->
                viewModel.onBarcodeScanned(barcode)
            }
        )
    }

    // Discount Dialog
    if (uiState.isDiscountDialogOpen) {
        val targetItem = uiState.itemForDiscount
        if (targetItem != null) {
            DiscountDialog(
                title = "Discount: ${targetItem.product.name}",
                subtotal = targetItem.grossAmount,
                initialType = targetItem.discountType,
                initialValue = targetItem.discountValue,
                onDismiss = { viewModel.closeDiscountDialog() },
                onApply = { type, value -> viewModel.applyDiscount(type, value) }
            )
        } else {
            DiscountDialog(
                title = "Whole-Bill Discount",
                subtotal = uiState.cartSummary.subtotal - uiState.cartSummary.itemDiscountTotal,
                initialType = uiState.wholeBillDiscountType,
                initialValue = uiState.wholeBillDiscountValue,
                onDismiss = { viewModel.closeDiscountDialog() },
                onApply = { type, value -> viewModel.applyDiscount(type, value) }
            )
        }
    }

    // Screen 7: Payment Screen Overlay
    AnimatedVisibility(
        visible = uiState.isPaymentDialogOpen,
        enter = SwiftUiMotion.ModalSlideIn,
        exit = SwiftUiMotion.ModalSlideOut,
        modifier = Modifier.fillMaxSize()
    ) {
        PaymentDialog(
            cartSummary = uiState.cartSummary,
            onDismiss = { viewModel.closePaymentDialog() },
            onCompleteCheckout = { customerName, customerPhone, mode, split ->
                viewModel.completeCheckout(customerName, customerPhone, mode, split)
            }
        )
    }

    // Screen 8: Receipt Screen Overlay
    AnimatedVisibility(
        visible = uiState.completedBill != null,
        enter = SwiftUiMotion.ModalSlideIn,
        exit = SwiftUiMotion.ModalSlideOut,
        modifier = Modifier.fillMaxSize()
    ) {
        uiState.completedBill?.let { billWithDetails ->
            ReceiptDialog(
                billWithDetails = billWithDetails,
                onDismiss = { viewModel.closeReceiptDialog() },
                onNewSale = { viewModel.closeReceiptDialog() }
            )
        }
    }
}

// =========================================================================
// Catalog Header with Search, Barcode Scan, and Category Chips (Screen 5 Mockup)
// =========================================================================

@Composable
private fun CatalogSearchAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanClick: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search & Scan Bar
        QuickBillSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Search product or scan barcode...",
            onScanClick = onScanClick
        )

        // Category Chips: [All] [Beverages] (active) [Snacks] [Main Course]
        val allCats = remember(categories) {
            val list = mutableListOf("All")
            list.addAll(categories.filter { it.isNotBlank() })
            list
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allCats) { cat ->
                val isSelected = if (cat == "All") selectedCategory.isEmpty() else selectedCategory.equals(cat, ignoreCase = true)
                QuickBillChip(
                    text = cat,
                    selected = isSelected,
                    onClick = { onSelectCategory(if (cat == "All") "" else cat) }
                )
            }
        }
    }
}

// =========================================================================
// Product Grid (Screen 5 Mockup)
// 3 columns or adaptive with soft warm cards and rich food thumbnails
// =========================================================================

@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    cartItems: List<com.quickbill.pos.data.model.CartItem>,
    onProductClick: (ProductEntity) -> Unit,
    onIncrement: (ProductEntity) -> Unit,
    onDecrement: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (products.isEmpty()) {
        QuickBillEmptyState(
            title = "No products found",
            description = "Try adjusting your search query or category filter",
            modifier = modifier.fillMaxSize()
        )
    } else {
        val cartItemMap = remember(cartItems) { cartItems.associateBy { it.product.id } }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 135.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                bottom = if (isLandscape) 40.dp else if (cartItems.isNotEmpty()) 170.dp else 100.dp,
                top = 4.dp
            ),
            modifier = modifier.fillMaxSize()
        ) {
            items(products, key = { it.id }) { product ->
                val cartItem = cartItemMap[product.id]
                QuickBillProductCard(
                    product = product,
                    onClick = { onProductClick(product) },
                    cartQuantity = cartItem?.quantity ?: 0,
                    onIncrement = { onIncrement(product) },
                    onDecrement = { onDecrement(product) }
                )
            }
        }
    }
}

// =========================================================================
// Current Bill Header (Screen 6 Mockup)
// "Current Bill" with [Hold] button top right
// =========================================================================

@Composable
private fun CurrentBillHeader(
    onHoldClick: () -> Unit,
    onClearCart: () -> Unit,
    hasItems: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Current Bill",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // [Hold] Button top right matching mockup 6
            Surface(
                onClick = onHoldClick,
                enabled = hasItems,
                shape = RoundedCornerShape(8.dp),
                color = if (hasItems) EmeraldContainer else SurfaceMutedLight,
                border = BorderStroke(1.dp, if (hasItems) EmeraldPrimary.copy(alpha = 0.5f) else OutlineLight)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Hold Bill",
                        tint = if (hasItems) EmeraldPrimary else TextSecondaryLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hold",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hasItems) EmeraldPrimary else TextSecondaryLight
                        )
                    )
                }
            }

            if (hasItems) {
                IconButton(
                    onClick = onClearCart,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear",
                        tint = ErrorCoral,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// Cart Items List (Screen 6 Mockup)
// Filter Coffee  - 2 +  ₹80.00
// Chicken Burger - 1 +  ₹150.00
// French Fries   - 1 +  ₹90.00
// =========================================================================

@Composable
private fun CartItemsList(
    cartItems: List<CartItem>,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDiscountClick: (CartItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cartItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = TextMutedLight,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Cart is Empty",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tap products from the catalog to add",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cartItems, key = { it.product.id }) { item ->
                CartItemCardRow(
                    item = item,
                    onIncrement = { onIncrement(item.product.id) },
                    onDecrement = { onDecrement(item.product.id) },
                    onRemove = { onRemove(item.product.id) },
                    onDiscountClick = { onDiscountClick(item) }
                )
            }
        }
    }
}

// Cart item row matching Screen 6 mockup
@Composable
private fun CartItemCardRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onDiscountClick: () -> Unit
) {
    val hasDiscount = item.discountType != DiscountType.NONE && item.discountValue > 0.0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, if (hasDiscount) EmeraldPrimary.copy(alpha = 0.4f) else OutlineLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                ProductThumbnail(
                    productName = item.product.name,
                    category = item.product.category,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Unit Price
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryLight
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹ ${String.format(Locale.US, "%.2f", item.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                        )
                        if (item.taxRate > 0) {
                            Text(
                                text = " · GST ${item.taxRate.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMutedLight,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Stepper [- 2 +]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceMutedLight)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Total Price
                Column(horizontalAlignment = Alignment.End) {
                    if (hasDiscount) {
                        Text(
                            text = "₹ ${String.format(Locale.US, "%.2f", item.grossAmount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMutedLight,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Text(
                        text = "₹ ${String.format(Locale.US, "%.2f", item.lineTotal)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hasDiscount) EmeraldPrimary else TextPrimaryLight
                        )
                    )
                }

                // Remove Icon
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove",
                        tint = CoralAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Per-Item Discount Bar / Pill
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasDiscount) {
                    Surface(
                        onClick = onDiscountClick,
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Item Discount",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val discountText = if (item.discountType == DiscountType.PERCENTAGE) {
                                "${item.discountValue.toInt()}% OFF (-₹${String.format(Locale.US, "%.2f", item.itemDiscountAmount)})"
                            } else {
                                "₹${String.format(Locale.US, "%.0f", item.discountValue)} FLAT OFF (-₹${String.format(Locale.US, "%.2f", item.itemDiscountAmount)})"
                            }
                            Text(
                                text = discountText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = onDiscountClick,
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceMutedLight,
                        border = BorderStroke(1.dp, OutlineLight.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Add Item Discount",
                                tint = TextSecondaryLight,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Item Discount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryLight,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Edit/Change discount text action
                Text(
                    text = if (hasDiscount) "Change" else "Add % / Flat",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onDiscountClick() }
                )
            }
        }
    }
}

// =========================================================================
// Current Bill Summary & Discount Section (Screen 6 Mockup)
// "Add Discount" segmented toggle, Subtotal, Discount, Tax, Grand Total,
// and dominant "PROCEED TO PAY" CTA button.
// =========================================================================

@Composable
private fun CurrentBillSummaryCard(
    summary: com.quickbill.pos.data.model.CartSummary,
    onAddDiscountClick: () -> Unit,
    onProceedToPay: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = WarmBackgroundLight,
        border = BorderStroke(1.dp, OutlineLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (isLandscape) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 8.dp)
        ) {
            // Add Discount Row (Screen 6 Mockup)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Discount",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    onClick = onAddDiscountClick,
                    shape = RoundedCornerShape(8.dp),
                    color = if (summary.billDiscountAmount > 0) CoralContainer else SurfaceWhite,
                    border = BorderStroke(1.dp, if (summary.billDiscountAmount > 0) CoralAccent else OutlineLight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = if (summary.billDiscountAmount > 0) CoralAccent else EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (summary.billDiscountAmount > 0) {
                                "${summary.billDiscountValue.toInt()}% Off"
                            } else {
                                "+ Bill Discount"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (summary.billDiscountAmount > 0) CoralAccent else EmeraldPrimary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Subtotal", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight))
                Text(
                    text = "₹ ${String.format(Locale.US, "%.2f", summary.subtotal)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Discount
            if (summary.totalDiscount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Discount (${summary.billDiscountValue.toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CoralAccent)
                    )
                    Text(
                        text = "- ₹ ${String.format(Locale.US, "%.2f", summary.totalDiscount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CoralAccent)
                    )
                }
            }

            // Tax
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Tax (GST)", style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight))
                Text(
                    text = "₹ ${String.format(Locale.US, "%.2f", summary.taxTotal)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            // Grand Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grand Total",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                )
                Text(
                    text = "₹ ${String.format(Locale.US, "%.2f", summary.grandTotal)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Proceed to Pay Primary Button (Screen 6 Mockup)
            QuickBillButton(
                text = "Proceed to Pay",
                onClick = onProceedToPay,
                enabled = summary.items.isNotEmpty(),
                containerColor = EmeraldPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
