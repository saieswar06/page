package com.example.page.teacher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.R
import com.example.page.SupervisorLoginActivity
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import com.example.page.api.TeacherModel
import com.example.page.databinding.ActivityTeacherDashboardBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class TeacherDashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private lateinit var teacherAdapter: TeacherAdapter
    private val teacherList = mutableListOf<TeacherModel>()
    private var currentSort: Sort = Sort.LATEST
    private var showActive = true

    private enum class Sort {
        LATEST,
        OLDEST,
        NAME_ASC,
        NAME_DESC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showActive = intent.getBooleanExtra("show_active", true)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = prefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SupervisorLoginActivity::class.java))
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = if (showActive) "ECCE Teachers" else "Inactive ECCE Teachers"

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener(this)

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        binding.rvTeachers.layoutManager = LinearLayoutManager(this)
        teacherAdapter = TeacherAdapter(
            showActive = showActive,
            onViewClick = { teacher ->
                val intent = Intent(this, ViewTeacherActivity::class.java)
                intent.putExtra("teacher", teacher)
                startActivity(intent)
            },
            onEditClick = { teacher ->
                val intent = Intent(this, EditTeacherActivity::class.java)
                intent.putExtra("teacher", teacher)
                startActivity(intent)
            },
            onDeleteClick = { teacher -> showDeleteConfirmation(teacher) },
            onRestoreClick = { teacher -> restoreTeacher(teacher.uid!!) }
        )
        binding.rvTeachers.adapter = teacherAdapter

        if (!showActive) {
            binding.btnAddTeacher.visibility = View.GONE
        }

        binding.btnAddTeacher.setOnClickListener {
            startActivity(Intent(this, AddTeacherActivity::class.java))
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndSortTeachers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupSortSpinner()

        binding.btnRefresh.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.spinnerSort.setText("Latest", false)
            currentSort = Sort.LATEST
            loadAllTeachers()
        }

        // Hide pagination buttons since we're loading all teachers
        binding.btnNext.visibility = View.GONE
        binding.btnPrev.visibility = View.GONE
        binding.tvPage.visibility = View.GONE

        loadAllTeachers()
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Latest", "Oldest", "Name (A-Z)", "Name (Z-A)")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sortOptions)

        binding.spinnerSort.setAdapter(sortAdapter)
        binding.spinnerSort.setText(sortOptions[0], false)

        binding.spinnerSort.setOnItemClickListener { _, _, position, _ ->
            currentSort = when (position) {
                0 -> Sort.LATEST
                1 -> Sort.OLDEST
                2 -> Sort.NAME_ASC
                3 -> Sort.NAME_DESC
                else -> Sort.LATEST
            }
            filterAndSortTeachers(binding.etSearch.text.toString())
            Toast.makeText(this, "Sorted by ${sortOptions[position]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToolbarSubtitle() {
        val count = teacherList.size
        binding.toolbarTitle.text = if (showActive) {
            "ECCE Teachers ($count)"
        } else {
            "Inactive ECCE Teachers ($count)"
        }
    }

    private fun filterAndSortTeachers(query: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            val lowerCaseQuery = query.toLowerCase(Locale.ROOT)
            val filtered = if (query.isEmpty()) {
                teacherList
            } else {
                teacherList.filter { teacher ->
                    teacher.name?.toLowerCase(Locale.ROOT)?.contains(lowerCaseQuery) == true ||
                            teacher.email?.toLowerCase(Locale.ROOT)?.contains(lowerCaseQuery) == true ||
                            teacher.phone?.contains(query) == true ||
                            teacher.centerName?.toLowerCase(Locale.ROOT)?.contains(lowerCaseQuery) == true
                }
            }.toMutableList()

            sortTeachers(filtered)

            withContext(Dispatchers.Main) {
                teacherAdapter.submitList(filtered)
                // Update UI to show count
                binding.toolbar.subtitle = "${filtered.size} teacher(s)"
            }
        }
    }

    private fun sortTeachers(list: MutableList<TeacherModel>) {
        when (currentSort) {
            Sort.LATEST -> list.sortByDescending { it.uid }
            Sort.OLDEST -> list.sortBy { it.uid }
            Sort.NAME_ASC -> list.sortBy { it.name?.toLowerCase(Locale.ROOT) }
            Sort.NAME_DESC -> list.sortByDescending { it.name?.toLowerCase(Locale.ROOT) }
        }
    }

    private fun loadAllTeachers() {
        binding.progressBar.visibility = View.VISIBLE
        val status = if (showActive) 1 else 0

        RetrofitClient.getInstance(this)
            .getTeachers(page = null, limit = null, search = null, sort = null, status = status)
            .enqueue(object : Callback<ApiResponse<List<TeacherModel>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<TeacherModel>>>,
                    response: Response<ApiResponse<List<TeacherModel>>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        val fetchedTeachers = response.body()?.data ?: emptyList()

                        teacherList.clear()
                        teacherList.addAll(fetchedTeachers)

                        // Update toolbar subtitle with count
                        updateToolbarSubtitle()

                        if (teacherList.isEmpty()) {
                            Toast.makeText(
                                this@TeacherDashboardActivity,
                                if (showActive) "No active teachers found" else "No inactive teachers found",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@TeacherDashboardActivity,
                                "Loaded ${teacherList.size} teacher(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        filterAndSortTeachers(binding.etSearch.text.toString())
                    } else {
                        Toast.makeText(
                            this@TeacherDashboardActivity,
                            "Failed to load teachers: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("TeachersAPI", "Failed response: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<TeacherModel>>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Log.e("TeachersAPI", "Error: ${t.message}", t)
                    Toast.makeText(
                        this@TeacherDashboardActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun restoreTeacher(teacherId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this).restoreTeacher(teacherId).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@TeacherDashboardActivity, "Teacher restored successfully", Toast.LENGTH_SHORT).show()
                    loadAllTeachers()
                } else {
                    Toast.makeText(this@TeacherDashboardActivity, "Failed to restore teacher", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TeacherDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showDeleteConfirmation(teacher: TeacherModel) {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter reason for deletion"
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Teacher")
            .setMessage("Please provide a reason for deleting '${teacher.name}'")
            .setView(editText)
            .setPositiveButton("Next") { _, _ ->
                val reason = editText.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                } else {
                    showFinalDeleteConfirmation(teacher, reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation(teacher: TeacherModel, reason: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete '${teacher.name}'?\n\nReason: $reason")
            .setPositiveButton("Delete") { _, _ ->
                teacher.uid?.let { deleteTeacher(it, reason) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTeacher(teacherId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = mapOf("reason" to reason)

        RetrofitClient.getInstance(this)
            .deleteTeacher(teacherId, requestBody)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(
                    call: Call<ApiResponse<Any>>,
                    response: Response<ApiResponse<Any>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@TeacherDashboardActivity,
                            "Teacher deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadAllTeachers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to delete teacher"
                        Toast.makeText(
                            this@TeacherDashboardActivity,
                            errorMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@TeacherDashboardActivity,
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
                startActivity(Intent(this, SupervisorLoginActivity::class.java))
                finish()
            }
            else -> Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning to this activity
        loadAllTeachers()
    }
}