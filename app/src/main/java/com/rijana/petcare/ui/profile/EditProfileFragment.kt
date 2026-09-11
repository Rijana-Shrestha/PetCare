package com.rijana.petcare.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentEditProfileBinding
import com.rijana.petcare.viewmodel.ProfileViewModel
import com.rijana.petcare.viewmodel.ProfileViewModelFactory
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedPhotoUri: String? = null

    private val profileViewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        ProfileViewModelFactory(UserRepository(AuthManager(), app.database.userDao()))
    }

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri.toString()
                Glide.with(this).load(uri).centerCrop().into(binding.imgAvatar)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        binding.photoFrame.setOnClickListener {
            pickPhotoLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        binding.btnSaveChanges.setOnClickListener { saveChanges() }

        prefillForm()
    }

    private fun prefillForm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.currentUser.collect { user ->
                    if (user == null) return@collect
                    binding.etFullName.setText(user.name)
                    binding.etEmail.setText(user.email)
                    binding.etPhone.setText(user.phone)
                    binding.etAddress.setText(user.address)
                    if (selectedPhotoUri == null && user.profileImageUri != null) {
                        selectedPhotoUri = user.profileImageUri
                        Glide.with(this@EditProfileFragment)
                            .load(user.profileImageUri)
                            .centerCrop()
                            .into(binding.imgAvatar)
                    }
                }
            }
        }
    }

    private fun saveChanges() {
        val name = binding.etFullName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        profileViewModel.updateProfile(
            name = name,
            phone = binding.etPhone.text.toString().trim().ifEmpty { null },
            address = binding.etAddress.text.toString().trim().ifEmpty { null },
            profileImageUri = selectedPhotoUri
        )
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}