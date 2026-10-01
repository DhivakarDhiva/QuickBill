package com.quickbill.pos.data.repository

import androidx.room.withTransaction
import com.quickbill.pos.data.local.QuickBillDatabase
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.local.entity.BillItemEntity
import com.quickbill.pos.data.local.entity.BillPaymentEntity
import com.quickbill.pos.data.local.entity.HeldCartEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.model.CartItem
import com.quickbill.pos.data.model.CartSummary
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.model.PaymentSplit
import com.quickbill.pos.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingRepository(
    private val database: QuickBillDatabase
) {
    private val billDao = database.billDao()
    private val billItemDao = database.billItemDao()
    private val billPaymentDao = database.billPaymentDao()
    private val productDao = database.productDao()
    private val heldCartDao = database.heldCartDao()

    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()
    val allHeldCarts: Flow<List<HeldCartEntity>> = heldCartDao.getAllHeldCarts()

    fun searchBills(query: String): Flow<List<BillEntity>> {
        return billDao.searchBills(query.trim())
    }

    fun getBillsByDateRange(startTime: Long, endTime: Long): Flow<List<BillEntity>> {
        return billDao.getBillsByDateRange(startTime, endTime)
    }

    suspend fun getBillDetails(billId: Long): BillWithDetails? {
        val bill = billDao.getBillById(billId) ?: return null
        val items = billItemDao.getItemsForBill(billId)
        val payments = billPaymentDao.getPaymentsForBill(billId)
        return BillWithDetails(bill, items, payments)
    }

    suspend fun processCheckout(
        cartSummary: CartSummary,
        cashier: UserEntity,
        customerName: String,
        customerPhone: String,
        paymentSplit: PaymentSplit,
        paymentMode: PaymentMode,
        notes: String = ""
    ): Result<BillWithDetails> {
        if (cartSummary.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty"))
        }

        return try {
            val billWithDetails = database.withTransaction {
                // 1. Verify and decrement stock for all items
                for (item in cartSummary.items) {
                    val currentProduct = productDao.getProductById(item.product.id)
                        ?: throw IllegalStateException("Product '${item.product.name}' no longer exists.")
                    
                    if (currentProduct.stockQuantity < item.quantity) {
                        throw IllegalStateException(
                            "Insufficient stock for '${item.product.name}'. Available: ${currentProduct.stockQuantity}, Requested: ${item.quantity}"
                        )
                    }

                    val updatedCount = productDao.decrementStock(item.product.id, item.quantity)
                    if (updatedCount == 0) {
                        throw IllegalStateException("Failed to deduct stock for '${item.product.name}'")
                    }
                }

                // 2. Generate sequential Bill Number
                val nextId = (billDao.getMaxBillId() ?: 0L) + 1
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val todayStr = dateFormat.format(Date())
                val billNumber = "QB-$todayStr-${String.format(Locale.US, "%04d", nextId)}"

                // 3. Insert Bill Entity
                val billEntity = BillEntity(
                    billNumber = billNumber,
                    cashierId = cashier.id,
                    cashierName = cashier.fullName,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    subtotal = cartSummary.subtotal,
                    discountType = cartSummary.billDiscountType,
                    discountValue = cartSummary.billDiscountValue,
                    discountAmount = cartSummary.billDiscountAmount,
                    cgstAmount = cartSummary.cgstTotal,
                    sgstAmount = cartSummary.sgstTotal,
                    taxAmount = cartSummary.taxTotal,
                    grandTotal = cartSummary.grandTotal,
                    paymentMode = paymentMode,
                    cashTendered = paymentSplit.cashTendered,
                    changeDue = paymentSplit.changeDue,
                    notes = notes,
                    status = BillStatus.COMPLETED,
                    timestamp = System.currentTimeMillis()
                )

                val billId = billDao.insertBill(billEntity)
                val persistedBill = billEntity.copy(id = billId)

                // 4. Insert Bill Items
                val discountRatio = if (cartSummary.subtotal > 0.0) {
                    (cartSummary.taxableSubtotal) / (cartSummary.subtotal - cartSummary.itemDiscountTotal).coerceAtLeast(1.0)
                } else 1.0

                val billItemEntities = cartSummary.items.map { item ->
                    val itemTaxable = item.taxableAmount * discountRatio
                    BillItemEntity(
                        billId = billId,
                        productId = item.product.id,
                        productName = item.product.name,
                        sku = item.product.sku,
                        unitPrice = item.unitPrice,
                        quantity = item.quantity,
                        taxRate = item.taxRate,
                        itemDiscountType = item.discountType,
                        itemDiscountValue = item.discountValue,
                        itemDiscountAmount = item.itemDiscountAmount,
                        taxableAmount = itemTaxable,
                        cgstAmount = item.cgstAmount * discountRatio,
                        sgstAmount = item.sgstAmount * discountRatio,
                        taxAmount = item.taxAmount * discountRatio,
                        lineTotal = item.lineTotal
                    )
                }
                billItemDao.insertBillItems(billItemEntities)

                // 5. Insert Payments
                val payments = mutableListOf<BillPaymentEntity>()
                when (paymentMode) {
                    PaymentMode.CASH -> {
                        payments.add(
                            BillPaymentEntity(
                                billId = billId,
                                mode = PaymentMode.CASH,
                                amount = cartSummary.grandTotal,
                                referenceNote = "Tendered: ₹${String.format(Locale.US, "%.2f", paymentSplit.cashTendered)}, Change: ₹${String.format(Locale.US, "%.2f", paymentSplit.changeDue)}"
                            )
                        )
                    }
                    PaymentMode.CARD -> {
                        payments.add(
                            BillPaymentEntity(
                                billId = billId,
                                mode = PaymentMode.CARD,
                                amount = cartSummary.grandTotal,
                                referenceNote = paymentSplit.cardRef.ifBlank { "Card Approved" }
                            )
                        )
                    }
                    PaymentMode.UPI -> {
                        payments.add(
                            BillPaymentEntity(
                                billId = billId,
                                mode = PaymentMode.UPI,
                                amount = cartSummary.grandTotal,
                                referenceNote = paymentSplit.upiRef.ifBlank { "UPI Success" }
                            )
                        )
                    }
                    PaymentMode.SPLIT -> {
                        if (paymentSplit.cashAmount > 0) {
                            payments.add(
                                BillPaymentEntity(
                                    billId = billId,
                                    mode = PaymentMode.CASH,
                                    amount = paymentSplit.cashAmount,
                                    referenceNote = "Tendered: ₹${paymentSplit.cashTendered}, Change: ₹${paymentSplit.changeDue}"
                                )
                            )
                        }
                        if (paymentSplit.cardAmount > 0) {
                            payments.add(
                                BillPaymentEntity(
                                    billId = billId,
                                    mode = PaymentMode.CARD,
                                    amount = paymentSplit.cardAmount,
                                    referenceNote = paymentSplit.cardRef.ifBlank { "Card Part" }
                                )
                            )
                        }
                        if (paymentSplit.upiAmount > 0) {
                            payments.add(
                                BillPaymentEntity(
                                    billId = billId,
                                    mode = PaymentMode.UPI,
                                    amount = paymentSplit.upiAmount,
                                    referenceNote = paymentSplit.upiRef.ifBlank { "UPI Part" }
                                )
                            )
                        }
                    }
                }
                billPaymentDao.insertPayments(payments)

                BillWithDetails(persistedBill, billItemEntities, payments)
            }
            Result.success(billWithDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refundBill(billId: Long): Result<Unit> {
        return try {
            database.withTransaction {
                val bill = billDao.getBillById(billId)
                    ?: throw IllegalArgumentException("Bill not found")

                if (bill.status == BillStatus.REFUNDED) {
                    throw IllegalStateException("Bill has already been refunded")
                }

                // 1. Restore product stock for all bill items
                val items = billItemDao.getItemsForBill(billId)
                for (item in items) {
                    productDao.incrementStock(item.productId, item.quantity)
                }

                // 2. Mark bill status as REFUNDED
                billDao.updateBillStatus(billId, BillStatus.REFUNDED)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun holdCart(
        note: String,
        cartSummary: CartSummary
    ): Long {
        val jsonArray = JSONArray()
        for (item in cartSummary.items) {
            val obj = JSONObject().apply {
                put("productId", item.product.id)
                put("quantity", item.quantity)
                put("discountType", item.discountType.name)
                put("discountValue", item.discountValue)
            }
            jsonArray.put(obj)
        }

        val entity = HeldCartEntity(
            note = note.ifBlank { "Held Order #${System.currentTimeMillis() % 1000}" },
            itemsJson = jsonArray.toString(),
            discountType = cartSummary.billDiscountType,
            discountValue = cartSummary.billDiscountValue,
            totalAmount = cartSummary.grandTotal,
            itemCount = cartSummary.totalItemCount
        )
        return heldCartDao.insertHeldCart(entity)
    }

    suspend fun deleteHeldCart(id: Long) {
        heldCartDao.deleteHeldCartById(id)
    }
}
