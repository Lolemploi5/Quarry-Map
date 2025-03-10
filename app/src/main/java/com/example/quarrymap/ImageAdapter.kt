package com.example.quarrymap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import android.graphics.Bitmap
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ImageAdapter(
    private val images: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val planchePreview: ImageView = view.findViewById(R.id.planchePreview)
        val plancheName: TextView = view.findViewById(R.id.plancheName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_planche, parent, false) // 🔴 Assure-toi que ce fichier existe
        return ImageViewHolder(view)
    }




    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = images[position]
        holder.plancheName.text = File(imagePath).name

        Glide.with(holder.itemView.context)
            .asBitmap() // Chargement sous forme de bitmap optimisé
            .load(imagePath)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Mise en cache pour éviter de recharger
            .format(DecodeFormat.PREFER_RGB_565) // Utilisation de RGB_565 pour économiser de la mémoire
            .override(500, 500) // Limite la taille à 500x500 pixels
            .into(holder.planchePreview)

        holder.itemView.setOnClickListener { onItemClick(imagePath) }
    }




    override fun getItemCount(): Int = images.size
}
