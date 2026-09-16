package com.rijana.petcare.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.entity.PlaceType
import com.rijana.petcare.data.local.entity.SavedPlace
import com.rijana.petcare.data.network.NominatimClient
import com.rijana.petcare.data.repository.SavedPlaceRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentSearchPlaceBinding
import com.rijana.petcare.viewmodel.PlaceViewModel
import com.rijana.petcare.viewmodel.PlaceViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchPlaceFragment : Fragment() {

    private var _binding: FragmentSearchPlaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var resultAdapter: SearchResultAdapter

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val placeViewModel: PlaceViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        PlaceViewModelFactory(SavedPlaceRepository(app.database.savedPlaceDao()), userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchPlaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        resultAdapter = SearchResultAdapter(
            onResultClick = { result -> returnToMap(result) },
            onBookmarkClick = { result, position -> saveResult(result, position) }
        )
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = resultAdapter

        binding.etSearchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearchBar.text.toString())
                true
            } else false
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = NominatimClient.api.search(query)
                resultAdapter.submitResults(results)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search failed - check your connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToMap(result: com.rijana.petcare.data.network.NominatimResult) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set("selected_lat", result.lat.toDouble())
        findNavController().previousBackStackEntry?.savedStateHandle?.set("selected_lon", result.lon.toDouble())
        findNavController().popBackStack()
    }

    private fun saveResult(result: com.rijana.petcare.data.network.NominatimResult, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val firebaseUid = userRepository.currentFirebaseUid
            val ownerId = firebaseUid?.let { userRepository.getUserProfile(it).first() }?.id

            if (ownerId == null) {
                Toast.makeText(requireContext(), "Couldn't identify your account", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val name = result.display_name.split(",").firstOrNull()?.trim() ?: result.display_name

            placeViewModel.addPlace(
                SavedPlace(
                    ownerId = ownerId,
                    name = name,
                    type = PlaceType.OTHER,
                    latitude = result.lat.toDouble(),
                    longitude = result.lon.toDouble(),
                    address = result.display_name
                )
            )
            resultAdapter.markSaved(position)
            Toast.makeText(requireContext(), R.string.saved_to_places, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}