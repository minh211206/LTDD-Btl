package com.example.cuoiki2.util

import android.content.Context

fun Context.dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

fun orderStatusColors(status: String): Pair<String, String> = when (status) {
    "Chờ xác nhận" -> "#F59E0B" to "#FFF8E1"
    "Đã xác nhận"  -> "#1565C0" to "#E3F2FD"
    "Đang giao"    -> "#6A1B9A" to "#F3E5F5"
    "Đã giao"      -> "#2E7D32" to "#E8F5E9"
    "Đã hủy"       -> "#E53935" to "#FFEBEE"
    else           -> "#9E9E9E" to "#F5F5F5"
}
