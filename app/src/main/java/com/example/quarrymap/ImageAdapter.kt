package com.example.quarrymap

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.text.InputType
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.caverock.androidsvg.SVG
import com.example.quarrymap.GlideApp

class ImageAdapter(
    private val images: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    // Liste mutable pour gérer les modifications
    private val mutableImages = images.toMutableList()
    
    // Interface pour notifier l'activité parente du renommage
    interface OnImageRenamedListener {
        fun onImageRenamed(oldPath: String, newPath: String)
    }
    
    // Écouteur pour le renommage d'image
    var onImageRenamedListener: OnImageRenamedListener? = null

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val planchePreview: ImageView = view.findViewById(R.id.planchePreview)
        val plancheName: TextView = view.findViewById(R.id.plancheName)
        val infoText: TextView = view.findViewById(R.id.infoText)
        val renameButton: ImageButton = view.findViewById(R.id.renameButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_planche, parent, false)
        return ImageViewHolder(view)
    }

    // Obtenir une RequestBuilder pour les SVG
    private fun getSvgRequestBuilder(context: View): RequestBuilder<PictureDrawable> {
        return GlideApp.with(context)
            .`as`(PictureDrawable::class.java)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerInside()
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = mutableImages[position]
        val file = File(imagePath)
        
        // Format plus élégant du nom de fichier
        holder.plancheName.text = formatFileName(file.name)
        
        // Afficher les informations du fichier
        val fileInfo = getFileInfo(file, holder.itemView.context)
        holder.infoText.text = fileInfo

        if (!file.exists()) {
            Log.e("ImageAdapter", "ERREUR: Le fichier n'existe pas: $imagePath")
            holder.planchePreview.setImageResource(R.drawable.ic_broken_image)
            return
        }

        val fileSize = file.length()
        Log.d("ImageAdapter", "Fichier: ${file.name}, Taille: $fileSize bytes, Chemin: $imagePath")
        
        if (fileSize <= 0) {
            Log.e("ImageAdapter", "ERREUR: Fichier vide ou inaccessible: ${file.name}")
            holder.planchePreview.setImageResource(R.drawable.ic_broken_image)
            return
        }

        try {
            val extension = file.extension.lowercase()
            Log.d("ImageAdapter", "Extension du fichier: $extension")
            
            val isSvg = extension == "svg"
            val isVector = extension in listOf("xml", "vector")
            val isJpg = extension in listOf("jpg", "jpeg")
            
            if (isJpg) {
                Log.d("ImageAdapter", "Chargement d'une image JPG: ${file.name}")
                
                Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(500, 500)
                    .into(holder.planchePreview)
            } else if (isSvg) {
                try {
                    Log.d("ImageAdapter", "Chargement d'une image SVG: ${file.name}")
                    getSvgRequestBuilder(holder.itemView)
                        .load(Uri.fromFile(file))
                        .into(holder.planchePreview)
                } catch (e: Exception) {
                    Log.w("ImageAdapter", "Erreur avec le module SVG, essai avec le chargement standard", e)
                    Glide.with(holder.itemView.context)
                        .load(Uri.fromFile(file))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.planchePreview)
                }
            } else if (isVector) {
                Log.d("ImageAdapter", "Chargement d'une image vectorielle XML: ${file.name}")
                Glide.with(holder.itemView.context)
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerInside()
                    .into(holder.planchePreview)
            } else {
                Log.d("ImageAdapter", "Chargement d'une image bitmap standard: ${file.name}")
                Glide.with(holder.itemView.context)
                    .asBitmap() 
                    .load(Uri.fromFile(file))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(500, 500)
                    .into(holder.planchePreview)
            }
        } catch (e: Exception) {
            Log.e("ImageAdapter", "Erreur lors du chargement de l'image: ${file.name}", e)
            // Afficher une image par défaut en cas d'erreur
            holder.planchePreview.setImageResource(R.drawable.ic_broken_image)
        }

        // Configuration du bouton de renommage
        holder.renameButton.setOnClickListener {
            // Éviter que le clic sur le bouton ne déclenche aussi le clic sur la carte
            it.isClickable = true
            showRenameDialog(holder.itemView.context, file, position)
        }

        holder.itemView.setOnClickListener { 
            // Animation subtile lors du clic
            it.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    
                    // Appeler le callback après l'animation
                    onItemClick(imagePath)
                }
                .start()
        }
    }

    // Afficher la boîte de dialogue de renommage
    private fun showRenameDialog(context: Context, file: File, position: Int) {
        // Créer une vue personnalisée à partir du layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        
        // Configurer le champ d'édition
        val editText = dialogView.findViewById<EditText>(R.id.edit_text_name)
        editText.setText(file.nameWithoutExtension)
        editText.selectAll()
        
        // Créer l'AlertDialog avec le style personnalisé
        val dialog = AlertDialog.Builder(context, R.style.DarkAlertDialog)
            .setView(dialogView)
            .create()
        
        // Configurer les coins arrondis
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Configurer les boutons
        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val buttonRename = dialogView.findViewById<Button>(R.id.button_rename)
        
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        buttonRename.setOnClickListener {
            val newName = editText.text.toString().trim()
            if (newName.isNotEmpty()) {
                dialog.dismiss()
                renameFile(context, file, newName, position)
            } else {
                Toast.makeText(context, "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Afficher automatiquement le clavier
        editText.post {
            editText.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        
        // Afficher le dialogue
        dialog.show()
    }

    // Renommer le fichier et mettre à jour l'interface
    private fun renameFile(context: Context, file: File, newName: String, position: Int) {
        try {
            val extension = file.extension
            val directory = file.parentFile
            val newFile = File(directory, "$newName.$extension")
            
            // Vérifier si un fichier avec ce nom existe déjà
            if (newFile.exists()) {
                Toast.makeText(context, "Un fichier avec ce nom existe déjà", Toast.LENGTH_SHORT).show()
                return
            }
            
            val oldPath = file.absolutePath
            
            // Renommer le fichier
            val success = file.renameTo(newFile)
            if (success) {
                val newPath = newFile.absolutePath
                
                // Mettre à jour notre liste interne
                mutableImages[position] = newPath
                notifyItemChanged(position)
                
                // Notifier l'activité parente
                onImageRenamedListener?.onImageRenamed(oldPath, newPath)
                
                Log.d("ImageAdapter", "Fichier renommé avec succès: ${file.name} -> ${newFile.name}")
                Toast.makeText(context, "Planche renommée avec succès", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("ImageAdapter", "Échec du renommage: ${file.name}")
                Toast.makeText(context, "Échec du renommage du fichier", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ImageAdapter", "Erreur lors du renommage du fichier", e)
            Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Formatter le nom de fichier pour l'affichage
    private fun formatFileName(fileName: String): String {
        // Remplacer les underscores par des espaces
        return fileName.replace("_", " ")
    }
    
    // Obtenir les informations du fichier formatées
    private fun getFileInfo(file: File, context: android.content.Context): String {
        val fileSize = Formatter.formatFileSize(context, file.length())
        
        // Tenter d'obtenir les dimensions de l'image si c'est une image bitmap
        val dimensions = try {
            if (file.extension.lowercase() in listOf("jpg", "jpeg", "png", "bmp")) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                "${options.outWidth}x${options.outHeight} • "
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
        
        return "$dimensions$fileSize"
    }

    override fun getItemCount(): Int = mutableImages.size
    
    // Permet de mettre à jour complètement la liste
    fun updateData(newImages: List<String>) {
        mutableImages.clear()
        mutableImages.addAll(newImages)
        notifyDataSetChanged()
    }
}
