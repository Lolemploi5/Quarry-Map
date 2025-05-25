package com.example.quarrymap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.graphics.ImageDecoder
import java.io.FileOutputStream
import java.io.InputStream
import java.io.File
import java.io.FileInputStream
import android.graphics.BitmapFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.constraintlayout.helper.widget.Carousel
import androidx.constraintlayout.motion.widget.MotionLayout
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.ItemTouchHelper

class PointBottomSheetDialogFragment : BottomSheetDialogFragment() {
    interface PointActionListener {
        fun onDescriptionChanged(pointId: String, newDescription: String)
        fun onDeletePoint(pointId: String)
        var onNameChanged: ((pointId: String, newName: String) -> Unit)?
        var onPhotoChanged: ((pointId: String, photoUris: ArrayList<String>) -> Unit)?
    }

    companion object {
        const val ARG_POINT_ID = "arg_point_id"
        const val ARG_POINT_NAME = "arg_point_name"
        const val ARG_POINT_COORDS = "arg_point_coords"
        const val ARG_POINT_DESC = "arg_point_desc"
        const val ARG_POINT_LAT = "arg_point_lat"
        const val ARG_POINT_LNG = "arg_point_lng"

        fun newInstance(pointId: String, name: String, coords: String, description: String?, lat: Double? = null, lng: Double? = null, photoUris: ArrayList<String>? = null): PointBottomSheetDialogFragment {
            val fragment = PointBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(ARG_POINT_ID, pointId)
            args.putString(ARG_POINT_NAME, name)
            args.putString(ARG_POINT_COORDS, coords)
            args.putString(ARG_POINT_DESC, description)
            if (lat != null) args.putDouble(ARG_POINT_LAT, lat)
            if (lng != null) args.putDouble(ARG_POINT_LNG, lng)
            if (photoUris != null) args.putStringArrayList("photoUris", photoUris)
            fragment.arguments = args
            return fragment
        }
    }

    private var listener: PointActionListener? = null
    private var selectedPhotoUri: Uri? = null
    private lateinit var ivPhotoPreview: ImageView
    private var currentPointId: String? = null
    private lateinit var images: MutableList<String>
    private lateinit var imagesAdapter: PointImagesAdapter

    // ActivityResultLauncher pour la sélection d'image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val progressBar = view?.findViewById<View>(R.id.progressImageLoading)
        progressBar?.visibility = View.VISIBLE
        uri?.let {
            // Copier l'image dans le stockage interne
            val context = requireContext().applicationContext
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            val path = file.absolutePath
            images.add(path)
            imagesAdapter.notifyItemInserted(images.size - 1)
            // Appelle le callback pour la persistance
            listener?.onPhotoChanged?.invoke(currentPointId ?: "", ArrayList(images))
            selectedPhotoUri = Uri.fromFile(file)
        }
        progressBar?.visibility = View.GONE
    }

    fun setPointActionListener(listener: PointActionListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_point, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val name = arguments?.getString(ARG_POINT_NAME) ?: ""
        val coords = arguments?.getString(ARG_POINT_COORDS) ?: ""
        val desc = arguments?.getString(ARG_POINT_DESC) ?: ""
        val pointId = arguments?.getString(ARG_POINT_ID) ?: ""
        val lat = arguments?.getDouble(ARG_POINT_LAT)
        val lng = arguments?.getDouble(ARG_POINT_LNG)
        currentPointId = pointId

        val tvPointName = view.findViewById<TextView>(R.id.tvPointName)
        val etPointName = view.findViewById<EditText>(R.id.etPointName)
        val ivEditName = view.findViewById<View>(R.id.ivEditName)
        tvPointName.text = name
        etPointName.setText(name)
        etPointName.visibility = View.GONE
        tvPointName.visibility = View.VISIBLE

        ivEditName.setOnClickListener {
            tvPointName.visibility = View.GONE
            ivEditName.visibility = View.GONE
            etPointName.visibility = View.VISIBLE
            etPointName.requestFocus()
        }

        val tvCoordinates = view.findViewById<TextView>(R.id.tvCoordinates)
        val ivCopyCoords = view.findViewById<View>(R.id.ivCopyCoords)
        tvCoordinates.text = coords
        ivCopyCoords.setOnClickListener {
            val googleMapsCoords = if (lat != null && lng != null) {
                "$lat,$lng"
            } else {
                coords
            }
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Coordonnées Google Maps", googleMapsCoords)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(requireContext(), "Coordonnées copiées au format Google Maps", android.widget.Toast.LENGTH_SHORT).show()
        }

        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        etDescription.setText(desc)

        etDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newDesc = etDescription.text.toString()
                listener?.onDescriptionChanged(currentPointId ?: "", newDesc)
            }
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val newDesc = etDescription.text.toString()
            val newName = if (etPointName.visibility == View.VISIBLE) etPointName.text.toString() else tvPointName.text.toString()
            listener?.onDescriptionChanged(currentPointId ?: "", newDesc)
            listener?.onNameChanged?.invoke(currentPointId ?: "", newName)
            // Sauvegarder le chemin absolu si une photo est sélectionnée
            listener?.onPhotoChanged?.invoke(currentPointId ?: "", ArrayList(images))
            dismiss()
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            listener?.onDeletePoint(currentPointId ?: "")
            dismiss()
        }

        // Carrousel fiable avec RecyclerView horizontal
        val rvImagesCarousel = view.findViewById<RecyclerView>(R.id.rvImagesCarousel)
        images = arguments?.getStringArrayList("photoUris")?.toMutableList() ?: mutableListOf()
        imagesAdapter = PointImagesAdapter(images, onImageClick = { imagePath ->
            if (imagePath.isEmpty()) {
                pickImageLauncher.launch("image/*")
            } else {
                FullscreenImageDialogFragment.newInstance(imagePath).show(parentFragmentManager, "fullscreenImage")
            }
        }, onImageDelete = { position ->
            // Confirmation avant suppression
            view?.let { v ->
                Snackbar.make(v, "Supprimer cette image ?", Snackbar.LENGTH_LONG)
                    .setAction("Oui") {
                        images.removeAt(position)
                        imagesAdapter.notifyItemRemoved(position)
                        listener?.onPhotoChanged?.invoke(currentPointId ?: "", ArrayList(images))
                    }
                    .setActionTextColor(resources.getColor(android.R.color.holo_red_dark))
                    .show()
            }
        })
        rvImagesCarousel.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvImagesCarousel.adapter = imagesAdapter

        // Drag & drop pour réordonner les images
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                // Ne pas permettre de déplacer le bouton d'ajout
                if (from == images.size || to == images.size) return false
                java.util.Collections.swap(images, from, to)
                imagesAdapter.selectedPosition = to
                imagesAdapter.notifyItemMoved(from, to)
                listener?.onPhotoChanged?.invoke(currentPointId ?: "", ArrayList(images))
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true
        })
        itemTouchHelper.attachToRecyclerView(rvImagesCarousel)
    }
}
