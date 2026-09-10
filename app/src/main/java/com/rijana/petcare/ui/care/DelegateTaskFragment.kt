package com.rijana.petcare.ui.care

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.rijana.petcare.data.local.entity.Contact
import com.rijana.petcare.data.local.entity.Pet
import com.rijana.petcare.data.repository.ContactRepository
import com.rijana.petcare.data.repository.PetRepository
import com.rijana.petcare.data.repository.RoutineRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentDelegateTaskBinding
import com.rijana.petcare.databinding.ItemDelegateContactBinding
import com.rijana.petcare.databinding.ItemDelegatePetBinding
import com.rijana.petcare.databinding.ItemRoutineBinding
import com.rijana.petcare.viewmodel.ContactViewModel
import com.rijana.petcare.viewmodel.ContactViewModelFactory
import com.rijana.petcare.viewmodel.PetViewModel
import com.rijana.petcare.viewmodel.PetViewModelFactory
import com.rijana.petcare.viewmodel.RoutineOccurrence
import com.rijana.petcare.viewmodel.RoutineViewModel
import com.rijana.petcare.viewmodel.RoutineViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DelegateTaskFragment : Fragment() {

    private var _binding: FragmentDelegateTaskBinding? = null
    private val binding get() = _binding!!

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

    private val contactViewModel: ContactViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        ContactViewModelFactory(ContactRepository(app.database.contactDao()), userRepository)
    }

    private val selectedPetIds = mutableSetOf<Long>()
    private val selectedRoutineIds = mutableSetOf<Long>()
    private val selectedContactIds = mutableSetOf<Long>()

    private var latestPets: List<Pet> = emptyList()
    private var latestOccurrences: List<RoutineOccurrence> = emptyList()
    private var lastAutoMessage: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDelegateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnAddNewContact.setOnClickListener { showAddContactDialog() }
        binding.btnSendViaSms.setOnClickListener { sendViaSms() }

        observePets()
        observeRoutines()
        observeContacts()
    }

    private fun observePets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petViewModel.pets.collect { pets ->
                    latestPets = pets
                    renderPetList()
                    renderTaskSections()
                }
            }
        }
    }

    private fun observeRoutines() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                routineViewModel.routinesForSelectedDay.collect { occurrences ->
                    latestOccurrences = occurrences
                    renderTaskSections()
                }
            }
        }
    }

    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                contactViewModel.contacts.collect { contacts -> renderContactList(contacts) }
            }
        }
    }

    private fun renderPetList() {
        binding.petListContainer.removeAllViews()
        latestPets.forEach { pet ->
            val row = ItemDelegatePetBinding.inflate(layoutInflater, binding.petListContainer, false)
            row.tvPetName.text = pet.name
            Glide.with(row.ivPetAvatar)
                .load(pet.photoUri)
                .placeholder(R.drawable.circle_avatar_placeholder)
                .circleCrop()
                .into(row.ivPetAvatar)
            bindSelectable(row.root, row.cbPetSelected, selectedPetIds.contains(pet.id))

            row.root.setOnClickListener {
                if (selectedPetIds.contains(pet.id)) selectedPetIds.remove(pet.id)
                else selectedPetIds.add(pet.id)
                renderPetList()
                renderTaskSections()
            }
            binding.petListContainer.addView(row.root)
        }
    }

    private fun renderTaskSections() {
        binding.taskSectionsContainer.removeAllViews()

        val relevantOccurrences = latestOccurrences.filter { it.routine.petId in selectedPetIds }
        val byPet = relevantOccurrences.groupBy { it.routine.petId }

        byPet.forEach { (petId, occurrences) ->
            val petName = latestPets.firstOrNull { it.id == petId }?.name ?: ""

            val header = TextView(requireContext()).apply {
                text = petName
                setTextColor(resources.getColor(R.color.text_primary, null))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                textSize = 14f
                setPadding(0, 0, 0, 8)
            }
            binding.taskSectionsContainer.addView(header)

            val card = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.card_stroke_unselected)
                setPadding(28, 20, 28, 20)
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 20
                layoutParams = params
            }

            occurrences.forEach { occurrence ->
                val row = ItemRoutineBinding.inflate(layoutInflater, card, false)
                row.tvRoutineTitle.text = occurrence.routine.taskName
                row.tvRoutineSubtitle.text = occurrence.routine.durationMinutes
                    ?.let { "$it minutes" }
                    ?: occurrence.routine.taskType
                row.tvRoutineTime.text = formatTime(occurrence.routine.time)
                row.cbDone.setOnCheckedChangeListener(null)
                row.cbDone.isChecked = selectedRoutineIds.contains(occurrence.routine.id)
                row.cbDone.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedRoutineIds.add(occurrence.routine.id)
                    else selectedRoutineIds.remove(occurrence.routine.id)
                    updateDefaultMessage()
                }
                card.addView(row.root)
            }

            binding.taskSectionsContainer.addView(card)
        }

        updateDefaultMessage()
    }

    private fun renderContactList(contacts: List<Contact>) {
        binding.contactListContainer.removeAllViews()
        contacts.forEach { contact ->
            val row = ItemDelegateContactBinding.inflate(layoutInflater, binding.contactListContainer, false)
            row.tvContactName.text = contact.name
            row.tvContactPhone.text = contact.phoneNumber
            Glide.with(row.ivContactAvatar)
                .load(contact.profileImageUri)
                .placeholder(R.drawable.circle_avatar_placeholder)
                .circleCrop()
                .into(row.ivContactAvatar)
            bindSelectable(row.root, row.cbContactSelected, selectedContactIds.contains(contact.id))

            row.root.setOnClickListener {
                if (selectedContactIds.contains(contact.id)) selectedContactIds.remove(contact.id)
                else selectedContactIds.add(contact.id)
                renderContactList(contacts)
            }
            binding.contactListContainer.addView(row.root)
        }
    }

    private fun bindSelectable(row: View, checkbox: android.widget.CheckBox, isSelected: Boolean) {
        checkbox.isChecked = isSelected
        row.setBackgroundResource(
            if (isSelected) R.drawable.card_stroke_selected else R.drawable.card_stroke_unselected
        )
    }

    private fun formatTime(rawTime: String): String = try {
        val parsed = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(rawTime)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsed!!)
    } catch (e: Exception) {
        rawTime
    }

    private fun updateDefaultMessage() {
        val selected = latestOccurrences.filter { it.routine.id in selectedRoutineIds }
        val newDefault = if (selected.isEmpty()) {
            ""
        } else {
            val petName = latestPets.firstOrNull { it.id == selected.first().routine.petId }?.name ?: ""
            val taskList = selected.joinToString(", ") {
                "${it.routine.taskName} (${formatTime(it.routine.time)})"
            }
            "Hi! Could you help with $petName's care today: $taskList? Thanks!"
        }

        if (binding.etMessage.text.toString() == lastAutoMessage) {
            binding.etMessage.setText(newDefault)
        }
        lastAutoMessage = newDefault
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etContactName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etContactPhone)
        val etRelationship = dialogView.findViewById<EditText>(R.id.etContactRelationship)

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Contact")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(requireContext(), "Name and phone are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                contactViewModel.addContact(name, phone, etRelationship.text.toString().trim())
            }
            .show()
    }

    private fun sendViaSms() {
        if (selectedRoutineIds.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one task to delegate", Toast.LENGTH_SHORT).show()
            return
        }
        val contacts = contactViewModel.contacts.value.filter { it.id in selectedContactIds }
        if (contacts.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one contact", Toast.LENGTH_SHORT).show()
            return
        }

        val numbers = contacts.joinToString(";") { it.phoneNumber }
        val message = binding.etMessage.text.toString().ifBlank { lastAutoMessage }

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$numbers")).apply {
            putExtra("sms_body", message)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No messaging app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}