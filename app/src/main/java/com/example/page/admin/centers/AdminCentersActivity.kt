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
import com.example.page.api.ApiResponse
import com.example.page.api.CenterResponse
import com.example.page.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminCentersActivity : AppCompatActivity() {

    private lateinit var btnAddCenter: Button
    private lateinit var btnRefresh: Button
    private lateinit var recyclerCenters: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var centersAdapter: CentersAdapter
    private val centersList = mutableListOf<CenterResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anganwadi_centers)  // ✅ Fixed!

        // ✅ Initialize views
        btnAddCenter = findViewById(R.id.btnAddCenter)
        btnRefresh = findViewById(R.id.btnRefresh)
        recyclerCenters = findViewById(R.id.recyclerCenters)
        progressBar = findViewById(R.id.progressBar)

        // ✅ Setup RecyclerView
        setupRecyclerView()

        // ✅ Add Center Button
        btnAddCenter.setOnClickListener {
            startActivity(Intent(this, AddCenterActivity::class.java))
        }

        // ✅ Refresh Button
        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing centers...", Toast.LENGTH_SHORT).show()
            loadCenters()
        }

        // ✅ Load centers
        loadCenters()
    }

    override fun onResume() {
        super.onResume()
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
        progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this).getCenters()
            .enqueue(object : Callback<List<CenterResponse>> {
                override fun onResponse(
                    call: Call<List<CenterResponse>>,
                    response: Response<List<CenterResponse>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val centers = response.body() ?: emptyList()
                        Log.d("AdminCenters", "✅ Loaded ${centers.size} centers")

                        centersList.clear()
                        centersList.addAll(centers)
                        centersAdapter.notifyDataSetChanged()

                        if (centers.isEmpty()) {
                            Toast.makeText(
                                this@AdminCentersActivity,
                                "No centers found. Click 'Add Center'!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@AdminCentersActivity,
                                "✅ Loaded ${centers.size} centers",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e("AdminCenters", "❌ Error: ${response.code()}")
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Failed to load centers",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<CenterResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("AdminCenters", "⚠️ Error", t)
                    Toast.makeText(
                        this@AdminCentersActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showDeleteConfirmation(center: CenterResponse) {
        AlertDialog.Builder(this)
            .setTitle("Delete Center")
            .setMessage("Delete '${center.center_name}'?")
            .setPositiveButton("Delete") { _, _ -> deleteCenter(center.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCenter(centerId: Int) {
        progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this).deleteCenter(centerId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "✅ Center deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCenters()
                    } else {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "❌ Delete failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@AdminCentersActivity,
                        "⚠️ Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}