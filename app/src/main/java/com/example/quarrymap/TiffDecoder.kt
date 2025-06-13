package com.example.quarrymap

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import org.beyka.tiffbitmapfactory.TiffBitmapFactory
import java.io.File
import java.io.InputStream

/**
 * Décodeur TIFF personnalisé pour Glide utilisant Android-TiffBitmapFactory
 * Cette implémentation utilise des fichiers plutôt que des streams pour plus de compatibilité
 */
class TiffDecoder(private val bitmapPool: com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool) : ResourceDecoder<File, Bitmap> {

    override fun handles(source: File, options: Options): Boolean {
        // Vérifier si c'est un fichier TIFF
        val extension = source.extension.lowercase()
        return extension == "tiff" || extension == "tif"
    }

    override fun decode(source: File, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        return try {
            // Configurer les options de décodage TIFF
            val tiffOptions = TiffBitmapFactory.Options().apply {
                // Optimiser pour la taille si des dimensions cibles sont spécifiées
                if (width > 0 && height > 0) {
                    inJustDecodeBounds = true
                    TiffBitmapFactory.decodeFile(source, this)
                    
                    // Calculer le facteur d'échantillonnage
                    inSampleSize = calculateInSampleSize(outWidth, outHeight, width, height)
                    inJustDecodeBounds = false
                }
                
                // Configuration pour optimiser la mémoire
                inPreferredConfig = TiffBitmapFactory.ImageConfig.RGB_565
            }

            // Décoder l'image TIFF
            val bitmap = TiffBitmapFactory.decodeFile(source, tiffOptions)
            
            if (bitmap != null) {
                BitmapResource.obtain(bitmap, bitmapPool)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("TiffDecoder", "Erreur lors du décodage TIFF", e)
            null
        }
    }

    /**
     * Calcule le facteur d'échantillonnage optimal pour réduire l'utilisation mémoire
     */
    private fun calculateInSampleSize(imageWidth: Int, imageHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        
        if (imageHeight > reqHeight || imageWidth > reqWidth) {
            val halfHeight = imageHeight / 2
            val halfWidth = imageWidth / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}