package com.example.quarrymap

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import androidx.core.content.ContextCompat

class PointImagesAdapter(
    private val images: List<String>,
    private val onImageClick: (String) -> Unit,
    private val onImageDelete: (Int) -> Unit
) : RecyclerView.Adapter<PointImagesAdapter.ImageViewHolder>() {

    var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivImagePreview)
        val btnDelete: ImageButton? = itemView.findViewById(R.id.btnDeleteImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = if (viewType == 1) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_point_image_add, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_point_image, parent, false)
        }
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        if (position == images.size) {
            // Miniature "ajouter"
            holder.imageView.setImageResource(R.drawable.ic_add_image)
            holder.imageView.scaleX = 1f
            holder.imageView.scaleY = 1f
            holder.imageView.setBackgroundColor(Color.TRANSPARENT)
            holder.btnDelete?.visibility = View.GONE
            holder.imageView.setOnClickListener {
                onImageClick("") // Convention : chaîne vide pour déclencher l'ajout
            }
            return
        }
        val imagePath = images[position]
        val bitmap = BitmapFactory.decodeFile(imagePath)
        holder.imageView.setImageBitmap(bitmap)
        // Animation de zoom et surbrillance
        if (position == selectedPosition) {
            holder.imageView.scaleX = 1.12f
            holder.imageView.scaleY = 1.12f
            holder.imageView.setBackgroundColor(ContextCompat.getColor(holder.imageView.context, android.R.color.holo_blue_light))
        } else {
            holder.imageView.scaleX = 1f
            holder.imageView.scaleY = 1f
            holder.imageView.setBackgroundColor(Color.TRANSPARENT)
        }
        holder.imageView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            onImageClick(imagePath)
        }
        holder.btnDelete?.setOnClickListener {
            onImageDelete(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = images.size + 1 // +1 pour la miniature "ajouter"

    override fun getItemViewType(position: Int): Int {
        return if (position == images.size) 1 else 0 // 1 = bouton ajouter, 0 = image normale
    }
}
