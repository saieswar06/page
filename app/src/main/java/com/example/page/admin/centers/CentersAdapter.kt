package com.example.page.admin.centers

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.page.R
import com.example.page.api.CenterResponse
import java.util.*

class CentersAdapter(
    private var centers: MutableList<CenterResponse>,
    private val showActive: Boolean,
    private val onViewClick: (CenterResponse) -> Unit,
    private val onEditClick: ((CenterResponse) -> Unit)? = null,
    private val onDeleteClick: ((CenterResponse) -> Unit)? = null,
    private val onRestoreClick: ((CenterResponse) -> Unit)? = null
) : RecyclerView.Adapter<CentersAdapter.CenterViewHolder>(), Filterable {

    private var centersFiltered = mutableListOf<CenterResponse>()

    init {
        centersFiltered.addAll(centers)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CenterViewHolder {
        val layoutId = if (showActive) {
            R.layout.item_center
        } else {
            R.layout.item_inactive_center
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return CenterViewHolder(view, showActive)
    }

    override fun onBindViewHolder(holder: CenterViewHolder, position: Int) {
        holder.bind(centersFiltered[position], position + 1)
    }

    override fun getItemCount() = centersFiltered.size

    /**
     * Replace data safely and notify on the main thread.
     */
    fun updateData(newCenters: List<CenterResponse>) {
        try {
            centersFiltered.clear()
            centersFiltered.addAll(newCenters)

            Log.d("CentersAdapter", "updateData -> centers=${centers.size}, centersFiltered=${centersFiltered.size}")

            // Ensure notify happens on main thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                notifyDataSetChanged()
            } else {
                Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
            }
        } catch (e: Exception) {
            Log.e("CentersAdapter", "Exception in updateData", e)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
                val filteredList: List<CenterResponse> = if (query.isEmpty()) {
                    centers.toList()
                } else {
                    centers.filter { center ->
                        (center.center_name?.lowercase(Locale.ROOT)?.contains(query) == true) ||
                                (center.center_code?.toString()?.lowercase(Locale.ROOT)?.contains(query) == true) ||
                                (center.village?.lowercase(Locale.ROOT)?.contains(query) == true)
                    }
                }
                val results = FilterResults()
                results.values = ArrayList(filteredList) // return a copy
                results.count = filteredList.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                try {
                    val values = when (val v = results?.values) {
                        is List<*> -> v.filterIsInstance<CenterResponse>()
                        is ArrayList<*> -> v.filterIsInstance<CenterResponse>()
                        else -> emptyList()
                    }
                    centersFiltered.clear()
                    centersFiltered.addAll(values)
                    Log.d("CentersAdapter", "publishResults -> filtered=${centersFiltered.size}")
                    // safe notify
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        notifyDataSetChanged()
                    } else {
                        Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
                    }
                } catch (e: Exception) {
                    Log.e("CentersAdapter", "Error publishing filter results", e)
                }
            }
        }
    }

    inner class CenterViewHolder(itemView: View, private val isActive: Boolean) :
        RecyclerView.ViewHolder(itemView) {

        private val tvSerial: TextView = itemView.findViewById(R.id.tv_serial)
        private val tvCenterName: TextView = itemView.findViewById(R.id.tv_center_name)
        private val btnView: ImageButton = itemView.findViewById(R.id.btn_view)

        // Optional / nullable views
        private val tvReason: TextView? = itemView.findViewById(R.id.tv_reason)
        private val tvTeacherCount: TextView? = itemView.findViewById(R.id.tv_teacher_count)
        private val tvVillage: TextView? = itemView.findViewById(R.id.tv_village)
        private val tvTotalChildren: TextView? = itemView.findViewById(R.id.tv_total_children)
        private val btnEdit: ImageButton? = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton? = itemView.findViewById(R.id.btn_delete)
        private val btnRestore: Button? = itemView.findViewById(R.id.btn_restore)

        fun bind(center: CenterResponse, position: Int) {
            tvSerial.text = position.toString()
            tvCenterName.text = center.center_name ?: "Unknown Center"

            // clear listeners to avoid recycled view issues
            btnView.setOnClickListener(null)
            btnEdit?.setOnClickListener(null)
            btnDelete?.setOnClickListener(null)
            btnRestore?.setOnClickListener(null)

            if (isActive) {
                tvTeacherCount?.text = (center.teacher_count ?: 0).toString()
                tvVillage?.text = center.village ?: "N/A"
                tvTotalChildren?.text = (center.total_children ?: 0).toString()

                btnEdit?.setOnClickListener { onEditClick?.invoke(center) }
                btnDelete?.setOnClickListener { onDeleteClick?.invoke(center) }
            } else {
                tvReason?.text = center.deletion_reason ?: "No reason provided"
                btnRestore?.setOnClickListener { onRestoreClick?.invoke(center) }
            }

            btnView.setOnClickListener { onViewClick(center) }
        }
    }
}
