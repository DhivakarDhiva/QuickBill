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

package com.quickbill.pos.ui.screens.products

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.ui.components.*
import com.quickbill.pos.ui.theme.*
import java.util.Locale

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isAddDialogOpen by viewModel.isAddDialogOpen.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    val message by viewModel.message.collectAsState()
    val duplicateSkuError by viewModel.duplicateSkuError.collectAsState()

    val filterLowStock by viewModel.filterLowStock.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to top smoothly whenever sorting changes
    LaunchedEffect(sortOption) {
        if (listState.firstVisibleItemIndex > 5) {
            listState.scrollToItem(5)
        }
        listState.animateScrollToItem(0)
    }

    // Auto-scroll to top when category or low-stock filter changes
    LaunchedEffect(selectedCategory, filterLowStock) {
        if (listState.firstVisibleItemIndex > 5) {
            listState.scrollToItem(5)
        }
        listState.animateScrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (isLandscape) 16.dp else 84.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(WarmBackgroundLight)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val allCategoryList = remember(categories) {
                    val list = mutableListOf("All")
                    list.addAll(categories.filter { it.isNotBlank() })
                    list
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(0.52f)) {
                            QuickBillSearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.onSearchChanged(it) },
                                placeholder = "Search by name, SKU or barcode...",
                                onFilterClick = {
                                    showSortMenu = true
                                },
                                isFilterActive = sortOption != ProductSortOption.DEFAULT
                            )

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(SurfaceWhite)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Sort Products",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = TextSecondaryLight,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                                ProductSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option.label,
                                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (option == sortOption) EmeraldPrimary else TextPrimaryLight
                                                )
                                                if (option == sortOption) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                            coroutineScope.launch {
                                                if (listState.firstVisibleItemIndex > 5) {
                                                    listState.scrollToItem(5)
                                                }
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(0.48f)
                        ) {
                            items(allCategoryList) { cat ->
                                val isSelected = if (cat == "All") selectedCategory.isEmpty() else selectedCategory.equals(cat, ignoreCase = true)
                                QuickBillChip(
                                    text = cat,
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.onCategorySelected(if (cat == "All") "" else cat)
                                        coroutineScope.launch {
                                            if (listState.firstVisibleItemIndex > 5) {
                                                listState.scrollToItem(5)
                                            }
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Portrait Layout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0)
                    ) {
                        QuickBillSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchChanged(it) },
                            placeholder = "Search by name, SKU or barcode...",
                            onFilterClick = {
                                showSortMenu = true
                            },
                            isFilterActive = sortOption != ProductSortOption.DEFAULT
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(SurfaceWhite)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Sort Products",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = TextSecondaryLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                            ProductSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == sortOption) EmeraldPrimary else TextPrimaryLight
                                            )
                                            if (option == sortOption) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                        coroutineScope.launch {
                                            if (listState.firstVisibleItemIndex > 5) {
                                                listState.scrollToItem(5)
                                            }
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(1)
                    ) {
                        items(allCategoryList) { cat ->
                            val isSelected = if (cat == "All") selectedCategory.isEmpty() else selectedCategory.equals(cat, ignoreCase = true)
                            QuickBillChip(
                                text = cat,
                                selected = isSelected,
                                onClick = {
                                    viewModel.onCategorySelected(if (cat == "All") "" else cat)
                                    coroutineScope.launch {
                                        if (listState.firstVisibleItemIndex > 5) {
                                            listState.scrollToItem(5)
                                        }
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            )
                        }
                    }
                }

                // Active Low Stock Filter Banner
                if (filterLowStock) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CoralContainer,
                        border = BorderStroke(1.dp, CoralAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filtered by Low Stock (${products.size} items)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = CoralAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setFilterLowStock(false) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Filter",
                                    tint = CoralAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Active Sort Banner
                if (sortOption != ProductSortOption.DEFAULT) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldContainer,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sorted: ${sortOption.label} (${products.size} items)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.setSortOption(ProductSortOption.DEFAULT)
                                    coroutineScope.launch {
                                        if (listState.firstVisibleItemIndex > 5) {
                                            listState.scrollToItem(5)
                                        }
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Reset Sort",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // Product List Items
                // Matches mockup 3: Thumbnail, Name, SKU, Price, Stock Badge, 3-dots menu
                // =========================================================================
                if (products.isEmpty()) {
                    QuickBillEmptyState(
                        title = "No products found",
                        description = "Try searching for a different name, SKU, or category",
                        actionText = "+ Add New Product",
                        onActionClick = { viewModel.openAddDialog() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = if (isLandscape) 40.dp else 150.dp),
                        modifier = Modifier
                            .weight(1f)
                            .staggeredEntrance(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductRowItem(
                                product = product,
                                onEdit = { viewModel.openEditDialog(product) },
                                onDelete = { productToDelete = product },
                                onAdjustStock = { delta -> viewModel.quickAdjustStock(product.id, delta) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            // Clean Floating Action Button (+ Add Product)
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                shape = CircleShape,
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = if (isLandscape) 16.dp else 84.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Product",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // =========================================================================
    // Screen 4: Add / Edit Product Form Screen (Dialog / Sheet)
    // =========================================================================
    AnimatedVisibility(
        visible = isAddDialogOpen && editingProduct != null,
        enter = SwiftUiMotion.ModalEnterTransition,
        exit = SwiftUiMotion.ModalExitTransition
    ) {
        editingProduct?.let { prodData ->
            ProductFormScreenDialog(
                initialData = prodData,
                onDismiss = { viewModel.closeDialog() },
                onSave = { formData -> viewModel.saveProduct(formData) }
            )
        }
    }

    // Delete Confirmation Dialog
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorCoral) },
            title = { Text("Delete Product?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${product.name}'? This product will be archived.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate SKU Alert Dialog
    duplicateSkuError?.let { errText ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateSkuError() },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ErrorCoral, modifier = Modifier.size(28.dp)) },
            title = { Text("Duplicate SKU Alert", fontWeight = FontWeight.Bold) },
            text = { Text(errText) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissDuplicateSkuError() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = SurfaceWhite
        )
    }
}

// =========================================================================
// Single Product Row Item (Screen 3 Mockup)
// =========================================================================

@Composable
private fun ProductRowItem(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = !isOutOfStock && product.stockQuantity <= product.minStockAlert

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, OutlineLight),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Thumbnail
            ProductThumbnail(
                productName = product.name,
                category = product.category,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name & SKU
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceMutedLight
                    ) {
                        Text(
                            text = product.sku,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextSecondaryLight,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }

                    if (isOutOfStock) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ErrorCoral.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "OUT OF STOCK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorCoral,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isLowStock) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WarningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "LOW STOCK (${product.stockQuantity} left)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price & Stock
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "₹ ${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Direct Stepper [-] [Stock] [+] on Item Card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceMutedLight)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (product.stockQuantity > 0) onAdjustStock(-1) },
                        enabled = product.stockQuantity > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Stock",
                            tint = if (product.stockQuantity > 0) ErrorCoral else TextMutedLight,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = if (isOutOfStock) "0" else "${product.stockQuantity}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOutOfStock) ErrorCoral else if (isLowStock) CoralAccent else EmeraldPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { onAdjustStock(1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Stock",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Context Menu (Three Dots)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceWhite)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Product") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add 10 Stock") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onAdjustStock(10)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorCoral) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorCoral, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// Screen 4: Add / Edit Product Form Screen
// Matches mockup 4: Top bar with Save button, product image card with camera
// badge, clean form inputs, Tax dropdown, Low stock alert toggle
// =========================================================================

@Composable
fun ProductFormScreenDialog(
    initialData: ProductFormData,
    onDismiss: () -> Unit,
    onSave: (ProductFormData) -> Unit
) {
    var name by remember { mutableStateOf(initialData.name) }
    var sku by remember { mutableStateOf(initialData.sku) }
    var category by remember { mutableStateOf(initialData.category) }
    var priceText by remember { mutableStateOf(initialData.priceText) }
    var taxRate by remember { mutableDoubleStateOf(initialData.taxRate) }
    var stockQuantityText by remember { mutableStateOf(initialData.stockQuantityText) }
    var minStockAlertText by remember { mutableStateOf(initialData.minStockAlertText) }
    var isLowStockAlertEnabled by remember { mutableStateOf(true) }
    var showIconPicker by remember { mutableStateOf(false) }
    var isSkuScannerOpen by remember { mutableStateOf(false) }

    val isEdit = initialData.id > 0
    val categories = listOf("Beverages", "Snacks", "Main Course", "Dessert", "Groceries", "Dairy")

    BackHandler(onBack = onDismiss)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryLight)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEdit) "Edit Product" else "Add Product",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryLight
                                )
                            )
                        }

                        // Save Button (Pill top right)
                        Button(
                            onClick = {
                                onSave(
                                    ProductFormData(
                                        id = initialData.id,
                                        name = name,
                                        sku = sku,
                                        category = category,
                                        priceText = priceText,
                                        taxRate = taxRate,
                                        stockQuantityText = stockQuantityText,
                                        minStockAlertText = if (isLowStockAlertEnabled) minStockAlertText else "0"
                                    )
                                )
                            },
                            enabled = name.isNotBlank() && sku.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarmBackgroundLight)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // =========================================================================
                    // Product Image Card with Interactive Camera Badge (Screen 4 Mockup)
                    // =========================================================================
                    val dialogConfig = androidx.compose.ui.platform.LocalConfiguration.current
                    val isDialogLandscape = dialogConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isDialogLandscape) 120.dp else 180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceWhite)
                            .border(BorderStroke(1.dp, OutlineLight), RoundedCornerShape(18.dp))
                            .clickable { showIconPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        ProductThumbnail(
                            productName = name.ifBlank { "Burger" },
                            category = category,
                            size = if (isDialogLandscape) 64.dp else 100.dp
                        )

                        // Interactive Camera badge button
                        Surface(
                            onClick = { showIconPicker = true },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(if (isDialogLandscape) 8.dp else 12.dp)
                                .size(if (isDialogLandscape) 34.dp else 40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Choose Product Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // Form Fields
                    // =========================================================================
                    QuickBillCard(modifier = Modifier.fillMaxWidth()) {
                        // Product Name
                        QuickBillTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Product Name",
                            placeholder = "e.g. Chicken Burger",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // SKU / Barcode with Camera Barcode Scanner
                        QuickBillTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = "SKU / Barcode",
                            placeholder = "e.g. BG001",
                            trailingIcon = {
                                IconButton(
                                    onClick = { isSkuScannerOpen = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan SKU Barcode",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Selector
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        var categoryDropdownExpanded by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { categoryDropdownExpanded = true },
                            shape = InputShape,
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, OutlineLight),
                            modifier = Modifier.fillMaxWidth().height(QuickBillDimens.inputHeight)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.ifBlank { "Select Category" },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryLight)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryLight)
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Price (₹) & Tax (%) in 2 Columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickBillTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                label = "Price (₹)",
                                placeholder = "150",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            // Tax (%) Dropdown
                            var taxDropdownExpanded by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tax (%)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Surface(
                                    onClick = { taxDropdownExpanded = true },
                                    shape = InputShape,
                                    color = SurfaceWhite,
                                    border = BorderStroke(1.dp, OutlineLight),
                                    modifier = Modifier.fillMaxWidth().height(QuickBillDimens.inputHeight)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${taxRate.toInt()} %",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryLight)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryLight)
                                    }

                                    DropdownMenu(
                                        expanded = taxDropdownExpanded,
                                        onDismissRequest = { taxDropdownExpanded = false }
                                    ) {
                                        listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                            DropdownMenuItem(
                                                text = { Text("${rate.toInt()}% GST") },
                                                onClick = {
                                                    taxRate = rate
                                                    taxDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stock Quantity
                        QuickBillTextField(
                            value = stockQuantityText,
                            onValueChange = { stockQuantityText = it },
                            label = "Stock Quantity",
                            placeholder = "20",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Low Stock Alert Switch & Threshold
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Low Stock Alert",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Switch(
                                checked = isLowStockAlertEnabled,
                                onCheckedChange = { isLowStockAlertEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }

                        if (isLowStockAlertEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            QuickBillTextField(
                                value = minStockAlertText,
                                onValueChange = { minStockAlertText = it },
                                label = "Alert when stock is below",
                                placeholder = "10",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    QuickBillButton(
                        text = if (isEdit) "Update Product" else "Save Product",
                        onClick = {
                            onSave(
                                ProductFormData(
                                    id = initialData.id,
                                    name = name,
                                    sku = sku,
                                    category = category,
                                    priceText = priceText,
                                    taxRate = taxRate,
                                    stockQuantityText = stockQuantityText,
                                    minStockAlertText = if (isLowStockAlertEnabled) minStockAlertText else "0"
                                )
                            )
                        },
                        enabled = name.isNotBlank() && sku.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Product Icon Picker Dialog
        if (showIconPicker) {
            AlertDialog(
                onDismissRequest = { showIconPicker = false },
                title = {
                    Text(
                        text = "Choose Product Icon",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    val presetIcons = listOf(
                        "Burger" to Icons.Default.Fastfood,
                        "Beverage" to Icons.Default.LocalBar,
                        "Coffee" to Icons.Default.Coffee,
                        "Pizza" to Icons.Default.LocalPizza,
                        "Dessert" to Icons.Default.Cake,
                        "Ice Cream" to Icons.Default.Icecream,
                        "Groceries" to Icons.Default.ShoppingBag,
                        "Dairy" to Icons.Default.Egg,
                        "Bakery" to Icons.Default.BakeryDining,
                        "Snack" to Icons.Default.LunchDining,
                        "Produce" to Icons.Default.Eco,
                        "Special" to Icons.Default.Star
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(presetIcons) { (label, icon) ->
                            Surface(
                                onClick = {
                                    if (name.isBlank()) {
                                        name = label
                                    }
                                    showIconPicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceMutedLight,
                                border = BorderStroke(1.dp, OutlineLight),
                                modifier = Modifier.aspectRatio(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimaryLight
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showIconPicker = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Barcode Scanner Modal for SKU Scanning
        if (isSkuScannerOpen) {
            BarcodeScannerModal(
                onDismiss = { isSkuScannerOpen = false },
                onBarcodeScanned = { scannedCode ->
                    sku = scannedCode
                    isSkuScannerOpen = false
                }
            )
        }
    }
