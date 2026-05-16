package com.example.cuoiki2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuoiki2.model.Order
import com.example.cuoiki2.model.Product
import com.example.cuoiki2.model.UserAccount
import com.example.cuoiki2.repository.OrderRepository
import com.example.cuoiki2.repository.ProductRepository
import com.example.cuoiki2.repository.UserRepository
import kotlinx.coroutines.launch

class AdminViewModel(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    val products = productRepo.products
    val orders   = orderRepo.orders
    val users    = userRepo.users

    // Products
    fun addProduct(product: Product)    = viewModelScope.launch { productRepo.addProduct(product) }
    fun updateProduct(product: Product) = viewModelScope.launch { productRepo.updateProduct(product) }
    fun deleteProduct(product: Product) = viewModelScope.launch { productRepo.deleteProduct(product) }
    fun updateStock(id: String, qty: Int) = viewModelScope.launch { productRepo.updateStock(id, qty) }

    // Orders
    fun updateOrderStatus(order: Order, status: String) =
        viewModelScope.launch { orderRepo.updateStatus(order, status) }

    // Users
    fun deleteUser(user: UserAccount) = viewModelScope.launch { userRepo.deleteUser(user) }
}
