package com.rijana.petcare.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.rijana.petcare.viewmodel.AuthErrorField
import com.rijana.petcare.viewmodel.AuthUiState
import com.rijana.petcare.viewmodel.AuthViewModel
import com.rijana.petcare.viewmodel.AuthViewModelFactory
import com.rijana.petcare.util.setupPasswordToggle
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

        binding.etPassword.setupPasswordToggle()

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, imeInsets.bottom)
            insets
        }

        // Clear a field's error as soon as the user starts fixing it
        binding.etEmail.addTextChangedListener(clearErrorOnEdit(binding.etEmail, binding.tvEmailError))
        binding.etPassword.addTextChangedListener(clearErrorOnEdit(binding.etPassword, binding.tvPasswordError))

        binding.tvDontHaveAccount.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_signUp)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            clearFieldError(binding.etEmail, binding.tvEmailError)
            clearFieldError(binding.etPassword, binding.tvPasswordError)

            if (email.isEmpty()) {
                showFieldError(binding.etEmail, binding.tvEmailError, "Please enter your email")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showFieldError(binding.etPassword, binding.tvPasswordError, "Please enter your password")
                return@setOnClickListener
            }

            authViewModel.signIn(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showFieldError(binding.etEmail, binding.tvEmailError, "Enter your email first")
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
                            when (state.field) {
                                AuthErrorField.EMAIL -> showFieldError(binding.etEmail, binding.tvEmailError, state.message)
                                AuthErrorField.PASSWORD -> showFieldError(binding.etPassword, binding.tvPasswordError, state.message)
                                AuthErrorField.CONFIRM_PASSWORD, AuthErrorField.GENERAL ->
                                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
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

    private fun showFieldError(field: EditText, errorView: TextView, message: String) {
        field.setBackgroundResource(R.drawable.outlined_border_error)
        errorView.text = message
        errorView.visibility = View.VISIBLE
    }

    private fun clearFieldError(field: EditText, errorView: TextView) {
        field.setBackgroundResource(R.drawable.outlined_border)
        errorView.visibility = View.GONE
    }

    private fun clearErrorOnEdit(field: EditText, errorView: TextView) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (errorView.visibility == View.VISIBLE) clearFieldError(field, errorView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}