package com.example.quarrymap

import android.content.Context
import android.content.Intent
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
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
    private val TAG = "ImageViewerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)
        vectorImageView = findViewById(R.id.vectorImageView)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            val file = File(imagePath)
            Log.d(TAG, "Tentative de chargement de l'image: $imagePath")
            
            // Vérifier si le fichier existe
            if (!file.exists()) {
                Log.e(TAG, "ERREUR: Le fichier n'existe pas: $imagePath")
                return
            }
            
            // Vérifier la taille du fichier
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
                        // Utiliser notre module SVG pour les fichiers SVG
                        try {
                            Log.d(TAG, "Chargement d'une image SVG avec GlideApp: ${file.name}")
                            GlideApp.with(this)
                                .`as`(PictureDrawable::class.java)
                                .load(Uri.fromFile(file))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(vectorImageView)
                        } catch (e: Exception) {
                            // Fallback en cas d'erreur avec le module SVG
                            Log.w(TAG, "Erreur avec le module SVG, essai avec le chargement standard", e)
                            Glide.with(this)
                                .load(Uri.fromFile(file))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(vectorImageView)
                        }
                    } else {
                        // Pour les fichiers XML vectoriels
                        Log.d(TAG, "Chargement d'une image vectorielle XML: ${file.name}")
                        Glide.with(this)
                            .load(Uri.fromFile(file))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(vectorImageView)
                    }
                } else if (isJpg) {
                    // Traitement spécifique pour les fichiers JPG
                    Log.d(TAG, "Chargement d'une image JPG: ${file.name}")
                    imageView.visibility = View.VISIBLE
                    vectorImageView.visibility = View.GONE
                    
                    try {
                        // Essayer d'abord avec Uri.fromFile
                        Log.d(TAG, "Tentative de chargement JPG avec Uri.fromFile: ${file.name}")
                        imageView.setImage(ImageSource.uri(Uri.fromFile(file).toString()))
                        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                        imageView.setMaxScale(20f)
                        imageView.setDoubleTapZoomScale(5f)
                    } catch (e: Exception) {
                        Log.w(TAG, "Erreur avec Uri.fromFile pour JPG, essai avec le chemin direct", e)
                        // Essayer avec le chemin direct en cas d'échec
                        imageView.setImage(ImageSource.uri(imagePath))
                        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                        imageView.setMaxScale(20f)
                        imageView.setDoubleTapZoomScale(5f)
                    }
                } else {
                    // Pour les autres images bitmap, utiliser SubsamplingScaleImageView
                    Log.d(TAG, "Chargement d'une image bitmap standard: ${file.name}")
                    imageView.visibility = View.VISIBLE
                    vectorImageView.visibility = View.GONE
                    
                    // Charge l'image en utilisant SubsamplingScaleImageView
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
