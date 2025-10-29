package com.example.page.teacher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.TeacherModel
import com.example.page.databinding.ActivityViewTeacherBinding

class ViewTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewTeacherBinding
    private var teacher: TeacherModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Safe non-deprecated way to get parcelable
        teacher = intent.getParcelableExtra("teacher", TeacherModel::class.java)

        // ✅ Populate all fields
        teacher?.let {
            binding.viewName.text = it.name
            binding.viewEmail.text = it.email ?: "N/A"
            binding.viewPhone.text = it.phone ?: "N/A"
            binding.viewCenter.text = it.centerName ?: "N/A"
            binding.viewCenterCode.text = it.centerCode?.toString() ?: "N/A"
            binding.viewStatus.text = if (it.status == 1) "Active" else "Inactive"

            // optional: coordinates placeholder (if you ever add it to model)
            binding.viewCoords.text = "--"
        }

        // ✅ Close buttons
        binding.btnCloseTeacher.setOnClickListener {
            finish()
        }

        binding.btnCloseHeader.setOnClickListener {
            finish()
        }

        // ✅ Edit button opens EditTeacherActivity
        binding.btnEditTeacher.setOnClickListener {
            val intent = Intent(this, EditTeacherActivity::class.java)
            intent.putExtra("teacher", teacher)
            startActivity(intent)
        }
    }
}
