package com.example.cuoiki2
import com.example.cuoiki2.model.*
import com.example.cuoiki2.repository.AppRepository

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var adapter: OrderAdapter
    private var allOrders: List<Order> = emptyList()

    private val tabs = listOf("Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy")
    private var selectedTab = "Tất cả"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = intent.getStringExtra("username") ?: ""
        if (username.isBlank()) { finish(); return }

        binding.btnBack.setOnClickListener { finish() }

        adapter = OrderAdapter { order -> showOrderDetail(order) }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        setupTabs()

        AppRepository.orders.observe(this) { orders ->
            allOrders = orders.filter { it.username == username }
                .sortedByDescending { it.id }
            applyFilter()
        }
    }

    private fun setupTabs() {
        binding.tabContainer.removeAllViews()
        tabs.forEach { tab ->
            val tv = TextView(this).apply {
                text = tab
                textSize = 13f
                setPadding(dpToPx(16), 0, dpToPx(16), 0)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                setOnClickListener { selectTab(tab) }
            }
            binding.tabContainer.addView(tv)
            styleTab(tv, tab == selectedTab)
        }
    }

    private fun selectTab(tab: String) {
        selectedTab = tab
        for (i in 0 until binding.tabContainer.childCount) {
            val tv = binding.tabContainer.getChildAt(i) as TextView
            styleTab(tv, tv.text == tab)
        }
        applyFilter()
    }

    private fun styleTab(tv: TextView, selected: Boolean) {
        if (selected) {
            tv.setTextColor(Color.parseColor("#3E1C00"))
            tv.setTypeface(null, Typeface.BOLD)
            tv.setBackgroundResource(R.drawable.bg_tab_selected_bottom)
        } else {
            tv.setTextColor(Color.parseColor("#9E9E9E"))
            tv.setTypeface(null, Typeface.NORMAL)
            tv.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun applyFilter() {
        val filtered = if (selectedTab == "Tất cả") allOrders
        else allOrders.filter { it.status == selectedTab }

        adapter.submitList(filtered)
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOrders.visibility    = if (filtered.isEmpty()) View.GONE   else View.VISIBLE
    }

    private fun showOrderDetail(order: Order) {
        val items = order.items.joinToString("\n") { ci ->
            val unitPrice = parsePrice(ci.selectedPrice)
            "• ${ci.product.name} (${ci.size}) ×${ci.quantity} — ${formatPrice(unitPrice * ci.quantity)}"
        }
        val msg = buildString {
            append("Trạng thái: ${order.status}\n")
            append("Người nhận: ${order.recipientName.ifBlank { "Chưa có" }}\n")
            append("Số điện thoại: ${order.phone.ifBlank { "Chưa có" }}\n")
            append("Địa chỉ: ${order.address.ifBlank { "Chưa có" }}\n")
            append("\n$items\n\nTổng: ${formatPrice(order.total)}")
        }

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

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
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
