package com.rijana.petcare.ui.expenses

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
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
import com.rijana.petcare.data.local.entity.Expense
import com.rijana.petcare.data.local.entity.ExpenseCategory
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.repository.ExpenseRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddExpenseBinding
import com.rijana.petcare.viewmodel.ExpenseViewModel
import com.rijana.petcare.viewmodel.ExpenseViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val radioIdToPetId = mutableMapOf<Int, Long>()
    private var selectedDate: Long? = null

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
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        setupCategoryDropdown()
        loadPetOptions()

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun loadPetOptions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.pets.collect { pets -> renderPetOptions(pets) }
            }
        }
    }

    private fun renderPetOptions(pets: List<Pet>) {
        binding.petRadioGroup.removeAllViews()
        radioIdToPetId.clear()
        pets.forEach { pet ->
            val radioButton = RadioButton(requireContext()).apply {
                text = pet.name
                id = View.generateViewId()
            }
            radioIdToPetId[radioButton.id] = pet.id
            binding.petRadioGroup.addView(radioButton)
        }
        if (pets.isNotEmpty()) {
            (binding.petRadioGroup.getChildAt(0) as RadioButton).isChecked = true
        }
    }

    private fun setupCategoryDropdown() {
        val options = ExpenseCategory.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        binding.actvCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.actvCategory.setOnClickListener { binding.actvCategory.showDropDown() }
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

    private fun saveExpense() {
        val categoryText = binding.actvCategory.text.toString().trim()
        val amountText = binding.etAmount.text.toString().trim()
        val checkedRadioId = binding.petRadioGroup.checkedRadioButtonId
        val petId = radioIdToPetId[checkedRadioId]

        if (categoryText.isEmpty() || amountText.isEmpty() || selectedDate == null) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (petId == null) {
            Toast.makeText(requireContext(), R.string.please_select_a_pet, Toast.LENGTH_SHORT).show()
            return
        }

        expenseViewModel.addExpense(
            Expense(
                petId = petId,
                category = ExpenseCategory.valueOf(categoryText.uppercase()),
                amount = amountText.toFloat(),
                date = selectedDate!!,
                note = binding.etNote.text.toString().trim().ifEmpty { null }
            )
        )

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}