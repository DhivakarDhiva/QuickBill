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
