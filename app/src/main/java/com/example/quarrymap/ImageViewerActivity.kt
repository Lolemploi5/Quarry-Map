package com.example.quarrymap

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: View
    private var imagePath: String? = null

    companion object {
        private const val EXTRA_IMAGE_PATH = "extra_image_path"
        
        fun start(context: Context, imagePath: String) {
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_PATH, imagePath)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        // Configuration de la toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        
        // Récupérer le chemin de l'image depuis l'intent
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            imagePath = intent.getStringExtra("IMAGE_PATH")
        }
        if (imagePath.isNullOrEmpty()) {
            Toast.makeText(this, "Erreur: Aucune image spécifiée", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialiser les vues
        val photoView = findViewById<PhotoView>(R.id.photoView)
        val subsamplingView = findViewById<SubsamplingScaleImageView>(R.id.subsamplingView)
        
        // Configurer le partage d'image
        findViewById<View>(R.id.shareButton).setOnClickListener {
            shareImage()
        }
        
        // Charger l'image
        loadImage(photoView, subsamplingView)
    }
    
    private fun loadImage(photoView: PhotoView, subsamplingView: SubsamplingScaleImageView) {
        val imageFile = File(imagePath!!)
        
        if (!imageFile.exists() || imageFile.length() == 0L) {
            Toast.makeText(this, "Erreur: Fichier image invalide", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            val extension = imageFile.extension.lowercase()
            
            if (extension in listOf("jpg", "jpeg", "png", "bmp", "gif", "webp")) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imageFile.absolutePath, options)
                
                // Déterminer si l'image est de grande taille
                val isLargeImage = options.outWidth > 4000 || options.outHeight > 4000
                
                if (isLargeImage) {
                    // Utiliser SubsamplingScaleImageView pour les grandes images
                    photoView.visibility = View.GONE
                    subsamplingView.visibility = View.VISIBLE
                    imageView = subsamplingView
                    
                    subsamplingView.setImage(ImageSource.uri(imageFile.absolutePath))
                    subsamplingView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                } else {
                    // Utiliser PhotoView pour les images normales
                    photoView.visibility = View.VISIBLE
                    subsamplingView.visibility = View.GONE
                    imageView = photoView
                    
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    photoView.setImageBitmap(bitmap)
                }
            } else if (extension in listOf("tiff", "tif")) {
                // Utiliser PhotoView pour les TIFF avec Glide et décodeur personnalisé
                photoView.visibility = View.VISIBLE
                subsamplingView.visibility = View.GONE
                imageView = photoView
                
                Glide.with(this)
                    .asBitmap()
                    .load(Uri.fromFile(imageFile))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(photoView)
            } else if (extension == "svg") {
                // Utiliser PhotoView pour les SVG
                photoView.visibility = View.VISIBLE
                subsamplingView.visibility = View.GONE
                imageView = photoView
                
                // Charger l'image SVG avec Glide et SVG support
                Glide.with(this)
                    .load(Uri.fromFile(imageFile))
                    .into(photoView)
            } else {
                // Fallback pour les autres types de fichiers
                photoView.visibility = View.VISIBLE
                subsamplingView.visibility = View.GONE
                imageView = photoView
                
                Glide.with(this)
                    .load(Uri.fromFile(imageFile))
                    .into(photoView)
            }
            
            // Définir le titre avec le nom du fichier
            supportActionBar?.title = imageFile.name
            
        } catch (e: Exception) {
            Log.e("ImageViewer", "Erreur lors du chargement de l'image", e)
            Toast.makeText(this, "Erreur lors du chargement de l'image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareImage() {
        imagePath?.let { path ->
            try {
                val imageFile = File(path)
                val uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    imageFile
                )
                
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Partager l'image via"))
            } catch (e: Exception) {
                Log.e("ImageViewer", "Erreur lors du partage de l'image", e)
                Toast.makeText(this, "Erreur lors du partage de l'image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
