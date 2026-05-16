package com.example.cuoiki2
import com.example.cuoiki2.model.*
import com.example.cuoiki2.repository.AppRepository

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cuoiki2.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: ProductAdapter
    private var selectedCategory = "Tất cả"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("product_name", product.name)
                    putExtra("product_price", product.price)
                    putExtra("product_category", product.category)
                    putExtra("product_color_hex", product.colorHex)
                    putExtra("product_firestore_id", product.firestoreId)
                    putExtra("product_image_url", product.imageUrl)
                    putExtra("product_stock", product.stock)
                }
                startActivity(intent)
            },
            onFavoriteClick = {}
        )

        binding.rvResults.layoutManager = GridLayoutManager(this, 2)
        binding.rvResults.adapter = adapter

        setupCategoryTabs()
        doSearch("")

        binding.etSearch.addTextChangedListener {
            binding.btnClear.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        binding.etSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                doSearch(binding.etSearch.text?.toString() ?: "")
                true
            } else false
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.setText("")
            doSearch("")
        }

        binding.btnSearchSubmit.setOnClickListener {
            doSearch(binding.etSearch.text?.toString() ?: "")
        }
    }

    private fun setupCategoryTabs() {
        binding.categoryTabs.removeAllViews()
        categories.forEach { cat ->
            val tv = TextView(this).apply {
                text = cat
                textSize = 14f
                setPadding(dpToPx(0), dpToPx(12), dpToPx(24), dpToPx(12))
                setOnClickListener { selectCategory(cat) }
            }
            binding.categoryTabs.addView(tv)
            if (cat == selectedCategory) styleSelected(tv) else styleUnselected(tv)
        }
    }

    private fun selectCategory(cat: String) {
        selectedCategory = cat
        for (i in 0 until binding.categoryTabs.childCount) {
            val tv = binding.categoryTabs.getChildAt(i) as TextView
            if (tv.text == cat) styleSelected(tv) else styleUnselected(tv)
        }
        doSearch(binding.etSearch.text?.toString() ?: "")
    }

    private fun doSearch(query: String) {
        val results = AppRepository.getProducts().filter { product ->
            val matchQuery = query.isBlank() || product.name.contains(query.trim(), ignoreCase = true)
            val matchCat = selectedCategory == "Tất cả" || product.category == selectedCategory
            matchQuery && matchCat
        }
        adapter.submitList(results)
        if (results.isEmpty()) {
            binding.tvNoResult.visibility = View.VISIBLE
            binding.rvResults.visibility = View.GONE
        } else {
            binding.tvNoResult.visibility = View.GONE
            binding.rvResults.visibility = View.VISIBLE
        }
    }

    private fun styleSelected(tv: TextView) {
        tv.setTextColor(Color.BLACK)
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun styleUnselected(tv: TextView) {
        tv.setTextColor(Color.GRAY)
        tv.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
