package com.example.quarrymap

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import java.io.InputStream
import java.io.IOException

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDiskCacheExecutor(GlideExecutor.newDiskCacheExecutor())
    }
    
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Ajouter le support SVG
        registry
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }
    
    // Désactiver les annotations de manifeste pour les applications utilisant < Glide 4.3.0
    override fun isManifestParsingEnabled(): Boolean = false
}

/**
 * Décodeur pour convertir InputStream en SVG
 */
class SvgDecoder : ResourceDecoder<InputStream, SVG> {
    override fun handles(source: InputStream, options: Options): Boolean = true

    override fun decode(source: InputStream, width: Int, height: Int, options: Options): Resource<SVG> {
        try {
            val svg = SVG.getFromInputStream(source)
            return SimpleResource(svg)
        } catch (e: IOException) {
            throw e
        }
    }
}

/**
 * Transcodeur pour convertir SVG en PictureDrawable
 */
class SvgDrawableTranscoder : ResourceTranscoder<SVG, PictureDrawable> {
    override fun transcode(resource: Resource<SVG>, options: Options): Resource<PictureDrawable> {
        val svg = resource.get()
        val picture = svg.renderToPicture()
        val drawable = PictureDrawable(picture)
        return SimpleResource(drawable)
    }
}
