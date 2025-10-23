package com.example.page.teacher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.page.api.TeacherModel
import com.example.page.databinding.ItemTeacherBinding

class TeacherAdapter(private val teachers: List<TeacherModel>) :
    RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

    inner class TeacherViewHolder(val binding: ItemTeacherBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val binding = ItemTeacherBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TeacherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        val teacher = teachers[position]
        holder.binding.apply {
            tvSno.text = (position + 1).toString()
            tvName.text = teacher.name ?: "N/A"
            tvEmail.text = teacher.email ?: "—"
            tvPhone.text = teacher.phone ?: "—"
            tvCenterName.text = teacher.centerName ?: "—"
            tvCenterCode.text = "Code: ${teacher.centerCode ?: "--"}"

            btnView.setOnClickListener { /* TODO: open details */ }
            btnEdit.setOnClickListener { /* TODO: open edit screen */ }
            btnDelete.setOnClickListener { /* TODO: delete API */ }
        }
    }

    override fun getItemCount(): Int = teachers.size
}
