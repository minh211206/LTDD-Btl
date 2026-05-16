package com.example.cuoiki2
import com.example.cuoiki2.model.*

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.cuoiki2.databinding.ActivityMainBinding
import com.example.cuoiki2.firebase.FirebaseManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try { FirebaseManager.init() }
        catch (e: Exception) { android.util.Log.e("MainActivity", "Firebase init: ${e.message}") }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home     -> HomeFragment()
                R.id.nav_favorite -> FavoriteFragment()
                R.id.nav_explore  -> ExploreFragment()
                R.id.nav_account  -> AccountFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun openProductDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("product_id", product.id)
            putExtra("product_name", product.name)
            putExtra("product_price", product.price)
            putExtra("product_price_l", product.priceL)
            putExtra("product_category", product.category)
            putExtra("product_color_hex", product.colorHex)
            putExtra("product_firestore_id", product.firestoreId)
            putExtra("product_image_url", product.imageUrl)
            putExtra("product_stock", product.stock)
        }
        startActivity(intent)
    }
}
