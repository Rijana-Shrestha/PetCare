package com.rijana.petcare.ui.expenses

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
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.repository.ExpenseRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentExpenseListBinding
import com.rijana.petcare.viewmodel.ExpenseViewModel
import com.rijana.petcare.viewmodel.ExpenseViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class ExpenseListFragment : Fragment() {

    private var _binding: FragmentExpenseListBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseAdapter: ExpenseAdapter
    private val chipViews = mutableListOf<TextView>()

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val petViewModel: PetViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        PetViewModelFactory(PetRepository(app.database.petDao()), userRepository)
    }

    private val expenseViewModel: ExpenseViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        ExpenseViewModelFactory(ExpenseRepository(app.database.expenseDao()), userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        expenseAdapter = ExpenseAdapter()
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.adapter = expenseAdapter

        binding.btnAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_expenseList_to_addExpense)
        }

        loadPetChips()
        observeExpenses()
        observeTotal()
    }

    private fun loadPetChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.pets.collect { pets -> renderChips(pets) }
            }
        }
    }

    private fun renderChips(pets: List<Pet>) {
        binding.petChipsContainer.removeAllViews()
        chipViews.clear()

        addChip(getString(R.string.all_pets), petId = null)
        pets.forEach { pet -> addChip(pet.name, petId = pet.id) }
    }

    private fun addChip(label: String, petId: Long?) {
        val chip = TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setPadding(32, 14, 32, 14)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 20
            layoutParams = params
        }
        applyChipStyle(chip, isSelected = petId == expenseViewModel.selectedPetId.value)
        chip.setOnClickListener {
            expenseViewModel.selectPet(petId)
            chipViews.forEach { applyChipStyle(it, isSelected = it == chip) }
        }
        chipViews.add(chip)
        binding.petChipsContainer.addView(chip)
    }

    private fun applyChipStyle(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_day_tab_selected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            chip.setBackgroundResource(R.drawable.bg_day_tab_unselected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
    }

    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(petViewModel.pets, expenseViewModel.expenses) { pets, expenses ->
                    val petNamesById = pets.associate { it.id to it.name }
                    expenses.map { expense -> ExpenseListItem(expense, petNamesById[expense.petId] ?: "") }
                }.collect { items -> expenseAdapter.submitList(items) }
            }
        }
    }

    private fun observeTotal() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                expenseViewModel.monthlyTotal.collect { total ->
                    binding.tvMonthlyTotal.text = "$${"%.2f".format(total)}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}