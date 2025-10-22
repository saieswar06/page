package com.example.page

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.page.databinding.ActivityDashboardBinding // Assumes you are using ViewBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDashboardBinding
    private var map: GoogleMap? = null

    // Constant for Google Play Services error dialog
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ **This check for Google Play Services is crucial and correct.**
        if (!isGooglePlayServicesAvailable()) {
            // If services are not available, we stop loading to prevent a crash.
            return
        }

        // Only set the content view if services are available.
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the map fragment
        // =========================== THE FIX IS HERE ===========================
        // The <caret> marker was removed from R.id.map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment // Make sure the ID in your XML is 'map'
        // ========================================================================
        mapFragment.getMapAsync(this)

        // You can add listeners for your other dashboard buttons here
        // For example:
        // binding.profileButton.setOnClickListener { /* open profile */ }
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
                Toast.makeText(this, "This device requires Google Play Services for maps, which are not supported.", Toast.LENGTH_LONG).show()
                finish()
            }
            return false
        }
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Example: Move camera to a default location
        val defaultLocation = com.google.android.gms.maps.model.LatLng(20.5937, 78.9629) // India
        map?.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation))

        // Request location permission to show user's location
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            // You can add logic here to zoom to the user's current location if needed
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was granted, now enable the location layer.
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map?.isMyLocationEnabled = true
            }
        } else {
            Toast.makeText(this, "Location permission is needed to show your position on the map.", Toast.LENGTH_SHORT).show()
        }
    }
}
