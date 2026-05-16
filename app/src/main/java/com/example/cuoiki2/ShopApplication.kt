package com.example.cuoiki2

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.cuoiki2.repository.*
import com.example.cuoiki2.viewmodel.*

class ShopApplication : Application(), ViewModelStoreOwner {

    // ── Repositories (singleton) ──────────────────────────────────────────────
    val productRepo  by lazy { ProductRepository() }
    val userRepo     by lazy { UserRepository() }
    val reviewRepo   by lazy { ReviewRepository() }
    val orderRepo    by lazy { OrderRepository(productRepo) }

    // ── App-level ViewModels (shared across Activities) ───────────────────────
    private val appViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = appViewModelStore

    val authViewModel: AuthViewModel by lazy {
        ViewModelProvider(this, AuthViewModelFactory(userRepo))[AuthViewModel::class.java]
    }

    val cartViewModel: CartViewModel by lazy {
        ViewModelProvider(this, CartViewModelFactory(orderRepo))[CartViewModel::class.java]
    }

    val favoriteViewModel: FavoriteViewModel by lazy {
        ViewModelProvider(this)[FavoriteViewModel::class.java]
    }
}
