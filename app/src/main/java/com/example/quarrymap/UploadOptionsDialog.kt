package com.example.quarrymap

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.quarrymap.databinding.DialogUploadOptionsBinding
import java.io.File

class UploadOptionsDialog : DialogFragment() {
    private var customDownloadPath: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogUploadOptionsBinding.inflate(LayoutInflater.from(context))
        val context = requireContext()
        
        // Définir le chemin de téléchargement par défaut
        val defaultPath = (activity as? MainActivity)?.getExternalFilesDir(null)?.absolutePath + "/plans_triés"
        binding.editTextDownloadPath.setText(defaultPath)
        
        // Configurer les boutons
        binding.buttonSetPath.setOnClickListener {
            val pathEditText = EditText(context).apply {
                setText(binding.editTextDownloadPath.text)
                hint = "Chemin de téléchargement"
                setSingleLine()
            }
            
            AlertDialog.Builder(context)
                .setTitle("Définir le chemin de téléchargement")
                .setView(pathEditText)
                .setPositiveButton("Confirmer") { _, _ ->
                    val newPath = pathEditText.text.toString().trim()
                    if (newPath.isNotEmpty()) {
                        binding.editTextDownloadPath.setText(newPath)
                        customDownloadPath = newPath
                        // Vérifier si le chemin existe et est accessible
                        val directory = File(newPath)
                        if (!directory.exists()) {
                            val success = directory.mkdirs()
                            if (!success) {
                                Toast.makeText(context, "Impossible de créer le dossier. Vérifiez les permissions.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
        
        return AlertDialog.Builder(context)
            .setTitle("Options d'importation")
            .setView(binding.root)
            .setPositiveButton("Fermer", null)
            .create().apply {
                setOnShowListener { dialog ->
                    binding.buttonImportFolder.setOnClickListener {
                        saveDownloadPath(binding.editTextDownloadPath.text.toString())
                        (activity as? MainActivity)?.openFolderPicker()
                        dialog.dismiss()
                    }
                    binding.buttonImportJson.setOnClickListener {
                        saveDownloadPath(binding.editTextDownloadPath.text.toString())
                        (activity as? MainActivity)?.openJsonPicker()
                        dialog.dismiss()
                    }
                }
            }
    }
    
    private fun saveDownloadPath(path: String) {
        if (path.isNotEmpty()) {
            customDownloadPath = path
            (activity as? MainActivity)?.setCustomDownloadPath(path)
        }
    }
}
