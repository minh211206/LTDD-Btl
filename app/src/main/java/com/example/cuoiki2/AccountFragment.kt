package com.example.cuoiki2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.example.cuoiki2.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = (requireActivity().application as ShopApplication).authViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
        }

        // Setup profile menu items
        binding.menuInfo.setOnClickListener {
            val user = viewModel.currentUser.value ?: return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Thông tin tài khoản")
                .setMessage("Tên đăng nhập: ${user.username}\nEmail: ${user.email}")
                .setPositiveButton("OK", null)
                .show()
        }
        binding.menuOrders.setOnClickListener {
            val user = viewModel.currentUser.value ?: return@setOnClickListener
            startActivity(Intent(requireContext(), UserOrdersActivity::class.java).apply {
                putExtra("username", user.username)
            })
        }
        binding.menuPassword.setOnClickListener {
            val user = viewModel.currentUser.value ?: return@setOnClickListener
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java).apply {
                putExtra("username", user.username)
                putExtra("firestore_id", user.firestoreId)
            })
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.layoutNotLoggedIn.visibility = View.GONE
                binding.layoutLoggedIn.visibility = View.VISIBLE
                binding.tvUsername.text = user.username
                binding.tvEmail.text = user.email
                if (user.avatarUrl.isNotBlank()) {
                    binding.ivAvatar.load(user.avatarUrl) {
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.ic_person)
                    }
                }
            } else {
                binding.layoutNotLoggedIn.visibility = View.VISIBLE
                binding.layoutLoggedIn.visibility = View.GONE
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ -> viewModel.logout() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
