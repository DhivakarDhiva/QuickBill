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
import com.quickbill.pos.data.local.entity.PendingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingEvent(event: PendingEventEntity)

    @Query("SELECT * FROM pending_events ORDER BY createdAt ASC")
    suspend fun getAllPendingEvents(): List<PendingEventEntity>

    @Query("SELECT * FROM pending_events ORDER BY createdAt ASC")
    fun getAllPendingEventsFlow(): Flow<List<PendingEventEntity>>

    @Query("SELECT COUNT(*) FROM pending_events")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_events")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM pending_events WHERE eventId = :eventId")
    suspend fun deletePendingEvent(eventId: String)

    @Query("UPDATE pending_events SET retryCount = retryCount + 1, status = :status WHERE eventId = :eventId")
    suspend fun incrementRetryCount(eventId: String, status: String = "FAILED")

    @Query("DELETE FROM pending_events")
    suspend fun clearAllPendingEvents()
}
