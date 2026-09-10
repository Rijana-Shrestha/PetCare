package com.rijana.petcare.ui.pets

import android.app.AlertDialog
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
import com.rijana.petcare.data.local.entity.PetType
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddPetBinding
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    // If this stays null, we're adding a new pet. If it gets set (edit mode),
    // saving updates THIS pet instead of inserting a new one.
    private var existingPet: Pet? = null
    private val isEditMode: Boolean get() = existingPet != null

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri.toString()
                showPickedPhoto(uri.toString())
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

        val petId = arguments?.getLong("petId", -1L) ?: -1L
        if (petId != -1L) {
            loadPetForEditing(petId)
        }
    }

    private fun loadPetForEditing(petId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.getPetById(petId).collect { pet ->
                    if (pet != null) {
                        existingPet = pet
                        prefillForm(pet)
                        switchToEditModeUi()
                    }
                }
            }
        }
    }

    private fun prefillForm(pet: Pet) {
        binding.etPetName.setText(pet.name)

        val typeLabel = pet.type.name.lowercase().replaceFirstChar(Char::uppercase)
        binding.actvPetType.setText(typeLabel, false)

        binding.etBreed.setText(pet.breed)

        selectedDateOfBirth = pet.dateOfBirth
        binding.etDateOfBirth.setText(formatDate(pet.dateOfBirth))

        when (pet.gender) {
            Gender.FEMALE -> binding.toggleGender.check(binding.btnFemale.id)
            else -> binding.toggleGender.check(binding.btnMale.id)
        }

        binding.etWeight.setText(pet.weightKg.toString())
        binding.etDietaryPreference.setText(pet.dietaryPreference)
        binding.etAllergies.setText(pet.allergies)
        binding.etFavoriteToy.setText(pet.favoriteToy)
        binding.etNote.setText(pet.note)

        selectedPhotoUri = pet.photoUri
        if (pet.photoUri != null) {
            showPickedPhoto(pet.photoUri)
        }
    }

    private fun switchToEditModeUi() {
        binding.tvTitle.text = getString(R.string.edit_pet)
        binding.ivNotification.setImageResource(R.drawable.ic_delete)
        binding.ivNotification.setOnClickListener { confirmDelete() }
    }

    private fun showPickedPhoto(uriOrPath: String) {
        Glide.with(this).load(uriOrPath).centerCrop().into(binding.imgPetPhoto)
        binding.ivCameraIcon.visibility = View.GONE
        binding.tvAddPhoto.visibility = View.GONE
    }

    private fun setupPetTypeDropdown() {
        val options = PetType.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        binding.actvPetType.setAdapter(adapter)
        binding.actvPetType.setOnClickListener { binding.actvPetType.showDropDown() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDateOfBirth?.let { calendar.timeInMillis = it }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                selectedDateOfBirth = calendar.timeInMillis
                binding.etDateOfBirth.setText(formatDate(calendar.timeInMillis))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(millis)

    private fun savePet() {
        val name = binding.etPetName.text.toString().trim()
        val typeText = binding.actvPetType.text.toString().trim()
        val breed = binding.etBreed.text.toString().trim()
        val weightText = binding.etWeight.text.toString().trim()
        val ownerId = existingPet?.ownerId ?: petViewModel.ownerId.value

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
            id = existingPet?.id ?: 0,
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

        if (isEditMode) {
            petViewModel.updatePet(pet)
        } else {
            petViewModel.addPet(pet)
        }
        findNavController().popBackStack()
    }

    private fun confirmDelete() {
        val pet = existingPet ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_pet_title)
            .setMessage(getString(R.string.delete_pet_message, pet.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                petViewModel.deletePet(pet)
                // Pop back past Pet Detail too — that pet no longer exists
                findNavController().popBackStack(R.id.petDetailFragment, true)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}