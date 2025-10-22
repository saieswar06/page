package com.example.page.admin.centers

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
//import android.util.Logimport
import android.view.View

import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.page.R
import com.example.page.api.AddCenterRequest
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
// ✅ **FIX: Import GoogleApiAvailability**
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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

    // --- Views and Map Objects ---
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
    private var map: GoogleMap? = null
    private var marker: Marker? = null

    // Constant for Google Play Services error dialog
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ **FIX: Check for Google Play Services availability BEFORE setting the content view.**
        if (!isGooglePlayServicesAvailable()) {
            // If services are not available, do not load the layout with the map.
            // The isGooglePlayServicesAvailable() method will show an error dialog.
            return
        }

        // Only set the content view if services are available.
        setContentView(R.layout.activity_add_center)

        initializeViews()

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnSave.setOnClickListener { validateAndSave() }
    }

    /**
     * Checks if Google Play Services are available on the device.
     * If they are not, it shows a user-fixable error dialog.
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                // Show the error dialog to the user.
                googleApiAvailability.getErrorDialog(this, status, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            } else {
                // If the error is not resolvable, show a simple toast and finish.
                Toast.makeText(this, "This device does not support Google Play Services, which are required for maps.", Toast.LENGTH_LONG).show()
                finish()
            }
            return false
        }
        return true
    }

    private fun initializeViews() {
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
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.uiSettings?.isZoomControlsEnabled = true
        val india = LatLng(20.5937, 78.9629)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 4f))

        map?.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = map?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            etLat.setText(latLng.latitude.toString())
            etLng.setText(latLng.longitude.toString())
            updateAddressFieldsFromCoordinates(latLng.latitude, latLng.longitude)
        }

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            showCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION])
    private fun showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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
            handleAddressNotFound()
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) setAddressFields(addresses[0]) else handleAddressNotFound()
            }
        } else {
            Executors.newSingleThreadExecutor().execute {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    runOnUiThread {
                        if (addresses != null && addresses.isNotEmpty()) setAddressFields(addresses[0]) else handleAddressNotFound()
                    }
                } catch (e: Exception) {
                    runOnUiThread { handleAddressError(e) }
                }
            }
        }
    }

    private fun setAddressFields(address: Address) {
        Log.d("Geocoder", "Full Address Object Received: $address")

        etLocality.text.clear()
        etDistrict.text.clear()
        etState.text.clear()

        val state = address.adminArea
        etState.setText(state ?: "Arunachal Pradesh")
        if (state == null) Log.w("Geocoder", "State (adminArea) was null. Defaulting to 'Arunachal Pradesh'.")

        val district = address.subAdminArea
        val locality = address.locality

        etDistrict.setText(district ?: "")
        etLocality.setText(locality ?: "")

        if (etDistrict.text.isBlank() && etLocality.text.isNotBlank()) {
            if (etLocality.text.toString() != etState.text.toString()) {
                etDistrict.setText(etLocality.text)
                etLocality.text.clear()
            }
        }
        if (etLocality.text.isBlank() && etDistrict.text.isNotBlank()) {
            etLocality.setText(etDistrict.text)
        }
    }

    private fun handleAddressNotFound() {
        Log.w("Geocoder", "No address found for these coordinates.")
        Toast.makeText(this, "Could not find address details. Setting default state.", Toast.LENGTH_SHORT).show()
        etLocality.text.clear()
        etDistrict.text.clear()
        etState.setText("Arunachal Pradesh")
    }

    private fun handleAddressError(e: Exception) {
        Log.e("Geocoder", "Error getting address", e)
        Toast.makeText(this, "Error finding address. Setting default state.", Toast.LENGTH_LONG).show()
        etLocality.text.clear()
        etDistrict.text.clear()
        etState.setText("Arunachal Pradesh")
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

        val request = AddCenterRequest(
            center_name = name, address = address,
            email = email.ifEmpty { null }, mobile = mobile.ifEmpty { null }, password = password.ifEmpty { null },
            latitude = latStr.toDoubleOrNull(), longitude = lngStr.toDoubleOrNull(),
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
                    Log.e("AddCenter", "❌ Error: ${response.errorBody()?.string()}")
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
        } else {
            Toast.makeText(this, "Location permission is required to use the map feature.", Toast.LENGTH_SHORT).show()
        }
    }
}
