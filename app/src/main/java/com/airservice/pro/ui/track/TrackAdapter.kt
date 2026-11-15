package com.airservice.pro.ui.track

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.TrabajoRow
import com.airservice.pro.databinding.ItemTrackBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil

class TrackAdapter(
    private val onAvanceChanged: (TrabajoRow, Int) -> Unit,
    private val onClick: (TrabajoRow) -> Unit
) :
    ListAdapter<TrabajoRow, TrackAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var current: TrabajoRow? = null

        init {
            binding.sliderAvance.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    current?.let { onAvanceChanged(it, value.toInt()) }
                }
            }
        }

        fun bind(row: TrabajoRow) {
            current = row
            val work = row.trabajo

            binding.tvTitulo.text = work.titulo
            binding.tvEstado.text = work.estado
            UiHelpers.applyBadge(binding.tvEstado, UiHelpers.colorEstado(work.estado))

            binding.tvCliente.text = listOfNotNull(
                row.clienteNombre.takeIf { it.isNotBlank() },
                row.equipoDescripcion.takeIf { it.isNotBlank() }
            ).joinToString(" · ").ifBlank { "Sin cliente asignado" }

            binding.tvTecnico.text = row.tecnicoNombre
                .takeIf { it.isNotBlank() }
                ?.let { "Téc. $it" }
                ?: "Sin técnico asignado"

            binding.tvPorcentaje.text = "${work.avance}%"
            binding.progressBar.progress = work.avance
            binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(
                    binding.root.context,
                    UiHelpers.colorAvance(work.avance)
                )
            )

            val valor = work.avance.coerceIn(0, 100).toFloat()
            if (binding.sliderAvance.value != valor) {
                binding.sliderAvance.value = valor
            }
            binding.sliderAvance.contentDescription =
                "Avance de ${work.titulo}: $valor por ciento"

            binding.root.setOnClickListener { onClick(row) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TrabajoRow>() {
            override fun areItemsTheSame(oldItem: TrabajoRow, newItem: TrabajoRow) =
                oldItem.trabajo.id == newItem.trabajo.id
            override fun areContentsTheSame(oldItem: TrabajoRow, newItem: TrabajoRow) =
                oldItem == newItem
        }
    }
}
