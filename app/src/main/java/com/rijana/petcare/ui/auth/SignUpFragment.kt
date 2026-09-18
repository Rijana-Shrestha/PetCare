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
import com.rijana.petcare.databinding.FragmentSignUpBinding
import com.rijana.petcare.viewmodel.AuthErrorField
import com.rijana.petcare.viewmodel.AuthUiState
import com.rijana.petcare.viewmodel.AuthViewModel
import com.rijana.petcare.viewmodel.AuthViewModelFactory
import com.rijana.petcare.util.setupPasswordToggle
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

        binding.etPassword.setupPasswordToggle()
        binding.etConfirmPassword.setupPasswordToggle()

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, imeInsets.bottom)
            insets
        }

        // Clear a field's error as soon as the user starts fixing it
        binding.etEmail.addTextChangedListener(clearErrorOnEdit(binding.etEmail, binding.tvEmailError))
        binding.etPassword.addTextChangedListener(clearErrorOnEdit(binding.etPassword, binding.tvPasswordError))
        binding.etConfirmPassword.addTextChangedListener(clearErrorOnEdit(binding.etConfirmPassword, binding.tvConfirmPasswordError))

        binding.tvAlreadyHaveAccount.setOnClickListener {
            findNavController().navigate(R.id.action_signUp_to_signIn)
        }

        binding.btnSignUp.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            clearFieldError(binding.etEmail, binding.tvEmailError)
            clearFieldError(binding.etPassword, binding.tvPasswordError)
            clearFieldError(binding.etConfirmPassword, binding.tvConfirmPasswordError)

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                showFieldError(binding.etPassword, binding.tvPasswordError, "Password must be at least 6 characters")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showFieldError(binding.etConfirmPassword, binding.tvConfirmPasswordError, "Passwords do not match")
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
                            when (state.field) {
                                AuthErrorField.EMAIL -> showFieldError(binding.etEmail, binding.tvEmailError, state.message)
                                AuthErrorField.PASSWORD -> showFieldError(binding.etPassword, binding.tvPasswordError, state.message)
                                AuthErrorField.CONFIRM_PASSWORD -> showFieldError(binding.etConfirmPassword, binding.tvConfirmPasswordError, state.message)
                                AuthErrorField.GENERAL -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        is AuthUiState.PasswordResetSent -> Unit // not used on this screen
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