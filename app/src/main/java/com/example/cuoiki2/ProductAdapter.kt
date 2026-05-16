package com.example.cuoiki2
import com.example.cuoiki2.model.*

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cuoiki2.databinding.ItemProductBinding

class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit,
    private val onQuickAdd: ((Product) -> Unit)? = null,
    private var favorites: Set<Int> = emptySet()
) : ListAdapter<Product, ProductAdapter.VH>(DIFF) {

    fun updateFavorites(newFavorites: Set<Int>) {
        favorites = newFavorites
        notifyItemRangeChanged(0, itemCount)
    }

    class VH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val product = getItem(position)
        with(holder.binding) {
            tvName.text = product.name
            tvPrice.text = product.price
            tvCategory.text = product.category

            // Image
            if (product.imageUrl.isNotBlank()) {
                ivProduct.load(product.imageUrl) { crossfade(true) }
            } else {
                try { ivProduct.setBackgroundColor(Color.parseColor(product.colorHex)) }
                catch (e: Exception) { ivProduct.setBackgroundColor(Color.parseColor("#FFF0E8DC")) }
                ivProduct.setImageDrawable(null)
            }

            // Out of stock overlay
            tvOutOfStock.visibility = if (product.stock == 0) android.view.View.VISIBLE
                                      else android.view.View.GONE

            // Favorite
            val isFav = product.id in favorites
            btnFavorite.setImageResource(
                if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            btnFavorite.setColorFilter(
                if (isFav) Color.parseColor("#E53935") else Color.parseColor("#9E9E9E")
            )

            // Quick add button
            btnAddQuick.setColorFilter(Color.WHITE)
            btnAddQuick.alpha = if (product.stock > 0) 1f else 0.4f

            root.setOnClickListener { onProductClick(product) }
            btnFavorite.setOnClickListener { onFavoriteClick(product) }
            btnAddQuick.setOnClickListener { onQuickAdd?.invoke(product) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}
