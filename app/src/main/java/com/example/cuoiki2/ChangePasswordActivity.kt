package com.example.cuoiki2

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cuoiki2.databinding.ActivityChangePasswordBinding
import com.example.cuoiki2.firebase.FirebaseManager
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firestoreId = intent.getStringExtra("firestore_id") ?: ""
        binding.btnBack.setOnClickListener { finish() }

        binding.btnConfirm.setOnClickListener {
            val oldPw    = binding.etOldPassword.text?.toString() ?: ""
            val newPw    = binding.etNewPassword.text?.toString() ?: ""
            val confirmPw = binding.etConfirmPassword.text?.toString() ?: ""

            val error = when {
                oldPw.isBlank() || newPw.isBlank() || confirmPw.isBlank() -> "Vui lòng điền đầy đủ"
                newPw.length < 6 -> "Mật khẩu mới phải ít nhất 6 ký tự"
                newPw != confirmPw -> "Mật khẩu nhập lại không khớp"
                else -> null
            }

            if (error != null) {
                showError(error); return@setOnClickListener
            }

            binding.btnConfirm.isEnabled = false
            hideMessages()

            lifecycleScope.launch {
                val ok = FirebaseManager.changePassword(firestoreId, oldPw, newPw)
                binding.btnConfirm.isEnabled = true
                if (ok) {
                    showSuccess("Đổi mật khẩu thành công!")
                    binding.etOldPassword.setText("")
                    binding.etNewPassword.setText("")
                    binding.etConfirmPassword.setText("")
                } else {
                    showError("Mật khẩu cũ không đúng")
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.layoutSuccess.visibility = View.GONE
        binding.tvError.text = msg
    }

    private fun showSuccess(msg: String) {
        binding.layoutSuccess.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        binding.tvSuccess.text = msg
    }

    private fun hideMessages() {
        binding.layoutError.visibility = View.GONE
        binding.layoutSuccess.visibility = View.GONE
    }
}
