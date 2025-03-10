package com.example.quarrymap

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
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
        
        // Animation d'entrée pour les cartes
        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        binding.buttonImportJson.startAnimation(fadeIn)
        binding.buttonImportFolder.startAnimation(fadeIn)
        
        // Configurer les boutons
        binding.buttonSetPath.setOnClickListener {
            showPathDialog(binding)
        }
        
        // Configurer les cartes d'importation
        setupImportCards(binding)
        
        return MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
            .setView(binding.root)
            .create().apply {
                window?.setWindowAnimations(R.style.DialogAnimation)
            }
    }
    
    private fun showPathDialog(binding: DialogUploadOptionsBinding) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_path, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayout)
        textInputLayout.editText?.setText(binding.editTextDownloadPath.text)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Définir l'emplacement des plans")
            .setView(dialogView)
            .setPositiveButton("Confirmer") { _, _ ->
                val newPath = textInputLayout.editText?.text.toString().trim()
                if (newPath.isNotEmpty()) {
                    binding.editTextDownloadPath.setText(newPath)
                    customDownloadPath = newPath
                    // Vérifier si le chemin existe et est accessible
                    val directory = File(newPath)
                    if (!directory.exists()) {
                        val success = directory.mkdirs()
                        if (!success) {
                            showErrorToast("Impossible de créer le dossier. Vérifiez les permissions.")
                        }
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
    
    private fun setupImportCards(binding: DialogUploadOptionsBinding) {
        binding.buttonImportFolder.setOnClickListener { view ->
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            saveDownloadPath(binding.editTextDownloadPath.text.toString())
                            (activity as? MainActivity)?.openFolderPicker()
                            dismiss()
                        }
                        .start()
                }
                .start()
        }
        
        binding.buttonImportJson.setOnClickListener { view ->
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            saveDownloadPath(binding.editTextDownloadPath.text.toString())
                            (activity as? MainActivity)?.openJsonPicker()
                            dismiss()
                        }
                        .start()
                }
                .start()
        }
    }
    
    private fun showErrorToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    private fun saveDownloadPath(path: String) {
        if (path.isNotEmpty()) {
            customDownloadPath = path
            (activity as? MainActivity)?.setCustomDownloadPath(path)
        }
    }
}
