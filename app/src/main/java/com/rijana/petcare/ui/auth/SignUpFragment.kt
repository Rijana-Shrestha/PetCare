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
import com.rijana.petcare.databinding.FragmentSignUpBinding
import com.rijana.petcare.viewmodel.AuthUiState
import com.rijana.petcare.viewmodel.AuthViewModel
import com.rijana.petcare.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val userRepository = UserRepository(AuthManager(), app.database.userDao())
        AuthViewModelFactory(userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigation listener to go back to SignInFragment
        binding.tvAlreadyHaveAccount.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_signIn)
        }

        binding.btnSignUp.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.signUp(name, email, password)
        }

        observeUiState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            binding.btnSignUp.isEnabled = false
                        }
                        is AuthUiState.Success -> {
                            binding.btnSignUp.isEnabled = true
                            findNavController().navigate(R.id.action_signUp_to_home)
                        }
                        is AuthUiState.Error -> {
                            binding.btnSignUp.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is AuthUiState.PasswordResetSent -> Unit // not used on this screen
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