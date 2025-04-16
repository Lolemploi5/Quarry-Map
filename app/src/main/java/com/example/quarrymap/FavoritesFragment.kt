package com.example.quarrymap

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FavoritesFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var prefs: SharedPreferences
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.favoritesRecyclerView)
        emptyText = view.findViewById(R.id.emptyFavoritesText)
        
        prefs = requireContext().getSharedPreferences("favorites_prefs", android.content.Context.MODE_PRIVATE)
        
        setupRecyclerView()
        loadFavorites()
    }
    
    override fun onResume() {
        super.onResume()
        loadFavorites() // Recharger à chaque affichage
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ImageAdapter(emptyList()) { imagePath ->
            // Ouvrir la visionneuse d'image
            val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
                putExtra("IMAGE_PATH", imagePath)
            }
            startActivity(intent)
        }
    }
    
    private fun loadFavorites() {
        val favoritesSet = prefs.getStringSet("favorite_images", mutableSetOf()) ?: mutableSetOf()
        
        // Filtrer les chemins qui existent toujours
        val validPaths = favoritesSet.filter { File(it).exists() }
        
        // Mettre à jour l'affichage
        (recyclerView.adapter as? ImageAdapter)?.updateData(validPaths)
        
        // Afficher ou masquer le message "pas de favoris"
        if (validPaths.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        
        // Nettoyer les favoris qui n'existent plus
        if (favoritesSet.size != validPaths.size) {
            val newFavoritesSet = validPaths.toMutableSet()
            prefs.edit().putStringSet("favorite_images", newFavoritesSet).apply()
        }
    }
}
