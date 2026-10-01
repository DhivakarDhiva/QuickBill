package com.quickbill.pos.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.ui.components.BarcodeScannerModal
import com.quickbill.pos.ui.components.DiscountDialog
import com.quickbill.pos.ui.components.PaymentDialog
import com.quickbill.pos.ui.components.ReceiptDialog
import com.quickbill.pos.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: BillingViewModel,
    onNavigateToHeldCarts: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720

    var showHoldCartDialog by remember { mutableStateOf(false) }
    var holdNote by remember { mutableStateOf("") }
    var mobileSelectedTab by remember { mutableStateOf(0) } // 0: Catalog, 1: Cart

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isWideScreen && mobileSelectedTab == 0 && uiState.cartItems.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { mobileSelectedTab = 1 },
                    containerColor = PrimaryGreen,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    text = {
                        Text(
                            text = "${uiState.cartSummary.totalItemCount} Items • ₹${String.format(Locale.US, "%.2f", uiState.cartSummary.grandTotal)}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isWideScreen) {
                // Wide Screen / Tablet Layout: Dual Pane
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Catalog & Categories (60% width)
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        CatalogHeader(
                            searchQuery = uiState.searchQuery,
                            onSearchChange = { viewModel.onSearchQueryChanged(it) },
                            onScanClick = { viewModel.openBarcodeScanner() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CategoryChips(
                            categories = categories,
                            selectedCategory = uiState.selectedCategory,
                            onSelectCategory = { viewModel.onCategorySelected(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ProductGrid(
                            products = products,
                            onProductClick = { viewModel.addToCart(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Right Pane: Active Cart & Totals (42% width)
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        CartHeader(
                            itemCount = uiState.cartSummary.totalItemCount,
                            onClearCart = { viewModel.clearCart() },
                            onHoldCart = { showHoldCartDialog = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CartItemsList(
                            cartItems = uiState.cartItems,
                            onIncrement = { viewModel.updateItemQuantity(it, 1) },
                            onDecrement = { viewModel.updateItemQuantity(it, -1) },
                            onRemove = { viewModel.removeItem(it) },
                            onDiscountClick = { viewModel.openItemDiscountDialog(it) },
                            modifier = Modifier.weight(1f)
                        )

                        CartSummarySection(
                            summary = uiState.cartSummary,
                            onBillDiscountClick = { viewModel.openWholeBillDiscountDialog() },
                            onCheckoutClick = { viewModel.openPaymentDialog() }
                        )
                    }
                }
            } else {
                // Phone Layout: Segmented View (Catalog & Cart tabs)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tab Bar
                    TabRow(
                        selectedTabIndex = mobileSelectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = mobileSelectedTab == 0,
                            onClick = { mobileSelectedTab = 0 },
                            text = { Text("Product Catalog") },
                            icon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = mobileSelectedTab == 1,
                            onClick = { mobileSelectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Cart")
                                    if (uiState.cartSummary.totalItemCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = PrimaryGreen) {
                                            Text("${uiState.cartSummary.totalItemCount}")
                                        }
                                    }
                                }
                            },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    if (mobileSelectedTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            CatalogHeader(
                                searchQuery = uiState.searchQuery,
                                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                                onScanClick = { viewModel.openBarcodeScanner() }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            CategoryChips(
                                categories = categories,
                                selectedCategory = uiState.selectedCategory,
                                onSelectCategory = { viewModel.onCategorySelected(it) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ProductGrid(
                                products = products,
                                onProductClick = { viewModel.addToCart(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            CartHeader(
                                itemCount = uiState.cartSummary.totalItemCount,
                                onClearCart = { viewModel.clearCart() },
                                onHoldCart = { showHoldCartDialog = true }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            CartItemsList(
                                cartItems = uiState.cartItems,
                                onIncrement = { viewModel.updateItemQuantity(it, 1) },
                                onDecrement = { viewModel.updateItemQuantity(it, -1) },
                                onRemove = { viewModel.removeItem(it) },
                                onDiscountClick = { viewModel.openItemDiscountDialog(it) },
                                modifier = Modifier.weight(1f)
                            )

                            CartSummarySection(
                                summary = uiState.cartSummary,
                                onBillDiscountClick = { viewModel.openWholeBillDiscountDialog() },
                                onCheckoutClick = { viewModel.openPaymentDialog() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Hold Cart Dialog
    if (showHoldCartDialog) {
        AlertDialog(
            onDismissRequest = { showHoldCartDialog = false },
            title = { Text("Park / Hold Order", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Hold this cart to serve another customer. You can resume it anytime.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = holdNote,
                        onValueChange = { holdNote = it },
                        label = { Text("Order Note / Customer Identifier") },
                        placeholder = { Text("e.g. Customer in Red Shirt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.holdCart(holdNote)
                        holdNote = ""
                        showHoldCartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Hold Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHoldCartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

    // Discount Dialog (Item or Whole Bill)
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

    // Payment Dialog
    if (uiState.isPaymentDialogOpen) {
        PaymentDialog(
            cartSummary = uiState.cartSummary,
            onDismiss = { viewModel.closePaymentDialog() },
            onCompleteCheckout = { customerName, customerPhone, mode, split ->
                viewModel.completeCheckout(customerName, customerPhone, mode, split)
            }
        )
    }

    // Receipt Modal
    uiState.completedBill?.let { billWithDetails ->
        ReceiptDialog(
            billWithDetails = billWithDetails,
            onDismiss = { viewModel.closeReceiptDialog() },
            onNewSale = { viewModel.closeReceiptDialog() }
        )
    }
}

@Composable
private fun CatalogHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search products by name or SKU...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )

        FilledTonalIconButton(
            onClick = onScanClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan Barcode",
                tint = PrimaryGreen
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCategory.isEmpty(),
                onClick = { onSelectCategory("") },
                label = { Text("All Products") },
                shape = RoundedCornerShape(20.dp)
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                label = { Text(category) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    onProductClick: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No products found",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = !isOutOfStock && product.stockQuantity <= product.minStockAlert

    Card(
        onClick = onClick,
        enabled = !isOutOfStock,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Category & Tax pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (product.taxRate > 0) {
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${product.taxRate.toInt()}% GST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // SKU
            Text(
                text = "SKU: ${product.sku}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Price & Stock indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isOutOfStock) Color.Gray else PrimaryGreen
                    )
                )

                // Stock Badge
                when {
                    isOutOfStock -> {
                        Surface(
                            color = ErrorRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Out of Stock",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    isLowStock -> {
                        Surface(
                            color = SecondaryAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${product.stockQuantity} left",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryAmber
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "${product.stockQuantity} in stock",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartHeader(
    itemCount: Int,
    onClearCart: () -> Unit,
    onHoldCart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Current Cart",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Badge(containerColor = PrimaryGreen) {
                Text("$itemCount")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (itemCount > 0) {
                FilledTonalIconButton(
                    onClick = onHoldCart,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircleOutline,
                        contentDescription = "Hold Cart",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalIconButton(
                    onClick = onClearCart,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Cart",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

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
                    imageVector = Icons.Default.RemoveShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cart is Empty",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Tap products on the catalog to add",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cartItems, key = { it.product.id }) { item ->
                CartItemRow(
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

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onDiscountClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", item.unitPrice)} each" + if (item.taxRate > 0) " • ${item.taxRate.toInt()}% GST" else "",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                // Line Total
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", item.lineTotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom controls: Discount chip & Quantity Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item Discount Pill
                Surface(
                    onClick = onDiscountClick,
                    color = if (item.itemDiscountAmount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = if (item.itemDiscountAmount > 0) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.itemDiscountAmount > 0) "-₹${String.format(Locale.US, "%.2f", item.itemDiscountAmount)}" else "Add Disc",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (item.itemDiscountAmount > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.itemDiscountAmount > 0) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Quantity Counter (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onDecrement,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    FilledTonalIconButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartSummarySection(
    summary: com.quickbill.pos.data.model.CartSummary,
    onBillDiscountClick: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Subtotal", style = MaterialTheme.typography.bodySmall)
                Text(text = "₹${String.format(Locale.US, "%.2f", summary.subtotal)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }

            // Item Discounts if any
            if (summary.itemDiscountTotal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Item Discounts", style = MaterialTheme.typography.bodySmall.copy(color = PrimaryGreen))
                    Text(text = "-₹${String.format(Locale.US, "%.2f", summary.itemDiscountTotal)}", style = MaterialTheme.typography.bodySmall.copy(color = PrimaryGreen, fontWeight = FontWeight.Bold))
                }
            }

            // Whole Bill Discount row / trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBillDiscountClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (summary.billDiscountAmount > 0) "Bill Discount (${summary.billDiscountValue.toInt()}${if (summary.billDiscountType == DiscountType.PERCENTAGE) "%" else "₹"})" else "+ Whole-Bill Discount",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (summary.billDiscountAmount > 0) {
                    Text(
                        text = "-₹${String.format(Locale.US, "%.2f", summary.billDiscountAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    )
                }
            }

            // GST Breakup: CGST + SGST
            if (summary.taxTotal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CGST (Central Tax)", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(text = "₹${String.format(Locale.US, "%.2f", summary.cgstTotal)}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "SGST (State Tax)", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(text = "₹${String.format(Locale.US, "%.2f", summary.sgstTotal)}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Grand Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GRAND TOTAL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", summary.grandTotal)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Checkout Button
            Button(
                onClick = onCheckoutClick,
                enabled = summary.items.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = PrimaryGreen.copy(alpha = 0.4f)
                )
            ) {
                Icon(imageVector = Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CHECKOUT (₹${String.format(Locale.US, "%.2f", summary.grandTotal)})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
