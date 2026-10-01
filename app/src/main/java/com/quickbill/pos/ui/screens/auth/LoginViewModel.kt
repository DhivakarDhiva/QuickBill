package com.quickbill.pos.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val enteredPin: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val activeUsers: StateFlow<List<UserEntity>> = authRepository.getAllActiveUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDigitEntered(digit: String) {
        if (_uiState.value.enteredPin.length < 4) {
            val newPin = _uiState.value.enteredPin + digit
            _uiState.value = _uiState.value.copy(enteredPin = newPin, error = null)
            if (newPin.length == 4) {
                attemptLogin(newPin)
            }
        }
    }

    fun onBackspace() {
        val current = _uiState.value.enteredPin
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(enteredPin = current.dropLast(1), error = null)
        }
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(enteredPin = "", error = null)
    }

    fun quickSelectUser(user: UserEntity) {
        authRepository.switchUser(user)
        _uiState.value = _uiState.value.copy(isSuccess = true, error = null)
    }

    private fun attemptLogin(pin: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = authRepository.loginWithPin(pin)
            if (success) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    enteredPin = "",
                    error = "Invalid PIN. Try '1234' for Admin or '0000' for Cashier."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}
