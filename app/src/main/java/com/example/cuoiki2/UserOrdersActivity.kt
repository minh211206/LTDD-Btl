package com.example.cuoiki2
import com.example.cuoiki2.model.*
import com.example.cuoiki2.repository.AppRepository

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cuoiki2.databinding.ActivityUserOrdersBinding
import com.example.cuoiki2.databinding.ItemOrderBinding
import com.example.cuoiki2.util.orderStatusColors
import kotlinx.coroutines.launch

class UserOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserOrdersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = intent.getStringExtra("username") ?: ""
        if (username.isBlank()) { finish(); return }

        binding.btnBack.setOnClickListener { finish() }

        val adapter = OrderAdapter { order -> showOrderDetail(order) }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        AppRepository.orders.observe(this) { orders ->
            val myOrders = orders.filter { it.username == username }
                .sortedByDescending { it.id }
            adapter.submitList(myOrders)
            binding.layoutEmpty.visibility = if (myOrders.isEmpty()) View.VISIBLE else View.GONE
            binding.rvOrders.visibility    = if (myOrders.isEmpty()) View.GONE   else View.VISIBLE
        }
    }

    private fun showOrderDetail(order: Order) {
        val items = order.items.joinToString("\n") { ci ->
            val unitPrice = parsePrice(ci.selectedPrice)
            "• ${ci.product.name} (${ci.size}) ×${ci.quantity} — ${formatPrice(unitPrice * ci.quantity)}"
        }
        val msg = "Trạng thái: ${order.status}\nĐịa chỉ: ${order.address.ifBlank { "Chưa có" }}\n\n$items\n\nTổng: ${formatPrice(order.total)}"

        val builder = AlertDialog.Builder(this)
            .setTitle("Đơn hàng #${order.id}")
            .setMessage(msg)
            .setPositiveButton("Đóng", null)

        if (order.status == "Chờ xác nhận") {
            builder.setNegativeButton("Hủy đơn") { _, _ ->
                val app = application as ShopApplication
                lifecycleScope.launch {
                    app.orderRepo.updateStatus(order, "Đã hủy")
                }
            }
        }
        builder.show()
    }
}

class OrderAdapter(
    private val onClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.VH>(DIFF) {

    class VH(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = getItem(position)
        with(holder.binding) {
            tvOrderId.text      = "Đơn #${order.id}"
            tvOrderSummary.text = "${order.items.size} sản phẩm · ${formatPrice(order.total)}"
            tvAddress.text      = order.address.ifBlank { "" }
            tvStatus.text       = order.status

            val (textColor, bgColor) = orderStatusColors(order.status)
            tvStatus.setTextColor(Color.parseColor(textColor))
            tvStatus.setBackgroundColor(Color.parseColor(bgColor))

            root.setOnClickListener { onClick(order) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(a: Order, b: Order) = a.firestoreId == b.firestoreId
            override fun areContentsTheSame(a: Order, b: Order) = a == b
        }
    }
}
