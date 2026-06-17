package com.example.cuoiki2.repository

import androidx.lifecycle.MutableLiveData
import com.example.cuoiki2.model.CartItem
import com.example.cuoiki2.model.Order
import com.example.cuoiki2.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OrderRepository(private val productRepo: ProductRepository) {

    private val db = FirebaseFirestore.getInstance()
    val orders = MutableLiveData<List<Order>>(emptyList())

    init { listenOrders() }

    private fun listenOrders() {
        db.collection("orders").addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val items = rawItems.map { item ->
                        val selectedPrice = item["selectedPrice"] as? String ?: ""
                        CartItem(
                            product = Product(
                                id          = 0,
                                name        = item["productName"] as? String ?: "",
                                price       = item["price"] as? String ?: "",
                                firestoreId = item["productId"] as? String ?: ""
                            ),
                            size          = item["size"] as? String ?: "",
                            quantity      = (item["quantity"] as? Long)?.toInt() ?: 1,
                            selectedPrice = selectedPrice.ifBlank { item["price"] as? String ?: "" }
                        )
                    }
                    Order(
                        id            = doc.id.hashCode(),
                        username      = doc.getString("username") ?: "",
                        items         = items,
                        recipientName = doc.getString("recipientName") ?: "",
                        phone         = doc.getString("phone") ?: "",
                        address       = doc.getString("address") ?: "",
                        total         = doc.getLong("total") ?: 0L,
                        status        = doc.getString("status") ?: "Chờ xác nhận",
                        firestoreId   = doc.id
                    )
                } catch (e: Exception) { null }
            }
            orders.postValue(list)
        }
    }

    suspend fun placeOrder(order: Order) {
        val model = mapOf(
            "username"      to order.username,
            "recipientName" to order.recipientName,
            "phone"         to order.phone,
            "address"       to order.address,
            "items"         to order.items.map { ci -> mapOf(
                "productId"     to ci.product.firestoreId,
                "productName"   to ci.product.name,
                "price"         to ci.selectedPrice,
                "selectedPrice" to ci.selectedPrice,
                "size"          to ci.size,
                "quantity"      to ci.quantity
            )},
            "address"   to order.address,
            "total"     to order.total,
            "status"    to order.status,
            "createdAt" to System.currentTimeMillis().toDouble()
        )
        val ref = db.collection("orders").add(model).await()
        db.collection("orders").document(ref.id).update("id", ref.id).await()
        order.items.forEach { ci ->
            if (ci.product.firestoreId.isNotEmpty())
                productRepo.decreaseStock(ci.product.firestoreId, ci.quantity)
        }
    }

    suspend fun updateStatus(order: Order, status: String) {
        if (order.firestoreId.isEmpty()) return
        db.collection("orders").document(order.firestoreId).update("status", status).await()
        if (status == "Đã hủy") {
            order.items.forEach { ci ->
                if (ci.product.firestoreId.isNotEmpty())
                    productRepo.increaseStock(ci.product.firestoreId, ci.quantity)
            }
        }
    }
}
