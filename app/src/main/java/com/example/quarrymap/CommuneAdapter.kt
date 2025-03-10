package com.example.quarrymap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CommuneAdapter(
    private val communes: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CommuneAdapter.CommuneViewHolder>() {

    inner class CommuneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val communeName: TextView = itemView.findViewById(R.id.communeName)

        fun bind(commune: String) {
            // Ajout de l'emoji 📌 devant le nom de chaque commune
            communeName.text = "$commune"
            itemView.setOnClickListener { onClick(commune) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommuneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_commune, parent, false)
        return CommuneViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommuneViewHolder, position: Int) {
        holder.bind(communes[position])
    }

    override fun getItemCount() = communes.size
}
