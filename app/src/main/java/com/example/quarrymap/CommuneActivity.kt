package com.example.quarrymap

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class CommuneActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private val images = mutableListOf<String>()
    private val TAG = "CommuneActivity"
    private lateinit var favoriteManager: FavoriteManager

    companion object {
        const val EXTRA_COMMUNE = "COMMUNE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commune)

        val communeName = intent.getStringExtra(EXTRA_COMMUNE)
        if (communeName == null) {
            Log.e(TAG, "Aucun nom de commune fourni")
            Toast.makeText(this, "Erreur: Aucun nom de commune fourni", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Récupérer le chemin de base personnalisé, s'il est fourni
        val basePath = intent.getStringExtra("EXTRA_BASE_PATH")
        
        title = communeName

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // Charger les images de la commune en utilisant le chemin spécifié
        loadImagesForCommune(communeName, basePath)
        
        adapter = ImageAdapter(images) { imagePath ->
            ImageViewerActivity.start(this, imagePath)
        }
        recyclerView.adapter = adapter

        favoriteManager = FavoriteManager(this)
        
        // Configurer l'adaptateur avec les écouteurs
        adapter.onFavoriteChangeListener = object : ImageAdapter.OnFavoriteChangeListener {
            override fun onFavoriteAdded(imagePath: String) {
                favoriteManager.addFavorite(imagePath)
                Toast.makeText(this@CommuneActivity, "Ajouté aux favoris", Toast.LENGTH_SHORT).show()
            }

            override fun onFavoriteRemoved(imagePath: String) {
                favoriteManager.removeFavorite(imagePath)
                Toast.makeText(this@CommuneActivity, "Retiré des favoris", Toast.LENGTH_SHORT).show()
            }

            override fun isFavorite(imagePath: String): Boolean {
                return favoriteManager.isFavorite(imagePath)
            }
        }

        // Définir l'écouteur de renommage
        adapter.onImageRenamedListener = object : ImageAdapter.OnImageRenamedListener {
            override fun onImageRenamed(oldPath: String, newPath: String) {
                // Mettre à jour la liste des images si nécessaire
                val index = images.indexOf(oldPath)
                if (index != -1) {
                    images[index] = newPath
                }
                
                // Si vous avez un cache ou autre chose à mettre à jour
                // updateCache(oldPath, newPath)
                
                Log.d("CommuneActivity", "Image renommée: $oldPath -> $newPath")
            }
        }
    }
    
    private fun loadImagesForCommune(communeName: String, customBasePath: String? = null) {
        try {
            val baseFolder = if (customBasePath != null) {
                File(customBasePath, communeName)
            } else {
                File(getExternalFilesDir(null), "plans_triés/$communeName")
            }
                
            Log.d(TAG, "Recherche d'images dans: ${baseFolder.absolutePath}")
            
            if (baseFolder.exists() && baseFolder.isDirectory) {
                val imageFiles = baseFolder.listFiles()
                
                if (imageFiles != null && imageFiles.isNotEmpty()) {
                    // Lister tous les fichiers pour le débogage
                    Log.d(TAG, "Fichiers trouvés dans le dossier:")
                    imageFiles.forEach { file ->
                        Log.d(TAG, "- ${file.name} (${file.extension}, ${file.length()} bytes, isFile=${file.isFile})")
                    }
                    
                    val supportedExtensions = listOf(
                        "jpg", "jpeg", "png", "gif", "bmp", "webp",  // Formats bitmap
                        "svg", "xml", "vector"                          // Formats vectoriels
                    )
                    
                    val jpgFiles = imageFiles.filter { it.isFile && (it.extension.lowercase() == "jpg" || it.extension.lowercase() == "jpeg") }
                    Log.d(TAG, "Fichiers JPG trouvés: ${jpgFiles.size}")
                    jpgFiles.forEach { file ->
                        Log.d(TAG, "  - JPG: ${file.name} (${file.absolutePath})")
                    }
                    
                    val filteredImages = imageFiles
                        .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
                        .map { it.absolutePath }
                    
                    images.addAll(filteredImages)
                    Log.d(TAG, "${filteredImages.size} images trouvées")
                    
                    val extensions = imageFiles.filter { it.isFile }.map { it.extension.lowercase() }.distinct()
                    Log.d(TAG, "Extensions trouvées: $extensions")
                    
                    if (filteredImages.isEmpty()) {
                        Toast.makeText(this, "Aucune image trouvée pour $communeName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Aucune image trouvée pour $communeName")
                    Toast.makeText(this, "Aucune image trouvée pour $communeName", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "Le dossier pour $communeName n'existe pas: ${baseFolder.absolutePath}")
                Toast.makeText(this, "Aucun dossier trouvé pour $communeName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement des images", e)
            Toast.makeText(this, "Erreur lors du chargement des images: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
