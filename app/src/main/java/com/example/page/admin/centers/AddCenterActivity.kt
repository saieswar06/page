package com.example.page.admin.centers

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.page.R
import com.example.page.api.AddCenterRequest
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import java.util.concurrent.Executors

class AddCenterActivity : AppCompatActivity(), OnMapReadyCallback {

    // --- Views ---
    private lateinit var etCenterName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etLocality: EditText
    private lateinit var etDistrict: EditText
    private lateinit var etState: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobile: EditText
    private lateinit var etPassword: EditText
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    // --- Map ---
    private var map: GoogleMap? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_center)

        // Initialize Views
        etCenterName = findViewById(R.id.etCenterName)
        etAddress = findViewById(R.id.etAddress)
        etLocality = findViewById(R.id.etLocality)
        etDistrict = findViewById(R.id.etDistrict)
        etState = findViewById(R.id.etState)
        etEmail = findViewById(R.id.etEmail)
        etMobile = findViewById(R.id.etMobile)
        etPassword = findViewById(R.id.etPassword)
        etLat = findViewById(R.id.etLat)
        etLng = findViewById(R.id.etLng)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnSave.setOnClickListener { validateAndSave() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val india = LatLng(20.5937, 78.9629)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 4f))

        map?.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = map?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            etLat.setText(latLng.latitude.toString())
            etLng.setText(latLng.longitude.toString())
            updateAddressFieldsFromCoordinates(latLng.latitude, latLng.longitude)
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map?.isMyLocationEnabled = true
            showCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100
            )
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun showCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                marker?.remove()
                marker = map?.addMarker(MarkerOptions().position(latLng).title("Your Location"))
                etLat.setText(it.latitude.toString())
                etLng.setText(it.longitude.toString())
                updateAddressFieldsFromCoordinates(it.latitude, it.longitude)
            }
        }
    }

    // --- Geocoder Logic ---

    private fun updateAddressFieldsFromCoordinates(latitude: Double, longitude: Double) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Geocoder service is not available.", Toast.LENGTH_LONG).show()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) {
                    setAddressFields(addresses[0])
                } else {
                    handleAddressNotFound()
                }
            }
        } else {
            Executors.newSingleThreadExecutor().execute {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    runOnUiThread {
                        if (addresses != null && addresses.isNotEmpty()) {
                            setAddressFields(addresses[0])
                        } else {
                            handleAddressNotFound()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { handleAddressError(e) }
                }
            }
        }
    }

    // =================== CORRECTED FUNCTION WITH DEFAULT STATE ===================
    /**
     * Helper function to populate UI fields from an Address object.
     * This version is robust and provides a default state if it cannot be found.
     */
    private fun setAddressFields(address: Address) {
        Log.d("Geocoder", "Full Address Object Received: $address")

        // Clear all fields first to prevent old data from sticking around.
        etLocality.text.clear()
        etDistrict.text.clear()
        etState.text.clear()

        // --- State ---
        // The state is almost always in 'adminArea'. If it's null, default to "Arunachal Pradesh".
        val state = address.adminArea
        etState.setText(state ?: "Arunachal Pradesh")
        if (state == null) {
            Log.w("Geocoder", "State (adminArea) was null. Defaulting to 'Arunachal Pradesh'.")
        } else {
            Log.d("Geocoder", "State found: '$state'")
        }

        // --- District ---
        val district = address.subAdminArea
        etDistrict.setText(district ?: "")
        Log.d("Geocoder", "District found: '$district'")

        // --- Locality / City ---
        val locality = address.locality
        etLocality.setText(locality ?: "")
        Log.d("Geocoder", "Locality found: '$locality'")

        // --- Fallback Logic ---
        // Fallback 1: If District is empty, try using Locality for it.
        if (etDistrict.text.isBlank() && etLocality.text.isNotBlank()) {
            // Only do this if locality is not the same as the state name.
            if (etLocality.text.toString() != etState.text.toString()) {
                etDistrict.setText(etLocality.text)
                etLocality.text.clear() // Clear locality since we "promoted" it
                Log.d("Geocoder", "Fallback: Used Locality ('${etDistrict.text}') for District.")
            }
        }

        // Fallback 2: If Locality is empty after adjustments, use the District name.
        if (etLocality.text.isBlank() && etDistrict.text.isNotBlank()) {
            etLocality.setText(etDistrict.text)
            Log.d("Geocoder", "Final Fallback: Copied District to empty Locality.")
        }
    }
    // =================================================================================================

    /** Helper function to handle cases where no address is found. */
    private fun handleAddressNotFound() {
        Log.w("Geocoder", "No address found for these coordinates.")
        Toast.makeText(this, "Could not find address details. Setting default state.", Toast.LENGTH_SHORT).show()
        etLocality.text.clear()
        etDistrict.text.clear()
        etState.setText("Arunachal Pradesh") // Set default state
    }

    /** Helper function to handle geocoder exceptions. */
    private fun handleAddressError(e: Exception) {
        Log.e("Geocoder", "Error getting address", e)
        Toast.makeText(this, "Error finding address. Setting default state.", Toast.LENGTH_LONG).show()
        etLocality.text.clear()
        etDistrict.text.clear()
        etState.setText("Arunachal Pradesh") // Set default state
    }

    // --- Form Validation and Save Logic ---
    private fun validateAndSave() {
        val name = etCenterName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val locality = etLocality.text.toString().trim()
        val district = etDistrict.text.toString().trim()
        val state = etState.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val latStr = etLat.text.toString().trim()
        val lngStr = etLng.text.toString().trim()

        if (name.isEmpty() || address.isEmpty() || locality.isEmpty() || district.isEmpty() || state.isEmpty()) {
            Toast.makeText(this, "Please ensure all address details are filled.", Toast.LENGTH_LONG).show()
            return
        }

        val latitude = latStr.toDoubleOrNull()
        val longitude = lngStr.toDoubleOrNull()

        val request = AddCenterRequest(
            center_name = name, address = address,
            email = email.ifEmpty { null }, mobile = mobile.ifEmpty { null }, password = password.ifEmpty { null },
            latitude = latitude, longitude = longitude,
            state = state, district = district, locality = locality,
            mandal = "NA", stateCode = 0, districtCode = 0, blockCode = 0, sectorCode = 0
        )
        addCenter(request)
    }

    private fun addCenter(request: AddCenterRequest) {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        RetrofitClient.getInstance(this).addCenter(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AddCenterActivity, "✅ Center added successfully!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("AddCenter", "❌ Error: $error")
                    Toast.makeText(this@AddCenterActivity, "❌ Failed to add center: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Log.e("AddCenter", "⚠️ Network Error", t)
                Toast.makeText(this@AddCenterActivity, "⚠️ Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // --- Permission Handling ---
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map?.isMyLocationEnabled = true
                showCurrentLocation()
            }
        } else {
            Toast.makeText(this, "Location permission is required to use the map feature.", Toast.LENGTH_SHORT).show()
        }
    }
}
