package com.example.cuoiki2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuoiki2.model.CartItem
import com.example.cuoiki2.model.Order
import com.example.cuoiki2.model.Product
import com.example.cuoiki2.repository.OrderRepository
import com.example.cuoiki2.util.parsePrice
import kotlinx.coroutines.launch

class CartViewModel(private val orderRepo: OrderRepository) : ViewModel() {

    val cartItems   = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val orderState  = MutableLiveData<OrderState>(OrderState.Idle)

    val total get() = cartItems.value?.sumOf { parsePrice(it.selectedPrice) * it.quantity } ?: 0L
    val itemCount get() = cartItems.value?.sumOf { it.quantity } ?: 0

    fun addToCart(product: Product, size: String) {
        val list = cartItems.value ?: mutableListOf()
        val existing = list.find { it.product.id == product.id && it.size == size }
        if (existing != null) existing.quantity++ else list.add(CartItem(product, size, 1))
        cartItems.value = list
    }

    fun increaseQty(item: CartItem) {
        item.quantity++
        cartItems.value = cartItems.value
    }

    fun decreaseQty(item: CartItem) {
        if (item.quantity > 1) {
            item.quantity--
            cartItems.value = cartItems.value
        } else {
            removeItem(item)
        }
    }

    fun changeSize(item: CartItem, newSize: String) {
        val list = cartItems.value ?: return
        val index = list.indexOf(item)
        if (index == -1) return
        // Kiểm tra nếu đã có item cùng sản phẩm + size mới thì gộp lại
        val existing = list.find { it.product.id == item.product.id && it.size == newSize && it != item }
        if (existing != null) {
            existing.quantity += item.quantity
            list.removeAt(index)
        } else {
            list[index] = CartItem(
                product       = item.product,
                size          = newSize,
                quantity      = item.quantity,
                selectedPrice = item.product.priceForSize(newSize)
            )
        }
        cartItems.value = list
    }

    fun removeItem(item: CartItem) {
        cartItems.value?.remove(item)
        cartItems.value = cartItems.value
    }

    fun clearCart() { cartItems.value = mutableListOf() }

    fun resetOrderState() {
        orderState.value = OrderState.Idle
    }

    fun placeOrder(username: String, address: String) {
        val items = cartItems.value ?: return
        if (items.isEmpty()) return
        orderState.value = OrderState.Loading
        viewModelScope.launch {
            try {
                val order = Order(
                    username = username,
                    items    = items.toList(),
                    address  = address,
                    total    = total
                )
                orderRepo.placeOrder(order)
                clearCart()
                orderState.value = OrderState.Success
            } catch (e: Exception) {
                orderState.value = OrderState.Error("Đặt hàng thất bại: ${e.message}")
            }
        }
    }
}

sealed class OrderState {
    object Idle    : OrderState()
    object Loading : OrderState()
    object Success : OrderState()
    data class Error(val message: String) : OrderState()
}
