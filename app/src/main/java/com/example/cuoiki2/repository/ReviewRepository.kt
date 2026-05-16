package com.example.cuoiki2.repository

import androidx.lifecycle.MutableLiveData
import com.example.cuoiki2.model.Review
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class ReviewRepository {

    private val db = Firebase.firestore
    val reviews = MutableLiveData<List<Review>>(emptyList())

    init { listenReviews() }

    private fun listenReviews() {
        db.collection("reviews").addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    Review(
                        id                 = doc.id.hashCode(),
                        productFirestoreId = doc.getString("productFirestoreId") ?: "",
                        username           = doc.getString("username") ?: "",
                        stars              = (doc.getLong("stars") ?: 5L).toInt(),
                        comment            = doc.getString("comment") ?: "",
                        createdAt          = (doc.getDouble("createdAt") ?: 0.0).toLong()
                    )
                } catch (e: Exception) { null }
            }
            reviews.postValue(list)
        }
    }

    suspend fun saveReview(review: Review) {
        val model = mapOf(
            "productFirestoreId" to review.productFirestoreId,
            "username"  to review.username,
            "stars"     to review.stars,
            "comment"   to review.comment,
            "createdAt" to review.createdAt.toDouble()
        )
        val ref = db.collection("reviews").add(model).await()
        db.collection("reviews").document(ref.id).update("id", ref.id).await()
    }
}
