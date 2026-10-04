package com.quickbill.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickbill.pos.data.local.entity.BillPaymentEntity

@Dao
interface BillPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<BillPaymentEntity>)

    @Query("SELECT * FROM bill_payments WHERE billId = :billId")
    suspend fun getPaymentsForBill(billId: Long): List<BillPaymentEntity>

    @Query("DELETE FROM bill_payments")
    suspend fun deleteAllPayments()
}
