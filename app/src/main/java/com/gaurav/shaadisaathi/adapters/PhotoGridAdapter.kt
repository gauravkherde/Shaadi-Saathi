package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.databinding.ItemPhotoGridBinding
import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.utils.ImageUtils

class PhotoGridAdapter(
    private val photos: List<Photo>,
    private val onItemClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(private val binding: ItemPhotoGridBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo) {
            // Load thumbnail for grid view (fast loading)
            if (photo.thumbnailBase64.isNotEmpty()) {
                val thumbnailBitmap = ImageUtils.decompressForViewing(photo.thumbnailBase64)
                if (thumbnailBitmap != null) {
                    binding.ivPhoto.setImageBitmap(thumbnailBitmap)
                } else {
                    binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.root.setOnClickListener {
                onItemClick(photo)
            }
        }
    }
}
