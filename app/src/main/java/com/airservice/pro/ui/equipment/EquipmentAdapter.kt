package com.airservice.pro.ui.equipment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.EquipoRow
import com.airservice.pro.databinding.ItemEquipoBinding

class EquipmentAdapter(private val onClick: (EquipoRow) -> Unit) :
    ListAdapter<EquipoRow, EquipmentAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemEquipoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEquipoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: EquipoRow) {
            val equipo = row.equipo
            binding.tvType.text = equipo.type.ifBlank { equipo.brand }
            binding.tvMarca.text = listOf(equipo.brand, equipo.model)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            binding.tvCliente.text = row.clienteNombre.ifBlank { "Sin cliente asignado" }
            binding.tvSerial.text = listOfNotNull(
                equipo.serial.takeIf { it.isNotBlank() }?.let { "Serie: $it" },
                equipo.location.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            binding.tvSerial.visibility =
                if (binding.tvSerial.text.isNullOrBlank()) View.GONE else View.VISIBLE

            binding.root.setOnClickListener { onClick(row) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<EquipoRow>() {
            override fun areItemsTheSame(oldItem: EquipoRow, newItem: EquipoRow) =
                oldItem.equipo.id == newItem.equipo.id
            override fun areContentsTheSame(oldItem: EquipoRow, newItem: EquipoRow) =
                oldItem == newItem
        }
    }
}
