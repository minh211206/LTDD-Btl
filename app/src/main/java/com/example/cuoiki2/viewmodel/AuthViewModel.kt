package com.example.cuoiki2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuoiki2.model.UserAccount
import com.example.cuoiki2.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val userRepo: UserRepository) : ViewModel() {

    val currentUser = MutableLiveData<UserAccount?>(null)
    val loginState  = MutableLiveData<LoginState>(LoginState.Idle)
    val registerState = MutableLiveData<RegisterState>(RegisterState.Idle)

    val isLoggedIn get() = currentUser.value != null
    val isAdmin    get() = currentUser.value?.role == "admin"

    fun resetLoginState() { loginState.value = LoginState.Idle }
    fun resetRegisterState() { registerState.value = RegisterState.Idle }

    /** Dùng khi đã có UserAccount (sau khi login thành công từ LoginActivity) */
    fun login(user: UserAccount) { currentUser.value = user }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            loginState.value = LoginState.Error("Vui lòng nhập đầy đủ thông tin")
            return
        }
        loginState.value = LoginState.Loading
        viewModelScope.launch {
            val user = userRepo.login(username, password)
            when {
                user == null -> loginState.value = LoginState.Error("Tên đăng nhập hoặc mật khẩu không đúng")
                user.id == -1 -> loginState.value = LoginState.Error("Email chưa được xác nhận.\nVui lòng kiểm tra hộp thư và nhấn link xác nhận.")
                else -> {
                    currentUser.value = user
                    loginState.value = LoginState.Success(user)
                }
            }
        }
    }

    fun register(username: String, email: String, phone: String, password: String, confirm: String) {
        val error = when {
            username.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() ->
                "Vui lòng điền đầy đủ thông tin"
            username.length < 3  -> "Tên đăng nhập phải ít nhất 3 ký tự"
            phone.length != 10 || !phone.all { it.isDigit() } ->
                "Số điện thoại phải đủ 10 chữ số"
            password.length < 6  -> "Mật khẩu phải ít nhất 6 ký tự"
            password != confirm  -> "Mật khẩu nhập lại không khớp"
            else -> null
        }
        if (error != null) { registerState.value = RegisterState.Error(error); return }

        registerState.value = RegisterState.Loading
        viewModelScope.launch {
            val result = userRepo.register(username, email, phone, password)
            registerState.value = if (result == null) RegisterState.Success
                                  else RegisterState.Error(result)
        }
    }

    fun logout() {
        userRepo.logout()
        currentUser.value = null
    }

    fun changePassword(firestoreId: String, oldPw: String, newPw: String, confirm: String,
                       onSuccess: () -> Unit, onError: (String) -> Unit) {
        when {
            oldPw.isBlank() || newPw.isBlank() || confirm.isBlank() ->
                { onError("Vui lòng điền đầy đủ"); return }
            newPw.length < 6 -> { onError("Mật khẩu mới phải ít nhất 6 ký tự"); return }
            newPw != confirm  -> { onError("Mật khẩu nhập lại không khớp"); return }
        }
        viewModelScope.launch {
            val ok = userRepo.changePassword(firestoreId, oldPw, newPw)
            if (ok) onSuccess() else onError("Mật khẩu cũ không đúng")
        }
    }
}

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserAccount) : LoginState()
    data class Error(val message: String)     : LoginState()
}

sealed class RegisterState {
    object Idle    : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}
