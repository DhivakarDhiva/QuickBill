package com.quickbill.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quickbill.pos.data.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val pin: String, // 4-digit PIN for quick cashier login
    val role: UserRole = UserRole.CASHIER,
    val active: Boolean = true
)
