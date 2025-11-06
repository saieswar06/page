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

        teacher = intent.getParcelableExtra("teacher", TeacherModel::class.java)

        teacher?.let {
            binding.viewName.text = it.name
            binding.viewEmail.text = it.email ?: "N/A"
            binding.viewPhone.text = it.phone ?: "N/A"
            binding.viewCenter.text = it.centerName ?: "N/A"
            binding.viewCenterCode.text = it.centerCode ?: "N/A"
            binding.viewState.text = it.state ?: "N/A"
            binding.viewDistrict.text = it.district ?: "N/A"
            binding.viewMandal.text = it.mandal ?: "N/A"
            binding.viewLocality.text = it.locality ?: "N/A"
            binding.viewCoords.text = if (it.latitude != null && it.longitude != null) "${it.latitude}, ${it.longitude}" else "N/A"
            binding.viewStatus.text = if (it.status == 1) "Active" else "Inactive"
        }

        binding.btnCloseTeacher.setOnClickListener {
            finish()
        }

        binding.btnCloseHeader.setOnClickListener {
            finish()
        }

        binding.btnEditTeacher.setOnClickListener {
            val intent = Intent(this, EditTeacherActivity::class.java)
            intent.putExtra("teacher", teacher)
            startActivity(intent)
        }
    }
}
