package com.rijana.petcare.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.rijana.petcare.BuildConfig
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.local.entity.SavedPlace
import com.rijana.petcare.data.repository.SavedPlaceRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentMapBinding
import com.rijana.petcare.viewmodel.PlaceViewModel
import com.rijana.petcare.viewmodel.PlaceViewModelFactory
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.BitmapUtils
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var maplibreMap: MapLibreMap
    private var currentPlaces: List<SavedPlace> = emptyList()

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(AuthManager(), app.database.userDao())
    }

    private val placeViewModel: PlaceViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication
        PlaceViewModelFactory(SavedPlaceRepository(app.database.savedPlaceDao()), userRepository)
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) centerOnCurrentLocation()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        MapLibre.getInstance(requireContext())
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddLocation.setOnClickListener {
            findNavController().navigate(R.id.action_map_to_addLocation)
        }
        binding.ivViewAllPlaces.setOnClickListener {
            findNavController().navigate(R.id.action_map_to_savedPlaces)
        }

        binding.mapView.getMapAsync { map ->
            maplibreMap = map
            val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
            map.setStyle(styleUrl) { style ->
                addMarkerImage(style)
                setupMapClickListener(map)
                requestLocationOrFallback()
                observePlaces(style)
            }
        }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<DoubleArray>(SavedPlacesListFragment.FOCUS_LAT_LNG_KEY)
            ?.observe(viewLifecycleOwner) { latLng ->
                moveCamera(latLng[0], latLng[1])
            }
    }

    private fun addMarkerImage(style: Style) {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin) ?: return
        style.addImage(MARKER_ICON_ID, BitmapUtils.getBitmapFromDrawable(drawable)!!)
    }

    private fun observePlaces(style: Style) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                placeViewModel.places.collect { places ->
                    currentPlaces = places
                    updateMapSource(style, places)
                }
            }
        }
    }

    private fun updateMapSource(style: Style, places: List<SavedPlace>) {
        val features = places.map { place ->
            Feature.fromGeometry(Point.fromLngLat(place.longitude, place.latitude)).apply {
                addNumberProperty("placeId", place.id)
            }
        }
        val collection = FeatureCollection.fromFeatures(features)

        val existingSource = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
        if (existingSource != null) {
            existingSource.setGeoJson(collection)
        } else {
            style.addSource(GeoJsonSource(SOURCE_ID, collection))
            style.addLayer(
                SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.iconImage(MARKER_ICON_ID),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconAnchor(org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM)
                )
            )
        }
    }

    private fun setupMapClickListener(map: MapLibreMap) {
        map.addOnMapClickListener { latLng ->
            val screenPoint = map.projection.toScreenLocation(latLng)
            val tapped = map.queryRenderedFeatures(screenPoint, LAYER_ID).firstOrNull()
            val placeId = tapped?.getNumberProperty("placeId")?.toLong()
            if (placeId != null) {
                showPlaceDetail(placeId)
                true
            } else {
                binding.cardPlaceDetail.visibility = View.GONE
                false
            }
        }
    }

    private fun showPlaceDetail(placeId: Long) {
        val place = currentPlaces.find { it.id == placeId } ?: return
        binding.tvPlaceName.text = place.name
        binding.tvPlaceType.text = place.type.name.lowercase().replace("_", " ")
            .replaceFirstChar(Char::uppercase)
        binding.tvPlaceAddress.text = place.address ?: getString(R.string.no_address_saved)
        binding.cardPlaceDetail.visibility = View.VISIBLE
    }

    private fun requestLocationOrFallback() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            centerOnCurrentLocation()
        } else {
            moveCamera(DEFAULT_LAT, DEFAULT_LNG)
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Suppress("MissingPermission")
    private fun centerOnCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    moveCamera(location.latitude, location.longitude)
                } else {
                    moveCamera(DEFAULT_LAT, DEFAULT_LNG)
                }
            }
    }

    private fun moveCamera(lat: Double, lng: Double) {
        if (!::maplibreMap.isInitialized) return
        maplibreMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(13.0)
            .build()
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        _binding?.mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        _binding?.mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MARKER_ICON_ID = "marker-icon"
        private const val SOURCE_ID = "places-source"
        private const val LAYER_ID = "places-layer"
        private const val DEFAULT_LAT = 27.7172   // Kathmandu - sensible fallback if location is denied/unavailable
        private const val DEFAULT_LNG = 85.3240
    }
}