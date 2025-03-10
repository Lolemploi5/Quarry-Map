package com.example.quarrymap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: SubsamplingScaleImageView
    private val TAG = "ImageViewerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageView = findViewById(R.id.imageView)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            try {
                // Charge l'image en utilisant SubsamplingScaleImageView
                imageView.setImage(ImageSource.uri(imagePath))
                imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                imageView.setMaxScale(20f) // Permet un zoom jusqu'à 20x
                imageView.setDoubleTapZoomScale(5f) // Double-tap pour zoomer à 5x

            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: $imagePath", e)
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
