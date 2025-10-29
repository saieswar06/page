package com.example.page.admin.centers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.R
import com.example.page.SupervisorLoginActivity
import com.example.page.api.*
import com.example.page.databinding.ActivityAnganwadiCentersBinding
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class AdminCentersActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAnganwadiCentersBinding
    private lateinit var centersAdapter: CentersAdapter
    private val centersList = mutableListOf<CenterResponse>()
    private var currentPage = 1
    private var currentSort: Sort = Sort.LATEST
    private var showActive = true

    private enum class Sort {
        LATEST,
        NAME_ASC,
        NAME_DESC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnganwadiCentersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showActive = intent.getBooleanExtra("show_active", true)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        if (prefs.getString("token", null).isNullOrEmpty()) {
            handleSessionExpired()
            return
        }

        setupUI()

        // Ensure adapter + layout manager are set before loading data
        if (binding.recyclerCenters.layoutManager == null) {
            binding.recyclerCenters.layoutManager = LinearLayoutManager(this)
            Log.d("AdminCenters", "LayoutManager was null; set LinearLayoutManager")
        }
        if (binding.recyclerCenters.adapter == null) {
            binding.recyclerCenters.adapter = centersAdapter
            Log.d("AdminCenters", "Adapter was null; set centersAdapter")
        }

        // Clear any search filter so initial load is not filtered
        binding.etSearch.setText("")

        loadCenters(currentPage)
    }

    override fun onResume() {
        super.onResume()
        // Refresh current page
        loadCenters(currentPage)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = if (showActive) "Anganwadi Centers" else "Inactive Anganwadi Centers"

        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.navView.setNavigationItemSelectedListener(this)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

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
            onDeleteClick = { center -> showDeleteConfirmation(center) },
            onRestoreClick = { center -> center.id?.let { restoreCenter(it) } }
        )

        binding.recyclerCenters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCenters.adapter = centersAdapter

        setupSortSpinner()

        if (!showActive) {
            binding.btnAddCenter.visibility = View.GONE
        }

        binding.btnAddCenter.setOnClickListener { startActivity(Intent(this, AddCenterActivity::class.java)) }
        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            loadCenters(currentPage)
        }
        binding.btnNext.setOnClickListener {
            currentPage++
            loadCenters(currentPage)
        }
        binding.btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadCenters(currentPage)
            }
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                centersAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Sort by Latest", "Sort by Name (A-Z)", "Sort by Name (Z-A)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortOptions)

        binding.spinnerSort.setAdapter(adapter)
        binding.spinnerSort.setText(sortOptions[0], false)

        binding.spinnerSort.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    currentSort = Sort.LATEST
                    sortCenters()
                    centersAdapter.updateData(centersList)
                    Toast.makeText(this, "Sorted by Latest", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    currentSort = Sort.NAME_ASC
                    sortCenters()
                    centersAdapter.updateData(centersList)
                    Toast.makeText(this, "Sorted by Name (A-Z)", Toast.LENGTH_SHORT).show()
                }
                2 -> {
                    currentSort = Sort.NAME_DESC
                    sortCenters()
                    centersAdapter.updateData(centersList)
                    Toast.makeText(this, "Sorted by Name (Z-A)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCenters(page: Int) {
        binding.progressBar.visibility = View.VISIBLE
        val status = if (showActive) 1 else 0

        // defensive: make sure adapter & layout manager exist
        if (binding.recyclerCenters.layoutManager == null) {
            binding.recyclerCenters.layoutManager = LinearLayoutManager(this)
        }
        if (binding.recyclerCenters.adapter == null) {
            binding.recyclerCenters.adapter = centersAdapter
        }

        RetrofitClient.getInstance(this).getCenters(page, status).enqueue(object : Callback<ApiResponse<List<CenterResponse>>> {
            override fun onResponse(call: Call<ApiResponse<List<CenterResponse>>>, response: Response<ApiResponse<List<CenterResponse>>>) {
                binding.progressBar.visibility = View.GONE

                Log.d("AdminCenters", "Raw response body = ${response.body()}")
                if (response.isSuccessful && response.body()?.success == true) {
                    val fetchedCenters = response.body()?.data ?: emptyList()
                    Log.d("AdminCenters", "Fetched centers size = ${fetchedCenters.size}")

                    if (fetchedCenters.isNotEmpty()) {
                        centersList.clear()
                        centersList.addAll(fetchedCenters)
                        sortCenters()

                        try {
                            centersAdapter.updateData(centersList)
                        } catch (e: Exception) {
                            Log.e("AdminCenters", "Exception while updating adapter", e)
                            Toast.makeText(this@AdminCentersActivity, "Adapter update failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                        // debug UI state on next frame
                        binding.recyclerCenters.post {
                            Log.d("AdminCenters", "centersList size = ${centersList.size}")
                            Log.d("AdminCenters", "adapter itemCount AFTER post = ${centersAdapter.itemCount}")
                            binding.recyclerCenters.visibility = View.VISIBLE
                            binding.recyclerCenters.setBackgroundColor(android.graphics.Color.argb(30, 100, 200, 255))
                        }

                        binding.tvPage.text = currentPage.toString()
                    } else {
                        Toast.makeText(this@AdminCentersActivity, "No more centers", Toast.LENGTH_SHORT).show()
                        if (currentPage > 1) currentPage--
                        // Also clear adapter if result empty
                        centersList.clear()
                        centersAdapter.updateData(centersList)
                    }
                } else {
                    Log.e("AdminCenters", "Failed to load centers: code=${response.code()} msg=${response.message()}")
                    Toast.makeText(this@AdminCentersActivity, "Failed to load centers.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CenterResponse>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.e("AdminCenters", "Network error: ${t.message}", t)
                Toast.makeText(this@AdminCentersActivity, "Network error, please try again.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun restoreCenter(centerId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this).restoreCenter(centerId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminCentersActivity, "Center restored successfully", Toast.LENGTH_SHORT).show()
                    loadCenters(currentPage)
                } else {
                    Toast.makeText(this@AdminCentersActivity, "Failed to restore center", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AdminCentersActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun sortCenters() {
        when (currentSort) {
            Sort.LATEST -> centersList.sortByDescending { it.id }
            Sort.NAME_ASC -> centersList.sortBy { it.center_name?.lowercase(Locale.ROOT) }
            Sort.NAME_DESC -> centersList.sortByDescending { it.center_name?.lowercase(Locale.ROOT) }
        }
    }

    private fun showDeleteConfirmation(center: CenterResponse) {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter reason for deletion"
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Center")
            .setMessage("Please provide a reason for deleting '${center.center_name}'")
            .setView(editText)
            .setPositiveButton("Next") { _, _ ->
                val reason = editText.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                } else {
                    showFinalDeleteConfirmation(center, reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation(center: CenterResponse, reason: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete '${center.center_name}'?\n\nReason: $reason")
            .setPositiveButton("Delete") { _, _ ->
                center.id?.let { deleteCenter(it, reason) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCenter(centerId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = mapOf("reason" to reason)

        RetrofitClient.getInstance(this)
            .deleteCenter(centerId, requestBody)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(
                    call: Call<ApiResponse<Any>>,
                    response: Response<ApiResponse<Any>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Center deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCenters(currentPage)
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to delete center"
                        Toast.makeText(
                            this@AdminCentersActivity,
                            errorMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@AdminCentersActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

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
        startActivity(Intent(this, SupervisorLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
