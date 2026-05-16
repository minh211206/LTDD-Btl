package com.example.cuoiki2
import com.example.cuoiki2.model.*
import com.example.cuoiki2.repository.AppRepository

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cuoiki2.databinding.FragmentFavoriteBinding

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity().application as ShopApplication).authViewModel
    private val favVm     get() = (requireActivity().application as ShopApplication).favoriteViewModel
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductAdapter(
            onProductClick = { product -> (activity as? MainActivity)?.openProductDetail(product) },
            onFavoriteClick = { product -> favVm.toggle(product.id) }
        )
        binding.rvFavorites.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvFavorites.adapter = adapter

        fun refresh() {
            val favIds = favVm.favoriteIds.value ?: emptySet()
            val favProducts = AppRepository.getProducts().filter { it.id in favIds }
            adapter.updateFavorites(favIds)
            adapter.submitList(favProducts)
            if (favProducts.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvFavorites.visibility = View.GONE
                binding.tvFavCount.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvFavorites.visibility = View.VISIBLE
                binding.tvFavCount.visibility = View.VISIBLE
                binding.tvFavCount.text = favProducts.size.toString()
            }
        }

        favVm.favoriteIds.observe(viewLifecycleOwner) { refresh() }
        AppRepository.products.observe(viewLifecycleOwner) { refresh() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
