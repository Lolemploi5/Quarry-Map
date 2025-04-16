package com.example.quarrymap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FavoritesFragment : Fragment(), ImageAdapter.OnFavoriteChangeListener {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var searchView: SearchView
    private lateinit var searchCard: CardView
    private lateinit var favoriteManager: FavoriteManager
    private lateinit var adapter: ImageAdapter
    
    // Liste complète des favoris pour la recherche
    private var allFavorites: List<String> = emptyList()
    
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
        searchView = view.findViewById(R.id.searchViewFavorites)
        searchCard = view.findViewById(R.id.searchCardFavorites)
        
        favoriteManager = FavoriteManager(requireContext())
        
        setupRecyclerView()
        setupSearchView()
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
    
    private fun setupSearchView() {
        // Rendre toute la zone de recherche cliquable, pas juste la carte
        searchView.setOnClickListener {
            searchView.isIconified = false
            searchView.requestFocus()
            
            // Afficher le clavier virtuel
            val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
        }
        
        // Configurer l'écouteur pour la recherche
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterFavorites(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterFavorites(newText)
                return true
            }
        })
    }
    
    private fun filterFavorites(query: String?) {
        if (query.isNullOrEmpty()) {
            // Si la requête est vide, afficher tous les favoris
            adapter.updateData(allFavorites)
        } else {
            // Filtrer les favoris qui contiennent la requête (insensible à la casse)
            val filteredFavorites = allFavorites.filter { 
                File(it).name.lowercase().contains(query.lowercase()) 
            }
            adapter.updateData(filteredFavorites)
            
            // Mettre à jour la visibilité du texte "vide" si nécessaire
            if (filteredFavorites.isEmpty() && allFavorites.isNotEmpty()) {
                emptyText.text = "Aucun résultat pour \"$query\""
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun loadFavorites() {
        val favorites = favoriteManager.getFavorites().toList()
        
        // Filtrer les chemins qui existent toujours
        val validPaths = favorites.filter { File(it).exists() }
        
        Log.d("FavoritesFragment", "Chargement des favoris: ${validPaths.size} éléments valides")
        
        // Sauvegarder la liste complète pour la recherche
        allFavorites = validPaths
        
        // Appliquer le filtre actuel s'il y en a un
        val currentQuery = searchView.query?.toString()
        if (!currentQuery.isNullOrEmpty()) {
            filterFavorites(currentQuery)
        } else {
            // Sinon, mettre à jour l'affichage avec tous les favoris
            adapter.updateData(validPaths)
            
            // Afficher ou masquer le message "pas de favoris"
            if (validPaths.isEmpty()) {
                emptyText.text = "Vous n'avez pas encore de planches favorites.\nMarquez des planches comme favorites pour les retrouver ici."
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
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
