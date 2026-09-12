package com.rijana.petcare.ui.map

import android.app.AlertDialog
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
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.entity.SavedPlace
import com.rijana.petcare.data.repository.SavedPlaceRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentSavedPlacesBinding
import com.rijana.petcare.viewmodel.PlaceViewModel
import com.rijana.petcare.viewmodel.PlaceViewModelFactory
import kotlinx.coroutines.launch

class SavedPlacesListFragment : Fragment() {

    private var _binding: FragmentSavedPlacesBinding? = null
    private val binding get() = _binding!!

    private lateinit var placesAdapter: SavedPlacesAdapter

    private val placeViewModel: PlaceViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        val userRepository = UserRepository(AuthManager(), app.database.userDao())
        PlaceViewModelFactory(SavedPlaceRepository(app.database.savedPlaceDao()), userRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        placesAdapter = SavedPlacesAdapter(
            onPlaceClick = { place -> focusPlaceOnMapAndGoBack(place) },
            onDeleteClick = { place -> confirmDelete(place) }
        )
        binding.rvSavedPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSavedPlaces.adapter = placesAdapter

        observePlaces()
    }

    private fun observePlaces() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                placeViewModel.places.collect { places ->
                    placesAdapter.submitList(places)
                    binding.tvEmpty.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvSavedPlaces.visibility = if (places.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun focusPlaceOnMapAndGoBack(place: SavedPlace) {
        findNavController().previousBackStackEntry
            ?.savedStateHandle
            ?.set(FOCUS_LAT_LNG_KEY, doubleArrayOf(place.latitude, place.longitude))
        findNavController().popBackStack()
    }

    private fun confirmDelete(place: SavedPlace) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Place")
            .setMessage("Remove \"${place.name}\" from your saved places?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> placeViewModel.deletePlace(place) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FOCUS_LAT_LNG_KEY = "focus_lat_lng"
    }
}