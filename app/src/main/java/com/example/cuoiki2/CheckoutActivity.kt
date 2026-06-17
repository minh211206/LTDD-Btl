package com.example.cuoiki2

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.cuoiki2.databinding.ActivityCheckoutBinding
import com.example.cuoiki2.util.formatPrice
import com.example.cuoiki2.util.parsePrice
import com.example.cuoiki2.viewmodel.OrderState

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ShopApplication
        val cartVm = app.cartViewModel
        val authVm = app.authViewModel

        cartVm.resetOrderState()

        binding.btnBack.setOnClickListener { finish() }

        // Điền sẵn tên và số điện thoại từ tài khoản nếu có
        val user = authVm.currentUser.value
        if (user != null) {
            binding.etName.setText(user.username)
        }

        // Hiển thị tóm tắt đơn hàng
        val items = cartVm.cartItems.value ?: emptyList()
        items.forEach { ci ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(6) }
            }
            val tvName = TextView(this).apply {
                text = "${ci.product.name} (${ci.size}) ×${ci.quantity}"
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#444444"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvPrice = TextView(this).apply {
                text = formatPrice(parsePrice(ci.selectedPrice) * ci.quantity)
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
            }
            row.addView(tvName)
            row.addView(tvPrice)
            binding.orderSummaryContainer.addView(row)
        }
        binding.tvTotal.text = formatPrice(cartVm.total)

        // Đặt hàng
        binding.btnPlaceOrder.setOnClickListener {
            if (user == null) {
                AlertDialog.Builder(this)
                    .setTitle("Yêu cầu đăng nhập")
                    .setMessage("Bạn cần đăng nhập để đặt hàng.")
                    .setPositiveButton("Đăng nhập") { _, _ ->
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    .setNegativeButton("Để sau", null).show()
                return@setOnClickListener
            }
            if (items.isEmpty()) {
                AlertDialog.Builder(this)
                    .setMessage("Giỏ hàng trống.")
                    .setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            val name    = binding.etName.text?.toString()?.trim() ?: ""
            val phone   = binding.etPhone.text?.toString()?.trim() ?: ""
            val address = binding.etAddress.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                binding.etName.error = "Vui lòng nhập họ tên"
                binding.etName.requestFocus(); return@setOnClickListener
            }
            if (phone.isEmpty() || phone.length != 10 || !phone.all { it.isDigit() }) {
                binding.etPhone.error = "Số điện thoại phải đủ 10 chữ số"
                binding.etPhone.requestFocus(); return@setOnClickListener
            }
            if (address.isEmpty()) {
                binding.etAddress.error = "Vui lòng nhập địa chỉ nhận hàng"
                binding.etAddress.requestFocus(); return@setOnClickListener
            }

            cartVm.placeOrder(user.username, name, phone, address)
        }

        cartVm.orderState.observe(this) { state ->
            when (state) {
                is OrderState.Loading -> {
                    binding.btnPlaceOrder.isEnabled = false
                    binding.btnPlaceOrder.text = "Đang xử lý..."
                }
                is OrderState.Success -> {
                    AlertDialog.Builder(this)
                        .setTitle("Đặt hàng thành công!")
                        .setMessage("Đơn hàng của bạn đã được ghi nhận.")
                        .setPositiveButton("Tuyệt vời!") { _, _ ->
                            // Quay về màn hình chính
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                        .setCancelable(false)
                        .show()
                }
                is OrderState.Error -> {
                    binding.btnPlaceOrder.isEnabled = true
                    binding.btnPlaceOrder.text = "Đặt hàng"
                    AlertDialog.Builder(this)
                        .setMessage((state as OrderState.Error).message)
                        .setPositiveButton("OK", null).show()
                }
                else -> {
                    binding.btnPlaceOrder.isEnabled = true
                    binding.btnPlaceOrder.text = "Đặt hàng"
                }
            }
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
