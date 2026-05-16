package com.example.cuoiki2

import com.example.cuoiki2.util.categories as _categories
import com.example.cuoiki2.util.parsePrice as _parsePrice
import com.example.cuoiki2.util.formatPrice as _formatPrice

// Re-export utils để code cũ không cần đổi import
val categories get() = _categories
fun parsePrice(price: String) = _parsePrice(price)
fun formatPrice(amount: Long) = _formatPrice(amount)
