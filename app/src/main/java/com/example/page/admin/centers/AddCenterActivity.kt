package com.example.page.admin.centers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.page.R
import com.example.page.api.AddCenterRequest
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAddCenterBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AddCenterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddCenterBinding
    private var map: GoogleMap? = null
    private var marker: Marker? = null
    private lateinit var geocoder: Geocoder
    private lateinit var mandalData: Map<String, List<String>>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isGooglePlayServicesAvailable()) {
            Toast.makeText(this, "Google Play Services is required to use this feature.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding = ActivityAddCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDependentDropdowns()

        geocoder = Geocoder(this, Locale.getDefault())

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        if (mapFragment == null) {
            val newMapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, newMapFragment)
                .commit()
            newMapFragment.getMapAsync(this)
        } else {
            mapFragment.getMapAsync(this)
        }

        binding.btnAddCenter.setOnClickListener { validateAndSave() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupDependentDropdowns() {
        mandalData = getMandalData()
        val districts = mandalData.keys.toTypedArray()
        val districtAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, districts)
        binding.etDistrict.setAdapter(districtAdapter)

        binding.etDistrict.setOnItemClickListener { _, _, position, _ ->
            val selectedDistrict = districtAdapter.getItem(position).toString()
            updateMandalDropdown(selectedDistrict)
            zoomToDistrict(selectedDistrict)
        }
    }

    private fun zoomToDistrict(districtName: String) {
        if (map == null) return

        val searchString = "$districtName, Arunachal Pradesh"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(searchString, 1) { addresses ->
                runOnUiThread {
                    if (addresses.isNotEmpty()) {
                        val location = addresses[0]
                        val latLng = LatLng(location.latitude, location.longitude)
                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                    } else {
                        Log.w("AddCenterActivity", "Could not find location for district: $districtName")
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val addresses = geocoder.getFromLocationName(searchString, 1)
                    withContext(Dispatchers.Main) {
                        if (!addresses.isNullOrEmpty()) {
                            val location = addresses[0]
                            val latLng = LatLng(location.latitude, location.longitude)
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                        } else {
                            Log.w("AddCenterActivity", "Could not find location for district: $districtName")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("AddCenterActivity", "Error geocoding district: $districtName", e)
                    }
                }
            }
        }
    }


    private fun updateMandalDropdown(district: String) {
        val mandals = mandalData[district] ?: emptyList()
        val mandalAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mandals)
        binding.etMandal.setAdapter(mandalAdapter)
        binding.etMandal.text.clear()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isZoomControlsEnabled = true
        map?.uiSettings?.isMyLocationButtonEnabled = true

        val india = LatLng(20.5937, 78.9629)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 4f))

        map?.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = map?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            binding.etLat.setText(latLng.latitude.toString())
            binding.etLng.setText(latLng.longitude.toString())
            getAddressFromLocation(latLng)
        }

        map?.setOnMyLocationButtonClickListener {
            getCurrentLocationAndFillAddress()
            true // Consume the event
        }

        requestLocationPermission()
    }

    private fun getAddressFromLocation(latLng: LatLng) {
        binding.progressBar.visibility = View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    if (addresses.isNotEmpty()) {
                        fillAddressFields(addresses[0])
                    } else {
                        Toast.makeText(this, "Could not fetch address", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        if (!addresses.isNullOrEmpty()) {
                            fillAddressFields(addresses[0])
                        } else {
                            Toast.makeText(this@AddCenterActivity, "Could not fetch address", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Log.e("Geocoding", "Error getting address", e)
                    }
                }
            }
        }
    }

    private fun fillAddressFields(address: Address) {
        val districtName = address.subAdminArea ?: address.locality ?: ""
        if (mandalData.containsKey(districtName)) {
            binding.etDistrict.setText(districtName, false)
            updateMandalDropdown(districtName)
        }

        val mandalName = address.subLocality ?: ""
        binding.etMandal.setText(mandalName, false)

        val locality = address.featureName ?: address.thoroughfare ?: ""
        binding.etLocality.setText(locality)
    }

    private fun validateAndSave() {
        val name = binding.etCenterName.text.toString().trim()
        val district = binding.etDistrict.text.toString().trim()
        val mandal = binding.etMandal.text.toString().trim()
        val locality = binding.etLocality.text.toString().trim()
        val latStr = binding.etLat.text.toString().trim()
        val lngStr = binding.etLng.text.toString().trim()

        if (name.isEmpty() || district.isEmpty() || mandal.isEmpty() || locality.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_LONG).show()
            return
        }

        if (latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_LONG).show()
            return
        }

        val request = AddCenterRequest(
            center_name = name,
            latitude = latStr.toDoubleOrNull(),
            longitude = lngStr.toDoubleOrNull(),
            state = "Arunachal Pradesh",
            district = district,
            locality = locality,
            mandal = mandal
        )

        addCenter(request)
    }

    private fun addCenter(request: AddCenterRequest) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnAddCenter.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@AddCenterActivity).addCenter(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AddCenterActivity, "Center added successfully!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val gson = Gson()
                            val errorResponse = gson.fromJson(errorBody, ApiResponse::class.java)
                            errorResponse.error ?: errorResponse.message ?: "An unknown error occurred"
                        } catch (e: Exception) {
                            response.message()
                        }
                    } else {
                        response.body()?.error ?: response.body()?.message ?: "An unknown error occurred"
                    }
                    Toast.makeText(this@AddCenterActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddCenterActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnAddCenter.isEnabled = true
            }
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(status)) {
                availability.getErrorDialog(this, status, 9001)?.show()
            }
            return false
        }
        return true
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            enableMyLocation()
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun enableMyLocation() {
        map?.isMyLocationEnabled = true
    }

    private fun processLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        marker?.remove()
        marker = map?.addMarker(MarkerOptions().position(latLng).title("Current Location"))
        binding.etLat.setText(location.latitude.toString())
        binding.etLng.setText(location.longitude.toString())
        getAddressFromLocation(latLng)
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun getCurrentLocationAndFillAddress() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Please enable location services in your device settings.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.progressBar.visibility = View.VISIBLE

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                binding.progressBar.visibility = View.GONE
                if (location != null) {
                    processLocation(location)
                } else {
                    // Fallback to last location if getCurrentLocation fails
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                        if (lastLocation != null) {
                            processLocation(lastLocation)
                        } else {
                            Toast.makeText(this, "Unable to fetch location. Please ensure you have a clear GPS signal.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("AddCenterActivity", "Failed to get current location", e)
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }

    private fun getMandalData(): Map<String, List<String>> {
        return mapOf(
            "Tawang" to listOf("Tawang", "Lumla", "Zemithang"),
            "West Kameng" to listOf("Bomdila", "Dirang", "Kalaktang"),
            "East Kameng" to listOf("Seppa", "Chayangtajo", "Bameng"),
            "Papum Pare" to listOf("Itanagar", "Naharlagun", "Banderdewa", "Doimukh", "Balijan", "Taraso", "Sangdupota"),
            "Lower Subansiri" to listOf("Ziro", "Yachuli", "Pistana"),
            "Upper Subansiri" to listOf("Daporijo", "Dumporijo", "Taliha"),
            "West Siang" to listOf("Aalo", "Likabali", "Basar"),
            "East Siang" to listOf("Pasighat", "Mebo", "Ruksin"),
            "Upper Siang" to listOf("Yingkiong", "Tuting", "Mariyang"),
            "Lower Dibang Valley" to listOf("Roing", "Dambuk", "Hunli"),
            "Dibang Valley" to listOf("Anini", "Etalin", "Malinye"),
            "Anjaw" to listOf("Hawai", "Hayuliang", "Manchal"),
            "Lohit" to listOf("Tezu", "Namsai", "Lekang"),
            "Namsai" to listOf("Namsai", "Chowkham", "Piyong"),
            "Changlang" to listOf("Changlang", "Miao", "Jairampur"),
            "Tirap" to listOf("Khonsa", "Deomali", "Laju"),
            "Longding" to listOf("Longding", "Kanubari", "Pumao")
        )
    }
}
