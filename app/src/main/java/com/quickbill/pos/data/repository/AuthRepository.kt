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

package com.quickbill.pos.data.repository

import android.content.Context
import com.quickbill.pos.data.local.dao.UserDao
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val userDao: UserDao,
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("quickbill_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    suspend fun restoreSession(): UserEntity? {
        val savedUserId = prefs?.getLong("saved_user_id", -1L) ?: -1L
        if (savedUserId != -1L) {
            val user = userDao.getUserById(savedUserId)
            if (user != null && user.active) {
                _currentUser.value = user
                return user
            } else {
                prefs?.edit()?.remove("saved_user_id")?.apply()
            }
        }
        return null
    }

    private fun persistUser(user: UserEntity?) {
        _currentUser.value = user
        if (user != null) {
            prefs?.edit()?.putLong("saved_user_id", user.id)?.apply()
        } else {
            prefs?.edit()?.remove("saved_user_id")?.apply()
        }
    }

    fun getAllActiveUsers(): Flow<List<UserEntity>> = userDao.getAllActiveUsers()

    suspend fun loginWithPin(pin: String): Boolean {
        val user = userDao.getUserByPin(pin)
        return if (user != null) {
            persistUser(user)
            true
        } else {
            false
        }
    }

    suspend fun loginWithUsername(username: String, pin: String): Boolean {
        val user = userDao.getUserByUsername(username)
        return if (user != null && user.pin == pin) {
            persistUser(user)
            true
        } else {
            false
        }
    }

    fun switchUser(user: UserEntity) {
        persistUser(user)
    }

    fun logout() {
        persistUser(null)
    }

    fun isAdmin(): Boolean = _currentUser.value?.role == UserRole.ADMIN
}
