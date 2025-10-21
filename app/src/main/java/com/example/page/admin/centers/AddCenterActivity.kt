package com.example.page.admin.centers

import android.Manifest
import android.content.pm.PackageManager
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

class AddCenterActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var etCenterName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobile: EditText
    private lateinit var etPassword: EditText
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    private var token: String? = null
    private var map: GoogleMap? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_center)

        // ✅ Initialize Views
        etCenterName = findViewById(R.id.etCenterName)
        etAddress = findViewById(R.id.etAddress)
        etEmail = findViewById(R.id.etEmail)
        etMobile = findViewById(R.id.etMobile)
        etPassword = findViewById(R.id.etPassword)
        etLat = findViewById(R.id.etLat)
        etLng = findViewById(R.id.etLng)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        // ✅ Load JWT token
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        token = prefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Not authorized. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ✅ Initialize Map
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnSave.setOnClickListener { validateAndSave() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val india = LatLng(20.5937, 78.9629)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 4f))

        // ✅ Tap-to-select marker
        map?.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = map?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            etLat.setText(latLng.latitude.toString())
            etLng.setText(latLng.longitude.toString())
        }

        // ✅ Current location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map?.isMyLocationEnabled = true
            showCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
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
            }
        }
    }

    private fun validateAndSave() {
        val name = etCenterName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val latStr = etLat.text.toString().trim()
        val lngStr = etLng.text.toString().trim()

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val latitude = latStr.toDoubleOrNull()
        val longitude = lngStr.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddCenterRequest(
            center_name = name,
            address = address,
            email = email.ifEmpty { null },
            mobile = mobile.ifEmpty { null },
            password = password.ifEmpty { null },
            latitude = latitude,
            longitude = longitude,
            state = "Andhra Pradesh",
            district = "Krishna",
            locality = "Vijayawada",
            mandal = "Vijayawada Urban",
            stateCode = 23,
            districtCode = 1,
            blockCode = 1,
            sectorCode = 1
        )

        addCenter(request)
    }

    private fun addCenter(request: AddCenterRequest) {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        RetrofitClient.getInstance(this)
            .addCenter("Bearer $token", request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@AddCenterActivity,
                            "✅ Center added successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        val error = response.errorBody()?.string()
                        Log.e("AddCenter", "❌ Error: $error")
                        Toast.makeText(
                            this@AddCenterActivity,
                            "❌ Failed (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Log.e("AddCenter", "⚠️ Network Error", t)
                    Toast.makeText(
                        this@AddCenterActivity,
                        "⚠️ Network error: ${t.message}",
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
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            showCurrentLocation()
        }
    }
}
