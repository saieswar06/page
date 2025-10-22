package com.example.page.admin.centers

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.page.R
import com.example.page.api.CenterResponse

// =========================== THE FIX IS HERE ===========================
// The constructor MUST be updated to accept the click listener lambdas from the Activity.
class CentersAdapter(
    private val centers: List<CenterResponse>,
    private val onEditClick: (CenterResponse) -> Unit,
    private val onDeleteClick: (CenterResponse) -> Unit
) : RecyclerView.Adapter<CentersAdapter.CenterViewHolder>() {

    inner class CenterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // These IDs must match the views in your item_center.xml layout
        private val tvSNo: TextView = itemView.findViewById(R.id.tvSNo)
        private val tvCenterName: TextView = itemView.findViewById(R.id.tvCenterName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(center: CenterResponse, position: Int) {
            tvSNo.text = (position + 1).toString()
            tvCenterName.text = center.center_name

            // FIX: Change 'center.address' to the correct property name
            tvAddress.text = center.center_address // Assuming the property is named 'center_address'


            // 👁️ View Button -> open details page
            btnView.setOnClickListener {
                // The adapter can still handle the simple "View" action internally.
                val intent = Intent(itemView.context, CenterDetailsActivity::class.java).apply {
                    // Pass the unique ID for the details activity to fetch its own data.
                    putExtra("CENTER_ID", center.id)
                }
                itemView.context.startActivity(intent)
            }

            // ✏️ Edit Button -> Calls the lambda passed from the Activity
            btnEdit.setOnClickListener { onEditClick(center) }

            // 🗑️ Delete Button -> Calls the lambda passed from the Activity
            btnDelete.setOnClickListener { onDeleteClick(center) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CenterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_center, parent, false)
        return CenterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CenterViewHolder, position: Int) {
        holder.bind(centers[position], position)
    }

    override fun getItemCount() = centers.size
}
