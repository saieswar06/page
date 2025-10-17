package com.example.page.admin.centers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.page.databinding.ItemCenterBinding
import com.example.page.api.Center


class CentersAdapter(
    private val onEdit: (Center) -> Unit,
    private val onDelete: (Center) -> Unit
) : RecyclerView.Adapter<CentersAdapter.ViewHolder>() {

    private val centers = mutableListOf<Center>()

    fun setData(newCenters: List<Center>) {
        centers.clear()
        centers.addAll(newCenters)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemCenterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(center: Center, index: Int, onEdit: (Center) -> Unit, onDelete: (Center) -> Unit) {
            binding.tvSNo.text = index.toString()
            binding.tvCenterName.text = center.center_name
            binding.tvAddress.text = center.address ?: "N/A"

            binding.btnEdit.setOnClickListener { onEdit(center) }
            binding.btnDelete.setOnClickListener { onDelete(center) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemCenterBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = centers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(centers[position], position + 1, onEdit, onDelete)
    }
}
