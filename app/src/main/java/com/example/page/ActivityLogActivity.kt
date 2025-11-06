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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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
        binding.rvActivityLog.adapter = ActivityLogAdapter(emptyList())

        // initial visibility
        binding.rvActivityLog.visibility = View.GONE
        binding.tvNoLogs.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadActivityLog()
    }

    private fun loadActivityLog() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvActivityLog.visibility = View.GONE
        binding.tvNoLogs.visibility = View.GONE

        val api = RetrofitClient.getInstance(this)

        val call = when {
            centerId != -1 -> api.getCenterActivityLog(centerId)
            teacherId != -1 -> api.getTeacherActivityLog(teacherId)
            else -> api.getAllActivityLogs()
        }

        call.enqueue(object : Callback<ApiResponse<List<ActivityLog>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<ActivityLog>>>,
                response: Response<ApiResponse<List<ActivityLog>>>
            ) {
                binding.progressBar.visibility = View.GONE

                if (!response.isSuccessful) {
                    val msg = response.message()
                    showError("Server error: $msg")
                    return
                }

                val body = response.body()
                if (body == null || body.success.not()) {
                    val msg = body?.message ?: "Unexpected server response"
                    showError(msg)
                    return
                }

                val logs = body.data ?: emptyList()

                // Sort by timestamp (newest first). We parse using expected format "yyyy-MM-dd HH:mm:ss".
                // If parse fails we fall back to string comparison.
                val sorted = logs.sortedWith(compareByDescending { parseTimestampToMillis(it.timestamp) })

                // take only latest 10
                val recent = sorted.take(10)

                showLogsOrEmpty(recent)
            }

            override fun onFailure(call: Call<ApiResponse<List<ActivityLog>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                showError("Network error: ${t.message}")
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
            val adapter = ActivityLogAdapter(activityLogs)
            binding.rvActivityLog.adapter = adapter

            // ensure the list shows newest at top
            binding.rvActivityLog.post {
                if (activityLogs.isNotEmpty()) binding.rvActivityLog.scrollToPosition(0)
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvNoLogs.visibility = View.VISIBLE
        binding.rvActivityLog.visibility = View.GONE
        binding.tvNoLogs.text = msg
        Toast.makeText(this@ActivityLogActivity, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Parse timestamp string to milliseconds. Expected format: "yyyy-MM-dd HH:mm:ss"
     * Returns 0 for unparsable values so they sink to the end when sorting descending.
     */
    private fun parseTimestampToMillis(ts: String?): Long {
        if (ts.isNullOrBlank()) return 0L
        // try a few common formats (server might vary)
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (fmtStr in formats) {
            try {
                val fmt = SimpleDateFormat(fmtStr, Locale.getDefault())
                fmt.timeZone = TimeZone.getDefault()
                val d = fmt.parse(ts)
                if (d != null) return d.time
            } catch (e: ParseException) {
                // try next
            } catch (e: Exception) {
                // try next
            }
        }
        // fallback: lexicographic compare by returning hash of string
        return ts.hashCode().toLong()
    }
}
