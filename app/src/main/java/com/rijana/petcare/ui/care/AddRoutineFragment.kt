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
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.local.entity.Routine
import com.rijana.petcare.data.local.entity.TaskRepeat
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.RoutineRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddRoutineBinding
import com.rijana.petcare.databinding.ItemPetCheckboxBinding
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import com.rijana.petcare.viewmodel.RoutineViewModel
import com.rijana.petcare.viewmodel.RoutineViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class AddRoutineFragment : Fragment() {

    private var _binding: FragmentAddRoutineBinding? = null
    private val binding get() = _binding!!

    private val petCheckboxes = mutableMapOf<Long, CheckBox>()
    private var selectedStartDate: Long? = null

    private val remindOffsetOptions = listOf(5, 10, 15, 30, 60)

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        PetViewModelFactory(PetRepository(app.database.petDao()), userRepository)
    }

    private val routineViewModel: RoutineViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val routineRepository =
            RoutineRepository(app.database.routineDao(), app.database.routineCompletionDao())
        RoutineViewModelFactory(routineRepository, userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        setupRepeatDropdown()
        setupRemindOffsetDropdown()
        loadPetCheckboxes()

        binding.etTime.setOnClickListener { showTimePicker() }
        binding.etStartDate.setOnClickListener { showStartDatePicker() }

        binding.btnAddRoutineSave.setOnClickListener { saveRoutine() }
        binding.tvSaveTop.setOnClickListener { saveRoutine() }
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
            val row = ItemPetCheckboxBinding.inflate(
                layoutInflater, binding.petCheckboxContainer, false
            )
            row.cbPet.text = pet.name
            petCheckboxes[pet.id] = row.cbPet
            binding.petCheckboxContainer.addView(row.root)
        }
    }

    private fun setupRepeatDropdown() {
        val options = TaskRepeat.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        binding.actvRepeat.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.actvRepeat.setOnClickListener { binding.actvRepeat.showDropDown() }
        binding.actvRepeat.setOnItemClickListener { _, _, position, _ ->
            binding.toggleCustomDays.visibility =
                if (TaskRepeat.entries[position] == TaskRepeat.CUSTOM) View.VISIBLE else View.GONE
        }
    }

    private fun setupRemindOffsetDropdown() {
        val options = remindOffsetOptions.map { "$it minutes before" }
        binding.actvRemindOffset.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.actvRemindOffset.setText(options[2], false) // default: 15 minutes before
        binding.actvRemindOffset.setOnClickListener { binding.actvRemindOffset.showDropDown() }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                binding.etTime.setText("%02d:%02d".format(hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                selectedStartDate = calendar.timeInMillis
                binding.etStartDate.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun selectedCustomDays(): String {
        val idToCode = mapOf(
            binding.btnMon.id to "MON", binding.btnTue.id to "TUE", binding.btnWed.id to "WED",
            binding.btnThu.id to "THU", binding.btnFri.id to "FRI", binding.btnSat.id to "SAT",
            binding.btnSun.id to "SUN"
        )
        return binding.toggleCustomDays.checkedButtonIds
            .mapNotNull { idToCode[it] }
            .joinToString(",")
    }

    private fun saveRoutine() {
        val taskName = binding.etTaskName.text.toString().trim()
        val taskType = binding.etTaskType.text.toString().trim()
        val time = binding.etTime.text.toString().trim()
        val repeatText = binding.actvRepeat.text.toString().trim()
        val selectedPetIds = petCheckboxes.filterValues { it.isChecked }.keys

        if (taskName.isEmpty() || time.isEmpty() || repeatText.isEmpty() || selectedStartDate == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedPetIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.select_at_least_one_pet, Toast.LENGTH_SHORT).show()
            return
        }

        val repeat = TaskRepeat.valueOf(repeatText.uppercase())
        val customDays = if (repeat == TaskRepeat.CUSTOM) selectedCustomDays() else null
        if (repeat == TaskRepeat.CUSTOM && customDays.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.select_at_least_one_day, Toast.LENGTH_SHORT).show()
            return
        }

        val duration = binding.etDuration.text.toString().trim().toIntOrNull()
        val reminderEnabled = binding.switchReminder.isChecked
        val reminderOffset = remindOffsetOptions.getOrElse(
            binding.actvRemindOffset.text.toString().trim().takeWhile { it.isDigit() }
                .toIntOrNull()?.let { remindOffsetOptions.indexOf(it) } ?: 2
        ) { 15 }

        // One Routine row per selected pet - each pet gets its own independent
        // copy of this same task, matching the multi-select checkboxes in the design.
        selectedPetIds.forEach { petId ->
            routineViewModel.addRoutine(
                Routine(
                    petId = petId,
                    taskName = taskName,
                    taskType = taskType,
                    time = time,
                    durationMinutes = duration,
                    repeat = repeat,
                    customDays = customDays,
                    startDate = selectedStartDate!!,
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