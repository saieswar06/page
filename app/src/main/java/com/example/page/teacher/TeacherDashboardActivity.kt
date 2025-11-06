package com.example.page.teacher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.R
import com.example.page.SupervisorLoginActivity
import com.example.page.api.DeactivateCenterRequest
import com.example.page.api.RetrofitClient
import com.example.page.api.TeacherModel
import com.example.page.databinding.ActivityTeacherDashboardBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TeacherDashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private lateinit var teacherAdapter: TeacherAdapter
    private val teacherList = mutableListOf<TeacherModel>()
    private var showActive = true

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

        binding.btnRefresh.setOnClickListener {
            binding.etSearch.text?.clear()
            loadTeachers()
        }

        loadTeachers()
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

            withContext(Dispatchers.Main) {
                teacherAdapter.updateData(filtered)
            }
        }
    }

    private fun loadTeachers() {
        binding.progressBar.visibility = View.VISIBLE

        val status = if (showActive) 1 else 2

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@TeacherDashboardActivity).getTeachers(status = status)
                handleTeacherResponse(response)
            } catch (t: Throwable) {
                handleTeacherFailure(t)
            }
        }
    }

    private fun handleTeacherResponse(response: retrofit2.Response<com.example.page.api.ApiResponse<List<TeacherModel>>>) {
        binding.progressBar.visibility = View.GONE
        if (response.isSuccessful && response.body()?.success == true) {
            val fetchedTeachers = response.body()?.data ?: emptyList()
            updateToolbarSubtitle(fetchedTeachers.size)

            teacherList.clear()
            teacherList.addAll(fetchedTeachers)
            filterAndSortTeachers("")

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

    private fun deactivateTeacher(teacherId: String, reason: String) {
        binding.progressBar.visibility = View.VISIBLE
        val request = DeactivateCenterRequest(reason = reason)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@TeacherDashboardActivity).deactivateTeacher(teacherId, request)
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@TeacherDashboardActivity, "Teacher deactivated successfully", Toast.LENGTH_SHORT).show()
                    loadTeachers()
                } else {
                    Toast.makeText(this@TeacherDashboardActivity, "Failed to deactivate teacher", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TeacherDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restoreTeacher(teacherId: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@TeacherDashboardActivity).restoreTeacher(teacherId)
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@TeacherDashboardActivity, "Teacher restored successfully", Toast.LENGTH_SHORT).show()
                    loadTeachers()
                } else {
                    Toast.makeText(this@TeacherDashboardActivity, "Failed to restore teacher", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TeacherDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        }
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
                    restoreTeacher(uid)
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
                    deleteTeacher(uid)
                }
            }
        }

        dialog.show()
    }

    private fun deleteTeacher(teacherId: String) {
        binding.progressBar.visibility = View.VISIBLE
        Log.d("DeleteTeacher", "Deleting teacher id=$teacherId")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@TeacherDashboardActivity).deleteTeacher(teacherId)
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
            } catch (t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TeacherDashboardActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("DeleteTeacher", "Error deleting teacher", t)
            }
        }
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
        loadTeachers()
    }
}
