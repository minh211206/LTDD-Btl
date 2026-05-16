package com.example.cuoiki2
import com.example.cuoiki2.model.*

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cuoiki2.databinding.ActivityCartBinding
import com.example.cuoiki2.databinding.ItemCartBinding
import com.example.cuoiki2.util.formatPrice
import com.example.cuoiki2.util.parsePrice
import com.example.cuoiki2.viewmodel.OrderState

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ShopApplication
        val appCartVm = app.cartViewModel

        // Reset state cũ khi mở lại màn hình
        appCartVm.resetOrderState()

        binding.btnBack.setOnClickListener { finish() }

        adapter = CartAdapter(
            onIncrease  = { appCartVm.increaseQty(it) },
            onDecrease  = { appCartVm.decreaseQty(it) },
            onSizeClick = { item ->
                val sizes = arrayOf("M", "L")
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Chọn size")
                    .setSingleChoiceItems(sizes, sizes.indexOf(item.size)) { dialog, which ->
                        appCartVm.changeSize(item, sizes[which])
                        dialog.dismiss()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter

        appCartVm.cartItems.observe(this) { items ->
            adapter.submitList(items.toList())
            binding.layoutEmpty.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvCart.visibility = if (items.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            binding.tvTotal.text = formatPrice(items.sumOf { parsePrice(it.selectedPrice) * it.quantity })
        }

        binding.btnOrder.setOnClickListener {
            val user = app.authViewModel.currentUser.value
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
            if (appCartVm.cartItems.value.isNullOrEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Giỏ hàng trống")
                    .setMessage("Vui lòng thêm sản phẩm vào giỏ hàng trước khi đặt hàng.")
                    .setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            val address = binding.etAddress.text?.toString()?.trim() ?: ""
            if (address.isEmpty()) {
                binding.etAddress.error = "Vui lòng nhập địa chỉ nhận hàng"
                binding.etAddress.requestFocus()
                return@setOnClickListener
            }
            appCartVm.placeOrder(user.username, address)
        }

        appCartVm.orderState.observe(this) { state ->
            when (state) {
                is OrderState.Success -> {
                    AlertDialog.Builder(this)
                        .setTitle("Đặt hàng thành công!")
                        .setMessage("Đơn hàng của bạn đã được ghi nhận.")
                        .setPositiveButton("Tuyệt vời!") { _, _ -> finish() }
                        .show()
                }
                is OrderState.Error -> {
                    AlertDialog.Builder(this)
                        .setMessage(state.message)
                        .setPositiveButton("OK", null).show()
                }
                else -> {}
            }
        }
    }
}

class CartAdapter(
    private val onIncrease:  (CartItem) -> Unit,
    private val onDecrease:  (CartItem) -> Unit,
    private val onSizeClick: (CartItem) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<CartAdapter.VH>() {

    private val items = mutableListOf<CartItem>()

    fun submitList(list: List<CartItem>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    class VH(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvName.text  = item.product.name
            tvSize.text  = "Size: ${item.size} ▾"
            tvSize.isClickable = true
            tvSize.setOnClickListener { onSizeClick(item) }
            tvQty.text   = item.quantity.toString()
            tvPrice.text = formatPrice(parsePrice(item.selectedPrice) * item.quantity)
            if (item.product.imageUrl.isNotBlank()) ivProduct.load(item.product.imageUrl) { crossfade(true) }
            else try { ivProduct.setBackgroundColor(android.graphics.Color.parseColor(item.product.colorHex)) }
                 catch (e: Exception) { ivProduct.setBackgroundColor(android.graphics.Color.LTGRAY) }
            btnIncrease.setOnClickListener { onIncrease(item) }
            btnDecrease.setOnClickListener { onDecrease(item) }
        }
    }
}
