package com.example.cuoiki2.firebase

import com.example.cuoiki2.model.CartItem
import com.example.cuoiki2.model.Order
import com.example.cuoiki2.model.Product
import com.example.cuoiki2.model.Review
import com.example.cuoiki2.model.UserAccount
import com.example.cuoiki2.repository.AppRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private val db = FirebaseFirestore.getInstance()

    fun init() {
        listenProducts()
        listenUsers()
        listenOrders()
        listenReviews()
    }

    // Products

    private fun listenProducts() {
        db.collection("products").addSnapshotListener { snap, err -> //data tự cập nhật không cần refresh
            if (err != null) { android.util.Log.e("FM", "products: ${err.message}"); return@addSnapshotListener }
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    Product(
                        id          = doc.id.hashCode(),
                        name        = doc.getString("name") ?: "",
                        price       = doc.getString("price") ?: "",
                        priceL      = doc.getString("priceL") ?: "",
                        category    = doc.getString("category") ?: "",
                        colorHex    = doc.getString("colorHex") ?: "#CCCCCC",
                        firestoreId = doc.id,
                        imageUrl    = doc.getString("imageUrl") ?: "",
                        stock       = (doc.getLong("stock") ?: 0L).toInt()
                    )
                } catch (e: Exception) { null }
            }
            AppRepository.setProducts(list)
        }
    }

    // Users

    private fun listenUsers() {
        db.collection("users").addSnapshotListener { snap, err ->
            if (err != null) { android.util.Log.e("FM", "users: ${err.message}"); return@addSnapshotListener }
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    UserAccount(
                        id          = doc.id.hashCode(),
                        username    = doc.getString("username") ?: "",
                        email       = doc.getString("email") ?: "",
                        password    = "",
                        role        = doc.getString("role") ?: "user",
                        firestoreId = doc.id,
                        avatarUrl   = doc.getString("avatarUrl") ?: ""
                    )
                } catch (e: Exception) { null }
            }
            AppRepository.setUsers(list)
        }
    }

    suspend fun changePassword(firestoreId: String, oldPassword: String, newPassword: String): Boolean {
        return try {
            val doc = db.collection("users").document(firestoreId).get().await()
            if (doc.getString("password") != oldPassword) return false
            db.collection("users").document(firestoreId).update("password", newPassword).await()
            true
        } catch (e: Exception) { false }
    }

    // Orders

    private fun listenOrders() {
        db.collection("orders").addSnapshotListener { snap, err ->
            if (err != null) { android.util.Log.e("FM", "orders: ${err.message}"); return@addSnapshotListener }
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val items = rawItems.map { item ->
                        CartItem(
                            product = Product(
                                id          = 0,
                                name        = item["productName"] as? String ?: "",
                                price       = item["price"] as? String ?: "",
                                firestoreId = item["productId"] as? String ?: ""
                            ),
                            size     = item["size"] as? String ?: "",
                            quantity = (item["quantity"] as? Long)?.toInt() ?: 1
                        )
                    }
                    Order(
                        id          = doc.id.hashCode(),
                        username    = doc.getString("username") ?: "",
                        items       = items,
                        address     = doc.getString("address") ?: "",
                        total       = doc.getLong("total") ?: 0L,
                        status      = doc.getString("status") ?: "Chờ xác nhận",
                        firestoreId = doc.id
                    )
                } catch (e: Exception) { null }
            }
            AppRepository.setOrders(list)
        }
    }

    fun updateOrderStatus(order: Order, status: String, onDone: () -> Unit = {}) {
        if (order.firestoreId.isEmpty()) return
        db.collection("orders").document(order.firestoreId).update("status", status)
            .addOnSuccessListener { onDone() }
    }

    //  Reviews

    private fun listenReviews() {
        db.collection("reviews").addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    Review(
                        id                 = doc.id.hashCode(),
                        productFirestoreId = doc.getString("productFirestoreId") ?: "",
                        username           = doc.getString("username") ?: "",
                        stars              = (doc.getLong("stars") ?: 5L).toInt(),
                        comment            = doc.getString("comment") ?: "",
                        createdAt          = (doc.getDouble("createdAt") ?: 0.0).toLong()
                    )
                } catch (e: Exception) { null }
            }
            AppRepository.setReviews(list)
        }
    }
}
