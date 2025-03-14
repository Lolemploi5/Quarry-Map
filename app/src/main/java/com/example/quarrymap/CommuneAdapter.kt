package com.example.quarrymap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CommuneAdapter(
    private var communes: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CommuneAdapter.CommuneViewHolder>() {
    
    private var filteredCommunes: List<String> = communes
    
    fun updateData(newCommunes: List<String>) {
        communes = newCommunes
        filteredCommunes = newCommunes
        notifyDataSetChanged()
    }
    
    fun filter(query: String) {
        filteredCommunes = if (query.isEmpty()) {
            communes
        } else {
            communes.filter {
                it.lowercase().contains(query.lowercase())
            }
        }
        notifyDataSetChanged()
    }

    inner class CommuneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val communeName: TextView = itemView.findViewById(R.id.communeName)

        fun bind(commune: String) {
            communeName.text = "$commune"
            itemView.setOnClickListener { onClick(commune) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommuneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_commune, parent, false)
        return CommuneViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommuneViewHolder, position: Int) {
        holder.bind(filteredCommunes[position])
    }

    override fun getItemCount() = filteredCommunes.size
}
