package com.example.cuoiki2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.cuoiki2.databinding.ActivityLoginBinding
import com.example.cuoiki2.viewmodel.AuthViewModel
import com.example.cuoiki2.viewmodel.AuthViewModelFactory
import com.example.cuoiki2.viewmodel.LoginState

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as ShopApplication
        viewModel = ViewModelProvider(this, AuthViewModelFactory(app.userRepo))[AuthViewModel::class.java]

        binding.btnBack.setOnClickListener { finish() }
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            viewModel.login(username, password)
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.tvError.visibility = View.GONE
                }
                is LoginState.Success -> {
                    // Sync vào app-level authViewModel
                    app.authViewModel.login(state.user)
                    if (state.user.role == "admin") {
                        startActivity(Intent(this, AdminActivity::class.java))
                    }
                    finish()
                }
                is LoginState.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
                else -> binding.btnLogin.isEnabled = true
            }
        }
    }
}
