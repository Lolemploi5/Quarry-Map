package com.example.quarrymap

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.example.quarrymap.R

class FullscreenImageDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        fun newInstance(imagePath: String): FullscreenImageDialogFragment {
            val fragment = FullscreenImageDialogFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_PATH, imagePath)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_fullscreen_image, container, false)
        val imageView = view.findViewById<ImageView>(R.id.ivFullscreenImage)
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        if (imagePath != null) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(bitmap)
        }
        imageView.setOnClickListener { dismiss() }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        return dialog
    }
}
