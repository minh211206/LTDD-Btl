package com.example.cuoiki2.repository

import androidx.lifecycle.MutableLiveData
import com.example.cuoiki2.model.UserAccount
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    val users = MutableLiveData<List<UserAccount>>(emptyList())

    init { listenUsers() }

    private fun listenUsers() {
        db.collection("users").addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            snap ?: return@addSnapshotListener
            val list = snap.documents.mapNotNull { doc ->
                try {
                    UserAccount(
                        id          = doc.id.hashCode(),
                        username    = doc.getString("username") ?: "",
                        email       = doc.getString("email") ?: "",
                        password    = "",
                        role        = doc.getString("role") ?: "user",
                        firestoreId = doc.id,
                        avatarUrl   = doc.getString("avatarUrl") ?: ""
                    )
                } catch (e: Exception) { null }
            }
            users.postValue(list)
        }
    }

    suspend fun login(username: String, password: String): UserAccount? {
        return try {
            withTimeout(30_000) {
                val snap = db.collection("users")
                    .whereEqualTo("username", username).limit(1).get().await()
                val doc = snap.documents.firstOrNull() ?: return@withTimeout null
                if (doc.getString("password") != password) return@withTimeout null
                UserAccount(
                    id          = doc.id.hashCode(),
                    username    = doc.getString("username") ?: "",
                    email       = doc.getString("email") ?: "",
                    password    = password,
                    role        = doc.getString("role") ?: "user",
                    firestoreId = doc.id,
                    avatarUrl   = doc.getString("avatarUrl") ?: ""
                )
            }
        } catch (e: Exception) { null }
    }

    suspend fun register(username: String, email: String, phone: String, password: String): String? {
        return try {
            withTimeout(30_000) {
                if (!db.collection("users").whereEqualTo("username", username).limit(1).get().await().isEmpty)
                    return@withTimeout "Tên đăng nhập đã được sử dụng"
                if (!db.collection("users").whereEqualTo("email", email).limit(1).get().await().isEmpty)
                    return@withTimeout "Email này đã được đăng ký"
                val model = mapOf("username" to username, "email" to email, "phone" to phone,
                    "password" to password, "role" to "user", "avatarUrl" to "",
                    "createdAt" to System.currentTimeMillis().toDouble(), "id" to "")
                val ref = db.collection("users").add(model).await()
                db.collection("users").document(ref.id).update("id", ref.id).await()
                null
            }
        } catch (e: Exception) { "Lỗi kết nối: ${e.message}" }
    }

    suspend fun changePassword(firestoreId: String, oldPassword: String, newPassword: String): Boolean {
        return try {
            val doc = db.collection("users").document(firestoreId).get().await()
            if (doc.getString("password") != oldPassword) return false
            db.collection("users").document(firestoreId).update("password", newPassword).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateAvatar(firestoreId: String, avatarUrl: String) {
        if (firestoreId.isEmpty()) return
        db.collection("users").document(firestoreId).update("avatarUrl", avatarUrl).await()
    }

    suspend fun deleteUser(user: UserAccount) {
        if (user.firestoreId.isEmpty()) return
        db.collection("users").document(user.firestoreId).delete().await()
    }
}
