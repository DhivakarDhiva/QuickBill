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

package com.quickbill.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.quickbill.pos.data.local.entity.OrderEntity
import com.quickbill.pos.data.local.entity.OrderItemEntity
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE kitchen_orders SET status = :newStatus, updatedAt = :updatedAt WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM kitchen_orders WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM kitchen_orders WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY createdAt ASC")
    fun getActiveOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM kitchen_orders WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY createdAt ASC")
    suspend fun getActiveOrdersList(): List<OrderEntity>

    @Transaction
    suspend fun getAllActiveKitchenOrders(): List<KitchenOrder> {
        val activeEntities = getActiveOrdersList()
        return activeEntities.map { entity ->
            val items = getItemsForOrder(entity.orderId).map {
                KitchenOrderItem(
                    id = it.id,
                    orderId = it.orderId,
                    productId = it.productId,
                    name = it.name,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    notes = it.notes,
                    isVeg = it.isVeg
                )
            }
            KitchenOrder(
                orderId = entity.orderId,
                orderNumber = entity.orderNumber,
                customerName = entity.customerName,
                tableNumber = entity.tableNumber,
                notes = entity.notes,
                orderType = entity.orderType,
                status = entity.status,
                items = items,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                synced = entity.synced
            )
        }
    }

    @Query("SELECT * FROM kitchen_orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM kitchen_orders WHERE status = :status ORDER BY createdAt ASC")
    fun getOrdersByStatusFlow(status: OrderStatus): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM kitchen_order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM kitchen_order_items WHERE orderId = :orderId")
    fun getItemsForOrderFlow(orderId: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM kitchen_order_items")
    fun getAllOrderItemsFlow(): Flow<List<OrderItemEntity>>


    @Query("DELETE FROM kitchen_orders WHERE orderId = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Query("SELECT * FROM kitchen_orders WHERE orderNumber = :orderNumber LIMIT 1")
    suspend fun getOrderByOrderNumber(orderNumber: String): OrderEntity?

    @Query("DELETE FROM kitchen_order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: String)

    @Transaction
    suspend fun saveFullKitchenOrder(order: KitchenOrder) {
        // Prevent order duplications: Check if an order with the same orderNumber already exists
        val existingByNumber = getOrderByOrderNumber(order.orderNumber)

        // If an old record existed with different orderId, clean it up so we don't have duplicate orders
        if (existingByNumber != null && existingByNumber.orderId != order.orderId) {
            deleteOrderItems(existingByNumber.orderId)
            deleteOrder(existingByNumber.orderId)
        }

        val entity = OrderEntity(
            orderId = order.orderId,
            orderNumber = order.orderNumber,
            customerName = order.customerName,
            tableNumber = order.tableNumber,
            notes = order.notes,
            orderType = order.orderType,
            status = order.status,
            createdAt = existingByNumber?.createdAt ?: order.createdAt,
            updatedAt = order.updatedAt,
            synced = order.synced
        )
        insertOrder(entity)
        deleteOrderItems(order.orderId)
        val itemEntities = order.items.map {
            OrderItemEntity(
                orderId = order.orderId,
                productId = it.productId,
                name = it.name,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                notes = it.notes,
                isVeg = it.isVeg
            )
        }
        if (itemEntities.isNotEmpty()) {
            insertOrderItems(itemEntities)
        }
    }

    @Transaction
    suspend fun getFullKitchenOrder(orderId: String): KitchenOrder? {
        val orderEntity = getOrderById(orderId) ?: return null
        val items = getItemsForOrder(orderId).map {
            KitchenOrderItem(
                id = it.id,
                orderId = it.orderId,
                productId = it.productId,
                name = it.name,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                notes = it.notes,
                isVeg = it.isVeg
            )
        }
        return KitchenOrder(
            orderId = orderEntity.orderId,
            orderNumber = orderEntity.orderNumber,
            customerName = orderEntity.customerName,
            tableNumber = orderEntity.tableNumber,
            notes = orderEntity.notes,
            orderType = orderEntity.orderType,
            status = orderEntity.status,
            items = items,
            createdAt = orderEntity.createdAt,
            updatedAt = orderEntity.updatedAt,
            synced = orderEntity.synced
        )
    }

    @Query("SELECT COUNT(*) FROM kitchen_orders WHERE status != 'COMPLETED' AND status != 'CANCELLED'")
    fun getActiveOrdersCountFlow(): Flow<Int>
}
