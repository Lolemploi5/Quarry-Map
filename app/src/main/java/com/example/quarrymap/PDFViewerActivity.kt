package com.example.quarrymap

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/**
 * Activité pour visualiser les fichiers PDF avec une interface moderne identique au visualiseur d'images
 */
class PDFViewerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PDFViewerActivity"
        private const val EXTRA_PDF_PATH = "pdf_path"

        fun start(context: Context, pdfPath: String) {
            val intent = Intent(context, PDFViewerActivity::class.java).apply {
                putExtra(EXTRA_PDF_PATH, pdfPath)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var photoViewPdf: PhotoView
    private lateinit var progressBar: ProgressBar
    private lateinit var textCurrentPage: TextView
    private lateinit var shareButton: FloatingActionButton
    private lateinit var buttonPrevious: android.widget.ImageButton
    private lateinit var buttonNext: android.widget.ImageButton

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0
    private var pageCount = 0
    private var pdfPath: String? = null
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        // Configuration de la toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        // Initialisation des vues
        initViews()

        // Récupération du chemin du PDF
        pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (pdfPath == null) {
            Toast.makeText(this, "Erreur: chemin du PDF manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Charger et afficher le PDF
        loadPdf(pdfPath!!)
    }

    private fun initViews() {
        photoViewPdf = findViewById(R.id.photoViewPdf)
        progressBar = findViewById(R.id.progressBar)
        textCurrentPage = findViewById(R.id.textCurrentPage)
        shareButton = findViewById(R.id.shareButton)
        buttonPrevious = findViewById(R.id.buttonPrevious)
        buttonNext = findViewById(R.id.buttonNext)

        // Configuration du bouton de partage
        shareButton.setOnClickListener { sharePdf() }
        
        // Configuration des boutons de navigation
        buttonPrevious.setOnClickListener { showPreviousPage() }
        buttonNext.setOnClickListener { showNextPage() }

        // Configuration du PhotoView pour le zoom
        photoViewPdf.maximumScale = 5.0f
        photoViewPdf.mediumScale = 2.5f
        photoViewPdf.minimumScale = 0.5f
        
        // Configuration des gestes pour la navigation
        setupGestureDetector()
    }
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                return if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            showPreviousPage()
                        } else {
                            showNextPage()
                        }
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        })
        
        // Configuration pour laisser PhotoView gérer le zoom nativement
        // Ne pas appliquer de TouchListener pour éviter les conflits
        Log.d(TAG, "PhotoView configuré pour zoom libre : ${photoViewPdf.minimumScale}x à ${photoViewPdf.maximumScale}x")
        
        // Alternative : utiliser des boutons de navigation visibles sur demande si nécessaire
        // Pour l'instant, privilégier le zoom libre sans swipe de navigation
        
        Log.d(TAG, "PhotoView configuré pour zoom : ${photoViewPdf.minimumScale}x à ${photoViewPdf.maximumScale}x")
    }

    private fun loadPdf(path: String) {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                
                withContext(Dispatchers.IO) {
                    val file = File(path)
                    if (!file.exists()) {
                        throw Exception("Le fichier PDF n'existe pas")
                    }

                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    pdfRenderer = PdfRenderer(fileDescriptor)
                    pageCount = pdfRenderer?.pageCount ?: 0
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (pageCount > 0) {
                        supportActionBar?.title = "PDF (${pageCount} pages)"
                        updatePageInfo()
                        showPage(0)
                    } else {
                        Toast.makeText(this@PDFViewerActivity, "Aucune page trouvée dans le PDF", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Erreur lors du chargement du PDF", e)
                    Toast.makeText(this@PDFViewerActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun showPage(index: Int) {
        if (pdfRenderer == null || index < 0 || index >= pageCount) {
            return
        }

        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                val bitmap = withContext(Dispatchers.IO) {
                    currentPage?.close()
                    currentPage = pdfRenderer?.openPage(index)
                    
                    currentPage?.let { page ->
                        val scaleFactor = 2.0f
                        val bitmapWidth = (page.width * scaleFactor).toInt()
                        val bitmapHeight = (page.height * scaleFactor).toInt()
                        
                        val maxDimension = 3000
                        val finalWidth: Int
                        val finalHeight: Int
                        
                        if (bitmapWidth > maxDimension || bitmapHeight > maxDimension) {
                            val ratio = page.width.toFloat() / page.height.toFloat()
                            if (bitmapWidth > bitmapHeight) {
                                finalWidth = maxDimension
                                finalHeight = (maxDimension / ratio).toInt()
                            } else {
                                finalHeight = maxDimension
                                finalWidth = (maxDimension * ratio).toInt()
                            }
                        } else {
                            finalWidth = bitmapWidth
                            finalHeight = bitmapHeight
                        }
                        
                        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (bitmap != null) {
                        photoViewPdf.setImageBitmap(bitmap)
                        photoViewPdf.setScale(1.0f, true)
                        currentPageIndex = index
                        updatePageInfo()
                    } else {
                        Toast.makeText(this@PDFViewerActivity, "Erreur: impossible d'afficher la page", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Erreur lors de l'affichage de la page", e)
                    Toast.makeText(this@PDFViewerActivity, "Erreur lors de l'affichage", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updatePageInfo() {
        textCurrentPage.text = "Page ${currentPageIndex + 1} sur $pageCount"
        
        // Mettre à jour la visibilité des boutons de navigation
        buttonPrevious.visibility = if (currentPageIndex > 0) View.VISIBLE else View.GONE
        buttonNext.visibility = if (currentPageIndex < pageCount - 1) View.VISIBLE else View.GONE
        
        if (currentPageIndex == 0 && pageCount > 1) {
            photoViewPdf.postDelayed({ 
                Toast.makeText(this, "💡 Utilisez les boutons ou zoomez librement", Toast.LENGTH_LONG).show()
            }, 1000)
        }
    }

    private fun showPreviousPage() {
        if (currentPageIndex > 0) {
            showPage(currentPageIndex - 1)
        } else {
            Toast.makeText(this, "Déjà à la première page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNextPage() {
        if (currentPageIndex < pageCount - 1) {
            showPage(currentPageIndex + 1)
        } else {
            Toast.makeText(this, "Déjà à la dernière page", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf() {
        pdfPath?.let { path ->
            try {
                val file = File(path)
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Partager le PDF"))
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du partage", e)
                Toast.makeText(this, "Erreur lors du partage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPage?.close()
        pdfRenderer?.close()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
