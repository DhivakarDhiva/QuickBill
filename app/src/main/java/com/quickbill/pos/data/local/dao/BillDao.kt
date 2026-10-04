package com.quickbill.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.model.BillStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE billNumber = :billNumber LIMIT 1")
    suspend fun getBillByNumber(billNumber: String): BillEntity?

    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getBillsByDateRange(startTime: Long, endTime: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getBillsByDateRangeSync(startTime: Long, endTime: Long): List<BillEntity>

    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    suspend fun getAllBillsSync(): List<BillEntity>

    @Query("""
        SELECT * FROM bills 
        WHERE billNumber LIKE '%' || :query || '%' 
        OR customerName LIKE '%' || :query || '%'
        OR customerPhone LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchBills(query: String): Flow<List<BillEntity>>

    @Query("UPDATE bills SET status = :status WHERE id = :billId")
    suspend fun updateBillStatus(billId: Long, status: BillStatus)

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getBillsCount(): Int

    @Query("SELECT MAX(id) FROM bills")
    suspend fun getMaxBillId(): Long?

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}
