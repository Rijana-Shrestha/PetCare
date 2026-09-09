package com.rijana.petcare.ui.pets

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.entity.Gender
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.local.entity.PetType
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddPetBinding
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import java.util.Calendar

class AddPetFragment : Fragment() {

    private var _binding: FragmentAddPetBinding? = null
    private val binding get() = _binding!!

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val petRepository = PetRepository(app.database.petDao())
        val userRepository = UserRepository(AuthManager(), app.database.userDao())
        PetViewModelFactory(petRepository, userRepository)
    }

    private var selectedPhotoUri: String? = null
    private var selectedDateOfBirth: Long? = null

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri.toString()
                Glide.with(this).load(uri).centerCrop().into(binding.imgPetPhoto)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPetTypeDropdown()

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        binding.photoFrame.setOnClickListener {
            pickPhotoLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        binding.etDateOfBirth.setOnClickListener { showDatePicker() }

        binding.btnSaveChanges.setOnClickListener { savePet() }
    }

    private fun setupPetTypeDropdown() {
        val options = PetType.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvPetType.setAdapter(adapter)
        binding.actvPetType.setOnClickListener { binding.actvPetType.showDropDown() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                selectedDateOfBirth = calendar.timeInMillis
                binding.etDateOfBirth.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun savePet() {
        val name = binding.etPetName.text.toString().trim()
        val typeText = binding.actvPetType.text.toString().trim()
        val breed = binding.etBreed.text.toString().trim()
        val weightText = binding.etWeight.text.toString().trim()
        val ownerId = petViewModel.ownerId.value

        if (name.isEmpty() || typeText.isEmpty() || breed.isEmpty() ||
            selectedDateOfBirth == null || weightText.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = when (binding.toggleGender.checkedButtonId) {
            binding.btnMale.id -> Gender.MALE
            binding.btnFemale.id -> Gender.FEMALE
            else -> {
                Toast.makeText(requireContext(), "Please select a gender", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (ownerId == null) {
            Toast.makeText(requireContext(), "Couldn't identify your account, please try again", Toast.LENGTH_SHORT).show()
            return
        }

        val pet = Pet(
            ownerId = ownerId,
            name = name,
            type = PetType.valueOf(typeText.uppercase()),
            breed = breed,
            dateOfBirth = selectedDateOfBirth!!,
            gender = gender,
            weightKg = weightText.toFloat(),
            dietaryPreference = binding.etDietaryPreference.text.toString().trim().ifEmpty { null },
            allergies = binding.etAllergies.text.toString().trim().ifEmpty { null },
            favoriteToy = binding.etFavoriteToy.text.toString().trim().ifEmpty { null },
            note = binding.etNote.text.toString().trim().ifEmpty { null },
            photoUri = selectedPhotoUri
        )

        petViewModel.addPet(pet)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}