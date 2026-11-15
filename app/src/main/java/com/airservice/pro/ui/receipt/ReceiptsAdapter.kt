package com.airservice.pro.ui.receipt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.ComprobanteRow
import com.airservice.pro.databinding.ItemReceiptBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.NumeroUtil

class ReceiptsAdapter(
    private val onClick: (ComprobanteRow) -> Unit,
    private val onWhatsApp: (ComprobanteRow) -> Unit,
    private val onPdf: (ComprobanteRow) -> Unit
) : ListAdapter<ComprobanteRow, ReceiptsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemReceiptBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: ComprobanteRow) {
            val receipt = row.comprobante
            binding.tvNumero.text = receipt.numero
            binding.tvMonto.text = NumeroUtil.moneda(receipt.monto)
            binding.tvFecha.text = FechaUtil.mostrar(receipt.fecha)
            binding.tvEstado.text = receipt.estado
            UiHelpers.applyBadge(binding.tvEstado, UiHelpers.colorEstado(receipt.estado))

            val detalle = listOfNotNull(
                row.clienteNombre.takeIf { it.isNotBlank() },
                row.equipoDescripcion.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            binding.tvCliente.text = detalle.ifBlank { "Sin cliente asignado" }

            binding.root.setOnClickListener { onClick(row) }
            binding.btnWhatsapp.setOnClickListener { onWhatsApp(row) }
            binding.btnPdf.setOnClickListener { onPdf(row) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ComprobanteRow>() {
            override fun areItemsTheSame(oldItem: ComprobanteRow, newItem: ComprobanteRow) =
                oldItem.comprobante.id == newItem.comprobante.id
            override fun areContentsTheSame(oldItem: ComprobanteRow, newItem: ComprobanteRow) =
                oldItem == newItem
        }
    }
}
