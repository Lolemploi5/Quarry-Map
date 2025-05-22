package com.example.quarrymap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PointBottomSheetDialogFragment : BottomSheetDialogFragment() {
    interface PointActionListener {
        fun onDescriptionChanged(pointId: String, newDescription: String)
        fun onDeletePoint(pointId: String)
        var onNameChanged: ((pointId: String, newName: String) -> Unit)?
    }

    companion object {
        const val ARG_POINT_ID = "arg_point_id"
        const val ARG_POINT_NAME = "arg_point_name"
        const val ARG_POINT_COORDS = "arg_point_coords"
        const val ARG_POINT_DESC = "arg_point_desc"
        const val ARG_POINT_LAT = "arg_point_lat"
        const val ARG_POINT_LNG = "arg_point_lng"

        fun newInstance(pointId: String, name: String, coords: String, description: String?, lat: Double? = null, lng: Double? = null): PointBottomSheetDialogFragment {
            val fragment = PointBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(ARG_POINT_ID, pointId)
            args.putString(ARG_POINT_NAME, name)
            args.putString(ARG_POINT_COORDS, coords)
            args.putString(ARG_POINT_DESC, description)
            if (lat != null) args.putDouble(ARG_POINT_LAT, lat)
            if (lng != null) args.putDouble(ARG_POINT_LNG, lng)
            fragment.arguments = args
            return fragment
        }
    }

    private var listener: PointActionListener? = null

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
                listener?.onDescriptionChanged(pointId, newDesc)
            }
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val newDesc = etDescription.text.toString()
            val newName = if (etPointName.visibility == View.VISIBLE) etPointName.text.toString() else tvPointName.text.toString()
            listener?.onDescriptionChanged(pointId, newDesc)
            listener?.onNameChanged?.invoke(pointId, newName)
            dismiss()
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            listener?.onDeletePoint(pointId)
            dismiss()
        }
    }
}
