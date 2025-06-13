package com.example.quarrymap

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import java.io.File

/**
 * Module Glide personnalisé pour supporter les formats TIFF
 * Version simplifiée qui utilise Android-TiffBitmapFactory
 */
@GlideModule
class MyGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Enregistrer le décodeur TIFF pour les fichiers
        registry.prepend(
            File::class.java,
            Bitmap::class.java,
            TiffDecoder(glide.bitmapPool)
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        // Désactiver l'analyse du manifeste pour de meilleures performances
        return false
    }
}