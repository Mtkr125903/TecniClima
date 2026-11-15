package com.airservice.pro.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.ServicioRow
import com.airservice.pro.databinding.ItemServiceBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil

class ServiceHistoryAdapter(private val onClick: (ServicioRow) -> Unit) :
    ListAdapter<ServicioRow, ServiceHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ServicioRow) {
            val service = row.servicio
            binding.tvTipo.text = service.tipo
            binding.tvDescripcion.text = service.descripcion
            binding.tvFecha.text = FechaUtil.mostrar(service.fecha)
            binding.tvEstado.text = service.estado
            UiHelpers.applyBadge(binding.tvEstado, UiHelpers.colorEstado(service.estado))

            val detalle = listOfNotNull(
                row.equipoDescripcion.takeIf { it.isNotBlank() },
                row.tecnicoNombre.takeIf { it.isNotBlank() }?.let { "Téc. $it" }
            ).joinToString(" · ")
            binding.tvEquipo.text = detalle
            binding.tvEquipo.visibility = if (detalle.isBlank()) View.GONE else View.VISIBLE

            binding.root.setOnClickListener { onClick(row) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ServicioRow>() {
            override fun areItemsTheSame(oldItem: ServicioRow, newItem: ServicioRow) =
                oldItem.servicio.id == newItem.servicio.id
            override fun areContentsTheSame(oldItem: ServicioRow, newItem: ServicioRow) =
                oldItem == newItem
        }
    }
}
