package com.rijana.petcare.ui.pets

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.rijana.petcare.data.local.entity.Gender
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentPetDetailBinding
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class PetDetailFragment : Fragment() {

    private var _binding: FragmentPetDetailBinding? = null
    private val binding get() = _binding!!

    private var currentPet: Pet? = null

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val petRepository = PetRepository(app.database.petDao())
        val userRepository = UserRepository(AuthManager(), app.database.userDao())
        PetViewModelFactory(petRepository, userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val petId = arguments?.getLong("petId") ?: run {
            findNavController().popBackStack()
            return
        }

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.ivDelete.setOnClickListener { confirmDelete() }
        binding.ivEdit.setOnClickListener {
            val pet = currentPet ?: return@setOnClickListener
            val bundle = Bundle().apply { putLong("petId", pet.id) }
            findNavController().navigate(R.id.action_petDetail_to_addPet, bundle)
        }

        observePet(petId)
    }

    private fun observePet(petId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.getPetById(petId).collect { pet ->
                    if (pet != null) {
                        currentPet = pet
                        bindPet(pet)
                    }
                }
            }
        }
    }

    private fun bindPet(pet: Pet) {
        binding.tvPetName.text = pet.name
        binding.tvPetBreedAge.text = "${pet.breed} · ${calculateAge(pet.dateOfBirth)}"
        binding.tvWeightChip.text = "Wt: ${pet.weightKg} kg"
        binding.tvGenderChip.text = if (pet.gender == Gender.MALE) "Male" else "Female"

        binding.tvBirthdayValue.text = formatDate(pet.dateOfBirth)
        binding.tvDietValue.text = pet.dietaryPreference ?: "Not specified"
        binding.tvAllergiesValue.text = pet.allergies ?: getString(R.string.no_known_allergies)
        binding.tvFavToyValue.text = pet.favoriteToy ?: "Not specified"

        Glide.with(this)
            .load(pet.photoUri)
            .placeholder(R.drawable.circle_avatar_placeholder)
            .error(R.drawable.circle_avatar_placeholder)
            .centerCrop()
            .into(binding.ivPetPhoto)
    }

    private fun calculateAge(dateOfBirthMillis: Long): String {
        val ageMillis = System.currentTimeMillis() - dateOfBirthMillis
        val years = TimeUnit.MILLISECONDS.toDays(ageMillis) / 365
        return if (years < 1) "<1 yr" else "$years yrs"
    }

    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(millis)
    }

    private fun confirmDelete() {
        val pet = currentPet ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_pet_title)
            .setMessage(getString(R.string.delete_pet_message, pet.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                petViewModel.deletePet(pet)
                findNavController().popBackStack()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}