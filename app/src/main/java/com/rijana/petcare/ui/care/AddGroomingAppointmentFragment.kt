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
import com.rijana.petcare.data.local.entity.GroomingAppointment
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.repository.GroomingAppointmentRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.data.repository.VetAppointmentRepository
import com.rijana.petcare.databinding.FragmentAddGroomingAppointmentBinding
import com.rijana.petcare.databinding.ItemPetCheckboxBinding
import com.rijana.petcare.viewmodel.AppointmentViewModel
import com.rijana.petcare.viewmodel.AppointmentViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class AddGroomingAppointmentFragment : Fragment() {

    private var _binding: FragmentAddGroomingAppointmentBinding? = null
    private val binding get() = _binding!!

    private val petCheckboxes = mutableMapOf<Long, CheckBox>()
    private var selectedDate: Long? = null

    private val remindOffsetOptions = listOf(15, 60, 1440, 2880)
    private val remindOffsetLabels = listOf("15 minutes before", "1 hour before", "1 day before", "2 days before")

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        PetViewModelFactory(PetRepository(app.database.petDao()), userRepository)
    }

    private val appointmentViewModel: AppointmentViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        AppointmentViewModelFactory(
            VetAppointmentRepository(app.database.vetAppointmentDao()),
            GroomingAppointmentRepository(app.database.groomingAppointmentDao()),
            userRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGroomingAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        setupRemindOffsetDropdown()
        loadPetCheckboxes()

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etTime.setOnClickListener { showTimePicker() }

        binding.btnSave.setOnClickListener { saveAppointment() }
        binding.tvSaveTop.setOnClickListener { saveAppointment() }
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

    private fun setupRemindOffsetDropdown() {
        binding.actvRemindOffset.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, remindOffsetLabels)
        )
        binding.actvRemindOffset.setText(remindOffsetLabels[2], false)
        binding.actvRemindOffset.setOnClickListener { binding.actvRemindOffset.showDropDown() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                selectedDate = calendar.timeInMillis
                binding.etDate.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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

    private fun saveAppointment() {
        val type = binding.etType.text.toString().trim()
        val provider = binding.etProviderName.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val selectedPetIds = petCheckboxes.filterValues { it.isChecked }.keys

        if (type.isEmpty() || provider.isEmpty() || time.isEmpty() || selectedDate == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedPetIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.select_at_least_one_pet, Toast.LENGTH_SHORT).show()
            return
        }

        val reminderEnabled = binding.switchReminder.isChecked
        val labelIndex = remindOffsetLabels.indexOf(binding.actvRemindOffset.text.toString())
        val reminderOffset = remindOffsetOptions.getOrElse(labelIndex) { 1440 }

        selectedPetIds.forEach { petId ->
            appointmentViewModel.addGroomingAppointment(
                GroomingAppointment(
                    petId = petId,
                    type = type,
                    salonName = provider,
                    date = selectedDate!!,
                    time = time,
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