package com.example.quarrymap

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File

class PlancheBottomSheetAdapter(
    private var planches: List<File>,
    private val onPlancheSelected: (File) -> Unit
) : RecyclerView.Adapter<PlancheBottomSheetAdapter.PlancheViewHolder>() {

    class PlancheViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.planche_thumbnail)
        val name: TextView = view.findViewById(R.id.planche_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlancheViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_planche_small, parent, false)
        return PlancheViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlancheViewHolder, position: Int) {
        val planche = planches[position]
        holder.name.text = formatFileName(planche.name)

        // Chargement de l'aperçu avec Glide
        Glide.with(holder.itemView.context)
            .load(Uri.fromFile(planche))
            .apply(RequestOptions()
                .centerInside()
                .override(200, 200)
                .placeholder(R.drawable.ic_image))
            .into(holder.thumbnail)

        // Gestion du clic
        holder.itemView.setOnClickListener {
            onPlancheSelected(planche)
        }
    }

    override fun getItemCount() = planches.size

    // Mettre à jour la liste des planches
    fun updatePlanches(newPlanches: List<File>) {
        planches = newPlanches
        notifyDataSetChanged()
    }

    // Formatter le nom de fichier pour l'affichage
    private fun formatFileName(fileName: String): String {
        return fileName.replace("_", " ")
    }
}
