package com.example.cuoiki2
import com.example.cuoiki2.model.*

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cuoiki2.databinding.ActivityAdminBinding
import com.example.cuoiki2.viewmodel.AdminViewModel
import com.example.cuoiki2.viewmodel.AdminViewModelFactory
import coil.load

// ── Main Activity ─────────────────────────────────────────────────────────────
class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    val sharedVm get() = (application as ShopApplication).authViewModel
    lateinit var adminVm: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val app = application as ShopApplication
        adminVm = androidx.lifecycle.ViewModelProvider(this,
            AdminViewModelFactory(app.productRepo, app.orderRepo, app.userRepo)
        )[AdminViewModel::class.java]
        if (savedInstanceState == null) showHome()
    }

    fun showHome() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, AdminHomeFragment())
            .commit()
    }

    fun showFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, f)
            .addToBackStack(null)
            .commit()
    }

    fun logout() { sharedVm.logout(); finish() }
}

// ── Home ──────────────────────────────────────────────────────────────────────
class AdminHomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_admin_home, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = activity as? AdminActivity ?: return

        view.findViewById<View>(R.id.card_products).setOnClickListener {
            act.showFragment(AdminProductsFragment())
        }
        view.findViewById<View>(R.id.card_orders).setOnClickListener {
            act.showFragment(AdminOrdersFragment())
        }
        view.findViewById<View>(R.id.card_inventory).setOnClickListener {
            act.showFragment(AdminInventoryFragment())
        }
        view.findViewById<View>(R.id.card_users).setOnClickListener {
            act.showFragment(AdminUsersFragment())
        }
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ -> act.logout() }
                .setNegativeButton("Hủy", null).show()
        }

        // Update stats
        act.adminVm.products.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_products)?.text = it.size.toString()
        }
        act.adminVm.orders.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_orders)?.text = it.size.toString()
        }
        act.adminVm.users.observe(viewLifecycleOwner) {
            view.findViewById<TextView>(R.id.tv_stat_users)?.text = it.size.toString()
        }
    }
}

// ── Products ──────────────────────────────────────────────────────────────────
class AdminProductsFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: AdminProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_admin_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_admin_title).text = "Quản lý Đồ uống"
        view.findViewById<View>(R.id.btn_admin_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.fab_add).visibility = View.VISIBLE
        view.findViewById<View>(R.id.fab_add).setOnClickListener { showProductDialog(null) }

        adapter = AdminProductAdapter(
            onEdit = { showProductDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        rv = view.findViewById(R.id.rv_admin)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        (activity as? AdminActivity)?.adminVm?.products?.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun showProductDialog(existing: Product?) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        fun field(hint: String, value: String = "", inputType: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
            return EditText(ctx).apply {
                this.hint = hint; setText(value); this.inputType = inputType
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = 16 }
            }
        }
        val etName   = field("Tên món", existing?.name ?: "")
        val etPrice  = field("Giá size M (vd: 25.000d)", existing?.price ?: "")
        val etPriceL = field("Giá size L (vd: 30.000d)", existing?.priceL ?: "")
        val etImage  = field("Link ảnh (URL)", existing?.imageUrl ?: "")
        val etStock  = field("Tồn kho", existing?.stock?.toString() ?: "0", android.text.InputType.TYPE_CLASS_NUMBER)

        // Spinner cho danh mục thay vì EditText
        val categoryList = listOf("Cà phê", "Trà", "Đá xay", "Bánh")
        val tvCategoryLabel = TextView(ctx).apply {
            text = "Danh mục:"
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = 4 }
        }
        val spinnerCategory = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, categoryList)
            val idx = categoryList.indexOf(existing?.category ?: "")
            setSelection(if (idx >= 0) idx else 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = 16 }
        }

        listOf(etName, etPrice, etPriceL, tvCategoryLabel, spinnerCategory, etImage, etStock).forEach { layout.addView(it) }

        AlertDialog.Builder(ctx)
            .setTitle(if (existing == null) "Thêm món mới" else "Sửa món")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val p = Product(
                    id          = existing?.id ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    name        = etName.text.toString().trim().uppercase(),
                    price       = etPrice.text.toString().trim(),
                    priceL      = etPriceL.text.toString().trim(),
                    category    = spinnerCategory.selectedItem.toString(),
                    colorHex    = "#A0522D",
                    firestoreId = existing?.firestoreId ?: "",
                    imageUrl    = etImage.text.toString().trim(),
                    stock       = etStock.text.toString().toIntOrNull() ?: 0
                )
                if (existing != null) (activity as? AdminActivity)?.adminVm?.updateProduct(p)
                else (activity as? AdminActivity)?.adminVm?.addProduct(p)
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun confirmDelete(p: Product) {
        AlertDialog.Builder(requireContext())            .setTitle("Xóa món")
            .setMessage("Xóa \"${p.name}\"?")
            .setPositiveButton("Xóa") { _, _ -> (activity as? AdminActivity)?.adminVm?.deleteProduct(p) }
            .setNegativeButton("Hủy", null).show()
    }
}

class AdminProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, AdminProductAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        val v = holder.itemView
        v.findViewById<TextView>(R.id.tv_product_name).text = p.name
        v.findViewById<TextView>(R.id.tv_product_price).text = p.price
        v.findViewById<TextView>(R.id.tv_product_category).text = p.category
        v.findViewById<TextView>(R.id.tv_product_stock).text = "Kho: ${p.stock}"
        val iv = v.findViewById<ImageView>(R.id.iv_product_thumb)
        if (p.imageUrl.isNotBlank()) iv.load(p.imageUrl) { crossfade(true) }
        else try { iv.setBackgroundColor(Color.parseColor(p.colorHex)) } catch (e: Exception) { iv.setBackgroundColor(Color.LTGRAY) }
        v.findViewById<View>(R.id.btn_edit).setOnClickListener { onEdit(p) }
        v.findViewById<View>(R.id.btn_delete).setOnClickListener { onDelete(p) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}

// ── Orders ────────────────────────────────────────────────────────────────────
class AdminOrdersFragment : Fragment() {

    private lateinit var adapter: AdminOrderAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_admin_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_admin_title).text = "Quản lý Đơn hàng"
        view.findViewById<View>(R.id.btn_admin_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.fab_add).visibility = View.GONE

        adapter = AdminOrderAdapter { order -> showOrderDetail(order) }
        view.findViewById<RecyclerView>(R.id.rv_admin).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminOrdersFragment.adapter
        }
        (activity as? AdminActivity)?.adminVm?.orders?.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun showOrderDetail(order: Order) {
        val ctx = requireContext()
        val statuses = arrayOf("Chờ xác nhận","Đã xác nhận","Đang giao","Đã nhận hàng","Hoàn thành","Đã hủy")
        var currentStatus = order.status
        val items = order.items.joinToString("\n") { "• ${it.product.name} x${it.quantity} — ${formatPrice(parsePrice(it.product.price)*it.quantity)}" }

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 8)
        }
        layout.addView(TextView(ctx).apply {
            text = "Khách: ${order.username}\nĐịa chỉ: ${order.address.ifBlank{"Chưa có"}}\n\n$items\n\nTổng: ${formatPrice(order.total)}"
            textSize = 13f; setPadding(0, 0, 0, 16)
        })
        layout.addView(TextView(ctx).apply { text = "Cập nhật trạng thái:"; textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD) })
        val spinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, statuses)
            setSelection(statuses.indexOf(order.status).coerceAtLeast(0))
        }
        layout.addView(spinner)

        AlertDialog.Builder(ctx)
            .setTitle("Đơn #${order.id}")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val newStatus = spinner.selectedItem.toString()
                if (newStatus != order.status)
                    (activity as? AdminActivity)?.adminVm?.updateOrderStatus(order, newStatus)
            }
            .setNegativeButton("Đóng", null).show()
    }
}

class AdminOrderAdapter(
    private val onClick: (Order) -> Unit
) : ListAdapter<Order, AdminOrderAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = getItem(position)
        val v = holder.itemView
        v.findViewById<TextView>(R.id.tv_order_id).text = "Đơn #${o.id} — ${o.username}"
        v.findViewById<TextView>(R.id.tv_order_info).text = "${o.items.size} món · ${formatPrice(o.total)}"
        val tvStatus = v.findViewById<TextView>(R.id.tv_order_status)
        tvStatus.text = o.status
        val (tc, bg) = statusColors(o.status)
        tvStatus.setTextColor(Color.parseColor(tc))
        tvStatus.setBackgroundColor(Color.parseColor(bg))
        v.setOnClickListener { onClick(o) }
    }

    private fun statusColors(s: String) = when(s) {
        "Chờ xác nhận" -> "#F59E0B" to "#FFF8E1"
        "Đã xác nhận"  -> "#1565C0" to "#E3F2FD"
        "Đang giao"    -> "#6A1B9A" to "#F3E5F5"
        "Đã nhận hàng","Hoàn thành" -> "#2E7D32" to "#E8F5E9"
        "Đã hủy"       -> "#E53935" to "#FFEBEE"
        else           -> "#9E9E9E" to "#F5F5F5"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(a: Order, b: Order) = a.id == b.id
            override fun areContentsTheSame(a: Order, b: Order) = a == b
        }
    }
}

