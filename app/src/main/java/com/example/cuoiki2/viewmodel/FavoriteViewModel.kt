package com.example.cuoiki2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FavoriteViewModel : ViewModel() {

    val favoriteIds = MutableLiveData<MutableSet<Int>>(mutableSetOf())

    fun toggle(productId: Int) {
        val set = favoriteIds.value ?: mutableSetOf()
        if (productId in set) set.remove(productId) else set.add(productId)
        favoriteIds.value = set
    }

    fun isFavorite(productId: Int) = favoriteIds.value?.contains(productId) == true
}
