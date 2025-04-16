package com.example.quarrymap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FavoritesFragment : Fragment(), ImageAdapter.OnFavoriteChangeListener {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: ImageAdapter
    
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
        
        favoriteManager = FavoriteManager(requireContext())
        
        setupRecyclerView()
    }
    
    override fun onResume() {
        super.onResume()
        loadFavorites() // Recharger à chaque affichage
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ImageAdapter(emptyList()) { imagePath ->
            // Ouvrir la visionneuse d'image
            val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
                putExtra("IMAGE_PATH", imagePath)
            }
            startActivity(intent)
        }
        
        // Configurer l'adaptateur avec l'écouteur pour les favoris
        adapter.onFavoriteChangeListener = this
        
        // Configurer l'écouteur pour le renommage
        adapter.onImageRenamedListener = object : ImageAdapter.OnImageRenamedListener {
            override fun onImageRenamed(oldPath: String, newPath: String) {
                favoriteManager.updatePath(oldPath, newPath)
                loadFavorites()
            }
        }
        
        recyclerView.adapter = adapter
    }
    
    private fun loadFavorites() {
        val favorites = favoriteManager.getFavorites().toList()
        
        // Filtrer les chemins qui existent toujours
        val validPaths = favorites.filter { File(it).exists() }
        
        Log.d("FavoritesFragment", "Chargement des favoris: ${validPaths.size} éléments valides")
        
        // Mettre à jour l'affichage
        adapter.updateData(validPaths)
        
        // Afficher ou masquer le message "pas de favoris"
        if (validPaths.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        
        // Nettoyer les favoris qui n'existent plus
        favorites.filter { !File(it).exists() }.forEach {
            favoriteManager.removeFavorite(it)
        }
    }
    
    // Implémentation des méthodes de l'interface OnFavoriteChangeListener
    override fun onFavoriteAdded(imagePath: String) {
        favoriteManager.addFavorite(imagePath)
        loadFavorites()
        Toast.makeText(requireContext(), "Ajouté aux favoris", Toast.LENGTH_SHORT).show()
    }

    override fun onFavoriteRemoved(imagePath: String) {
        favoriteManager.removeFavorite(imagePath)
        loadFavorites()
        Toast.makeText(requireContext(), "Retiré des favoris", Toast.LENGTH_SHORT).show()
    }

    override fun isFavorite(imagePath: String): Boolean {
        return favoriteManager.isFavorite(imagePath)
    }
}
