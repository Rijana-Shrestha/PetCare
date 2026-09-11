package com.rijana.petcare.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.ExpenseRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentProfileBinding
import com.rijana.petcare.ui.expenses.ExpenseAdapter
import com.rijana.petcare.ui.expenses.ExpenseListItem
import com.rijana.petcare.ui.home.HomePetAdapter
import com.rijana.petcare.viewmodel.ExpenseViewModel
import com.rijana.petcare.viewmodel.ExpenseViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import com.rijana.petcare.viewmodel.ProfileViewModel
import com.rijana.petcare.viewmodel.ProfileViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var petAdapter: HomePetAdapter
    private lateinit var expenseAdapter: ExpenseAdapter

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(userRepository)
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        petAdapter = HomePetAdapter { pet ->
            val bundle = Bundle().apply { putLong("petId", pet.id) }
            findNavController().navigate(R.id.action_profile_to_petDetail, bundle)
        }
        binding.rvPets.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPets.adapter = petAdapter

        expenseAdapter = ExpenseAdapter()
        binding.expensesCard.rvRecentExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.expensesCard.rvRecentExpenses.adapter = expenseAdapter

        binding.ivEdit.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }
        binding.btnAddNewPet.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_addPet)
        }
        binding.expensesCard.btnViewAllExpenses.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_expenseList)
        }
        binding.expensesCard.btnAddExpenses.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_expenseList)
            findNavController().navigate(R.id.action_expenseList_to_addExpense)
        }
        binding.rowPersonalInfo.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }
        binding.rowChangePassword.setOnClickListener { changePassword() }
        binding.rowNotificationPreference.setOnClickListener {
            Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.rowEmergencyContacts.setOnClickListener {
            Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnLogOut.setOnClickListener { confirmLogOut() }

        observeUser()
        observePets()
        observeExpenses()
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.currentUser.collect { user ->
                    if (user == null) return@collect
                    binding.tvName.text = user.name
                    binding.tvEmail.text = user.email
                    Glide.with(binding.ivAvatar)
                        .load(user.profileImageUri)
                        .placeholder(R.drawable.circle_avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivAvatar)
                }
            }
        }
    }

    private fun observePets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.pets.collect { pets -> petAdapter.submitList(pets) }
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

    private fun changePassword() {
        profileViewModel.sendPasswordResetEmail { success, error ->
            val message = if (success) {
                "Password reset email sent — check your inbox"
            } else {
                error ?: "Couldn't send reset email"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmLogOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log Out") { _, _ -> logOut() }
            .show()
    }

    private fun logOut() {
        profileViewModel.signOut()

        val outerNavController = Navigation.findNavController(requireActivity(), R.id.main)
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        outerNavController.navigate(R.id.getStartedFragment, null, navOptions)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}