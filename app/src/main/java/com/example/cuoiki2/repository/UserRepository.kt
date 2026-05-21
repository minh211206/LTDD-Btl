package com.example.cuoiki2.repository

import androidx.lifecycle.MutableLiveData
import com.example.cuoiki2.model.UserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class UserRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
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

    /**
     * Đăng nhập bằng Firebase Auth (email + password thật).
     * Kiểm tra email đã xác nhận chưa trước khi cho vào app.
     */
    suspend fun login(username: String, password: String): UserAccount? {
        return try {
            withTimeout(30_000) {
                // Tìm email từ username trong Firestore
                val snap = db.collection("users")
                    .whereEqualTo("username", username).limit(1).get().await()
                val doc = snap.documents.firstOrNull() ?: return@withTimeout null
                val email = doc.getString("email") ?: return@withTimeout null

                // Đăng nhập qua Firebase Auth
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: return@withTimeout null

                // Kiểm tra email đã xác nhận chưa (trừ admin)
                val role = doc.getString("role") ?: "user"
                if (role != "admin") {
                    firebaseUser.reload().await()
                    if (!firebaseUser.isEmailVerified) {
                        auth.signOut()
                        return@withTimeout UserAccount(id = -1) // signal chưa verify
                    }
                }

                UserAccount(
                    id          = doc.id.hashCode(),
                    username    = doc.getString("username") ?: "",
                    email       = email,
                    password    = "",
                    role        = role,
                    firestoreId = doc.id,
                    avatarUrl   = doc.getString("avatarUrl") ?: ""
                )
            }
        } catch (e: Exception) { null }
    }

    /**
     * Đăng ký: tạo tài khoản Firebase Auth trước (xác minh email thật),
     * sau đó lưu thêm thông tin vào Firestore.
     */
    suspend fun register(username: String, email: String, phone: String, password: String): String? {
        return try {
            withTimeout(30_000) {
                // Kiểm tra username đã tồn tại chưa
                if (!db.collection("users").whereEqualTo("username", username)
                        .limit(1).get().await().isEmpty)
                    return@withTimeout "Tên đăng nhập đã được sử dụng"

                // Tạo tài khoản Firebase Auth — tự động kiểm tra email hợp lệ và chưa tồn tại
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@withTimeout "Đăng ký thất bại"

                // Gửi email xác nhận
                result.user?.sendEmailVerification()?.await()

                // Lưu thông tin bổ sung vào Firestore
                val model = mapOf(
                    "username"  to username,
                    "email"     to email,
                    "phone"     to phone,
                    "role"      to "user",
                    "avatarUrl" to "",
                    "createdAt" to System.currentTimeMillis().toDouble(),
                    "uid"       to uid
                )
                db.collection("users").add(model).await()
                null // null = thành công
            }
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            "Email này đã được đăng ký"
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            "Mật khẩu quá yếu, cần ít nhất 6 ký tự"
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            "Email không hợp lệ"
        } catch (e: Exception) {
            "Lỗi kết nối: ${e.message}"
        }
    }

    /**
     * Đổi mật khẩu qua Firebase Auth.
     */
    suspend fun changePassword(firestoreId: String, oldPassword: String, newPassword: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val email = user.email ?: return false
            // Re-authenticate trước khi đổi mật khẩu
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, oldPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
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

    fun logout() {
        auth.signOut()
    }
}
