package com.example.cuoiki2.util

fun parsePrice(price: String): Long =
    price.replace(".", "").replace("đ", "").replace(",", "").trim().toLongOrNull() ?: 0L

fun formatPrice(amount: Long): String {
    val s = amount.toString()
    val result = StringBuilder()
    s.reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) result.append('.')
        result.append(c)
    }
    return result.reverse().toString() + "đ"
}

val categories = listOf("Tất cả", "Cà phê", "Trà", "Sữa chua", "Trà sữa")
