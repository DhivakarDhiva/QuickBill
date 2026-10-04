package com.quickbill.pos.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.UserRole
import com.quickbill.pos.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "admin",
    val password: String = "1234",
    val selectedRole: UserRole = UserRole.ADMIN,
    val isPasswordVisible: Boolean = false,
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

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun onRoleSelected(role: UserRole) {
        val defaultUser = if (role == UserRole.ADMIN) "admin" else "cashier1"
        val defaultPass = if (role == UserRole.ADMIN) "1234" else "0000"
        _uiState.value = _uiState.value.copy(
            selectedRole = role,
            username = defaultUser,
            password = defaultPass,
            error = null
        )
    }

    fun quickSelectUser(user: UserEntity) {
        authRepository.switchUser(user)
        _uiState.value = _uiState.value.copy(isSuccess = true, error = null)
    }

    fun attemptLogin() {
        val current = _uiState.value
        val userText = current.username.trim()
        val passText = current.password.trim()

        if (userText.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter username")
            return
        }
        if (passText.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter password/PIN")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Try username + pin match
            val success = authRepository.loginWithUsername(userText, passText)
            if (success) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                // Also check if PIN directly matches any user of selected role
                val pinSuccess = authRepository.loginWithPin(passText)
                if (pinSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Invalid credentials. Use 'admin' / '1234' or 'cashier1' / '0000'."
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}
