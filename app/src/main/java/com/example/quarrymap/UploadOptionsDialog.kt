package com.example.quarrymap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.quarrymap.databinding.DialogUploadOptionsBinding
import

class UploadOptionsDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogUploadOptionsBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(requireContext())
            .setTitle("Choisir une méthode d'importation")
            .setView(binding.root)
            .setNegativeButton("Annuler", null)
            .create().apply {
                binding.btnImportFolder.setOnClickListener {
                    (activity as? MainActivity)?.openFolderPicker()
                    dismiss()
                }
                binding.btnImportJson.setOnClickListener {
                    (activity as? MainActivity)?.openJsonPicker()
                    dismiss()
                }
            }
    }
}
