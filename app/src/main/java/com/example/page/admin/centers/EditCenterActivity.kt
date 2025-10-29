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
import com.example.page.R
import com.example.page.api.ApiResponse
import com.example.page.api.CenterResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityEditCenterBinding
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class EditCenterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityEditCenterBinding
    private var center: CenterResponse? = null
    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private lateinit var geocoder: Geocoder
    private lateinit var mandalData: Map<String, List<String>>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        center = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CENTER_DATA", CenterResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CENTER_DATA")
        }

        if (center == null) {
            Toast.makeText(this, "Failed to load center data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupDependentDropdowns()

        geocoder = Geocoder(this, Locale.getDefault())

        val mapFragment = SupportMapFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        prefillData()

        binding.btnUpdate.setOnClickListener { validateAndSave() }
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
        }
    }

    private fun updateMandalDropdown(district: String) {
        val mandals = mandalData[district] ?: emptyList()
        val mandalAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mandals)
        binding.etMandal.setAdapter(mandalAdapter)
        binding.etMandal.text.clear()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        center?.let {
            val lat = it.latitude?.toDoubleOrNull()
            val lng = it.longitude?.toDoubleOrNull()
            if (lat != null && lng != null) {
                val centerLocation = LatLng(lat, lng)
                marker = googleMap?.addMarker(MarkerOptions().position(centerLocation).title(it.center_name))
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLocation, 15f))
            }
        }

        googleMap?.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = googleMap?.addMarker(MarkerOptions().position(latLng).title("New Location"))
            binding.etLat.setText(latLng.latitude.toString())
            binding.etLng.setText(latLng.longitude.toString())

            getAddressFromLocation(latLng)
        }

        googleMap?.setOnMyLocationButtonClickListener {
            getCurrentLocationAndFillAddress()
            true // Consume the event
        }

        enableMyLocation()
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
                            Toast.makeText(this@EditCenterActivity, "Could not fetch address", Toast.LENGTH_SHORT).show()
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

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun processLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        marker?.remove()
        marker = googleMap?.addMarker(MarkerOptions().position(latLng).title("Current Location"))
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
                if (location != null) {
                    binding.progressBar.visibility = View.GONE
                    processLocation(location)
                } else {
                    // Fallback to last location if getCurrentLocation fails
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                        binding.progressBar.visibility = View.GONE
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
                Log.e("EditCenterActivity", "Failed to get current location", e)
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prefillData() {
        center?.let {
            binding.etCenterName.setText(it.center_name ?: "")
            binding.etDistrict.setText(it.district ?: "", false)
            updateMandalDropdown(it.district ?: "")
            binding.etMandal.setText(it.mandal ?: "", false)
            binding.etLocality.setText(it.locality ?: "")
            binding.etLat.setText(it.latitude ?: "")
            binding.etLng.setText(it.longitude ?: "")
        }
    }



    private fun validateAndSave() {
        val changes = mutableMapOf<String, Any>()

        val name = binding.etCenterName.text.toString().trim()
        if (name != center?.center_name) {
            changes["center_name"] = name
        }

        val district = binding.etDistrict.text.toString().trim()
        if (district != center?.district) {
            changes["district"] = district
        }

        val mandal = binding.etMandal.text.toString().trim()
        if (mandal != center?.mandal) {
            changes["mandal"] = mandal
        }

        val locality = binding.etLocality.text.toString().trim()
        if (locality != center?.locality) {
            changes["locality"] = locality
        }

        val lat = binding.etLat.text.toString().trim()
        if (lat != center?.latitude) {
            changes["latitude"] = lat.toDoubleOrNull()!!
        }
        val lng = binding.etLng.text.toString().trim()
        if (lng != center?.longitude) {
            changes["longitude"] = lng.toDoubleOrNull()!!
        }

        if (changes.isEmpty()) {
            Toast.makeText(this, "No changes were made", Toast.LENGTH_SHORT).show()
            return
        }

        // Add required fields for validation, even if not changed
        if (!changes.containsKey("center_name")) changes["center_name"] = name
        if (!changes.containsKey("district")) changes["district"] = district
        if (!changes.containsKey("mandal")) changes["mandal"] = mandal

        updateCenter(changes)
    }

    private fun updateCenter(changes: Map<String, Any>) {
        val centerId = center?.id ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdate.isEnabled = false

        RetrofitClient.getInstance(this)
            .updateCenter(centerId, changes)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    binding.btnUpdate.isEnabled = true
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@EditCenterActivity,
                            response.body()?.message ?: "Center updated successfully!",
                            Toast.LENGTH_LONG
                        ).show()
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
                        Toast.makeText(this@EditCenterActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    binding.btnUpdate.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@EditCenterActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
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
