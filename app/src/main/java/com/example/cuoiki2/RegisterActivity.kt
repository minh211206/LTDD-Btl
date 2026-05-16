package com.example.cuoiki2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.cuoiki2.databinding.ActivityRegisterBinding
import com.example.cuoiki2.viewmodel.AuthViewModelFactory
import com.example.cuoiki2.viewmodel.RegisterState

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ShopApplication
        val viewModel = ViewModelProvider(this, AuthViewModelFactory(app.userRepo))[
            com.example.cuoiki2.viewmodel.AuthViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }
        binding.tvGoLogin.paintFlags = binding.tvGoLogin.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }

        binding.btnRegister.setOnClickListener {
            viewModel.register(
                username = binding.etUsername.text?.toString()?.trim() ?: "",
                email    = binding.etEmail.text?.toString()?.trim() ?: "",
                phone    = binding.etPhone.text?.toString()?.trim() ?: "",
                password = binding.etPassword.text?.toString() ?: "",
                confirm  = binding.etConfirmPassword.text?.toString() ?: ""
            )
        }

        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterState.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.tvError.visibility = View.GONE
                }
                is RegisterState.Success -> {
                    AlertDialog.Builder(this)
                        .setTitle("Đăng ký thành công!")
                        .setMessage("Tài khoản của bạn đã được tạo.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .show()
                }
                is RegisterState.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
                else -> binding.btnRegister.isEnabled = true
            }
        }
    }
}
