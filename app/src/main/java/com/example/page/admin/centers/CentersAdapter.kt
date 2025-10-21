package com.example.page.admin.centers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.page.R
import com.example.page.api.CenterResponse

class CentersAdapter(
    private val centers: List<CenterResponse>,
    private val onEditClick: (CenterResponse) -> Unit,
    private val onDeleteClick: (CenterResponse) -> Unit
) : RecyclerView.Adapter<CentersAdapter.CenterViewHolder>() {

    inner class CenterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSNo: TextView = itemView.findViewById(R.id.tvSNo)
        val tvCenterName: TextView = itemView.findViewById(R.id.tvCenterName)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(center: CenterResponse, position: Int) {
            tvSNo.text = (position + 1).toString()
            tvCenterName.text = center.center_name
            tvAddress.text = center.address

            btnEdit.setOnClickListener {
                onEditClick(center)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(center)
            }
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