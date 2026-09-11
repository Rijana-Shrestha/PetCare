package com.rijana.petcare.ui.home

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.ExpenseRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.RoutineRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentHomeDashboardBinding
import com.rijana.petcare.ui.care.RoutineAdapter
import com.rijana.petcare.ui.care.RoutineListItem
import com.rijana.petcare.ui.expenses.ExpenseAdapter
import com.rijana.petcare.ui.expenses.ExpenseListItem
import com.rijana.petcare.viewmodel.ExpenseViewModel
import com.rijana.petcare.viewmodel.ExpenseViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import com.rijana.petcare.viewmodel.RoutineViewModel
import com.rijana.petcare.viewmodel.RoutineViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var homePetAdapter: HomePetAdapter
    private lateinit var todaysCareAdapter: RoutineAdapter
    private lateinit var expenseAdapter: ExpenseAdapter

    private var hasPets = false

    private var hasTodaysCareItems = false
    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val petRepository = PetRepository(app.database.petDao())
        PetViewModelFactory(petRepository, userRepository)
    }

    private val routineViewModel: RoutineViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val routineRepository =
            RoutineRepository(app.database.routineDao(), app.database.routineCompletionDao())
        RoutineViewModelFactory(routineRepository, userRepository)
    }

    private val expenseViewModel: ExpenseViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        ExpenseViewModelFactory(ExpenseRepository(app.database.expenseDao()), userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homePetAdapter = HomePetAdapter { pet ->
            val bundle = Bundle().apply { putLong("petId", pet.id) }
            findNavController().navigate(R.id.action_homeDashboard_to_petDetail, bundle)
        }
        binding.rvMyPets.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMyPets.adapter = homePetAdapter

        todaysCareAdapter = RoutineAdapter { occurrence -> routineViewModel.toggleComplete(occurrence) }
        binding.rvTodaysCare.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodaysCare.adapter = todaysCareAdapter

        expenseAdapter = ExpenseAdapter()
        binding.expensesCard.rvRecentExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.expensesCard.rvRecentExpenses.adapter = expenseAdapter

        binding.btnAddPet.setOnClickListener {
            findNavController().navigate(R.id.action_homeDashboard_to_addPet)
        }
        binding.expensesCard.btnViewAllExpenses.setOnClickListener {
            findNavController().navigate(R.id.action_homeDashboard_to_expenseList)
        }
        binding.expensesCard.btnAddExpenses.setOnClickListener {
            findNavController().navigate(R.id.action_homeDashboard_to_expenseList)
            findNavController().navigate(R.id.action_expenseList_to_addExpense)
        }

        observeGreeting()
        observePets()
        observeTodaysCare()
        observeExpenses()
    }

    private fun observeGreeting() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val firebaseUid = userRepository.currentFirebaseUid ?: return@repeatOnLifecycle
                val user = userRepository.getUserProfile(firebaseUid).filterNotNull().first()
                binding.tvGreeting.text = "${timeOfDayGreeting()}, ${user.name}!"
            }
        }
    }

    private fun timeOfDayGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> getString(R.string.good_morning)
            hour < 17 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }
    }

    private fun observePets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.pets.collect { pets ->
                    homePetAdapter.submitList(pets)
                    hasPets = pets.isNotEmpty()
                    binding.cardNoPets.visibility = if (hasPets) View.GONE else View.VISIBLE
                    binding.rvMyPets.visibility = if (hasPets) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun observeTodaysCare() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(petViewModel.pets, routineViewModel.routinesForSelectedDay) { pets, occurrences ->
                    val petNamesById = pets.associate { it.id to it.name }
                    occurrences.map { occurrence ->
                        RoutineListItem(occurrence = occurrence, petName = petNamesById[occurrence.routine.petId] ?: "")
                    }
                }.collect { items ->
                    todaysCareAdapter.submitList(items)
                    hasTodaysCareItems = items.isNotEmpty()
                    binding.cardNoRoutines.visibility = if (hasTodaysCareItems) View.GONE else View.VISIBLE
                    binding.rvTodaysCare.visibility = if (hasTodaysCareItems) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    petViewModel.pets, expenseViewModel.expenses, expenseViewModel.monthlyTotal
                ) { pets, expenses, total -> Triple(pets, expenses, total) }
                    .collect { (pets, expenses, total) ->
                        val hasExpenses = expenses.isNotEmpty()
                        binding.expensesCard.tvNoExpenses.visibility =
                            if (hasExpenses) View.GONE else View.VISIBLE
                        binding.expensesCard.hasExpensesGroup.visibility =
                            if (hasExpenses) View.VISIBLE else View.GONE

                        if (hasExpenses) {
                            binding.expensesCard.tvMonthlyTotal.text = "$${"%.2f".format(total)}"
                            val petNamesById = pets.associate { it.id to it.name }
                            val recent = expenses.take(3)
                                .map { ExpenseListItem(it, petNamesById[it.petId] ?: "") }
                            expenseAdapter.submitList(recent)
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}