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
import com.quickbill.pos.data.local.dao.ProductDao
import com.quickbill.pos.data.local.dao.UserDao
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.local.entity.BillItemEntity
import com.quickbill.pos.data.local.entity.BillPaymentEntity
import com.quickbill.pos.data.local.entity.HeldCartEntity
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.local.entity.UserEntity

@Database(
    entities = [
        ProductEntity::class,
        BillEntity::class,
        BillItemEntity::class,
        BillPaymentEntity::class,
        UserEntity::class,
        HeldCartEntity::class
    ],
    version = 2,
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
