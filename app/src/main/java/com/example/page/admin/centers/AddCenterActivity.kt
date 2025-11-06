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
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AddCenterActivity : AppCompatActivity(), OnMapReadyCallback, OnMapsSdkInitializedCallback {

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
        binding = ActivityAddCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDependentDropdowns()

        geocoder = Geocoder(this, Locale.getDefault())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this@AddCenterActivity)
            } catch (e: Exception) {
                Log.e("AddCenterActivity", "Failed to initialize maps", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddCenterActivity, "Failed to initialize maps", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAddCenter.setOnClickListener { validateAndSave() }
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (isGooglePlayServicesAvailable()) {
                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
                if (mapFragment == null) {
                    val newMapFragment = SupportMapFragment.newInstance()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mapContainer, newMapFragment)
                        .commit()
                    newMapFragment.getMapAsync(this@AddCenterActivity)
                } else {
                    mapFragment.getMapAsync(this@AddCenterActivity)
                }
            } else {
                Toast.makeText(
                    this@AddCenterActivity,
                    "Google Play Services is required to use this feature.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
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
            lifecycleScope.launch(Dispatchers.IO) {
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

        lifecycleScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        if (addresses.isNotEmpty()) {
                            fillAddressFields(addresses[0])
                        } else {
                            Toast.makeText(this@AddCenterActivity, "Could not fetch address", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
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

        val pincode = address.postalCode ?: ""
        binding.etPincode.setText(pincode)
    }

    private fun validateAndSave() {
        val name = binding.etCenterName.text.toString().trim()
        val district = binding.etDistrict.text.toString().trim()
        val mandal = binding.etMandal.text.toString().trim()
        val locality = binding.etLocality.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()
        val stateCode = binding.etStateCode.text.toString().trim()
        val districtCode = binding.etDistrictCode.text.toString().trim()
        val projectCode = binding.etProjectCode.text.toString().trim()
        val sectorCode = binding.etSectorCode.text.toString().trim()
        val latStr = binding.etLat.text.toString().trim()
        val lngStr = binding.etLng.text.toString().trim()

        if (name.isEmpty() || district.isEmpty() || mandal.isEmpty() || locality.isEmpty() || pincode.isEmpty() || stateCode.isEmpty() || districtCode.isEmpty() || projectCode.isEmpty() || sectorCode.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_LONG).show()
            return
        }

        if (pincode.length != 6) {
            binding.etPincode.error = "Pincode must be 6 digits"
            return
        }

        if (stateCode.length != 2) {
            binding.etStateCode.error = "State code must be 2 digits"
            return
        }

        if (districtCode.length != 3) {
            binding.etDistrictCode.error = "District code must be 3 digits"
            return
        }

        if (projectCode.length != 2) {
            binding.etProjectCode.error = "Project code must be 2 digits"
            return
        }

        if (sectorCode.length != 2) {
            binding.etSectorCode.error = "Sector code must be 2 digits"
            return
        }

        if (latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_LONG).show()
            return
        }

        val latitude = latStr.toDoubleOrNull()
        val longitude = lngStr.toDoubleOrNull()

        if (latitude == null || longitude == null) {
            Toast.makeText(this, "Invalid latitude or longitude", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddCenterRequest(
            center_name = name,
            latitude = latitude,
            longitude = longitude,
            state = "Arunachal Pradesh",
            district = district,
            locality = locality,
            mandal = mandal,
            pincode = pincode,
            stateCode = stateCode,
            districtCode = districtCode,
            projectCode = projectCode,
            sectorCode = sectorCode
        )

        addCenter(request)
    }

    private fun addCenter(request: AddCenterRequest) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnAddCenter.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getInstance(this@AddCenterActivity).addCenter(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            Toast.makeText(this@AddCenterActivity, "Center added successfully!", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val errorMessage = apiResponse?.message ?: apiResponse?.error ?: "An unknown error occurred"
                            Toast.makeText(this@AddCenterActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
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
                            response.message() ?: "An unknown error occurred"
                        }
                        Toast.makeText(this@AddCenterActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddCenterActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAddCenter.isEnabled = true
                }
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
