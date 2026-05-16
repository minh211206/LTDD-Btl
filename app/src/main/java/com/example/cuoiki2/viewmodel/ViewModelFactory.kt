package com.example.cuoiki2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cuoiki2.repository.*

class AuthViewModelFactory(private val userRepo: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(userRepo) as T
    }
}

class CartViewModelFactory(private val orderRepo: OrderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CartViewModel(orderRepo) as T
    }
}

class AdminViewModelFactory(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository,
    private val userRepo: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AdminViewModel(productRepo, orderRepo, userRepo) as T
    }
}
