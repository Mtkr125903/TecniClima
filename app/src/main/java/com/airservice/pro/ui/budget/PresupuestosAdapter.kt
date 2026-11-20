package com.airservice.pro.ui.budget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.R
import com.airservice.pro.data.PresupuestoRow
import com.airservice.pro.databinding.ItemPresupuestoBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.NumeroUtil

class PresupuestosAdapter(
    private val onClick: (PresupuestoRow) -> Unit,
    private val onAprobar: (PresupuestoRow) -> Unit,
    private val onRechazar: (PresupuestoRow) -> Unit,
    private val onWhatsApp: (PresupuestoRow) -> Unit,
    private val onPdf: (PresupuestoRow) -> Unit
) : ListAdapter<PresupuestoRow, PresupuestosAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemPresupuestoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPresupuestoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: PresupuestoRow) {
            val budget = row.presupuesto
            binding.tvNumero.text = budget.numero
            binding.tvMonto.text = NumeroUtil.moneda(budget.monto)
            binding.tvFecha.text = FechaUtil.mostrar(budget.fecha)
            binding.tvEstado.text = budget.estado
            UiHelpers.applyBadge(binding.tvEstado, UiHelpers.colorEstado(budget.estado))

            val detalle = listOfNotNull(
                row.clienteNombre.takeIf { it.isNotBlank() },
                row.equipoDescripcion.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            binding.tvCliente.text = detalle.ifBlank { "Sin cliente asignado" }

            val pendiente = budget.estado == binding.root.context.getString(R.string.estado_pendiente_aprobacion)
            binding.btnAprobar.visibility = if (pendiente) View.VISIBLE else View.GONE
            binding.btnRechazar.visibility = if (pendiente) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onClick(row) }
            binding.btnAprobar.setOnClickListener { onAprobar(row) }
            binding.btnRechazar.setOnClickListener { onRechazar(row) }
            binding.btnWhatsapp.setOnClickListener { onWhatsApp(row) }
            binding.btnPdf.setOnClickListener { onPdf(row) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PresupuestoRow>() {
            override fun areItemsTheSame(oldItem: PresupuestoRow, newItem: PresupuestoRow) =
                oldItem.presupuesto.id == newItem.presupuesto.id
            override fun areContentsTheSame(oldItem: PresupuestoRow, newItem: PresupuestoRow) =
                oldItem == newItem
        }
    }
}
