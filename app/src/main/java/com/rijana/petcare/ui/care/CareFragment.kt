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
import androidx.recyclerview.widget.LinearLayoutManager
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.RoutineRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentCareBinding
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import com.rijana.petcare.viewmodel.RoutineViewModel
import com.rijana.petcare.viewmodel.RoutineViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CareFragment : Fragment() {

    private var _binding: FragmentCareBinding? = null
    private val binding get() = _binding!!

    private lateinit var routineAdapter: RoutineAdapter
    private val dayCells = mutableListOf<TextView>()

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

        setupDayTabs()
        setupSectionToggle()

        routineAdapter = RoutineAdapter { occurrence -> routineViewModel.toggleComplete(occurrence) }
        binding.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutines.adapter = routineAdapter

        binding.btnAddRoutine.setOnClickListener {
            // TODO: navigate to Add Routine once that form is built
        }

        observeRoutines()
    }

    private fun setupDayTabs() {
        binding.dayTabsContainer.removeAllViews()
        dayCells.clear()

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun ... 7=Sat
        val daysSinceMonday = ((dayOfWeek - Calendar.MONDAY) + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday) // roll back to this week's Monday

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
            val showRoutines = checkedId == binding.btnRoutinesTab.id
            binding.rvRoutines.visibility = if (showRoutines) View.VISIBLE else View.GONE
            binding.btnAddRoutine.visibility = if (showRoutines) View.VISIBLE else View.GONE
            binding.tvComingSoon.visibility = if (showRoutines) View.GONE else View.VISIBLE
        }
    }

    private fun observeRoutines() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    petViewModel.pets,
                    routineViewModel.routinesForSelectedDay
                ) { pets, occurrences ->
                    val petNamesById = pets.associate { it.id to it.name }
                    occurrences.map { occurrence ->
                        RoutineListItem(
                            occurrence = occurrence,
                            petName = petNamesById[occurrence.routine.petId] ?: ""
                        )
                    }
                }.collect { items ->
                    routineAdapter.submitList(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}