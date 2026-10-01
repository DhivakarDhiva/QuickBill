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

    val categories: StateFlow<List<String>> = productRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = combine(
        productRepository.allProducts,
        _searchQuery,
        _selectedCategory
    ) { all, query, cat ->
        all.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true)
            val matchesCat = cat.isBlank() || p.category.equals(cat, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editingProduct = MutableStateFlow<ProductFormData?>(null)
    val editingProduct: StateFlow<ProductFormData?> = _editingProduct.asStateFlow()

    private val _isAddDialogOpen = MutableStateFlow(false)
    val isAddDialogOpen: StateFlow<Boolean> = _isAddDialogOpen.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
                val entity = ProductEntity(
                    id = formData.id,
                    name = formData.name.trim(),
                    sku = formData.sku.trim(),
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

    fun clearMessage() {
        _message.value = null
    }
}
