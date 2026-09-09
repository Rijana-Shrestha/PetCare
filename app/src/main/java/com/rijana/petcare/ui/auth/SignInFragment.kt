package com.rijana.petcare.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentSignInBinding
import com.rijana.petcare.viewmodel.AuthUiState
import com.rijana.petcare.viewmodel.AuthViewModel
import com.rijana.petcare.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val userRepository = UserRepository(AuthManager(), app.database.userDao())
        AuthViewModelFactory(userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.signIn(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.sendPasswordResetEmail(email)
        }

        observeUiState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            binding.btnLogin.isEnabled = false
                        }
                        is AuthUiState.Success -> {
                            binding.btnLogin.isEnabled = true
                            findNavController().navigate(R.id.action_signIn_to_home)
                        }
                        is AuthUiState.Error -> {
                            binding.btnLogin.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is AuthUiState.PasswordResetSent -> {
                            binding.btnLogin.isEnabled = true
                            Toast.makeText(requireContext(), "Reset email sent — check your inbox", Toast.LENGTH_LONG).show()
                        }
                        is AuthUiState.Idle -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}