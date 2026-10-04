package com.quickbill.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickbill.pos.data.local.entity.BillItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getItemsForBill(billId: Long): List<BillItemEntity>

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    fun getItemsForBillFlow(billId: Long): Flow<List<BillItemEntity>>

    @Query("""
        SELECT bi.productId, bi.productName, bi.sku, SUM(bi.quantity) as totalQty, SUM(bi.lineTotal) as totalRevenue
        FROM bill_items bi
        INNER JOIN bills b ON bi.billId = b.id
        WHERE b.status = 'COMPLETED'
        AND b.timestamp >= :startTime AND b.timestamp <= :endTime
        GROUP BY bi.productId, bi.productName, bi.sku
        ORDER BY totalQty DESC
        LIMIT :limit
    """)
    suspend fun getTopSellingItems(startTime: Long, endTime: Long, limit: Int = 5): List<TopSellingItemResult>

    @Query("""
        SELECT COALESCE(SUM(bi.quantity), 0)
        FROM bill_items bi
        INNER JOIN bills b ON bi.billId = b.id
        WHERE b.status = 'COMPLETED'
        AND b.timestamp >= :startTime AND b.timestamp <= :endTime
    """)
    suspend fun getTotalItemsSold(startTime: Long, endTime: Long): Int

    @Query("""
        SELECT bi.productId, bi.productName, bi.sku, SUM(bi.quantity) as totalQty, SUM(bi.lineTotal) as totalRevenue
        FROM bill_items bi
        INNER JOIN bills b ON bi.billId = b.id
        WHERE b.status = 'COMPLETED'
        AND b.timestamp >= :startTime AND b.timestamp <= :endTime
        GROUP BY bi.productId, bi.productName, bi.sku
        ORDER BY totalQty DESC
    """)
    suspend fun getAllSellingItems(startTime: Long, endTime: Long): List<TopSellingItemResult>

    @Query("DELETE FROM bill_items")
    suspend fun deleteAllBillItems()
}

data class TopSellingItemResult(
    val productId: Long,
    val productName: String,
    val sku: String,
    val totalQty: Int,
    val totalRevenue: Double
)
