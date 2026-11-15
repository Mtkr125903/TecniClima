package com.airservice.pro.ui.client

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.databinding.ItemClienteBinding

class ClientAdapter(
    private val onClick: (ClientEntity) -> Unit,
    private val onCall: (ClientEntity) -> Unit,
    private val onWhatsApp: (ClientEntity) -> Unit
) : ListAdapter<ClientEntity, ClientAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemClienteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(cliente: ClientEntity) {
            binding.tvNombre.text = cliente.nombre
            binding.tvContacto.text = cliente.telefono.ifBlank { cliente.email }
            binding.tvDireccion.text = cliente.direccion
            binding.tvDireccion.visibility =
                if (cliente.direccion.isBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.root.setOnClickListener { onClick(cliente) }
            binding.btnCall.setOnClickListener { onCall(cliente) }
            binding.btnWhatsapp.setOnClickListener { onWhatsApp(cliente) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ClientEntity>() {
            override fun areItemsTheSame(oldItem: ClientEntity, newItem: ClientEntity) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ClientEntity, newItem: ClientEntity) =
                oldItem == newItem
        }
    }
}
