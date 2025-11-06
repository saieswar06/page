package com.example.page.admin.centers

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.R
import com.example.page.SupervisorLoginActivity
import com.example.page.api.CenterResponse
import com.example.page.api.DeactivateCenterRequest
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAnganwadiCentersBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminCentersActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAnganwadiCentersBinding
    private lateinit var centersAdapter: CentersAdapter
    private val centersList = mutableListOf<CenterResponse>()
    private var showActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnganwadiCentersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showActive = intent.getBooleanExtra("show_active", true)
        Log.d("AdminCentersActivity", "onCreate - showActive: $showActive")

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        if (prefs.getString("token", null).isNullOrEmpty()) {
            handleSessionExpired()
            return
        }

        setupUI()
        initializeAdapter()
        lifecycleScope.launch(Dispatchers.IO) {
            loadCenters()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            loadCenters()
        }
    }

    // -----------------------------------------------------------------------------------
    // UI SETUP AND INITIALIZATION
    // -----------------------------------------------------------------------------------

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.navView.setNavigationItemSelectedListener(this)

        binding.recyclerCenters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCenters.setHasFixedSize(true)

        binding.btnRefresh.setOnClickListener {
            Log.d("AdminCentersActivity", "Refresh button clicked, reloading")
            lifecycleScope.launch(Dispatchers.IO) {
                loadCenters()
            }
        }
        binding.btnAddCenter.setOnClickListener {
            startActivity(Intent(this, AddCenterActivity::class.java))
        }

        // Update UI based on active/inactive mode
        if (!showActive) {
            binding.btnAddCenter.visibility = View.GONE
            binding.tvNoOfTeachersHeader.text = "Reason"
            binding.toolbarTitle.text = "Deactivated Centers"
        } else {
            binding.btnAddCenter.visibility = View.VISIBLE
            binding.tvNoOfTeachersHeader.text = "No of Teachers"
            binding.toolbarTitle.text = "Active Centers"
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                centersAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initializeAdapter() {
        centersAdapter = CentersAdapter(
            centers = centersList,
            showActive = showActive,
            onViewClick = { center ->
                val intent = Intent(this, CenterDetailsActivity::class.java)
                intent.putExtra("CENTER_ID", center.id)
                startActivity(intent)
            },
            onEditClick = { center ->
                val intent = Intent(this, EditCenterActivity::class.java)
                intent.putExtra("CENTER_DATA", center)
                startActivity(intent)
            },
            onDeactivateClick = { center -> showDeactivateDialog(center) },
            onDeleteClick = { center -> showDeleteDialog(center) },
            onRestoreClick = { center -> showRestoreDialog(center) }
        )
        binding.recyclerCenters.adapter = centersAdapter
        Log.d("AdminCentersActivity", "Adapter initialized with showActive: $showActive")
    }

    // -----------------------------------------------------------------------------------
    // API CALLS AND DATA LOADING
    // -----------------------------------------------------------------------------------

    private suspend fun loadCenters() {
        if (!isNetworkAvailable()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AdminCentersActivity, "No internet connection.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val status = if (showActive) 1 else 2

        Log.d("AdminCentersActivity", "=== LOADING CENTERS ===")
        Log.d("AdminCentersActivity", "Status: $status (${if (showActive) "ACTIVE" else "DEACTIVATED"})")

        withContext(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
        }

        try {
            val response = RetrofitClient.getInstance(this@AdminCentersActivity).getCenters(status)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                Log.d("AdminCentersActivity", "Response Code: ${response.code()}, Success: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val fetchedCenters = body.data ?: emptyList()
                        Log.d("AdminCentersActivity", "Number of centers fetched: ${fetchedCenters.size}")

                        centersAdapter.updateData(fetchedCenters)

                        if (fetchedCenters.isEmpty()) {
                            val message = if (showActive) "No active centers found" else "No deactivated centers found"
                            Toast.makeText(this@AdminCentersActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("AdminCentersActivity", "API returned success=false. Message: ${body?.message}")
                        Toast.makeText(this@AdminCentersActivity, body?.message ?: "Failed to load centers", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleApiError(response.code())
                }
            }
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                Log.e("AdminCentersActivity", "=== NETWORK ERROR ===", t)
                handleNetworkError(t)
            }
        }
    }

    private fun showDeactivateDialog(center: CenterResponse) {
        val editText = EditText(this).apply {
            hint = "Enter reason for deactivation"
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Deactivate Center")
            .setMessage("Please provide a reason for deactivating '${center.center_name}'")
            .setView(editText)
            .setPositiveButton("Deactivate") { _, _ ->
                val reason = editText.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                } else {
                    center.id?.let { deactivateCenter(it, reason) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateCenter(centerId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = DeactivateCenterRequest(reason = reason)
                val response = RetrofitClient.getInstance(this@AdminCentersActivity).deactivateCenter(centerId.toString(), request)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminCentersActivity, "Center deactivated successfully", Toast.LENGTH_SHORT).show()
                        loadCenters()
                    } else {
                        handleApiError(response.code())
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    handleNetworkError(t)
                }
            }
        }
    }

    private fun showDeleteDialog(center: CenterResponse) {
        val editText = EditText(this).apply {
            hint = "Enter reason for deletion"
            setPadding(50, 40, 50, 40)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Center")
            .setMessage("Are you sure you want to permanently delete '${center.center_name}'? Please provide a reason.")
            .setView(editText)
            .setPositiveButton("Delete", null) // set later to control validation
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val reason = editText.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val id = center.id
                if (id == null) {
                    Toast.makeText(this, "Invalid center id", Toast.LENGTH_SHORT).show()
                    Log.e("DeleteCenter", "Attempt to delete center with null id: $center")
                    dialog.dismiss()
                } else {
                    // disable button to avoid double taps
                    btn.isEnabled = false
                    dialog.dismiss()
                    deleteCenter(id)
                }
            }
        }

        dialog.show()
    }

    private fun deleteCenter(centerId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getInstance(this@AdminCentersActivity).deleteCenter(centerId.toString())
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminCentersActivity, "Center deleted successfully", Toast.LENGTH_SHORT).show()
                        loadCenters()
                    } else {
                        handleApiError(response.code())
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    handleNetworkError(t)
                }
            }
        }
    }

    private fun showRestoreDialog(center: CenterResponse) {
        val reasonInput = EditText(this)
        reasonInput.hint = "Enter reason for restoration (from deactivation)"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Restore Center")
            .setMessage("Provide a reason for restoring ${center.center_name}:")
            .setView(reasonInput)
            .setPositiveButton("Restore", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val reason = reasonInput.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val id = center.id
                if (id == null) {
                    Toast.makeText(this, "Invalid center id", Toast.LENGTH_SHORT).show()
                    Log.e("AdminCentersActivity", "Attempt to restore center with null id: $center")
                    dialog.dismiss()
                } else {
                    btn.isEnabled = false
                    dialog.dismiss()
                    restoreCenter(id)
                }
            }
        }

        dialog.show()
    }

    private fun restoreCenter(centerId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getInstance(this@AdminCentersActivity).restoreCenter(centerId.toString())
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminCentersActivity, "Center restored successfully", Toast.LENGTH_SHORT).show()
                        loadCenters()
                    } else {
                        handleApiError(response.code())
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    handleNetworkError(t)
                }
            }
        }
    }

    private fun handleApiError(errorCode: Int) {
        val message = when (errorCode) {
            401 -> {
                handleSessionExpired()
                "Unauthorized. Please login again."
            }
            403 -> "Access forbidden"
            404 -> "Centers not found"
            500 -> "Server error. Please try again later."
            else -> "Failed to load data (Error $errorCode)"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleNetworkError(t: Throwable) {
        val message = "Network error: ${t.message ?: "Unknown error"}"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // -----------------------------------------------------------------------------------
    // NAVIGATION
    // -----------------------------------------------------------------------------------

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()
                RetrofitClient.clearInstance()
                handleSessionExpired()
            }
            else -> Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleSessionExpired() {
        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, SupervisorLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.isConnected ?: false
        }
    }
}
