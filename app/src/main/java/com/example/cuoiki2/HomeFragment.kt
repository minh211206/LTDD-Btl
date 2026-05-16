package com.example.cuoiki2
import com.example.cuoiki2.model.*
import com.example.cuoiki2.repository.AppRepository

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.cuoiki2.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity().application as ShopApplication).authViewModel
    private val cartVm    get() = (requireActivity().application as ShopApplication).cartViewModel
    private val favVm     get() = (requireActivity().application as ShopApplication).favoriteViewModel
    private lateinit var adapter: ProductAdapter
    private var selectedCategory = "Tất cả"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductAdapter(
            onProductClick = { product -> (activity as? MainActivity)?.openProductDetail(product) },
            onFavoriteClick = { product -> favVm.toggle(product.id) },
            onQuickAdd = { product ->
                if (product.stock > 0) {
                    cartVm.addToCart(product, "M")
                    Toast.makeText(requireContext(), "${product.name} đã thêm vào giỏ!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = adapter
        binding.rvProducts.isNestedScrollingEnabled = false

        setupCategoryTabs()

        // Search click
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        // Cart click
        binding.btnCart.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }

        // See all
        binding.tvSeeAll.setOnClickListener {
            selectedCategory = "Tất cả"
            setupCategoryTabs()
            filterAndSubmit(AppRepository.products.value ?: emptyList())
        }

        // Observe products
        AppRepository.products.observe(viewLifecycleOwner) { filterAndSubmit(it) }

        // Observe favorites
        favVm.favoriteIds.observe(viewLifecycleOwner) { favs ->
            adapter.updateFavorites(favs)
        }

        // Observe cart badge
        cartVm.cartItems.observe(viewLifecycleOwner) { items ->
            val count = items.sumOf { it.quantity }
            if (count > 0) {
                binding.tvCartBadge.visibility = View.VISIBLE
                binding.tvCartBadge.text = if (count > 9) "9+" else count.toString()
            } else {
                binding.tvCartBadge.visibility = View.GONE
            }
        }

        // Observe user for avatar + name
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvHomeUsername.text = user.username
                if (user.avatarUrl.isNotBlank()) {
                    binding.ivAvatarHome.load(user.avatarUrl) {
                        transformations(CircleCropTransformation())
                    }
                }
            } else {
                binding.tvHomeUsername.text = "Khách hàng"
            }
        }
    }

    private fun setupCategoryTabs() {
        binding.categoryTabs.removeAllViews()
        categories.forEach { cat ->
            val tv = TextView(requireContext()).apply {
                text = cat
                textSize = 13f
                setTextColor(if (cat == selectedCategory) Color.WHITE else Color.parseColor("#99FFFFFF"))
                val hPad = dpToPx(16); val vPad = dpToPx(8)
                setPadding(hPad, vPad, hPad, vPad)
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = dpToPx(8)
                layoutParams = lp
                setOnClickListener { selectCategory(cat) }
            }
            binding.categoryTabs.addView(tv)
            if (cat == selectedCategory) styleTabSelected(tv) else styleTabUnselected(tv)
        }
    }

    private fun selectCategory(cat: String) {
        selectedCategory = cat
        for (i in 0 until binding.categoryTabs.childCount) {
            val tv = binding.categoryTabs.getChildAt(i) as TextView
            if (tv.text == cat) styleTabSelected(tv) else styleTabUnselected(tv)
        }
        filterAndSubmit(AppRepository.products.value ?: emptyList())
    }

    private fun filterAndSubmit(list: List<Product>) {
        val filtered = if (selectedCategory == "Tất cả") list
        else list.filter { it.category.trim().equals(selectedCategory.trim(), ignoreCase = true) }
        adapter.submitList(filtered)
    }

    private fun styleTabSelected(tv: TextView) {
        tv.setBackgroundResource(R.drawable.bg_tab_selected)
        tv.setTextColor(Color.WHITE)
        tv.setTypeface(null, Typeface.BOLD)
    }

    private fun styleTabUnselected(tv: TextView) {
        tv.setBackgroundResource(R.drawable.bg_tab_unselected)
        tv.setTextColor(Color.parseColor("#99FFFFFF"))
        tv.setTypeface(null, Typeface.NORMAL)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
