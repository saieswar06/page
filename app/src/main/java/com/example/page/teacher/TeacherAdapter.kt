package com.example.page.teacher

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.page.R
import com.example.page.api.TeacherModel
import java.util.*

class TeacherAdapter(
    private var teachers: MutableList<TeacherModel>,
    private val showActive: Boolean,
    private val onViewClick: (TeacherModel) -> Unit,
    private val onEditClick: ((TeacherModel) -> Unit)? = null,
    private val onDeactivateClick: ((TeacherModel) -> Unit)? = null,
    private val onDeleteClick: ((TeacherModel) -> Unit)? = null,
    private val onRestoreClick: ((TeacherModel) -> Unit)? = null
) : RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder>(), Filterable {

    private var teachersFiltered = mutableListOf<TeacherModel>()

    init {
        teachersFiltered.addAll(teachers)
    }

    fun submitList(list: List<TeacherModel>) {
        teachers.clear()
        teachers.addAll(list)
        teachersFiltered.clear()
        teachersFiltered.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherViewHolder {
        val layoutId = if (showActive) R.layout.item_teacher else R.layout.item_inactive_teacher
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return TeacherViewHolder(view, showActive)
    }

    override fun onBindViewHolder(holder: TeacherViewHolder, position: Int) {
        holder.bind(teachersFiltered[position], position + 1)
    }

    override fun getItemCount() = teachersFiltered.size

    fun updateData(newTeachers: List<TeacherModel>) {
        try {
            teachers.clear()
            teachers.addAll(newTeachers)
            teachersFiltered.clear()
            teachersFiltered.addAll(newTeachers)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                notifyDataSetChanged()
            } else {
                Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
            }
        } catch (e: Exception) {
            Log.e("TeacherAdapter", "Exception in updateData", e)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
                val filteredList: List<TeacherModel> = if (query.isEmpty()) {
                    teachers.toList()
                } else {
                    teachers.filter { teacher ->
                        (teacher.name?.lowercase(Locale.ROOT)?.contains(query) == true) ||
                                (teacher.email?.lowercase(Locale.ROOT)?.contains(query) == true)
                    }
                }
                return FilterResults().apply { 
                    values = ArrayList(filteredList)
                    count = filteredList.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                try {
                    val values = results?.values as? List<TeacherModel> ?: emptyList()
                    teachersFiltered.clear()
                    teachersFiltered.addAll(values)
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        notifyDataSetChanged()
                    } else {
                        Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
                    }
                } catch (e: Exception) {
                    Log.e("TeacherAdapter", "Error publishing filter results", e)
                }
            }
        }
    }

    inner class TeacherViewHolder(itemView: View, private val isActive: Boolean) :
        RecyclerView.ViewHolder(itemView) {

        private val tvSerial: TextView = itemView.findViewById(R.id.tv_serial)
        private val tvTeacherName: TextView = itemView.findViewById(R.id.tv_teacher_name)
        private val btnView: ImageButton = itemView.findViewById(R.id.btn_view)
        private val tvEmail: TextView? = itemView.findViewById(R.id.tv_email)
        private val tvPhone: TextView? = itemView.findViewById(R.id.tv_phone)
        private val tvCenterName: TextView? = itemView.findViewById(R.id.tv_center_name)
        private val btnEdit: ImageButton? = itemView.findViewById(R.id.btn_edit)
        private val btnDeactivate: ImageButton? = itemView.findViewById(R.id.btn_deactivate)
        private val btnDelete: ImageButton? = itemView.findViewById(R.id.btn_delete)
        private val tvReason: TextView? = itemView.findViewById(R.id.tv_reason)
        private val btnRestore: Button? = itemView.findViewById(R.id.btn_restore)

        fun bind(teacher: TeacherModel, position: Int) {
            tvSerial.text = position.toString()
            tvTeacherName.text = teacher.name ?: "Unknown Teacher"

            btnView.setOnClickListener { onViewClick(teacher) }

            if (isActive) {
                tvEmail?.text = teacher.email ?: "N/A"
                tvPhone?.text = teacher.phone ?: "N/A"
                tvCenterName?.text = teacher.centerName ?: "N/A"

                btnEdit?.setOnClickListener { onEditClick?.invoke(teacher) }
                btnDeactivate?.setOnClickListener { onDeactivateClick?.invoke(teacher) }
                btnDelete?.setOnClickListener { onDeleteClick?.invoke(teacher) }
            } else {
                tvReason?.text = teacher.reason ?: "No reason provided"
                btnRestore?.setOnClickListener { onRestoreClick?.invoke(teacher) }
            }
        }
    }
}