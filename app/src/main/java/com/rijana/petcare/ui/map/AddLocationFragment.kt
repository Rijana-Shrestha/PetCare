package com.rijana.petcare.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
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
import com.rijana.petcare.data.network.NominatimResult
import com.rijana.petcare.data.repository.SavedPlaceRepository
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentAddLocationBinding
import com.rijana.petcare.util.applyImeBottomPadding
import com.rijana.petcare.viewmodel.PlaceViewModel
import com.rijana.petcare.viewmodel.PlaceViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /**
     * Full address returned by reverse geocoding.
     * This is saved into SavedPlace.address.
     */
    private var selectedAddress: String? = null

    /**
     * Used to cancel an older reverse-geocoding request
     * when the user moves the map again.
     */
    private var reverseGeocodeJob: Job? = null

    /**
     * Used to identify the latest map position.
     *
     * If request A starts and then the user moves the map,
     * request B starts. If A finishes after B, we don't want
     * A to overwrite the newer result.
     */
    private var reverseRequestId = 0L

    /**
     * True when the user manually changes the Location Name.
     *
     * If the user types their own name such as:
     * "Buddy's Vet"
     *
     * we should not overwrite it when the map moves.
     */
    private var locationNameManuallyEdited = false

    private val userRepository by lazy {
        val app = requireActivity().application as PetCareApplication
        UserRepository(
            AuthManager(),
            app.database.userDao()
        )
    }

    private val placeViewModel: PlaceViewModel by viewModels {
        val app = requireActivity().application as PetCareApplication

        PlaceViewModelFactory(
            SavedPlaceRepository(app.database.savedPlaceDao()),
            userRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        MapLibre.getInstance(requireContext())

        _binding = FragmentAddLocationBinding.inflate(
            inflater,
            container,
            false
        )

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.setOnTouchListener { view, _ ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applyImeBottomPadding()

        setupBackButton()
        setupTypeDropdown()
        setupLocationNameField()
        setupSearch()

        setupMap()

        binding.btnSaveLocation.setOnClickListener {
            saveLocation()
        }
    }

    // ---------------------------------------------------------
    // BACK BUTTON
    // ---------------------------------------------------------

    private fun setupBackButton() {

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // ---------------------------------------------------------
    // LOCATION NAME FIELD
    // ---------------------------------------------------------

    private fun setupLocationNameField() {

        binding.etLocationName.doAfterTextChanged {

            /*
             * We only consider it manually edited if the user
             * actually changes the automatically generated value.
             */
            locationNameManuallyEdited = true
        }
    }

    // ---------------------------------------------------------
    // TYPE DROPDOWN
    // ---------------------------------------------------------

    private fun setupTypeDropdown() {

        val options = PlaceType.entries.map {

            it.name
                .lowercase()
                .replace("_", " ")
                .replaceFirstChar(Char::uppercase)
        }

        binding.actvType.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options
            )
        )

        binding.actvType.setOnClickListener {
            binding.actvType.showDropDown()
        }
    }

    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    private fun setupSearch() {

        binding.etSearch.setOnEditorActionListener {
                _,
                actionId,
                _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                performSearch(
                    binding.etSearch.text.toString().trim()
                )

                true

            } else {

                false
            }
        }
    }

    private fun performSearch(query: String) {

        if (query.isBlank()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val results = NominatimClient.api.search(
                    query = query
                )

                val firstResult = results.firstOrNull()

                if (firstResult != null) {

                    /*
                     * Search result moves the map.
                     *
                     * The camera-idle listener will then perform
                     * a fresh reverse geocoding request for the
                     * new center position.
                     */
                    moveCamera(
                        latitude = firstResult.lat.toDouble(),
                        longitude = firstResult.lon.toDouble()
                    )

                } else {

                    Toast.makeText(
                        requireContext(),
                        "No results found",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Search failed - check your connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------------------------------------------------------
    // MAP
    // ---------------------------------------------------------

    private fun setupMap() {

        binding.mapView.getMapAsync { map ->

            maplibreMap = map

            val styleUrl =
                "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

            map.setStyle(styleUrl) {

                /*
                 * Initial position.
                 */
                map.cameraPosition =
                    CameraPosition.Builder()
                        .target(
                            LatLng(
                                27.7172,
                                85.3240
                            )
                        )
                        .zoom(15.0)
                        .build()

                /*
                 * Get the address/name for the initial
                 * map position.
                 */
                scheduleReverseGeocode()

                /*
                 * IMPORTANT:
                 *
                 * This fires after the user finishes moving
                 * the map.
                 *
                 * Therefore the reverse geocoding uses the
                 * final center position rather than an old one.
                 */
                map.addOnCameraIdleListener {

                    scheduleReverseGeocode()
                }
            }
        }
    }

    private fun moveCamera(
        latitude: Double,
        longitude: Double
    ) {

        if (!::maplibreMap.isInitialized) {
            return
        }

        maplibreMap.cameraPosition =
            CameraPosition.Builder()
                .target(
                    LatLng(
                        latitude,
                        longitude
                    )
                )
                .zoom(17.0)
                .build()
    }

    // ---------------------------------------------------------
    // REVERSE GEOCODING
    // ---------------------------------------------------------

    private fun scheduleReverseGeocode() {

        if (!::maplibreMap.isInitialized) {
            return
        }

        /*
         * Cancel any previous pending request.
         *
         * This prevents several requests from being launched
         * while the user is moving the map.
         */
        reverseGeocodeJob?.cancel()

        reverseGeocodeJob =
            viewLifecycleOwner.lifecycleScope.launch {

                /*
                 * Small delay after camera movement.
                 *
                 * This gives MapLibre time to settle at the
                 * final position.
                 */
                delay(600)

                reverseGeocodeCurrentCenter()
            }
    }

    private fun reverseGeocodeCurrentCenter() {

        if (!::maplibreMap.isInitialized) {
            return
        }

        val center =
            maplibreMap.cameraPosition.target
                ?: return

        /*
         * Every request receives a new ID.
         */
        reverseRequestId++

        val currentRequestId =
            reverseRequestId

        reverseGeocodeJob =
            viewLifecycleOwner.lifecycleScope.launch {

                try {

                    val result =
                        NominatimClient.api.reverse(
                            lat = center.latitude,
                            lon = center.longitude
                        )

                    /*
                     * If another request was started after this
                     * request, this result is old and must not
                     * update the UI.
                     */
                    if (currentRequestId != reverseRequestId) {
                        return@launch
                    }

                    /*
                     * Store the full address.
                     */
                    selectedAddress =
                        result.display_name

                    /*
                     * Update the address shown below the map.
                     */
                    binding.tvSelectedAddress.text =
                        "Selected: ${result.display_name}"

                    /*
                     * Only automatically update the Location Name
                     * if the user hasn't manually entered one.
                     */
                    if (!locationNameManuallyEdited) {

                        val placeName =
                            getBestPlaceName(result)

                        if (placeName.isNotBlank()) {

                            binding.etLocationName.setText(
                                placeName
                            )

                            /*
                             * setText() triggers TextWatcher.
                             *
                             * Reset the flag because this was
                             * our automatic change, not the user.
                             */
                            locationNameManuallyEdited = false
                        }
                    }

                } catch (e: Exception) {

                    /*
                     * Only display the fallback if this is still
                     * the latest request.
                     */
                    if (currentRequestId != reverseRequestId) {
                        return@launch
                    }

                    selectedAddress =
                        null

                    binding.tvSelectedAddress.text =
                        "Selected: ${center.latitude}, ${center.longitude}"
                }
            }
    }

    // ---------------------------------------------------------
    // FIND BEST LOCATION NAME
    // ---------------------------------------------------------

    private fun getBestPlaceName(
        result: NominatimResult
    ): String {

        /*
         * 1. Primary name returned by Nominatim.
         *
         * Example:
         * "Narayanhiti Palace Museum"
         */
        result.name
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let {
                return it
            }

        /*
         * 2. Try namedetails.
         *
         * Nominatim can return names such as:
         *
         * name
         * name:en
         * name:ne
         * official_name
         * brand
         */
        result.namedetails?.let { names ->

            val possibleNames =
                listOf(
                    names["name:en"],
                    names["name"],
                    names["official_name:en"],
                    names["official_name"],
                    names["brand"]
                )

            possibleNames
                .mapNotNull { it?.trim() }
                .firstOrNull { it.isNotBlank() }
                ?.let {
                    return it
                }
        }

        /*
         * 3. If there is no POI/place name, use a useful
         * address component.
         */
        result.address?.let { address ->

            val possibleAddressNames =
                listOf(
                    address["road"],
                    address["neighbourhood"],
                    address["suburb"],
                    address["village"],
                    address["town"],
                    address["city"]
                )

            possibleAddressNames
                .mapNotNull { it?.trim() }
                .firstOrNull { it.isNotBlank() }
                ?.let {
                    return it
                }
        }

        /*
         * 4. Last fallback:
         * take the first section of display_name.
         */
        return result.display_name
            .split(",")
            .firstOrNull()
            ?.trim()
            .orEmpty()
    }

    // ---------------------------------------------------------
    // SAVE LOCATION
    // ---------------------------------------------------------

    private fun saveLocation() {

        val name =
            binding.etLocationName.text
                .toString()
                .trim()

        val typeText =
            binding.actvType.text
                .toString()
                .trim()

        if (name.isEmpty() || typeText.isEmpty()) {

            Toast.makeText(
                requireContext(),
                R.string.please_enter_a_name,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (!::maplibreMap.isInitialized) {
            return
        }

        val center =
            maplibreMap.cameraPosition.target
                ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            val firebaseUid =
                userRepository.currentFirebaseUid

            val ownerIdResolved =
                firebaseUid?.let { uid ->

                    userRepository
                        .getUserProfile(uid)
                        .first()

                }?.id

            if (ownerIdResolved == null) {

                Toast.makeText(
                    requireContext(),
                    "Couldn't identify your account",
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            val placeType =
                try {

                    PlaceType.valueOf(
                        typeText
                            .uppercase()
                            .replace(" ", "_")
                    )

                } catch (e: IllegalArgumentException) {

                    Toast.makeText(
                        requireContext(),
                        "Please select a valid place type",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

            placeViewModel.addPlace(

                SavedPlace(

                    ownerId = ownerIdResolved,

                    name = name,

                    type = placeType,

                    latitude = center.latitude,

                    longitude = center.longitude,

                    address = selectedAddress
                )
            )

            Toast.makeText(
                requireContext(),
                "Location saved",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().popBackStack()
        }
    }

    // ---------------------------------------------------------
    // MAP LIFECYCLE
    // ---------------------------------------------------------

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

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        super.onSaveInstanceState(outState)

        _binding
            ?.mapView
            ?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {

        reverseGeocodeJob?.cancel()

        _binding?.mapView?.onDestroy()

        super.onDestroyView()

        _binding = null
    }
}