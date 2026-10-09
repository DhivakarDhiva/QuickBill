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

package com.quickbill.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quickbill.pos.data.local.dao.BillDao
import com.quickbill.pos.data.local.dao.BillItemDao
import com.quickbill.pos.data.local.dao.BillPaymentDao
import com.quickbill.pos.data.local.dao.HeldCartDao
import com.quickbill.pos.data.local.dao.OrderDao
import com.quickbill.pos.data.local.dao.PendingEventDao
import com.quickbill.pos.data.local.dao.ProductDao
import com.quickbill.pos.data.local.dao.ReceivedEventDao
import com.quickbill.pos.data.local.dao.UserDao
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.local.entity.BillItemEntity
import com.quickbill.pos.data.local.entity.BillPaymentEntity
import com.quickbill.pos.data.local.entity.HeldCartEntity
import com.quickbill.pos.data.local.entity.OrderEntity
import com.quickbill.pos.data.local.entity.OrderItemEntity
import com.quickbill.pos.data.local.entity.PendingEventEntity
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.local.entity.ReceivedEventEntity
import com.quickbill.pos.data.local.entity.UserEntity

@Database(
    entities = [
        ProductEntity::class,
        BillEntity::class,
        BillItemEntity::class,
        BillPaymentEntity::class,
        UserEntity::class,
        HeldCartEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PendingEventEntity::class,
        ReceivedEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QuickBillDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun billDao(): BillDao
    abstract fun billItemDao(): BillItemDao
    abstract fun billPaymentDao(): BillPaymentDao
    abstract fun userDao(): UserDao
    abstract fun heldCartDao(): HeldCartDao
    abstract fun orderDao(): OrderDao
    abstract fun pendingEventDao(): PendingEventDao
    abstract fun receivedEventDao(): ReceivedEventDao


    companion object {
        @Volatile
        private var INSTANCE: QuickBillDatabase? = null

        fun getDatabase(context: Context): QuickBillDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuickBillDatabase::class.java,
                    "quickbill_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
