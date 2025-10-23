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

        teacher = intent.getParcelableExtra("teacher")

        setupUI()
        setupActions()
    }

    private fun setupUI() {
        teacher?.let {
            binding.viewName.text = it.name ?: "N/A"
            binding.viewEmail.text = it.email ?: "N/A"
            binding.viewPhone.text = it.phone ?: "N/A"
            binding.viewCenter.text = it.centerName ?: "N/A"
            binding.viewCenterCode.text = it.centerCode ?: "-"
            binding.viewStatus.text = if (it.status == 1) "Active" else "Inactive"
        }
    }

    private fun setupActions() {
        binding.btnCloseHeader.setOnClickListener { finish() }
        binding.btnCloseTeacher.setOnClickListener { finish() }

        binding.btnEditTeacher.setOnClickListener {
            val intent = Intent(this, EditTeacherActivity::class.java)
            intent.putExtra("teacher", teacher)
            startActivity(intent)
        }
    }
}
