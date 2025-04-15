package com.example.quarrymap

import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import java.io.File
import android.graphics.Bitmap
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.caverock.androidsvg.SVG
import com.example.quarrymap.GlideApp

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
            .inflate(R.layout.item_planche, parent, false)
        return ImageViewHolder(view)
    }




    // Obtenir une RequestBuilder pour les SVG
    private fun getSvgRequestBuilder(context: View): RequestBuilder<PictureDrawable> {
        return GlideApp.with(context)
            .`as`(PictureDrawable::class.java)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerInside()
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = images[position]
        val file = File(imagePath)
        holder.plancheName.text = file.name

        if (!file.exists()) {
            Log.e("ImageAdapter", "ERREUR: Le fichier n'existe pas: $imagePath")
            holder.planchePreview.setImageResource(R.drawable.ic_broken_image)
            return
        }

        val fileSize = file.length()
        Log.d("ImageAdapter", "Fichier: ${file.name}, Taille: $fileSize bytes, Chemin: $imagePath")
        
        if (fileSize <= 0) {
            Log.e("ImageAdapter", "ERREUR: Fichier vide ou inaccessible: ${file.name}")
            holder.planchePreview.setImageResource(R.drawable.ic_broken_image)
            return
        }

        try {
            val extension = file.extension.lowercase()
            Log.d("ImageAdapter", "Extension du fichier: $extension")
            
            val isSvg = extension == "svg"
            val isVector = extension in listOf("xml", "vector")
            val isJpg = extension in listOf("jpg", "jpeg")
            
            if (isJpg) {
                Log.d("ImageAdapter", "Chargement d'une image JPG: ${file.name}")
                
                Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(500, 500)
                    .into(holder.planchePreview)
            } else if (isSvg) {
                try {
                    Log.d("ImageAdapter", "Chargement d'une image SVG: ${file.name}")
                    getSvgRequestBuilder(holder.itemView)
                        .load(Uri.fromFile(file))
                        .into(holder.planchePreview)
                } catch (e: Exception) {
                    Log.w("ImageAdapter", "Erreur avec le module SVG, essai avec le chargement standard", e)
                    Glide.with(holder.itemView.context)
                        .load(Uri.fromFile(file))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.planchePreview)
                }
            } else if (isVector) {
                Log.d("ImageAdapter", "Chargement d'une image vectorielle XML: ${file.name}")
                Glide.with(holder.itemView.context)
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerInside()
                    .into(holder.planchePreview)
            } else {
                Log.d("ImageAdapter", "Chargement d'une image bitmap standard: ${file.name}")
                Glide.with(holder.itemView.context)
                    .asBitmap() 
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(500, 500)
                    .into(holder.planchePreview)
            }
        } catch (e: Exception) {
            Log.e("ImageAdapter", "Erreur lors du chargement de l'image: ${file.name}", e)
            // Afficher une image par défaut en cas d'erreur
            holder.planchePreview.setImageResource(R.drawable.ic_broken_image)
        }

        holder.itemView.setOnClickListener { onItemClick(imagePath) }
    }




    override fun getItemCount(): Int = images.size
}
