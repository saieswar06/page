package com.example.page.admin.centers

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.api.AddCenterRequest
import com.example.page.api.Center
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAnganwadiCentersBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminCentersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnganwadiCentersBinding
    private lateinit var adapter: CentersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnganwadiCentersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Setup RecyclerView
        adapter = CentersAdapter(
            onEdit = { center -> openEditDialog(center) },
            onDelete = { center -> deleteCenter(center) }
        )

        binding.recyclerCenters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCenters.adapter = adapter

        // ✅ Buttons
        binding.btnRefresh.setOnClickListener { loadCenters() }
        binding.btnAddCenter.setOnClickListener {
            AddCenterDialog(this) { addCenter(it) }.show()
        }

        // ✅ Load centers initially
        loadCenters()
    }

    private fun loadCenters() {
        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this).getCenters()
            .enqueue(object : Callback<List<Center>> {
                override fun onResponse(call: Call<List<Center>>, response: Response<List<Center>>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        adapter.setData(response.body()!!)
                    } else {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Failed to load centers",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<Center>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@AdminCentersActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun addCenter(request: AddCenterRequest) {
        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this).addCenter(request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Center added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCenters()
                    } else {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Failed to add center",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@AdminCentersActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun openEditDialog(center: Center) {
        val request = AddCenterRequest(
            center_name = center.center_name,
            address = center.address ?: "",
            latitude = center.latitude,   // ✅ Correct type: Double?
            longitude = center.longitude  // ✅ Correct type: Double?
        )

        AddCenterDialog(this, request) { updated ->
            RetrofitClient.getInstance(this)
                .updateCenter(center.id ?: 0, updated)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@AdminCentersActivity,
                                "Center updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadCenters()
                        } else {
                            Toast.makeText(
                                this@AdminCentersActivity,
                                "Failed to update center",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Error: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }.show()
    }

    private fun deleteCenter(center: Center) {
        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this).deleteCenter(center.id ?: 0)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Center deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCenters()
                    } else {
                        Toast.makeText(
                            this@AdminCentersActivity,
                            "Failed to delete center",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@AdminCentersActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