// ── Inventory ─────────────────────────────────────────────────────────────────
class AdminInventoryFragment : Fragment() {

    private lateinit var adapter: AdminInventoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_admin_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_admin_title).text = "Quản lý Tồn kho"
        view.findViewById<View>(R.id.btn_admin_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.fab_add).visibility = View.GONE

        adapter = AdminInventoryAdapter { product -> showStockDialog(product) }
        view.findViewById<RecyclerView>(R.id.rv_admin).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminInventoryFragment.adapter
        }
        (activity as? AdminActivity)?.adminVm?.products?.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun showStockDialog(p: Product) {
        val et = EditText(requireContext()).apply {
            setText(p.stock.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(48, 24, 48, 8)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Cập nhật tồn kho: ${p.name}")
            .setView(et)
            .setPositiveButton("Lưu") { _, _ ->
                val newStock = et.text.toString().toIntOrNull() ?: return@setPositiveButton
                (activity as? AdminActivity)?.adminVm?.updateStock(p.firestoreId, newStock)
            }
            .setNegativeButton("Hủy", null).show()
    }
}

class AdminInventoryAdapter(
    private val onEdit: (Product) -> Unit
) : ListAdapter<Product, AdminInventoryAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_inventory, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        val v = holder.itemView
        v.findViewById<TextView>(R.id.tv_inv_name).text = p.name
        v.findViewById<TextView>(R.id.tv_inv_category).text = p.category
        val tvStock = v.findViewById<TextView>(R.id.tv_inv_stock)
        tvStock.text = "Còn: ${p.stock}"
        tvStock.setTextColor(when {
            p.stock == 0  -> Color.parseColor("#E53935")
            p.stock <= 5  -> Color.parseColor("#E65100")
            else          -> Color.parseColor("#2E7D32")
        })
        v.findViewById<View>(R.id.btn_edit_stock).setOnClickListener { onEdit(p) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}

// ── Users ─────────────────────────────────────────────────────────────────────
class AdminUsersFragment : Fragment() {

    private lateinit var adapter: AdminUserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_admin_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_admin_title).text = "Quản lý Người dùng"
        view.findViewById<View>(R.id.btn_admin_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.fab_add).visibility = View.GONE

        adapter = AdminUserAdapter { user -> showUserDetail(user) }
        view.findViewById<RecyclerView>(R.id.rv_admin).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminUsersFragment.adapter
        }
        (activity as? AdminActivity)?.adminVm?.users?.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.filter { it.role != "admin" })
        }
    }

    private fun showUserDetail(user: UserAccount) {
        AlertDialog.Builder(requireContext())
            .setTitle(user.username)
            .setMessage("Email: ${user.email}\nVai trò: ${user.role}")
            .setPositiveButton("Đóng", null)
            .setNegativeButton("Xóa tài khoản") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Xóa tài khoản \"${user.username}\"?")
                    .setPositiveButton("Xóa") { _, _ ->
                        (activity as? AdminActivity)?.adminVm?.deleteUser(user)
                    }
                    .setNegativeButton("Hủy", null).show()
            }.show()
    }
}

class AdminUserAdapter(
    private val onClick: (UserAccount) -> Unit
) : ListAdapter<UserAccount, AdminUserAdapter.VH>(DIFF) {

    class VH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = getItem(position)
        val v = holder.itemView
        v.findViewById<TextView>(R.id.tv_user_name).text = u.username
        v.findViewById<TextView>(R.id.tv_user_email).text = u.email
        v.setOnClickListener { onClick(u) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<UserAccount>() {
            override fun areItemsTheSame(a: UserAccount, b: UserAccount) = a.id == b.id
            override fun areContentsTheSame(a: UserAccount, b: UserAccount) = a == b
        }
    }
}
