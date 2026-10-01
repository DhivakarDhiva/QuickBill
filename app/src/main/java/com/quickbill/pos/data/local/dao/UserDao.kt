package com.quickbill.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quickbill.pos.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE active = 1")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE pin = :pin AND active = 1 LIMIT 1")
    suspend fun getUserByPin(pin: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND active = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
