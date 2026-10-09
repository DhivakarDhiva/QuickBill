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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickbill.pos.data.local.entity.HeldCartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeldCartDao {
    @Query("SELECT * FROM held_carts ORDER BY createdAt DESC")
    fun getAllHeldCarts(): Flow<List<HeldCartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldCart(cart: HeldCartEntity): Long

    @Query("DELETE FROM held_carts WHERE id = :id")
    suspend fun deleteHeldCartById(id: Long)

    @Delete
    suspend fun deleteHeldCart(cart: HeldCartEntity)

    @Query("DELETE FROM held_carts")
    suspend fun deleteAllHeldCarts()
}
