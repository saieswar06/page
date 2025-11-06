package com.example.page.teacher

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
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.ActivityLogActivity
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
    private var currentPage = 1
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

        // Set header visibility and toolbar title
        if (showActive) {
            binding.activeListHeader.visibility = View.VISIBLE
            binding.inactiveListHeader.visibility = View.GONE
            binding.toolbarTitle.text = "Active ECCE Teachers"
        } else {
            binding.activeListHeader.visibility = View.GONE
            binding.inactiveListHeader.visibility = View.VISIBLE
            binding.toolbarTitle.text = "Inactive ECCE Teachers"
        }

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
            teachers = teacherList,
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
            onDeactivateClick = { teacher -> showDeactivateConfirmation(teacher) },
            onDeleteClick = { teacher -> showDeleteConfirmation(teacher) }, // wired delete
            onRestoreClick = { teacher -> showRestoreConfirmation(teacher) }
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
                teacherAdapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupSortSpinner()

        binding.btnRefresh.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.spinnerSort.setText("Latest", false)
            currentSort = Sort.LATEST
            loadTeachers(1)
        }

        binding.btnNext.setOnClickListener {
            loadTeachers(currentPage + 1)
        }

        binding.btnPrev.setOnClickListener {
            if (currentPage > 1) {
                loadTeachers(currentPage - 1)
            }
        }

        loadTeachers(1)
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

    private fun updateToolbarSubtitle(total: Int? = null) {
        val title = if (showActive) {
            "Active ECCE Teachers"
        } else {
            "Inactive ECCE Teachers"
        }
        binding.toolbarTitle.text = title
    }

    private fun filterAndSortTeachers(query: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            val lowerCaseQuery = query.lowercase(Locale.ROOT)
            val filtered = if (query.isEmpty()) {
                teacherList
            } else {
                teacherList.filter { teacher ->
                    teacher.name?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true ||
                            teacher.email?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true ||
                            teacher.phone?.contains(query) == true ||
                            teacher.centerName?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true
                }
            }.toMutableList()

            sortTeachers(filtered)

            withContext(Dispatchers.Main) {
                teacherAdapter.updateData(filtered)
            }
        }
    }

    private fun sortTeachers(list: MutableList<TeacherModel>) {
        when (currentSort) {
            Sort.LATEST -> list.sortByDescending { it.uid }
            Sort.OLDEST -> list.sortBy { it.uid }
            Sort.NAME_ASC -> list.sortBy { it.name?.lowercase(Locale.ROOT) }
            Sort.NAME_DESC -> list.sortByDescending { it.name?.lowercase(Locale.ROOT) }
        }
    }

    private fun loadTeachers(page: Int) {
        binding.progressBar.visibility = View.VISIBLE

        val status = if (showActive) listOf(1) else listOf(2)

        RetrofitClient.getInstance(this).getTeachers(page = page, limit = 10, status = status).enqueue(object : Callback<ApiResponse<List<TeacherModel>>> {
            override fun onResponse(call: Call<ApiResponse<List<TeacherModel>>>, response: Response<ApiResponse<List<TeacherModel>>>) {
                handleTeacherResponse(response, page)
            }

            override fun onFailure(call: Call<ApiResponse<List<TeacherModel>>>, t: Throwable) {
                handleTeacherFailure(t)
            }
        })
    }

    private fun handleTeacherResponse(response: Response<ApiResponse<List<TeacherModel>>>, page: Int) {
        binding.progressBar.visibility = View.GONE
        if (response.isSuccessful && response.body()?.success == true) {
            val fetchedTeachers = response.body()?.data ?: emptyList()
            updateToolbarSubtitle(fetchedTeachers.size)

            if (fetchedTeachers.isNotEmpty()) {
                if (page == 1) teacherList.clear()
                teacherList.addAll(fetchedTeachers)
                currentPage = page
                binding.tvPage.text = currentPage.toString()
                filterAndSortTeachers("")
            } else {
                if (page > 1) {
                    Toast.makeText(this, "No more teachers", Toast.LENGTH_SHORT).show()
                } else {
                    teacherList.clear()
                    teacherAdapter.updateData(emptyList())
                    Toast.makeText(this, if (showActive) "No active teachers found" else "No inactive teachers found", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            Toast.makeText(
                this,
                "Failed to load teachers: ${response.message()}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("TeachersAPI", "Failed response: ${response.code()} ${response.message()}")
        }
    }

    private fun handleTeacherFailure(t: Throwable) {
        binding.progressBar.visibility = View.GONE
        Log.e("TeachersAPI", "Error: ${t.message}", t)
        Toast.makeText(
            this,
            "Network error: ${t.message}",
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun showDeactivateConfirmation(teacher: TeacherModel) {
        val editText = EditText(this).apply {
            hint = "Enter reason for deactivation"
            setPadding(50, 40, 50, 40)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Deactivate Teacher")
            .setMessage("Please provide a reason for deactivating '${teacher.name}'")
            .setView(editText)
            .setPositiveButton("Deactivate", null)
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

                val uid = teacher.uid
                if (uid == null) {
                    Toast.makeText(this, "Invalid teacher id", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    btn.isEnabled = false
                    dialog.dismiss()
                    deactivateTeacher(uid, reason)
                }
            }
        }

        dialog.show()
    }

    private fun deactivateTeacher(teacherId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        val requestBody = mapOf("reason" to reason)

        RetrofitClient.getInstance(this).deactivateTeacher(teacherId, requestBody).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@TeacherDashboardActivity, "Teacher deactivated successfully", Toast.LENGTH_SHORT).show()
                    loadTeachers(1)
                } else {
                    Toast.makeText(this@TeacherDashboardActivity, "Failed to deactivate teacher", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TeacherDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun restoreTeacher(teacherId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        val requestBody = mapOf("reason" to reason)

        RetrofitClient.getInstance(this).restoreTeacher(teacherId, requestBody).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@TeacherDashboardActivity, "Teacher restored successfully", Toast.LENGTH_SHORT).show()
                    loadTeachers(1)
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

    private fun showRestoreConfirmation(teacher: TeacherModel) {
        val editText = EditText(this).apply {
            hint = "Enter reason for restoration"
            setPadding(50, 40, 50, 40)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Restore Teacher")
            .setMessage("Are you sure you want to restore '${teacher.name}'?")
            .setView(editText)
            .setPositiveButton("Restore", null)
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

                val uid = teacher.uid
                if (uid == null) {
                    Toast.makeText(this, "Invalid teacher id", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    btn.isEnabled = false
                    dialog.dismiss()
                    restoreTeacher(uid, reason)
                }
            }
        }

        dialog.show()
    }

    // --- NEW: delete confirmation (asks for reason) + API call + local update ---
    private fun showDeleteConfirmation(teacher: TeacherModel) {
        // Create input for the reason
        val editText = EditText(this).apply {
            hint = "Enter reason for deletion"
            setPadding(50, 40, 50, 40)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Teacher")
            .setMessage("Are you sure you want to permanently delete '${teacher.name}'? Please provide a reason.")
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

                val uid = teacher.uid
                if (uid == null) {
                    Toast.makeText(this, "Invalid teacher id", Toast.LENGTH_SHORT).show()
                    Log.e("DeleteTeacher", "Attempt to delete teacher with null id: $teacher")
                    dialog.dismiss()
                } else {
                    // disable button to avoid double taps
                    btn.isEnabled = false
                    dialog.dismiss()
                    deleteTeacher(uid, reason)
                }
            }
        }

        dialog.show()
    }

    private fun deleteTeacher(teacherId: Int, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        Log.d("DeleteTeacher", "Deleting teacher id=$teacherId with reason='$reason'")

        // Match your Retrofit signature: Map<String, String>
        val requestBody: Map<String, String> = mapOf("reason" to reason)

        RetrofitClient.getInstance(this).deleteTeacher(teacherId, requestBody)
            .enqueue(object : Callback<ApiResponse<Any>> {
                override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@TeacherDashboardActivity, "Teacher deleted", Toast.LENGTH_SHORT).show()

                        // remove from local list
                        val idx = teacherList.indexOfFirst { it.uid == teacherId }
                        if (idx != -1) {
                            teacherList.removeAt(idx)
                        }
                        // update adapter with the new list (keeps filter logic working)
                        teacherAdapter.updateData(teacherList)

                        // optionally refresh counts or reload page
                        // loadTeachers(1)
                    } else {
                        Toast.makeText(this@TeacherDashboardActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                        Log.e("DeleteTeacher", "Failed code=${response.code()} msg=${response.message()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@TeacherDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("DeleteTeacher", "Error deleting teacher", t)
                }
            })
    }
    // --- end delete flow ---

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
        loadTeachers(currentPage)
    }
}
