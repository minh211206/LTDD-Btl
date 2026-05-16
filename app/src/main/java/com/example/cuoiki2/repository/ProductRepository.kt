package com.example.cuoiki2.repository

import androidx.lifecycle.MutableLiveData
import com.example.cuoiki2.model.Product
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val db = Firebase.firestore
    val products = MutableLiveData<List<Product>>(emptyList())

    init { listenProducts() }

    private fun listenProducts() {
        db.collection("products").addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
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
            products.postValue(list)
        }
    }

    suspend fun addProduct(product: Product) {
        val model = mapOf(
            "name" to product.name, "price" to product.price,
            "priceL" to product.priceL,
            "category" to product.category, "colorHex" to product.colorHex,
            "imageUrl" to product.imageUrl, "stock" to product.stock,
            "createdAt" to System.currentTimeMillis().toDouble()
        )
        val ref = db.collection("products").add(model).await()
        db.collection("products").document(ref.id).update("id", ref.id).await()
    }

    suspend fun updateProduct(product: Product) {
        if (product.firestoreId.isEmpty()) return
        db.collection("products").document(product.firestoreId).update(
            mapOf("name" to product.name, "price" to product.price,
                  "priceL" to product.priceL,
                  "category" to product.category, "colorHex" to product.colorHex,
                  "imageUrl" to product.imageUrl, "stock" to product.stock)
        ).await()
    }

    suspend fun deleteProduct(product: Product) {
        if (product.firestoreId.isEmpty()) return
        db.collection("products").document(product.firestoreId).delete().await()
    }

    suspend fun updateStock(firestoreId: String, stock: Int) {
        if (firestoreId.isEmpty()) return
        db.collection("products").document(firestoreId).update("stock", stock).await()
    }

    suspend fun decreaseStock(firestoreId: String, qty: Int) {
        if (firestoreId.isEmpty()) return
        val doc = db.collection("products").document(firestoreId).get().await()
        val current = (doc.getLong("stock") ?: 0L).toInt()
        db.collection("products").document(firestoreId).update("stock", maxOf(0, current - qty)).await()
    }

    suspend fun increaseStock(firestoreId: String, qty: Int) {
        if (firestoreId.isEmpty()) return
        val doc = db.collection("products").document(firestoreId).get().await()
        val current = (doc.getLong("stock") ?: 0L).toInt()
        db.collection("products").document(firestoreId).update("stock", current + qty).await()
    }
}
