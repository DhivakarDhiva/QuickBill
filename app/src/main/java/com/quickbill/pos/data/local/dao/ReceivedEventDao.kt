package com.quickbill.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickbill.pos.data.local.entity.ReceivedEventEntity

@Dao
interface ReceivedEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivedEvent(event: ReceivedEventEntity)

    @Query("SELECT COUNT(*) FROM received_events WHERE eventId = :eventId")
    suspend fun countByEventId(eventId: String): Int

    suspend fun isEventReceived(eventId: String): Boolean {
        return countByEventId(eventId) > 0
    }

    @Query("SELECT * FROM received_events WHERE orderId = :orderId")
    suspend fun getEventsForOrder(orderId: String): List<ReceivedEventEntity>

    @Query("DELETE FROM received_events WHERE receivedAt < :cutoffTimestamp")
    suspend fun cleanOldEvents(cutoffTimestamp: Long)
}
