package com.quickbill.pos.data.repository

import com.quickbill.pos.data.local.dao.UserDao
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val userDao: UserDao) {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    fun getAllActiveUsers(): Flow<List<UserEntity>> = userDao.getAllActiveUsers()

    suspend fun loginWithPin(pin: String): Boolean {
        val user = userDao.getUserByPin(pin)
        return if (user != null) {
            _currentUser.value = user
            true
        } else {
            false
        }
    }

    suspend fun loginWithUsername(username: String, pin: String): Boolean {
        val user = userDao.getUserByUsername(username)
        return if (user != null && user.pin == pin) {
            _currentUser.value = user
            true
        } else {
            false
        }
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null

    fun isAdmin(): Boolean = _currentUser.value?.role == UserRole.ADMIN
}
