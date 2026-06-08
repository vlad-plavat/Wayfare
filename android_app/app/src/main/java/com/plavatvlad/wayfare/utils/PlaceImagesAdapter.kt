package com.plavatvlad.wayfare.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.plavatvlad.wayfare.R

class PlaceImagesAdapter(
    private var images: List<String>,
    private val canManageImages: Boolean,
    private val onLongClick: (String) -> Unit
) : RecyclerView.Adapter<PlaceImagesAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = images[position]

        Glide.with(holder.imageView)
            .load(images[position])
            .into(holder.imageView)

        if (canManageImages) {
            holder.imageView.setOnLongClickListener {
                onLongClick(url)
                true
            }
        } else {
            holder.imageView.setOnLongClickListener(null)
            holder.imageView.isLongClickable = false
        }
    }

    override fun getItemCount(): Int = images.size
}