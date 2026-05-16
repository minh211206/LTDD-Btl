package com.example.cuoiki2.repository

import androidx.lifecycle.MutableLiveData
import com.example.cuoiki2.model.*

/**
 * Legacy singleton — giữ để tương thích với code cũ.
 * Code mới nên dùng ProductRepository, UserRepository, OrderRepository, ReviewRepository.
 */
object AppRepository {
    val products = MutableLiveData<List<Product>>(emptyList())
    val users    = MutableLiveData<List<UserAccount>>(emptyList())
    val orders   = MutableLiveData<List<Order>>(emptyList())
    val reviews  = MutableLiveData<List<Review>>(emptyList())

    private val _products = mutableListOf<Product>()
    private val _users    = mutableListOf<UserAccount>()
    private val _orders   = mutableListOf<Order>()
    private val _reviews  = mutableListOf<Review>()

    fun setProducts(list: List<Product>)  { _products.clear(); _products.addAll(list); products.postValue(list) }
    fun setUsers(list: List<UserAccount>) { _users.clear();    _users.addAll(list);    users.postValue(list) }
    fun setOrders(list: List<Order>)      { _orders.clear();   _orders.addAll(list);   orders.postValue(list) }
    fun setReviews(list: List<Review>)    { _reviews.clear();  _reviews.addAll(list);  reviews.postValue(list) }

    fun getProducts() = _products.toList()
    fun getUsers()    = _users.toList()
    fun getOrders()   = _orders.toList()
    fun getReviews()  = _reviews.toList()
}
