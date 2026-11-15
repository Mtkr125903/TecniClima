package com.airservice.pro.ui.agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.TurnoRow
import com.airservice.pro.databinding.ItemAgendaBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil

class AgendaAdapter(private val onClick: (TurnoRow) -> Unit) :
    ListAdapter<TurnoRow, AgendaAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAgendaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAgendaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: TurnoRow) {
            val turno = row.turno
            binding.tvHora.text = turno.hora.ifBlank { "--:--" }
            binding.tvTitulo.text = turno.titulo
            binding.tvCliente.text = row.clienteNombre.ifBlank { "Sin cliente" }
            binding.tvDireccion.text = turno.direccion.ifBlank { "Sin dirección" }
            binding.tvDireccion.visibility =
                if (turno.direccion.isBlank()) View.GONE else View.VISIBLE
            binding.tvTecnico.text = row.tecnicoNombre
                .takeIf { it.isNotBlank() }
                ?.let { "Téc. $it" }
                ?: "Sin técnico"
            binding.tvEstado.text = turno.estado
            UiHelpers.applyBadge(binding.tvEstado, UiHelpers.colorEstado(turno.estado))

            binding.root.setOnClickListener { onClick(row) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TurnoRow>() {
            override fun areItemsTheSame(oldItem: TurnoRow, newItem: TurnoRow) =
                oldItem.turno.id == newItem.turno.id
            override fun areContentsTheSame(oldItem: TurnoRow, newItem: TurnoRow) =
                oldItem == newItem
        }
    }
}
