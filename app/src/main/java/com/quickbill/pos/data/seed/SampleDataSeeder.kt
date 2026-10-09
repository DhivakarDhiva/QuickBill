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

package com.quickbill.pos.data.seed

import com.quickbill.pos.data.local.dao.ProductDao
import com.quickbill.pos.data.local.dao.UserDao
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SampleDataSeeder {

    suspend fun seedInitialDataIfEmpty(
        productDao: ProductDao,
        userDao: UserDao
    ) = withContext(Dispatchers.IO) {
        // Seed default users if empty
        if (userDao.getUserCount() == 0) {
            val defaultUsers = listOf(
                UserEntity(
                    id = 1,
                    username = "admin",
                    fullName = "Store Manager",
                    pin = "1234",
                    role = UserRole.ADMIN
                ),
                UserEntity(
                    id = 2,
                    username = "cashier1",
                    fullName = "Rahul Sharma",
                    pin = "0000",
                    role = UserRole.CASHIER
                ),
                UserEntity(
                    id = 3,
                    username = "cashier2",
                    fullName = "Priya Patel",
                    pin = "1111",
                    role = UserRole.CASHIER
                )
            )
            userDao.insertUsers(defaultUsers)
        }

        // Seed products if empty
        if (productDao.getProductCount() == 0) {
            val sampleProducts = listOf(
                // Groceries & Staples (0% - 5% GST)
                ProductEntity(
                    name = "Basmati Rice (1kg)",
                    sku = "890103000101",
                    category = "Groceries",
                    price = 120.0,
                    taxRate = 5.0,
                    stockQuantity = 45,
                    minStockAlert = 10
                ),
                ProductEntity(
                    name = "Whole Wheat Atta (5kg)",
                    sku = "890103000102",
                    category = "Groceries",
                    price = 240.0,
                    taxRate = 0.0,
                    stockQuantity = 28,
                    minStockAlert = 5
                ),
                ProductEntity(
                    name = "Toor Dal (1kg)",
                    sku = "890103000103",
                    category = "Groceries",
                    price = 160.0,
                    taxRate = 5.0,
                    stockQuantity = 35,
                    minStockAlert = 8
                ),
                ProductEntity(
                    name = "Sugar Crystals (1kg)",
                    sku = "890103000104",
                    category = "Groceries",
                    price = 48.0,
                    taxRate = 5.0,
                    stockQuantity = 60,
                    minStockAlert = 15
                ),
                ProductEntity(
                    name = "Refined Sunflower Oil (1L)",
                    sku = "890103000105",
                    category = "Groceries",
                    price = 145.0,
                    taxRate = 5.0,
                    stockQuantity = 22,
                    minStockAlert = 6
                ),

                // Dairy & Cold (5% - 12% GST)
                ProductEntity(
                    name = "Amul Butter (500g)",
                    sku = "890103000201",
                    category = "Dairy",
                    price = 275.0,
                    taxRate = 12.0,
                    stockQuantity = 18,
                    minStockAlert = 5
                ),
                ProductEntity(
                    name = "Fresh Paneer (200g)",
                    sku = "890103000202",
                    category = "Dairy",
                    price = 90.0,
                    taxRate = 5.0,
                    stockQuantity = 14,
                    minStockAlert = 4
                ),
                ProductEntity(
                    name = "Greek Yogurt Blueberry (100g)",
                    sku = "890103000203",
                    category = "Dairy",
                    price = 60.0,
                    taxRate = 5.0,
                    stockQuantity = 2, // Low stock demo!
                    minStockAlert = 5
                ),
                ProductEntity(
                    name = "Full Cream Milk (1L)",
                    sku = "890103000204",
                    category = "Dairy",
                    price = 68.0,
                    taxRate = 0.0,
                    stockQuantity = 0, // Out of stock demo!
                    minStockAlert = 5
                ),

                // Beverages (12% - 28% GST)
                ProductEntity(
                    name = "Roasted Coffee Beans (250g)",
                    sku = "890103000301",
                    category = "Beverages",
                    price = 320.0,
                    taxRate = 5.0,
                    stockQuantity = 15,
                    minStockAlert = 3
                ),
                ProductEntity(
                    name = "Green Tea Lemon (25 bags)",
                    sku = "890103000302",
                    category = "Beverages",
                    price = 180.0,
                    taxRate = 5.0,
                    stockQuantity = 25,
                    minStockAlert = 5
                ),
                ProductEntity(
                    name = "Sparkling Soda Can (300ml)",
                    sku = "890103000303",
                    category = "Beverages",
                    price = 40.0,
                    taxRate = 28.0,
                    stockQuantity = 50,
                    minStockAlert = 10
                ),
                ProductEntity(
                    name = "Cold Pressed Orange Juice (500ml)",
                    sku = "890103000304",
                    category = "Beverages",
                    price = 95.0,
                    taxRate = 12.0,
                    stockQuantity = 12,
                    minStockAlert = 4
                ),

                // Snacks & Bakery (12% - 18% GST)
                ProductEntity(
                    name = "Dark Chocolate Almonds (150g)",
                    sku = "890103000401",
                    category = "Snacks",
                    price = 199.0,
                    taxRate = 18.0,
                    stockQuantity = 30,
                    minStockAlert = 5
                ),
                ProductEntity(
                    name = "Baked Potato Crisps (120g)",
                    sku = "890103000402",
                    category = "Snacks",
                    price = 65.0,
                    taxRate = 12.0,
                    stockQuantity = 40,
                    minStockAlert = 8
                ),
                ProductEntity(
                    name = "Artisan Sourdough Loaf (400g)",
                    sku = "890103000403",
                    category = "Snacks",
                    price = 110.0,
                    taxRate = 5.0,
                    stockQuantity = 8,
                    minStockAlert = 2
                ),

                // Personal & Home Care (18% GST)
                ProductEntity(
                    name = "Moisturizing Bath Soap (125g)",
                    sku = "890103000501",
                    category = "Personal Care",
                    price = 55.0,
                    taxRate = 18.0,
                    stockQuantity = 50,
                    minStockAlert = 10
                ),
                ProductEntity(
                    name = "Herbal Toothpaste (150g)",
                    sku = "890103000502",
                    category = "Personal Care",
                    price = 115.0,
                    taxRate = 18.0,
                    stockQuantity = 24,
                    minStockAlert = 5
                ),
                ProductEntity(
                    name = "Liquid Dishwash Gel (750ml)",
                    sku = "890103000503",
                    category = "Household",
                    price = 155.0,
                    taxRate = 18.0,
                    stockQuantity = 20,
                    minStockAlert = 5
                )
            )
            productDao.insertProducts(sampleProducts)
        }
    }
}
