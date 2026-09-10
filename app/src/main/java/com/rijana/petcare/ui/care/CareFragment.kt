package com.rijana.petcare.ui.care

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.GroomingAppointmentRepository
import com.rijana.petcare.data.repository.MedicationRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.RoutineRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.data.repository.VetAppointmentRepository
import com.rijana.petcare.databinding.FragmentCareBinding
import com.rijana.petcare.viewmodel.AppointmentViewModel
import com.rijana.petcare.viewmodel.AppointmentViewModelFactory
import com.rijana.petcare.viewmodel.MedicationViewModel
import com.rijana.petcare.viewmodel.MedicationViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import com.rijana.petcare.viewmodel.RoutineViewModel
import com.rijana.petcare.viewmodel.RoutineViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class CareTab { ROUTINES, MEDICATION, VET_GROOMING }

class CareFragment : Fragment() {

    private var _binding: FragmentCareBinding? = null
    private val binding get() = _binding!!

    private lateinit var routineAdapter: RoutineAdapter
    private lateinit var medicationAdapter: MedicationAdapter
    private lateinit var vetAdapter: VetAppointmentAdapter
    private lateinit var groomingAdapter: GroomingAppointmentAdapter
    private val dayCells = mutableListOf<TextView>()
    private var currentTab = CareTab.ROUTINES

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val routineViewModel: RoutineViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val routineRepository =
            RoutineRepository(app.database.routineDao(), app.database.routineCompletionDao())
        RoutineViewModelFactory(routineRepository, userRepository)
    }

    private val medicationViewModel: MedicationViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        MedicationViewModelFactory(MedicationRepository(app.database.medicationDao()), userRepository)
    }

    private val appointmentViewModel: AppointmentViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        AppointmentViewModelFactory(
            VetAppointmentRepository(app.database.vetAppointmentDao()),
            GroomingAppointmentRepository(app.database.groomingAppointmentDao()),
            userRepository
        )
    }

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val petRepository = PetRepository(app.database.petDao())
        PetViewModelFactory(petRepository, userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnShareRoutine.setOnClickListener {
            findNavController().navigate(R.id.action_care_to_delegateTask)
        }

        setupDayTabs()
        setupSectionToggle()

        routineAdapter = RoutineAdapter { occurrence -> routineViewModel.toggleComplete(occurrence) }
        binding.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutines.adapter = routineAdapter

        medicationAdapter = MedicationAdapter { medication -> medicationViewModel.deleteMedication(medication) }
        binding.rvMedications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMedications.adapter = medicationAdapter

        vetAdapter = VetAppointmentAdapter { appt -> appointmentViewModel.deleteVetAppointment(appt) }
        binding.rvVetAppointments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVetAppointments.adapter = vetAdapter

        groomingAdapter = GroomingAppointmentAdapter { appt -> appointmentViewModel.deleteGroomingAppointment(appt) }
        binding.rvGroomingAppointments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGroomingAppointments.adapter = groomingAdapter

        updateAddButton()
        observeRoutines()
        observeMedications()
        observeAppointments()
    }

    private fun setupDayTabs() {
        binding.dayTabsContainer.removeAllViews()
        dayCells.clear()

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = ((dayOfWeek - Calendar.MONDAY) + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)

        val todayStart = RoutineViewModel.startOfDay(System.currentTimeMillis())
        val dayLetterFormat = SimpleDateFormat("EEE", Locale.getDefault())

        repeat(7) {
            val cellDate = calendar.timeInMillis
            val cell = layoutInflater.inflate(
                R.layout.item_day_tab, binding.dayTabsContainer, false
            ) as TextView
            cell.text = "${dayLetterFormat.format(cellDate)}\n${calendar.get(Calendar.DAY_OF_MONTH)}"
            cell.isSelected = RoutineViewModel.startOfDay(cellDate) == todayStart
            applyDayTabStyle(cell)

            cell.setOnClickListener {
                dayCells.forEach { c -> c.isSelected = false; applyDayTabStyle(c) }
                cell.isSelected = true
                applyDayTabStyle(cell)
                routineViewModel.selectDate(cellDate)
            }

            dayCells.add(cell)
            binding.dayTabsContainer.addView(cell)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun applyDayTabStyle(cell: TextView) {
        if (cell.isSelected) {
            cell.setBackgroundResource(R.drawable.bg_day_tab_selected)
            cell.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            cell.setBackgroundResource(R.drawable.bg_day_tab_unselected)
            cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
    }

    private fun setupSectionToggle() {
        binding.toggleCareSection.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentTab = when (checkedId) {
                binding.btnRoutinesTab.id -> CareTab.ROUTINES
                binding.btnMedicationTab.id -> CareTab.MEDICATION
                else -> CareTab.VET_GROOMING
            }
            binding.rvRoutines.visibility = if (currentTab == CareTab.ROUTINES) View.VISIBLE else View.GONE
            binding.rvMedications.visibility = if (currentTab == CareTab.MEDICATION) View.VISIBLE else View.GONE
            binding.vetGroomingScroll.visibility = if (currentTab == CareTab.VET_GROOMING) View.VISIBLE else View.GONE
            updateAddButton()
        }
    }

    private fun updateAddButton() {
        when (currentTab) {
            CareTab.ROUTINES -> {
                binding.btnAddRoutine.visibility = View.VISIBLE
                binding.btnAddRoutine.text = getString(R.string.add_routine)
                binding.btnAddRoutine.setOnClickListener {
                    findNavController().navigate(R.id.action_care_to_addRoutine)
                }
            }
            CareTab.MEDICATION -> {
                binding.btnAddRoutine.visibility = View.VISIBLE
                binding.btnAddRoutine.text = getString(R.string.add_medication)
                binding.btnAddRoutine.setOnClickListener {
                    findNavController().navigate(R.id.action_care_to_addMedication)
                }
            }
            CareTab.VET_GROOMING -> {
                binding.btnAddRoutine.visibility = View.VISIBLE
                binding.btnAddRoutine.text = getString(R.string.add_appointment)
                binding.btnAddRoutine.setOnClickListener {
                    android.app.AlertDialog.Builder(requireContext())
                        .setItems(arrayOf("Vet Appointment", "Grooming Appointment")) { _, which ->
                            if (which == 0) {
                                findNavController().navigate(R.id.action_care_to_addVetAppointment)
                            } else {
                                findNavController().navigate(R.id.action_care_to_addGroomingAppointment)
                            }
                        }
                        .show()
                }
            }
        }
    }

    private fun observeRoutines() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(petViewModel.pets, routineViewModel.routinesForSelectedDay) { pets, occurrences ->
                    val petNamesById = pets.associate { it.id to it.name }
                    occurrences.map { occurrence ->
                        RoutineListItem(occurrence = occurrence, petName = petNamesById[occurrence.routine.petId] ?: "")
                    }
                }.collect { items -> routineAdapter.submitList(items) }
            }
        }
    }

    private fun observeMedications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(petViewModel.pets, medicationViewModel.medications) { pets, meds ->
                    val petNamesById = pets.associate { it.id to it.name }
                    meds.map { med -> MedicationListItem(medication = med, petName = petNamesById[med.petId] ?: "") }
                }.collect { items -> medicationAdapter.submitList(items) }
            }
        }
    }

    private fun observeAppointments() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(petViewModel.pets, appointmentViewModel.vetAppointments) { pets, appts ->
                    val petNamesById = pets.associate { it.id to it.name }
                    appts.map { appt -> VetAppointmentListItem(appointment = appt, petName = petNamesById[appt.petId] ?: "") }
                }.collect { items -> vetAdapter.submitList(items) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(petViewModel.pets, appointmentViewModel.groomingAppointments) { pets, appts ->
                    val petNamesById = pets.associate { it.id to it.name }
                    appts.map { appt -> GroomingAppointmentListItem(appointment = appt, petName = petNamesById[appt.petId] ?: "") }
                }.collect { items -> groomingAdapter.submitList(items) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}