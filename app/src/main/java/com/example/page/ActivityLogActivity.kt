package com.example.page

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.api.ActivityLog
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityLogBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivityLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private var centerId: Int = -1
    private var teacherId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        centerId = intent.getIntExtra("center_id", -1)
        teacherId = intent.getIntExtra("teacher_id", -1)

        binding.rvActivityLog.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        loadActivityLog()
    }

    private fun loadActivityLog() {
        if (centerId != -1) {
            fetchCenterActivityLog(centerId)
        } else if (teacherId != -1) {
            fetchTeacherActivityLog(teacherId)
        } else {
            fetchAllActivityLogs()
        }
    }

    private fun fetchAllActivityLogs() {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this).getAllActivityLogs().enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
            override fun onResponse(call: Call<ApiResponse<List<ActivityLog>>>, response: Response<ApiResponse<List<ActivityLog>>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val activityLogs = response.body()?.data ?: emptyList()
                    showLogsOrEmpty(activityLogs)
                } else {
                    Toast.makeText(this@ActivityLogActivity, "Failed to fetch activity log: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ActivityLogActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchCenterActivityLog(centerId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this).getCenterActivityLog(centerId).enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
            override fun onResponse(call: Call<ApiResponse<List<ActivityLog>>>, response: Response<ApiResponse<List<ActivityLog>>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val activityLogs = response.body()?.data ?: emptyList()
                    showLogsOrEmpty(activityLogs)
                } else {
                    Toast.makeText(this@ActivityLogActivity, "Failed to fetch activity log: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ActivityLogActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchTeacherActivityLog(teacherId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this).getTeacherActivityLog(teacherId).enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
            override fun onResponse(call: Call<ApiResponse<List<ActivityLog>>>, response: Response<ApiResponse<List<ActivityLog>>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val activityLogs = response.body()?.data ?: emptyList()
                    showLogsOrEmpty(activityLogs)
                } else {
                    Toast.makeText(this@ActivityLogActivity, "Failed to fetch activity log: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ActivityLogActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLogsOrEmpty(activityLogs: List<ActivityLog>) {
        if (activityLogs.isEmpty()) {
            binding.tvNoLogs.visibility = View.VISIBLE
            binding.rvActivityLog.visibility = View.GONE
        } else {
            binding.tvNoLogs.visibility = View.GONE
            binding.rvActivityLog.visibility = View.VISIBLE
            setupRecyclerView(activityLogs)
        }
    }

    private fun setupRecyclerView(activityLogs: List<ActivityLog>) {
        // show all logs (not limited) so reasons are visible for every entry
        binding.rvActivityLog.adapter = ActivityLogAdapter(activityLogs)
    }
}
