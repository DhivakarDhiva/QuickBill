package com.quickbill.pos.ui.screens.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.entity.HeldCartEntity
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.CartSummary
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.model.PaymentSplit
import com.quickbill.pos.data.repository.AuthRepository
import com.quickbill.pos.data.repository.BillingRepository
import com.quickbill.pos.data.repository.ProductRepository
import com.quickbill.pos.data.util.BillingCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

data class BillingUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val wholeBillDiscountType: DiscountType = DiscountType.NONE,
    val wholeBillDiscountValue: Double = 0.0,
    val cartSummary: CartSummary = CartSummary(),
    val isPaymentDialogOpen: Boolean = false,
    val isDiscountDialogOpen: Boolean = false,
    val itemForDiscount: CartItem? = null,
    val isBarcodeScannerOpen: Boolean = false,
    val completedBill: BillWithDetails? = null,
    val isProcessingCheckout: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BillingViewModel(
    private val productRepository: ProductRepository,
    private val billingRepository: BillingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("")
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    data class BillDiscount(
        val type: DiscountType = DiscountType.NONE,
        val value: Double = 0.0
    )

    private val _billDiscount = MutableStateFlow(BillDiscount())

    data class BillingDialogState(
        val isPaymentOpen: Boolean = false,
        val isWholeBillDiscountOpen: Boolean = false,
        val itemForDiscount: CartItem? = null,
        val isBarcodeScannerOpen: Boolean = false,
        val completedBill: BillWithDetails? = null,
        val isProcessingCheckout: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null
    )

    private val _dialogState = MutableStateFlow(BillingDialogState())

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
    val allCategories: StateFlow<List<String>> = productRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heldCarts: StateFlow<List<HeldCartEntity>> = billingRepository.allHeldCarts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        productRepository.allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.sku.contains(query, ignoreCase = true)
            val matchesCategory = category.isBlank() || product.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<BillingUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _cartItems,
        _billDiscount,
        _dialogState
    ) { query, category, items, discount, dialogs ->
        val summary = BillingCalculator.calculateCartSummary(
            items = items,
            billDiscountType = discount.type,
            billDiscountValue = discount.value
        )
        BillingUiState(
            searchQuery = query,
            selectedCategory = category,
            cartItems = items,
            wholeBillDiscountType = discount.type,
            wholeBillDiscountValue = discount.value,
            cartSummary = summary,
            isPaymentDialogOpen = dialogs.isPaymentOpen,
            isDiscountDialogOpen = dialogs.isWholeBillDiscountOpen || dialogs.itemForDiscount != null,
            itemForDiscount = dialogs.itemForDiscount,
            isBarcodeScannerOpen = dialogs.isBarcodeScannerOpen,
            completedBill = dialogs.completedBill,
            isProcessingCheckout = dialogs.isProcessingCheckout,
            errorMessage = dialogs.errorMessage,
            successMessage = dialogs.successMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BillingUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = if (_selectedCategory.value == category) "" else category
    }

    fun addToCart(product: ProductEntity) {
        // Stock check: Block selling out of stock items
        if (product.stockQuantity <= 0) {
            _dialogState.value = _dialogState.value.copy(
                errorMessage = "'${product.name}' is Out of Stock!"
            )
            return
        }

        val currentItems = _cartItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val existingItem = currentItems[existingIndex]
            if (existingItem.quantity >= product.stockQuantity) {
                _dialogState.value = _dialogState.value.copy(
                    errorMessage = "Cannot add more. Only ${product.stockQuantity} in stock for '${product.name}'"
                )
                return
            }
            currentItems[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            currentItems.add(CartItem(product = product, quantity = 1))
        }

        _cartItems.value = currentItems
    }

    fun updateItemQuantity(productId: Long, delta: Int) {
        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentItems[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentItems.removeAt(index)
            } else {
                if (newQty > item.product.stockQuantity) {
                    _dialogState.value = _dialogState.value.copy(
                        errorMessage = "Cannot exceed available stock (${item.product.stockQuantity}) for '${item.product.name}'"
                    )
                    return
                }
                currentItems[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = currentItems
        }
    }

    fun removeItem(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _billDiscount.value = BillDiscount()
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = productRepository.getProductBySku(barcode)
            if (product != null) {
                addToCart(product)
                _dialogState.value = _dialogState.value.copy(
                    successMessage = "Added '${product.name}' via barcode scan"
                )
            } else {
                _dialogState.value = _dialogState.value.copy(
                    errorMessage = "No product found with barcode: $barcode"
                )
            }
        }
    }

    // Discounts
    fun openItemDiscountDialog(item: CartItem) {
        _dialogState.value = _dialogState.value.copy(itemForDiscount = item)
    }

    fun openWholeBillDiscountDialog() {
        _dialogState.value = _dialogState.value.copy(isWholeBillDiscountOpen = true)
    }

    fun applyDiscount(type: DiscountType, value: Double) {
        val targetItem = _dialogState.value.itemForDiscount
        if (targetItem != null) {
            val maxItemEligible = targetItem.product.price * targetItem.quantity
            if (type == DiscountType.PERCENTAGE && (value < 0.0 || value > 100.0)) {
                _dialogState.value = _dialogState.value.copy(errorMessage = "Item discount percentage must be between 0% and 100%")
                return
            }
            if (type == DiscountType.FLAT && (value < 0.0 || value > maxItemEligible)) {
                _dialogState.value = _dialogState.value.copy(errorMessage = "Item flat discount cannot exceed item total (₹$maxItemEligible)")
                return
            }
            // Per-item discount
            val currentItems = _cartItems.value.toMutableList()
            val index = currentItems.indexOfFirst { it.product.id == targetItem.product.id }
            if (index >= 0) {
                currentItems[index] = targetItem.copy(discountType = type, discountValue = value)
                _cartItems.value = currentItems
            }
            _dialogState.value = _dialogState.value.copy(itemForDiscount = null)
        } else {
            val subtotal = _cartItems.value.sumOf { it.product.price * it.quantity }
            if (type == DiscountType.PERCENTAGE && (value < 0.0 || value > 100.0)) {
                _dialogState.value = _dialogState.value.copy(errorMessage = "Bill discount percentage must be between 0% and 100%")
                return
            }
            if (type == DiscountType.FLAT && (value < 0.0 || value > subtotal)) {
                _dialogState.value = _dialogState.value.copy(errorMessage = "Bill flat discount cannot exceed eligible subtotal (₹$subtotal)")
                return
            }
            // Whole bill discount
            _billDiscount.value = BillDiscount(type, value)
            _dialogState.value = _dialogState.value.copy(isWholeBillDiscountOpen = false)
        }
    }

    fun closeDiscountDialog() {
        _dialogState.value = _dialogState.value.copy(
            isWholeBillDiscountOpen = false,
            itemForDiscount = null
        )
    }

    // Hold / Park Cart
    fun holdCart(note: String) {
        val summary = uiState.value.cartSummary
        if (summary.items.isEmpty()) return

        viewModelScope.launch {
            try {
                billingRepository.holdCart(note, summary)
                clearCart()
                _dialogState.value = _dialogState.value.copy(
                    successMessage = "Order held successfully. You can resume it anytime!"
                )
            } catch (e: Exception) {
                _dialogState.value = _dialogState.value.copy(errorMessage = "Failed to hold cart: ${e.message}")
            }
        }
    }

    fun resumeCart(heldCart: HeldCartEntity) {
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray(heldCart.itemsJson)
                val restoredItems = mutableListOf<CartItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val productId = obj.getLong("productId")
                    val quantity = obj.getInt("quantity")
                    val discountTypeStr = obj.optString("discountType", DiscountType.NONE.name)
                    val discountValue = obj.optDouble("discountValue", 0.0)

                    val product = productRepository.getProductById(productId)
                    if (product != null) {
                        restoredItems.add(
                            CartItem(
                                product = product,
                                quantity = quantity,
                                discountType = try { DiscountType.valueOf(discountTypeStr) } catch (_: Exception) { DiscountType.NONE },
                                discountValue = discountValue
                            )
                        )
                    }
                }

                _cartItems.value = restoredItems
                _billDiscount.value = BillDiscount(heldCart.discountType, heldCart.discountValue)

                billingRepository.deleteHeldCart(heldCart.id)
                _dialogState.value = _dialogState.value.copy(
                    successMessage = "Restored order '${heldCart.note}'"
                )
            } catch (e: Exception) {
                _dialogState.value = _dialogState.value.copy(errorMessage = "Failed to resume order: ${e.message}")
            }
        }
    }

    // Payment & Checkout
    fun openPaymentDialog() {
        if (_cartItems.value.isEmpty()) {
            _dialogState.value = _dialogState.value.copy(errorMessage = "Cart is empty")
            return
        }
        _dialogState.value = _dialogState.value.copy(isPaymentOpen = true)
    }

    fun closePaymentDialog() {
        _dialogState.value = _dialogState.value.copy(isPaymentOpen = false)
    }

    fun completeCheckout(
        customerName: String,
        customerPhone: String,
        paymentMode: PaymentMode,
        paymentSplit: PaymentSplit
    ) {
        val user = currentUser.value
        if (user == null) {
            _dialogState.value = _dialogState.value.copy(errorMessage = "Please login first")
            return
        }

        viewModelScope.launch {
            _dialogState.value = _dialogState.value.copy(isProcessingCheckout = true)
            val result = billingRepository.processCheckout(
                cartSummary = uiState.value.cartSummary,
                cashier = user,
                customerName = customerName,
                customerPhone = customerPhone,
                paymentSplit = paymentSplit,
                paymentMode = paymentMode
            )

            result.onSuccess { billWithDetails ->
                _dialogState.value = _dialogState.value.copy(
                    isProcessingCheckout = false,
                    isPaymentOpen = false,
                    completedBill = billWithDetails,
                    successMessage = "Bill #${billWithDetails.bill.billNumber} generated successfully!"
                )
                clearCart()
            }.onFailure { error ->
                _dialogState.value = _dialogState.value.copy(
                    isProcessingCheckout = false,
                    errorMessage = error.message ?: "Checkout failed"
                )
            }
        }
    }

    fun closeReceiptDialog() {
        _dialogState.value = _dialogState.value.copy(completedBill = null)
    }

    fun openBarcodeScanner() {
        _dialogState.value = _dialogState.value.copy(isBarcodeScannerOpen = true)
    }

    fun closeBarcodeScanner() {
        _dialogState.value = _dialogState.value.copy(isBarcodeScannerOpen = false)
    }

    fun dismissMessage() {
        _dialogState.value = _dialogState.value.copy(errorMessage = null, successMessage = null)
    }
}
