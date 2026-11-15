package com.airservice.pro.ui.technician

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.TechnicianEntity
import com.airservice.pro.databinding.ItemTechnicianBinding

class TechnicianAdapter(private val onClick: (TechnicianEntity) -> Unit) :
    ListAdapter<TechnicianEntity, TechnicianAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTechnicianBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTechnicianBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(technician: TechnicianEntity) {
            binding.tvNombre.text = technician.nombre
            binding.tvEspecialidad.text = technician.especialidad.ifBlank { "HVAC" }
            binding.tvTelefono.text = technician.telefono
            binding.root.setOnClickListener { onClick(technician) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TechnicianEntity>() {
            override fun areItemsTheSame(oldItem: TechnicianEntity, newItem: TechnicianEntity) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: TechnicianEntity, newItem: TechnicianEntity) =
                oldItem == newItem
        }
    }
}
