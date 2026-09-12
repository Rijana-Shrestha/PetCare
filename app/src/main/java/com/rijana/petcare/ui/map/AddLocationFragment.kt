package com.rijana.petcare.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rijana.petcare.BuildConfig
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.entity.PlaceType
import com.rijana.petcare.data.local.entity.SavedPlace
import com.rijana.petcare.data.network.NominatimClient
import com.rijana.petcare.data.repository.SavedPlaceRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddLocationBinding
import com.rijana.petcare.viewmodel.PlaceViewModel
import com.rijana.petcare.viewmodel.PlaceViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class AddLocationFragment : Fragment() {

    private var _binding: FragmentAddLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var maplibreMap: MapLibreMap
    private var selectedAddress: String? = null

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
        MapLibre.getInstance(requireContext())
        _binding = FragmentAddLocationBinding.inflate(inflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        setupTypeDropdown()

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.text.toString())
                true
            } else false
        }

        binding.mapView.getMapAsync { map ->
            maplibreMap = map
            val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
            map.setStyle(styleUrl) {
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(27.7172, 85.3240))
                    .zoom(13.0)
                    .build()
                reverseGeocodeCurrentCenter()

                map.addOnCameraIdleListener { reverseGeocodeCurrentCenter() }
            }
        }

        binding.btnSaveLocation.setOnClickListener { saveLocation() }
    }

    private fun setupTypeDropdown() {
        val options = PlaceType.entries.map {
            it.name.lowercase().replace("_", " ").replaceFirstChar(Char::uppercase)
        }
        binding.actvType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.actvType.setOnClickListener { binding.actvType.showDropDown() }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = NominatimClient.api.search(query)
                val first = results.firstOrNull()
                if (first != null) {
                    moveCamera(first.lat.toDouble(), first.lon.toDouble())
                } else {
                    Toast.makeText(requireContext(), "No results found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search failed - check your connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveCamera(lat: Double, lng: Double) {
        if (!::maplibreMap.isInitialized) return
        maplibreMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(15.0)
            .build()
    }

    private fun reverseGeocodeCurrentCenter() {
        if (!::maplibreMap.isInitialized) return
        val center = maplibreMap.cameraPosition.target ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = NominatimClient.api.reverse(center.latitude, center.longitude)
                selectedAddress = result.display_name
                binding.tvSelectedAddress.text = "Selected: ${result.display_name}"
            } catch (e: Exception) {
                binding.tvSelectedAddress.text = "Selected: ${center.latitude}, ${center.longitude}"
            }
        }
    }

    private fun saveLocation() {
        val name = binding.etLocationName.text.toString().trim()
        val typeText = binding.actvType.text.toString().trim()
        val ownerId = placeViewModel.let { null } // placeholder, resolved below

        if (name.isEmpty() || typeText.isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_enter_a_name, Toast.LENGTH_SHORT).show()
            return
        }
        if (!::maplibreMap.isInitialized) return
        val center = maplibreMap.cameraPosition.target ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val firebaseUid = userRepository.currentFirebaseUid
            val ownerIdResolved = firebaseUid?.let { uid ->
                userRepository.getUserProfile(uid).first()
            }?.id

            if (ownerIdResolved == null) {
                Toast.makeText(requireContext(), "Couldn't identify your account", Toast.LENGTH_SHORT).show()
                return@launch
            }

            placeViewModel.addPlace(
                SavedPlace(
                    ownerId = ownerIdResolved,
                    name = name,
                    type = PlaceType.valueOf(typeText.uppercase().replace(" ", "_")),
                    latitude = center.latitude,
                    longitude = center.longitude,
                    address = selectedAddress
                )
            )
            findNavController().popBackStack()
        }
    }

    override fun onStart() { super.onStart(); _binding?.mapView?.onStart() }
    override fun onResume() { super.onResume(); _binding?.mapView?.onResume() }
    override fun onPause() { _binding?.mapView?.onPause(); super.onPause() }
    override fun onStop() { _binding?.mapView?.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); _binding?.mapView?.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }
}