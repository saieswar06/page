package com.example.page.admin.centers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.page.R
import com.example.page.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminCentersActivity : AppCompatActivity() {

    private lateinit var recyclerCenters: RecyclerView
    private lateinit var btnAddCenter: Button
    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var centersAdapter: CentersAdapter
    private val centersList = mutableListOf<CenterResponse>()
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anganwadi_centers)

        recyclerCenters = findViewById(R.id.recyclerCenters)
        btnAddCenter = findViewById(R.id.btnAddCenter)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressBar = findViewById(R.id.progressBar)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        token = prefs.getString("token", null)

        setupRecyclerView()

        btnAddCenter.setOnClickListener {
            startActivity(Intent(this, AddCenterActivity::class.java))
        }

        btnRefresh.setOnClickListener {
            loadCenters()
        }

        loadCenters()
    }

    private fun setupRecyclerView() {
        centersAdapter = CentersAdapter(
            centers = centersList,
            onEditClick = { center ->
                Toast.makeText(this, "Edit: ${center.center_name}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { center ->
                showDeleteConfirmation(center)
            }
        )
        recyclerCenters.layoutManager = LinearLayoutManager(this)
        recyclerCenters.adapter = centersAdapter
    }

    private fun loadCenters() {
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Not authorized. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this)
            .getCenters("Bearer $token")
            .enqueue(object : Callback<CentersResponse> {
                override fun onResponse(call: Call<CentersResponse>, response: Response<CentersResponse>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        centersList.clear()
                        centersList.addAll(response.body()?.data ?: emptyList())
                        centersAdapter.notifyDataSetChanged()
                        Toast.makeText(this@AdminCentersActivity, "Loaded centers", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AdminCentersActivity, "Failed to load centers", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<CentersResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@AdminCentersActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun showDeleteConfirmation(center: CenterResponse) {
        AlertDialog.Builder(this)
            .setTitle("Delete Center")
            .setMessage("Delete ${center.center_name}?")
            .setPositiveButton("Delete") { _, _ -> deleteCenter(center.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCenter(centerId: Int) {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this)
            .deleteCenter("Bearer $token", centerId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AdminCentersActivity, "Center deleted", Toast.LENGTH_SHORT).show()
                        loadCenters()
                    } else {
                        Toast.makeText(this@AdminCentersActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@AdminCentersActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
