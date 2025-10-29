package com.example.page.admin.centers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.ActivityLogActivity
import com.example.page.R
import com.example.page.SupervisorLoginActivity
import com.example.page.api.ApiResponse
import com.example.page.api.CenterResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAnganwadiCentersBinding
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminCentersActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAnganwadiCentersBinding
    private lateinit var centersAdapter: CentersAdapter
    private val centersList = mutableListOf<CenterResponse>()
    private var currentPage = 1
    private var totalPages = 1
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
        loadCenters(currentPage)
    }

    override fun onResume() {
        super.onResume()
        Log.d("AdminCentersActivity", "onResume - Refreshing data")
        loadCenters(currentPage)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.navView.setNavigationItemSelectedListener(this)

        binding.recyclerCenters.layoutManager = LinearLayoutManager(this)

        binding.btnRefresh.setOnClickListener {
            Log.d("AdminCentersActivity", "Refresh button clicked")
            loadCenters(1)
        }
        binding.btnAddCenter.setOnClickListener { startActivity(Intent(this, AddCenterActivity::class.java)) }

        // Update UI based on active/inactive mode
        if (!showActive) {
            binding.btnAddCenter.visibility = View.GONE
            binding.tvNoOfTeachersHeader.text = "Reason"
            supportActionBar?.title = "Deactivated Centers"
        } else {
            binding.btnAddCenter.visibility = View.VISIBLE
            binding.tvNoOfTeachersHeader.text = "No of Teachers"
            supportActionBar?.title = "Active Centers"
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                centersAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup AutoCompleteTextView
        val sortOptions = arrayOf("Sort by Latest", "Sort by Name (A-Z)", "Sort by Name (Z-A)")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortOptions)
        binding.spinnerSort.setAdapter(sortAdapter)

        binding.spinnerSort.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> Log.d("AdminCentersActivity", "Sort by Latest selected")
                1 -> Log.d("AdminCentersActivity", "Sort by Name (A-Z) selected")
                2 -> Log.d("AdminCentersActivity", "Sort by Name (Z-A) selected")
            }
        }

        binding.btnPrev.setOnClickListener {
            if (currentPage > 1) {
                Log.d("AdminCentersActivity", "Previous page clicked: $currentPage -> ${currentPage - 1}")
                loadCenters(currentPage - 1)
            }
        }
        binding.btnNext.setOnClickListener {
            if (currentPage < totalPages) {
                Log.d("AdminCentersActivity", "Next page clicked: $currentPage -> ${currentPage + 1}")
                loadCenters(currentPage + 1)
            }
        }
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
            // changed: show a reason dialog before restoring
            onRestoreClick = { center -> showRestoreDialog(center) },
            onHistoryClick = { center ->
                val intent = Intent(this, ActivityLogActivity::class.java)
                intent.putExtra("center_id", center.id)
                startActivity(intent)
            }
        )
        binding.recyclerCenters.adapter = centersAdapter
        Log.d("AdminCentersActivity", "Adapter initialized with showActive: $showActive")
    }

    private fun showDeactivateDialog(center: CenterResponse) {
        val reasonInput = EditText(this)
        reasonInput.hint = "Enter reason for deactivation"

        AlertDialog.Builder(this)
            .setTitle("Deactivate Center")
            .setMessage("Are you sure you want to deactivate ${center.center_name}?")
            .setView(reasonInput)
            .setPositiveButton("Yes") { _, _ ->
                val reason = reasonInput.text.toString()
                if (reason.isNotBlank()) {
                    center.id?.let { deactivateCenter(it, reason) }
                } else {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deactivateCenter(centerId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        val body = mapOf("reason" to reason)
        RetrofitClient.getInstance(this).deactivateCenter(centerId, body).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminCentersActivity, "Center deactivated successfully", Toast.LENGTH_SHORT).show()
                    loadCenters(currentPage)
                } else {
                    handleApiError(response.code())
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                handleNetworkError(t)
            }
        })
    }

    private fun showDeleteDialog(center: CenterResponse) {
        val reasonInput = EditText(this)
        reasonInput.hint = "Enter reason for deletion"
        AlertDialog.Builder(this)
            .setTitle("Delete Center")
            .setMessage("Are you sure you want to delete ${center.center_name}?")
            .setView(reasonInput)
            .setPositiveButton("Yes") { _, _ ->
                val reason = reasonInput.text.toString()
                if (reason.isNotBlank()) {
                    center.id?.let { deleteCenter(it, reason) }
                } else {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteCenter(centerId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        val body = mapOf("reason" to reason)
        RetrofitClient.getInstance(this).deleteCenter(centerId, body).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminCentersActivity, "Center deleted successfully", Toast.LENGTH_SHORT).show()
                    loadCenters(currentPage)
                } else {
                    handleApiError(response.code())
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                handleNetworkError(t)
            }
        })
    }

    /**
     * Show a dialog to collect the reason for restoring a center (i.e. why it was deactivated / why we are restoring)
     * and then call restoreCenter(centerId, reason).
     */
    private fun showRestoreDialog(center: CenterResponse) {
        val reasonInput = EditText(this)
        reasonInput.hint = "Enter reason for restoration (from deactivation)"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Restore Center")
            .setMessage("Provide a reason for restoring ${center.center_name}:")
            .setView(reasonInput)
            .setPositiveButton("Restore", null) // we'll override to validate
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
                    restoreCenter(id, reason)
                }
            }
        }

        dialog.show()
    }

    private fun restoreCenter(centerId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        val body = mapOf("reason" to reason)
        RetrofitClient.getInstance(this).restoreCenter(centerId, body).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminCentersActivity, "Center restored successfully", Toast.LENGTH_SHORT).show()
                    loadCenters(currentPage)
                } else {
                    handleApiError(response.code())
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                handleNetworkError(t)
            }
        })
    }

    private fun loadCenters(page: Int) {
        // ✅ FIX: Use status=2 for deactivated centers, status=1 for active
        // Status values: 1 = Active, 2 = Deactivated, 0 = Deleted
        val status = if (showActive) 1 else 2

        Log.d("AdminCentersActivity", "=== LOADING CENTERS ===")
        Log.d("AdminCentersActivity", "Page: $page")
        Log.d("AdminCentersActivity", "Status: $status (${if (showActive) "ACTIVE" else "DEACTIVATED"})")
        Log.d("AdminCentersActivity", "showActive: $showActive")

        binding.progressBar.visibility = View.VISIBLE

        val call = RetrofitClient.getInstance(this).getCenters(page, status)
        Log.d("AdminCentersActivity", "API Call URL: ${call.request().url}")

        call.enqueue(object : Callback<ApiResponse<List<CenterResponse>>> {
            override fun onResponse(call: Call<ApiResponse<List<CenterResponse>>>, response: Response<ApiResponse<List<CenterResponse>>>) {
                binding.progressBar.visibility = View.GONE
                Log.d("AdminCentersActivity", "=== API RESPONSE ===")
                Log.d("AdminCentersActivity", "Response Code: ${response.code()}")
                Log.d("AdminCentersActivity", "Response Success: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("AdminCentersActivity", "Response Body Success: ${body?.success}")
                    Log.d("AdminCentersActivity", "Response Body Message: ${body?.message}")
                    Log.d("AdminCentersActivity", "Response Body Total: ${body?.total}")

                    if (body?.success == true) {
                        val fetchedCenters = body.data ?: emptyList()
                        Log.d("AdminCentersActivity", "Number of centers fetched: ${fetchedCenters.size}")

                        // Log first few centers for debugging
                        fetchedCenters.take(3).forEachIndexed { index, center ->
                            Log.d("AdminCentersActivity", "Center $index: id=${center.id}, name=${center.center_name}, status=${center.status}, reason=${center.reason}")
                        }

                        centersAdapter.updateData(fetchedCenters)

                        val totalItems = body.total ?: fetchedCenters.size
                        totalPages = if (totalItems > 0) (totalItems + 9) / 10 else 1

                        binding.tvPage.text = page.toString()
                        binding.btnPrev.isEnabled = page > 1
                        binding.btnNext.isEnabled = page < totalPages
                        currentPage = page

                        // Show message if no centers found
                        if (fetchedCenters.isEmpty()) {
                            val message = if (showActive) "No active centers found" else "No deactivated centers found"
                            Toast.makeText(this@AdminCentersActivity, message, Toast.LENGTH_SHORT).show()
                            Log.w("AdminCentersActivity", message)
                        } else {
                            Log.d("AdminCentersActivity", "Successfully loaded ${fetchedCenters.size} centers")
                        }
                    } else {
                        Log.e("AdminCentersActivity", "API returned success=false")
                        Toast.makeText(this@AdminCentersActivity, body?.message ?: "Failed to load centers", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("AdminCentersActivity", "API Error: ${response.code()}, ${response.message()}")
                    try {
                        Log.e("AdminCentersActivity", "Error Body: ${response.errorBody()?.string()}")
                    } catch (e: Exception) {
                        Log.e("AdminCentersActivity", "Could not read error body", e)
                    }
                    handleApiError(response.code())
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CenterResponse>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.e("AdminCentersActivity", "=== NETWORK ERROR ===")
                Log.e("AdminCentersActivity", "Error: ${t.message}", t)
                handleNetworkError(t)
            }
        })
    }

    private fun handleApiError(errorCode: Int) {
        val message = when (errorCode) {
            401 -> "Unauthorized. Please login again."
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_logout -> {
                getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()
                RetrofitClient.clearInstance()
                handleSessionExpired()
            }
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
}
