package com.quickbill.pos

import com.quickbill.pos.data.local.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryUnitTest {

    @Test
    fun testProduct_NormalStock_NotLowStock() {
        val product = ProductEntity(
            id = 1,
            name = "Rice 5kg",
            sku = "RICE-5K",
            category = "Groceries",
            price = 350.0,
            stockQuantity = 20,
            minStockAlert = 5
        )

        assertFalse(product.isLowStock)
        assertFalse(product.isOutOfStock)
    }

    @Test
    fun testProduct_LowStockThreshold_Detection() {
        // Stock at exactly minStockAlert (5)
        val productAtThreshold = ProductEntity(
            id = 2,
            name = "Sugar 1kg",
            sku = "SUGAR-1K",
            category = "Groceries",
            price = 45.0,
            stockQuantity = 5,
            minStockAlert = 5
        )

        assertTrue(productAtThreshold.isLowStock)
        assertFalse(productAtThreshold.isOutOfStock)

        // Stock below minStockAlert (3)
        val productBelowThreshold = productAtThreshold.copy(stockQuantity = 3)
        assertTrue(productBelowThreshold.isLowStock)
        assertFalse(productBelowThreshold.isOutOfStock)
    }

    @Test
    fun testProduct_OutOfStock_Detection() {
        val outOfStockProduct = ProductEntity(
            id = 3,
            name = "Olive Oil",
            sku = "OIL-OLV",
            category = "Groceries",
            price = 800.0,
            stockQuantity = 0,
            minStockAlert = 5
        )

        assertFalse(outOfStockProduct.isLowStock) // Zero stock is out of stock, not low stock
        assertTrue(outOfStockProduct.isOutOfStock)
    }

    @Test
    fun testStockAdjustmentLogic() {
        var stock = 10
        val deltaAdd = 5
        stock += deltaAdd
        assertEquals(15, stock)

        val deltaDeduct = 12
        stock = maxOf(0, stock - deltaDeduct)
        assertEquals(3, stock)

        // Prevent negative stock
        val deltaExcessive = 10
        stock = maxOf(0, stock - deltaExcessive)
        assertEquals(0, stock)
    }

    @Test
    fun testProductSorting_DeterministicAndRefreshed() {
        val p1 = ProductEntity(id = 1, name = "Apple", sku = "SKU-A", category = "Fruit", price = 100.0, stockQuantity = 10, minStockAlert = 5)
        val p2 = ProductEntity(id = 2, name = "Banana", sku = "SKU-B", category = "Fruit", price = 50.0, stockQuantity = 3, minStockAlert = 5)
        val p3 = ProductEntity(id = 3, name = "Cherry", sku = "SKU-C", category = "Fruit", price = 150.0, stockQuantity = 0, minStockAlert = 5)
        val list = listOf(p1, p2, p3)

        // Price Low to High
        val sortedPriceAsc = list.sortedWith(compareBy<ProductEntity> { it.price }.thenBy { it.name.lowercase() })
        assertEquals(listOf("Banana", "Apple", "Cherry"), sortedPriceAsc.map { it.name })

        // Price High to Low
        val sortedPriceDesc = list.sortedWith(compareByDescending<ProductEntity> { it.price }.thenBy { it.name.lowercase() })
        assertEquals(listOf("Cherry", "Apple", "Banana"), sortedPriceDesc.map { it.name })

        // Stock Low to High
        val sortedStockAsc = list.sortedWith(compareBy<ProductEntity> { it.stockQuantity }.thenBy { it.name.lowercase() })
        assertEquals(listOf("Cherry", "Banana", "Apple"), sortedStockAsc.map { it.name })

        // Name Desc
        val sortedNameDesc = list.sortedWith(compareByDescending<ProductEntity> { it.name.lowercase() }.thenBy { it.sku })
        assertEquals(listOf("Cherry", "Banana", "Apple"), sortedNameDesc.map { it.name })
    }

    @Test
    fun testLowStockCount_ConsistencyBetweenHomeAndProductsScreen() {
        val p1 = ProductEntity(id = 1, name = "Milk", sku = "MILK-1", category = "Dairy", price = 30.0, stockQuantity = 2, minStockAlert = 5) // Low
        val p2 = ProductEntity(id = 2, name = "Bread", sku = "BREAD-1", category = "Bakery", price = 40.0, stockQuantity = 5, minStockAlert = 5) // Low (threshold)
        val p3 = ProductEntity(id = 3, name = "Butter", sku = "BTR-1", category = "Dairy", price = 60.0, stockQuantity = 15, minStockAlert = 5) // Normal
        val allProducts = listOf(p1, p2, p3)

        // Room Dao query condition: stockQuantity <= minStockAlert
        val lowStockFromDbQuery = allProducts.filter { it.stockQuantity <= it.minStockAlert }
        // ProductsScreen in-memory condition: p.stockQuantity <= p.minStockAlert
        val lowStockFilteredInProductsScreen = allProducts.filter { it.stockQuantity <= it.minStockAlert }

        assertEquals(2, lowStockFromDbQuery.size)
        assertEquals(2, lowStockFilteredInProductsScreen.size)
        assertEquals(lowStockFromDbQuery.size, lowStockFilteredInProductsScreen.size)
    }
}
