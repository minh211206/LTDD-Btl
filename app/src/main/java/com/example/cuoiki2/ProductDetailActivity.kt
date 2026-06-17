package com.example.cuoiki2
import com.example.cuoiki2.model.*
import com.example.cuoiki2.repository.AppRepository

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cuoiki2.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val cartVm get() = (application as ShopApplication).cartViewModel
    private val favVm  get() = (application as ShopApplication).favoriteViewModel
    private val reviewRepo get() = (application as ShopApplication).reviewRepo
    private var selectedSize = "M"
    private val sizes = listOf("M", "L")
    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        product = Product(
            id          = intent.getIntExtra("product_id", 0),
            name        = intent.getStringExtra("product_name") ?: "",
            price       = intent.getStringExtra("product_price") ?: "",
            priceL      = intent.getStringExtra("product_price_l") ?: "",
            category    = intent.getStringExtra("product_category") ?: "",
            colorHex    = intent.getStringExtra("product_color_hex") ?: "#CCCCCC",
            firestoreId = intent.getStringExtra("product_firestore_id") ?: "",
            imageUrl    = intent.getStringExtra("product_image_url") ?: "",
            stock       = intent.getIntExtra("product_stock", 0)
        )

        binding.btnBack.setOnClickListener { finish() }

        // Image
        if (product.imageUrl.isNotBlank()) {
            binding.ivProduct.load(product.imageUrl) { crossfade(true) }
        } else {
            try { binding.ivProduct.setBackgroundColor(Color.parseColor(product.colorHex)) }
            catch (e: Exception) { binding.ivProduct.setBackgroundColor(Color.LTGRAY) }
        }

        binding.tvName.text = product.name

        // Stock warning
        when {
            product.stock == 0 -> {
                binding.layoutStockWarning.visibility = View.VISIBLE
                binding.tvStockWarning.text = "Sản phẩm đã hết hàng"
                binding.layoutStockWarning.setBackgroundResource(R.drawable.bg_error_box)
                binding.tvStockWarning.setTextColor(Color.parseColor("#E53935"))
            }
            product.stock in 1..5 -> {
                binding.layoutStockWarning.visibility = View.VISIBLE
                binding.tvStockWarning.text = "Chỉ còn ${product.stock} sản phẩm!"
            }
            else -> binding.layoutStockWarning.visibility = View.GONE
        }

        // Size selector
        setupSizes()

        // Favorite
        updateFavoriteIcon()
        binding.btnFavorite.setOnClickListener {
            favVm.toggle(product.id)
            updateFavoriteIcon()
        }

        // Add to cart
        if (product.stock == 0) {
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "Hết hàng"
        }
        binding.btnAddToCart.setOnClickListener {
            cartVm.addToCart(product, selectedSize)
            binding.btnAddToCart.text = "Đã thêm ✓"
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.postDelayed({
                if (!isFinishing) {
                    binding.btnAddToCart.text = "Thêm vào giỏ"
                    binding.btnAddToCart.isEnabled = true
                }
            }, 1500)
        }

        // Observe reviews realtime
        AppRepository.reviews.observe(this) { allReviews ->
            val reviews = allReviews.filter { it.productFirestoreId == product.firestoreId }
            // Cập nhật rating
            binding.tvRating.text = if (reviews.isEmpty()) "Chưa có đánh giá"
            else "★ ${"%.1f".format(reviews.sumOf { it.stars }.toFloat() / reviews.size)} (${reviews.size})"
            // Cập nhật danh sách review
            loadReviews(reviews)
        }

        // Nút viết đánh giá
        binding.btnWriteReview.setOnClickListener {
            val user = (application as ShopApplication).authViewModel.currentUser.value
            if (user == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showReviewDialog(user.username)
        }
    }

    private fun showReviewDialog(username: String) {
        val ctx = this
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        // Chọn số sao
        val tvStarLabel = TextView(ctx).apply {
            text = "Chọn số sao:"
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A1A"))
        }
        layout.addView(tvStarLabel)

        var selectedStars = 5
        val starRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }

        // Tạo list sao trước, gán listener sau
        val starButtons = mutableListOf<TextView>()
        for (star in 1..5) {
            val tv = TextView(ctx).apply {
                text = "★"
                textSize = 28f
                setTextColor(if (star <= selectedStars) Color.parseColor("#F59E0B") else Color.LTGRAY)
                setPadding(4, 0, 4, 0)
            }
            starButtons.add(tv)
            starRow.addView(tv)
        }
        // Gán listener sau khi đã có đủ list
        starButtons.forEachIndexed { index, tv ->
            tv.setOnClickListener {
                selectedStars = index + 1
                starButtons.forEachIndexed { i, btn ->
                    btn.setTextColor(
                        if (i < selectedStars) Color.parseColor("#F59E0B") else Color.LTGRAY
                    )
                }
            }
        }
        layout.addView(starRow)

        // Nhập bình luận
        val etComment = EditText(ctx).apply {
            hint = "Nhập nhận xét của bạn..."
            textSize = 14f
            minLines = 3
            maxLines = 5
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(etComment)

        AlertDialog.Builder(ctx)
            .setTitle("Đánh giá sản phẩm")
            .setView(layout)
            .setPositiveButton("Gửi") { _, _ ->
                val comment = etComment.text.toString().trim()
                if (comment.isEmpty()) {
                    Toast.makeText(ctx, "Vui lòng nhập nhận xét", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val review = Review(
                    productFirestoreId = product.firestoreId,
                    username           = username,
                    stars              = selectedStars,
                    comment            = comment,
                    createdAt          = System.currentTimeMillis()
                )
                lifecycleScope.launch {
                    reviewRepo.saveReview(review)
                    Toast.makeText(ctx, "Đã gửi đánh giá!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupSizes() {
        binding.sizeContainer.removeAllViews()
        sizes.forEach { size ->
            val tv = TextView(this).apply {
                text = size
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                val pad = dpToPx(12)
                setPadding(pad, pad, pad, pad)
                val lp = android.widget.LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
                lp.marginEnd = dpToPx(10)
                layoutParams = lp
                gravity = android.view.Gravity.CENTER
                setOnClickListener { selectSize(size) }
            }
            binding.sizeContainer.addView(tv)
            if (size == selectedSize) styleSizeSelected(tv) else styleSizeUnselected(tv)
        }
        updatePrice()
    }

    private fun selectSize(size: String) {
        selectedSize = size
        for (i in 0 until binding.sizeContainer.childCount) {
            val tv = binding.sizeContainer.getChildAt(i) as TextView
            if (tv.text == size) styleSizeSelected(tv) else styleSizeUnselected(tv)
        }
        updatePrice()
    }

    private fun updatePrice() {
        binding.tvPrice.text = product.priceForSize(selectedSize)
    }

    private fun styleSizeSelected(tv: TextView) {
        tv.setBackgroundResource(R.drawable.bg_size_selected)
        tv.setTextColor(Color.WHITE)
    }

    private fun styleSizeUnselected(tv: TextView) {
        tv.setBackgroundResource(R.drawable.bg_size_unselected)
        tv.setTextColor(Color.parseColor("#1A1A1A"))
    }

    private fun updateFavoriteIcon() {
        val isFav = favVm.isFavorite(product.id)
        binding.btnFavorite.setImageResource(
            if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        binding.btnFavorite.setColorFilter(
            if (isFav) Color.parseColor("#E53935") else Color.GRAY
        )
    }

    private fun loadReviews(reviews: List<Review>) {
        binding.reviewsContainer.removeAllViews()
        if (reviews.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Chưa có đánh giá nào"
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(0, dpToPx(8), 0, dpToPx(8))
            }
            binding.reviewsContainer.addView(tv)
            return
        }
        reviews.sortedByDescending { it.createdAt }.take(10).forEach { review ->
            val tv = TextView(this).apply {
                val stars = "★".repeat(review.stars) + "☆".repeat(5 - review.stars)
                text = "${review.username}  $stars\n${review.comment}"
                textSize = 13f
                setTextColor(Color.parseColor("#444444"))
                setPadding(0, dpToPx(8), 0, dpToPx(8))
                setLineSpacing(dpToPx(4).toFloat(), 1f)
            }
            binding.reviewsContainer.addView(tv)
            val divider = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(Color.parseColor("#F0F0F0"))
            }
            binding.reviewsContainer.addView(divider)
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
