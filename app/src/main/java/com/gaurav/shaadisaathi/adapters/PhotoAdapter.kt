package com.gaurav.shaadisaathi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gaurav.shaadisaathi.databinding.ItemPhotoBinding
import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class PhotoAdapter(
    private val photos: List<Photo>,
    private val onItemClick: (Photo) -> Unit,
    private val onLikeClick: (Photo) -> Unit,
    private val onCommentClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(photo: Photo) {
            // Load thumbnail for list view
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

            binding.tvUploaderName.text = photo.uploaderName

            // Set caption
            if (photo.caption.isNotEmpty()) {
                binding.tvCaption.text = photo.caption
                binding.tvCaption.visibility = View.VISIBLE
            } else {
                binding.tvCaption.visibility = View.GONE
            }

            // NEW: Show image quality info (only if tvImageInfo exists in layout)
            try {
                binding.tvImageInfo.text = "${photo.originalWidth}×${photo.originalHeight} • ${photo.fileSize}"
            } catch (e: Exception) {
                // tvImageInfo doesn't exist in layout - skip this
            }

            // Show like and comment counts
            val likeCount = photo.likes.size
            binding.tvLikeCount.text = if (likeCount > 0) {
                "$likeCount ${if (likeCount == 1) "like" else "likes"}"
            } else {
                "No likes yet"
            }

            val commentCount = photo.comments.size
            binding.tvCommentCount.text = if (commentCount > 0) {
                "$commentCount ${if (commentCount == 1) "comment" else "comments"}"
            } else {
                "No comments"
            }
        }


        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
