package com.airservice.pro.ui.equipment

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.airservice.pro.data.PhotoEntity
import com.airservice.pro.databinding.ItemFotoBinding

/** Galeria horizontal de fotos de un equipo. */
class PhotoAdapter(
    private val onClick: (PhotoEntity) -> Unit,
    private val onDelete: (PhotoEntity) -> Unit
) : ListAdapter<PhotoEntity, PhotoAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemFotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity) {
            binding.ivFoto.load(Uri.parse(photo.uri)) {
                crossfade(true)
                size(240, 240)
            }
            binding.root.setOnClickListener { onClick(photo) }
            binding.btnBorrar.setOnClickListener { onDelete(photo) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PhotoEntity>() {
            override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity) =
                oldItem == newItem
        }
    }
}
