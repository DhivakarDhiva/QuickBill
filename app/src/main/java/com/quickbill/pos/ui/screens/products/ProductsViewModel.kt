package com.quickbill.pos.ui.screens.products

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.repository.ProductRepository
import com.quickbill.pos.data.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductSortOption(val label: String) {
    DEFAULT("Default"),
    NAME_ASC("Name: A to Z"),
    NAME_DESC("Name: Z to A"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    STOCK_LOW_HIGH("Stock: Low to High"),
    STOCK_HIGH_LOW("Stock: High to Low")
}

data class ProductFormData(
    val id: Long = 0,
    val name: String = "",
    val sku: String = "",
    val category: String = "Groceries",
    val priceText: String = "",
    val taxRate: Double = 5.0,
    val stockQuantityText: String = "10",
    val minStockAlertText: String = "5"
)

class ProductsViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filterLowStock = MutableStateFlow(false)
    val filterLowStock: StateFlow<Boolean> = _filterLowStock.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.DEFAULT)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    val categories: StateFlow<List<String>> = productRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = combine(
        productRepository.allProducts,
        _searchQuery,
        _selectedCategory,
        _filterLowStock,
        _sortOption
    ) { all, query, cat, lowStockOnly, sort ->
        val filtered = all.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true)
            val matchesCat = cat.isBlank() || p.category.equals(cat, ignoreCase = true)
            val matchesLowStock = !lowStockOnly || (p.stockQuantity <= p.minStockAlert)
            matchesQuery && matchesCat && matchesLowStock
        }
        when (sort) {
            ProductSortOption.DEFAULT -> filtered.sortedWith(compareBy<ProductEntity> { it.name.lowercase() }.thenBy { it.id })
            ProductSortOption.NAME_ASC -> filtered.sortedWith(compareBy<ProductEntity> { it.name.lowercase() }.thenBy { it.sku })
            ProductSortOption.NAME_DESC -> filtered.sortedWith(compareByDescending<ProductEntity> { it.name.lowercase() }.thenBy { it.sku })
            ProductSortOption.PRICE_LOW_HIGH -> filtered.sortedWith(compareBy<ProductEntity> { it.price }.thenBy { it.name.lowercase() })
            ProductSortOption.PRICE_HIGH_LOW -> filtered.sortedWith(compareByDescending<ProductEntity> { it.price }.thenBy { it.name.lowercase() })
            ProductSortOption.STOCK_LOW_HIGH -> filtered.sortedWith(compareBy<ProductEntity> { it.stockQuantity }.thenBy { it.name.lowercase() })
            ProductSortOption.STOCK_HIGH_LOW -> filtered.sortedWith(compareByDescending<ProductEntity> { it.stockQuantity }.thenBy { it.name.lowercase() })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOption(option: ProductSortOption) {
        _sortOption.value = option
    }

    fun setFilterLowStock(enabled: Boolean) {
        _filterLowStock.value = enabled
    }

    private val _editingProduct = MutableStateFlow<ProductFormData?>(null)
    val editingProduct: StateFlow<ProductFormData?> = _editingProduct.asStateFlow()

    private val _isAddDialogOpen = MutableStateFlow(false)
    val isAddDialogOpen: StateFlow<Boolean> = _isAddDialogOpen.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _duplicateSkuError = MutableStateFlow<String?>(null)
    val duplicateSkuError: StateFlow<String?> = _duplicateSkuError.asStateFlow()

    fun dismissDuplicateSkuError() {
        _duplicateSkuError.value = null
    }

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(cat: String) {
        _selectedCategory.value = if (_selectedCategory.value == cat) "" else cat
    }

    fun openAddDialog() {
        _editingProduct.value = ProductFormData()
        _isAddDialogOpen.value = true
    }

    fun openEditDialog(product: ProductEntity) {
        _editingProduct.value = ProductFormData(
            id = product.id,
            name = product.name,
            sku = product.sku,
            category = product.category,
            priceText = product.price.toString(),
            taxRate = product.taxRate,
            stockQuantityText = product.stockQuantity.toString(),
            minStockAlertText = product.minStockAlert.toString()
        )
        _isAddDialogOpen.value = true
    }

    fun closeDialog() {
        _isAddDialogOpen.value = false
        _editingProduct.value = null
    }

    fun saveProduct(formData: ProductFormData) {
        val price = formData.priceText.toDoubleOrNull() ?: 0.0
        val stock = formData.stockQuantityText.toIntOrNull() ?: 0
        val minAlert = formData.minStockAlertText.toIntOrNull() ?: 5

        if (formData.name.isBlank()) {
            _message.value = "Product name cannot be empty"
            return
        }
        if (formData.sku.isBlank()) {
            _message.value = "SKU / Barcode cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                val trimmedSku = formData.sku.trim()
                // Validate duplicate SKU
                val existing = productRepository.getProductBySku(trimmedSku)
                if (existing != null && existing.id != formData.id) {
                    _duplicateSkuError.value = "A product with SKU '$trimmedSku' already exists ('${existing.name}'). Each product must have a unique SKU."
                    return@launch
                }

                val entity = ProductEntity(
                    id = formData.id,
                    name = formData.name.trim(),
                    sku = trimmedSku,
                    category = formData.category.trim(),
                    price = price,
                    taxRate = formData.taxRate,
                    stockQuantity = stock,
                    minStockAlert = minAlert
                )
                if (formData.id == 0L) {
                    productRepository.saveProduct(entity)
                    _message.value = "Product added successfully!"
                } else {
                    productRepository.updateProduct(entity)
                    _message.value = "Product updated successfully!"
                }
                closeDialog()
            } catch (e: Exception) {
                _message.value = "Failed to save: ${e.message}"
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(product)
                _message.value = "Deleted '${product.name}'"
            } catch (e: Exception) {
                _message.value = "Failed to delete: ${e.message}"
            }
        }
    }

    fun quickAdjustStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            if (product != null) {
                val newStock = maxOf(0, product.stockQuantity + delta)
                productRepository.updateStock(productId, newStock)
            }
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            try {
                val currentProducts = products.value
                val file = CsvExporter.exportProductsToCsv(context, currentProducts)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Inventory Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Inventory CSV"))
            } catch (e: Exception) {
                _message.value = "CSV Export failed: ${e.message}"
            }
        }
    }

    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            try {
                val currentProducts = products.value
                val file = CsvExporter.exportProductsToExcel(context, currentProducts)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.ms-excel"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "QuickBill Inventory Catalog")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open or Share Excel Catalog"))
            } catch (e: Exception) {
                _message.value = "Excel Export failed: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
