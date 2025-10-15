package com.example.page.admin.centers

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.api.Center
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAnganwadiCentersBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnganwadiCentersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnganwadiCentersBinding
    private lateinit var adapter: CentersAdapter
    private val centersList = mutableListOf<Center>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnganwadiCentersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CentersAdapter(centersList)
        binding.recyclerCenters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCenters.adapter = adapter

        fetchCenters()

        binding.btnAddCenter.setOnClickListener {
            startActivity(Intent(this, AddCenterActivity::class.java))
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun fetchCenters() {
        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.instance.getCenters().enqueue(object : Callback<List<Center>> {
            override fun onResponse(call: Call<List<Center>>, response: Response<List<Center>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    centersList.clear()
                    centersList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@AnganwadiCentersActivity, "No centers found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Center>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AnganwadiCentersActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
