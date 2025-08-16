package com.gaurav.shaadisaathi.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gaurav.shaadisaathi.R
import com.gaurav.shaadisaathi.databinding.ItemPhotoBinding
import com.gaurav.shaadisaathi.models.Photo

class PhotoAdapter(
    private val photos: MutableList<Photo>,
    private val onPhotoClick: (Photo, Int) -> Unit,
    private val onPhotoLongClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount(): Int = photos.size

    fun updatePhotos(newPhotos: List<Photo>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: Photo, position: Int) {
            binding.apply {
                // FIX: Load photo from base64 instead of URL
                try {
                    val thumbnailToUse = if (photo.thumbnailBase64.isNotEmpty()) {
                        photo.thumbnailBase64
                    } else {
                        photo.imageBase64
                    }

                    if (thumbnailToUse.isNotEmpty()) {
                        val imageBytes = Base64.decode(thumbnailToUse, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.ivPhoto.setImageBitmap(bitmap)
                    } else {
                        binding.ivPhoto.setImageResource(R.drawable.ic_photo)
                    }
                } catch (e: Exception) {
                    binding.ivPhoto.setImageResource(R.drawable.ic_photo)
                }

                // Show caption if available
                if (photo.caption.isNotEmpty()) {
                    binding.tvCaption.text = photo.caption
                    binding.tvCaption.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvCaption.visibility = android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onPhotoClick(photo, position)
                }

                root.setOnLongClickListener {
                    onPhotoLongClick(photo)
                    true
                }
            }
        }
    }
}
