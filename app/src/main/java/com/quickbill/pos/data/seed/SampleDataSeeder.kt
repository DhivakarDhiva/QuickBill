package com.quickbill.pos.data.seed

import com.quickbill.pos.data.local.dao.BillDao
import com.quickbill.pos.data.local.dao.BillItemDao
import com.quickbill.pos.data.local.dao.BillPaymentDao
import com.quickbill.pos.data.local.dao.ProductDao
import com.quickbill.pos.data.local.dao.UserDao
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.local.entity.BillItemEntity
import com.quickbill.pos.data.local.entity.BillPaymentEntity
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SampleDataSeeder {

    suspend fun seedInitialDataIfEmpty(
        productDao: ProductDao,
        userDao: UserDao,
        billDao: BillDao,
        billItemDao: BillItemDao,
        billPaymentDao: BillPaymentDao
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

        // Seed a few past bills if empty so history and daily report look rich immediately
        if (billDao.getBillsCount() == 0) {
            val now = System.currentTimeMillis()
            val hourMs = 3600_000L

            // Bill 1: Completed cash bill
            val bill1 = BillEntity(
                billNumber = "QB-20261001-0001",
                cashierId = 2,
                cashierName = "Rahul Sharma",
                customerName = "Anand Kumar",
                customerPhone = "9876543210",
                subtotal = 385.0,
                discountType = DiscountType.PERCENTAGE,
                discountValue = 5.0,
                discountAmount = 19.25,
                cgstAmount = 12.50,
                sgstAmount = 12.50,
                taxAmount = 25.00,
                grandTotal = 390.75,
                paymentMode = PaymentMode.CASH,
                cashTendered = 500.0,
                changeDue = 109.25,
                status = BillStatus.COMPLETED,
                timestamp = now - (3 * hourMs)
            )
            val bill1Id = billDao.insertBill(bill1)
            billItemDao.insertBillItems(
                listOf(
                    BillItemEntity(
                        billId = bill1Id,
                        productId = 1,
                        productName = "Basmati Rice (1kg)",
                        sku = "890103000101",
                        unitPrice = 120.0,
                        quantity = 2,
                        taxRate = 5.0,
                        taxableAmount = 240.0,
                        cgstAmount = 6.0,
                        sgstAmount = 6.0,
                        taxAmount = 12.0,
                        lineTotal = 252.0
                    ),
                    BillItemEntity(
                        billId = bill1Id,
                        productId = 5,
                        productName = "Refined Sunflower Oil (1L)",
                        sku = "890103000105",
                        unitPrice = 145.0,
                        quantity = 1,
                        taxRate = 5.0,
                        taxableAmount = 145.0,
                        cgstAmount = 3.63,
                        sgstAmount = 3.63,
                        taxAmount = 7.25,
                        lineTotal = 152.25
                    )
                )
            )
            billPaymentDao.insertPayments(
                listOf(
                    BillPaymentEntity(
                        billId = bill1Id,
                        mode = PaymentMode.CASH,
                        amount = 390.75,
                        referenceNote = "Tendered: ₹500.00, Change: ₹109.25"
                    )
                )
            )

            // Bill 2: Split payment bill (Cash + UPI)
            val bill2 = BillEntity(
                billNumber = "QB-20261001-0002",
                cashierId = 2,
                cashierName = "Rahul Sharma",
                customerName = "Sunita Verma",
                customerPhone = "9123456780",
                subtotal = 595.0,
                discountType = DiscountType.FLAT,
                discountValue = 50.0,
                discountAmount = 50.0,
                cgstAmount = 28.50,
                sgstAmount = 28.50,
                taxAmount = 57.00,
                grandTotal = 602.00,
                paymentMode = PaymentMode.SPLIT,
                cashTendered = 300.0,
                changeDue = 0.0,
                status = BillStatus.COMPLETED,
                timestamp = now - (1 * hourMs)
            )
            val bill2Id = billDao.insertBill(bill2)
            billItemDao.insertBillItems(
                listOf(
                    BillItemEntity(
                        billId = bill2Id,
                        productId = 6,
                        productName = "Amul Butter (500g)",
                        sku = "890103000201",
                        unitPrice = 275.0,
                        quantity = 1,
                        taxRate = 12.0,
                        taxableAmount = 275.0,
                        cgstAmount = 16.5,
                        sgstAmount = 16.5,
                        taxAmount = 33.0,
                        lineTotal = 308.0
                    ),
                    BillItemEntity(
                        billId = bill2Id,
                        productId = 10,
                        productName = "Roasted Coffee Beans (250g)",
                        sku = "890103000301",
                        unitPrice = 320.0,
                        quantity = 1,
                        taxRate = 5.0,
                        taxableAmount = 320.0,
                        cgstAmount = 8.0,
                        sgstAmount = 8.0,
                        taxAmount = 16.0,
                        lineTotal = 336.0
                    )
                )
            )
            billPaymentDao.insertPayments(
                listOf(
                    BillPaymentEntity(billId = bill2Id, mode = PaymentMode.CASH, amount = 300.0, referenceNote = "Cash portion"),
                    BillPaymentEntity(billId = bill2Id, mode = PaymentMode.UPI, amount = 302.0, referenceNote = "UPI ID: sunita@upi")
                )
            )
        }
    }
}
