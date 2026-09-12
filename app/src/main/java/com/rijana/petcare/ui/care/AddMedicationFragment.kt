package com.rijana.petcare.ui.care

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
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
import com.rijana.petcare.data.local.entity.Medication
import com.rijana.petcare.data.local.entity.MedicationType
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.repository.MedicationRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddMedicationBinding
import com.rijana.petcare.databinding.ItemPetCheckboxBinding
import com.rijana.petcare.viewmodel.MedicationViewModel
import com.rijana.petcare.viewmodel.MedicationViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class AddMedicationFragment : Fragment() {

    private var _binding: FragmentAddMedicationBinding? = null
    private val binding get() = _binding!!

    private val petCheckboxes = mutableMapOf<Long, CheckBox>()
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null

    private val remindOffsetOptions = listOf(5, 10, 15, 30, 60)

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        PetViewModelFactory(PetRepository(app.database.petDao()), userRepository)
    }

    private val medicationViewModel: MedicationViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        MedicationViewModelFactory(
            MedicationRepository(app.database.medicationDao(), app.database.medicationCompletionDao()),
            userRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        setupTypeDropdown()
        setupRemindOffsetDropdown()
        loadPetCheckboxes()

        binding.etTime.setOnClickListener { showTimePicker() }
        binding.etStartDate.setOnClickListener { showDatePicker(isStartDate = true) }
        binding.etEndDate.setOnClickListener { showDatePicker(isStartDate = false) }

        binding.btnAddMedicationSave.setOnClickListener { saveMedication() }
        binding.tvSaveTop.setOnClickListener { saveMedication() }
    }

    private fun loadPetCheckboxes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.pets.collect { pets -> renderPetCheckboxes(pets) }
            }
        }
    }

    private fun renderPetCheckboxes(pets: List<Pet>) {
        binding.petCheckboxContainer.removeAllViews()
        petCheckboxes.clear()
        pets.forEach { pet ->
            val row = ItemPetCheckboxBinding.inflate(layoutInflater, binding.petCheckboxContainer, false)
            row.cbPet.text = pet.name
            petCheckboxes[pet.id] = row.cbPet
            binding.petCheckboxContainer.addView(row.root)
        }
    }

    private fun setupTypeDropdown() {
        val options = MedicationType.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        binding.actvMedicationType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.actvMedicationType.setOnClickListener { binding.actvMedicationType.showDropDown() }
    }

    private fun setupRemindOffsetDropdown() {
        val options = remindOffsetOptions.map { "$it minutes before" }
        binding.actvRemindOffset.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.actvRemindOffset.setText(options[2], false)
        binding.actvRemindOffset.setOnClickListener { binding.actvRemindOffset.showDropDown() }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute -> binding.etTime.setText("%02d:%02d".format(hour, minute)) },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                val text = "%02d/%02d/%04d".format(day, month + 1, year)
                if (isStartDate) {
                    selectedStartDate = calendar.timeInMillis
                    binding.etStartDate.setText(text)
                } else {
                    selectedEndDate = calendar.timeInMillis
                    binding.etEndDate.setText(text)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveMedication() {
        val name = binding.etMedicationName.text.toString().trim()
        val typeText = binding.actvMedicationType.text.toString().trim()
        val dosage = binding.etDosage.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val frequency = binding.etFrequency.text.toString().trim()
        val selectedPetIds = petCheckboxes.filterValues { it.isChecked }.keys

        if (name.isEmpty() || typeText.isEmpty() || dosage.isEmpty() ||
            time.isEmpty() || frequency.isEmpty() || selectedStartDate == null
        ) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedPetIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.select_at_least_one_pet, Toast.LENGTH_SHORT).show()
            return
        }

        val reminderEnabled = binding.switchReminder.isChecked
        val reminderOffset = remindOffsetOptions.getOrElse(
            remindOffsetOptions.indexOf(
                binding.actvRemindOffset.text.toString().takeWhile { it.isDigit() }.toIntOrNull() ?: 15
            ).coerceAtLeast(0)
        ) { 15 }

        selectedPetIds.forEach { petId ->
            medicationViewModel.addMedication(
                Medication(
                    petId = petId,
                    name = name,
                    type = MedicationType.valueOf(typeText.uppercase()),
                    dosage = dosage,
                    instructions = binding.etInstructions.text.toString().trim().ifEmpty { null },
                    time = time,
                    frequency = frequency,
                    startDate = selectedStartDate!!,
                    endDate = selectedEndDate,
                    reminderEnabled = reminderEnabled,
                    reminderOffsetMinutes = reminderOffset
                )
            )
        }

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}