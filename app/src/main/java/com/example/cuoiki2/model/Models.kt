package com.example.cuoiki2.model


data class Product(
    val id: Int = 0,
    val name: String = "",
    val price: String = "",       // giá size M
    val priceL: String = "",      // giá size L
    val category: String = "",
    val colorHex: String = "#CCCCCC",
    val firestoreId: String = "",
    val imageUrl: String = "",
    val stock: Int = 0
) {
    // Lấy giá theo size
    fun priceForSize(size: String): String = when (size) {
        "L"  -> priceL.ifBlank { price }
        else -> price  // M
    }
}

data class CartItem(
    val product: Product,
    val size: String,
    var quantity: Int,
    val selectedPrice: String = product.priceForSize(size)  // giá theo size đã chọn
)

data class UserAccount(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "user",
    val firestoreId: String = "",
    val avatarUrl: String = ""
)

data class Order(
    val id: Int = 0,
    val username: String = "",
    val items: List<CartItem> = emptyList(),
    val recipientName: String = "",
    val phone: String = "",
    val address: String = "",
    val total: Long = 0L,
    var status: String = "Chờ xác nhận",
    val firestoreId: String = ""
)

data class Review(
    val id: Int = 0,
    val productFirestoreId: String = "",
    val username: String = "",
    val stars: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
