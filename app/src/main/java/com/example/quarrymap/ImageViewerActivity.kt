package com.example.quarrymap

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.quarrymap.GlideApp
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: SubsamplingScaleImageView
    private lateinit var vectorImageView: ImageView
    private lateinit var prefs: SharedPreferences
    private var currentImagePath: String? = null
    private val TAG = "ImageViewerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)
        vectorImageView = findViewById(R.id.vectorImageView)

        prefs = getSharedPreferences("favorites_prefs", MODE_PRIVATE)
        currentImagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            val file = File(imagePath)
            Log.d(TAG, "Tentative de chargement de l'image: $imagePath")
            
            if (!file.exists()) {
                Log.e(TAG, "ERREUR: Le fichier n'existe pas: $imagePath")
                return
            }
            
            val fileSize = file.length()
            Log.d(TAG, "Fichier: ${file.name}, Taille: $fileSize bytes, Chemin: $imagePath")
            
            if (fileSize <= 0) {
                Log.e(TAG, "ERREUR: Fichier vide ou inaccessible: ${file.name}")
                return
            }
            
            val extension = file.extension.lowercase()
            Log.d(TAG, "Extension du fichier: $extension")
            
            val isSvg = extension == "svg"
            val isVector = extension in listOf("xml", "vector")
            val isJpg = extension in listOf("jpg", "jpeg")
            
            Log.d(TAG, "Type de fichier détecté: isSvg=$isSvg, isVector=$isVector, isJpg=$isJpg")
            
            try {
                if (isSvg || isVector) {
                    // Pour les images vectorielles, utiliser ImageView avec Glide
                    Log.d(TAG, "Utilisation du mode vectoriel pour: ${file.name}")
                    imageView.visibility = View.GONE
                    vectorImageView.visibility = View.VISIBLE
                    
                    if (isSvg) {
                        try {
                            Log.d(TAG, "Chargement d'une image SVG avec GlideApp: ${file.name}")
                            GlideApp.with(this)
                                .`as`(PictureDrawable::class.java)
                                .load(Uri.fromFile(file))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(vectorImageView)
                        } catch (e: Exception) {
                            Log.w(TAG, "Erreur avec le module SVG, essai avec le chargement standard", e)
                            Glide.with(this)
                                .load(Uri.fromFile(file))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(vectorImageView)
                        }
                    } else {
                        Log.d(TAG, "Chargement d'une image vectorielle XML: ${file.name}")
                        Glide.with(this)
                            .load(Uri.fromFile(file))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(vectorImageView)
                    }
                } else if (isJpg) {
                    Log.d(TAG, "Chargement d'une image JPG: ${file.name}")
                    imageView.visibility = View.VISIBLE
                    vectorImageView.visibility = View.GONE
                    
                    try {
                        Log.d(TAG, "Tentative de chargement JPG avec Uri.fromFile: ${file.name}")
                        imageView.setImage(ImageSource.uri(Uri.fromFile(file).toString()))
                        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                        imageView.setMaxScale(20f)
                        imageView.setDoubleTapZoomScale(5f)
                    } catch (e: Exception) {
                        Log.w(TAG, "Erreur avec Uri.fromFile pour JPG, essai avec le chemin direct", e)
                        imageView.setImage(ImageSource.uri(imagePath))
                        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                        imageView.setMaxScale(20f)
                        imageView.setDoubleTapZoomScale(5f)
                    }
                } else {
                    Log.d(TAG, "Chargement d'une image bitmap standard: ${file.name}")
                    imageView.visibility = View.VISIBLE
                    vectorImageView.visibility = View.GONE
                    
                    imageView.setImage(ImageSource.uri(imagePath))
                    imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    imageView.setMaxScale(20f) // Permet un zoom jusqu'à 20x
                    imageView.setDoubleTapZoomScale(5f) // Double-tap pour zoomer à 5x
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement de l'image: $imagePath", e)
                e.printStackTrace() // Afficher la stack trace complète
            }
        } else {
            Log.e(TAG, "Image path is null")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_image_viewer, menu)
        
        // Mettre à jour l'icône du favori en fonction de l'état
        updateFavoriteIcon(menu.findItem(R.id.menu_favorite))
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_favorite -> {
                toggleFavorite()
                updateFavoriteIcon(item)
                true
            }
            // ...autres options...
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun toggleFavorite() {
        val path = currentImagePath ?: return
        
        val favorites = prefs.getStringSet("favorite_images", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        if (favorites.contains(path)) {
            favorites.remove(path)
            Toast.makeText(this, "Retiré des favoris", Toast.LENGTH_SHORT).show()
        } else {
            favorites.add(path)
            Toast.makeText(this, "Ajouté aux favoris", Toast.LENGTH_SHORT).show()
        }
        
        prefs.edit().putStringSet("favorite_images", favorites).apply()
    }
    
    private fun updateFavoriteIcon(item: MenuItem) {
        val path = currentImagePath ?: return
        val favorites = prefs.getStringSet("favorite_images", mutableSetOf()) ?: mutableSetOf()
        
        if (favorites.contains(path)) {
            item.setIcon(R.drawable.ic_favorite_filled)
        } else {
            item.setIcon(R.drawable.ic_favorite_border)
        }
    }

    companion object {
        private const val EXTRA_IMAGE_PATH = "IMAGE_PATH"

        fun start(context: Context, imagePath: String) {
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_PATH, imagePath)
            }
            context.startActivity(intent)
        }
    }
}
