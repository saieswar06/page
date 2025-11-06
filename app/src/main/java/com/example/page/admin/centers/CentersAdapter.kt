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
    private val onDeactivateClick: ((CenterResponse) -> Unit)? = null,
    private val onDeleteClick: ((CenterResponse) -> Unit)? = null,
    private val onRestoreClick: ((CenterResponse) -> Unit)? = null
) : RecyclerView.Adapter<CentersAdapter.CenterViewHolder>(), Filterable {

    private var centersFiltered = mutableListOf<CenterResponse>()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        centersFiltered.addAll(centers)
        Log.d("CentersAdapter", "Initialized with ${centers.size} centers, showActive: $showActive")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CenterViewHolder {
        // Assuming R.layout.item_inactive_center exists for deactivated centers
        val layoutId = if (showActive) R.layout.item_center else R.layout.item_inactive_center
        Log.d("CentersAdapter", "Creating ViewHolder with layout: ${if (showActive) "item_center" else "item_inactive_center"}")
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return CenterViewHolder(view, showActive)
    }

    override fun onBindViewHolder(holder: CenterViewHolder, position: Int) {
        val center = centersFiltered[position]
        Log.d("CentersAdapter", "Binding position $position: ${center.center_name}, status=${center.status}")
        holder.bind(center, position + 1)
    }

    override fun getItemCount(): Int {
        return centersFiltered.size
    }

    private fun notifyDataChangedOnMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifyDataSetChanged()
        } else {
            mainHandler.post { notifyDataSetChanged() }
        }
    }

    fun updateData(newCenters: List<CenterResponse>) {
        try {
            Log.d("CentersAdapter", "updateData called with ${newCenters.size} centers")

            centers.clear()
            centers.addAll(newCenters)
            centersFiltered.clear()
            centersFiltered.addAll(newCenters)

            notifyDataChangedOnMainThread()

            Log.d("CentersAdapter", "Data updated successfully. New size: ${centersFiltered.size}")
        } catch (e: Exception) {
            Log.e("CentersAdapter", "Exception in updateData", e)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
                Log.d("CentersAdapter", "Filtering with query: '$query'")

                val filteredList: List<CenterResponse> = if (query.isEmpty()) {
                    centers.toList()
                } else {
                    centers.filter { center ->
                        val nameMatch = center.center_name?.lowercase(Locale.ROOT)?.contains(query) == true
                        val codeMatch = center.center_code?.toString()?.lowercase(Locale.ROOT)?.contains(query) == true
                        val reasonMatch = if (!showActive) center.reason?.lowercase(Locale.ROOT)?.contains(query) == true else false

                        nameMatch || codeMatch || reasonMatch
                    }
                }

                Log.d("CentersAdapter", "Filter results: ${filteredList.size} items")
                return FilterResults().apply {
                    values = ArrayList(filteredList)
                    count = filteredList.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                try {
                    val values = results?.values as? List<CenterResponse> ?: emptyList()
                    centersFiltered.clear()
                    centersFiltered.addAll(values)

                    notifyDataChangedOnMainThread()

                    Log.d("CentersAdapter", "Published filter results: ${centersFiltered.size} items")
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

        private val tvReason: TextView? = itemView.findViewById(R.id.tv_reason)
        private val tvTeacherCount: TextView? = itemView.findViewById(R.id.tv_teacher_count)
        private val btnEdit: ImageButton? = itemView.findViewById(R.id.btn_edit)
        private val btnDeactivate: ImageButton? = itemView.findViewById(R.id.btn_deactivate)
        private val btnDelete: ImageButton? = itemView.findViewById(R.id.btn_delete)
        private val btnRestore: Button? = itemView.findViewById(R.id.btn_restore)


        fun bind(center: CenterResponse, position: Int) {
            try {
                tvSerial.text = position.toString()
                tvCenterName.text = center.center_name ?: "Unknown Center"
                btnView.setOnClickListener { onViewClick(center) }

                if (isActive) {
                    bindActive(center)
                } else {
                    bindInactive(center)
                }
            } catch (e: Exception) {
                Log.e("CentersAdapter", "Error binding center at position $position", e)
            }
        }

        private fun bindActive(center: CenterResponse) {
            tvTeacherCount?.text = (center.teacher_count ?: 0).toString()

            btnEdit?.setOnClickListener { onEditClick?.invoke(center) }
            btnDeactivate?.setOnClickListener { onDeactivateClick?.invoke(center) }
            btnDelete?.setOnClickListener { onDeleteClick?.invoke(center) }

            tvTeacherCount?.visibility = View.VISIBLE
            tvReason?.visibility = View.GONE
            btnRestore?.visibility = View.GONE

            btnEdit?.visibility = View.VISIBLE
            btnDeactivate?.visibility = View.VISIBLE
            btnDelete?.visibility = View.VISIBLE
        }

        private fun bindInactive(center: CenterResponse) {
            val reasonText = center.reason?.takeIf { it.isNotBlank() } ?: "No reason provided"
            tvReason?.text = reasonText

            btnRestore?.setOnClickListener {                Log.d("CentersAdapter", "Restore clicked for center: ${center.center_name}")
                onRestoreClick?.invoke(center)
            }

            tvTeacherCount?.visibility = View.GONE
            tvReason?.visibility = View.VISIBLE
            btnRestore?.visibility = View.VISIBLE

            btnEdit?.visibility = View.GONE
            btnDeactivate?.visibility = View.GONE
            btnDelete?.visibility = View.GONE
        }
    }
}