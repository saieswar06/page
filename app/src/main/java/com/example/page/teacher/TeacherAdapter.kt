package com.example.page.teacher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.page.R
import com.example.page.api.TeacherModel

class TeacherAdapter(
    private val showActive: Boolean,
    private val onViewClick: (TeacherModel) -> Unit,
    private val onEditClick: ((TeacherModel) -> Unit)? = null,
    private val onDeleteClick: ((TeacherModel) -> Unit)? = null,
    private val onRestoreClick: ((TeacherModel) -> Unit)? = null
) : RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>() {

    private var teachers = mutableListOf<TeacherModel>()

    fun submitList(list: List<TeacherModel>) {
        teachers.clear()
        teachers.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val layoutId = if (showActive) {
            R.layout.item_teacher
        } else {
            R.layout.item_inactive_teacher
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return TeacherViewHolder(view, showActive)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        holder.bind(teachers[position], position + 1)
    }

    override fun getItemCount() = teachers.size

    inner class TeacherViewHolder(itemView: View, private val isActive: Boolean) :
        RecyclerView.ViewHolder(itemView) {

        // IDs match item_teacher.xml / item_inactive_teacher.xml
        private val tvSerial: TextView = itemView.findViewById(R.id.tv_serial)
        private val tvTeacherName: TextView = itemView.findViewById(R.id.tv_teacher_name)
        private val btnView: ImageButton = itemView.findViewById(R.id.btn_view)

        // Active layout views (may be null in inactive layout)
        private val tvEmail: TextView? = itemView.findViewById(R.id.tv_email)
        private val tvPhone: TextView? = itemView.findViewById(R.id.tv_phone)
        private val tvCenterName: TextView? = itemView.findViewById(R.id.tv_center_name)
        private val btnEdit: ImageButton? = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton? = itemView.findViewById(R.id.btn_delete)

        // Inactive layout views (may be null in active layout)
        private val tvReason: TextView? = itemView.findViewById(R.id.tv_reason)
        private val btnRestore: Button? = itemView.findViewById(R.id.btn_restore)

        fun bind(teacher: TeacherModel, position: Int) {
            // Basic fields (always present)
            tvSerial.text = position.toString()
            tvTeacherName.text = teacher.name ?: "Unknown Teacher"

            // Unbind previous listeners to avoid accidental reuse side-effects
            btnView.setOnClickListener(null)
            btnEdit?.setOnClickListener(null)
            btnDelete?.setOnClickListener(null)
            btnRestore?.setOnClickListener(null)

            if (isActive) {
                // Active teacher - bind fields and actions
                tvEmail?.text = teacher.email ?: "N/A"
                tvPhone?.text = teacher.phone ?: "N/A"
                tvCenterName?.text = teacher.centerName ?: "N/A"

                btnEdit?.setOnClickListener { onEditClick?.invoke(teacher) }
                btnDelete?.setOnClickListener { onDeleteClick?.invoke(teacher) }
            } else {
                // Inactive teacher - show reason and restore action
                val reasonText = teacher.deletion_reason ?: teacher.reason ?: "No reason provided"
                tvReason?.text = reasonText
                btnRestore?.setOnClickListener { onRestoreClick?.invoke(teacher) }
            }

            // View button for both layouts
            btnView.setOnClickListener { onViewClick(teacher) }
        }
    }
}
