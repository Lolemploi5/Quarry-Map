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
    }

    companion object {
        const val ARG_POINT_ID = "arg_point_id"
        const val ARG_POINT_NAME = "arg_point_name"
        const val ARG_POINT_COORDS = "arg_point_coords"
        const val ARG_POINT_DESC = "arg_point_desc"

        fun newInstance(pointId: String, name: String, coords: String, description: String?): PointBottomSheetDialogFragment {
            val fragment = PointBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(ARG_POINT_ID, pointId)
            args.putString(ARG_POINT_NAME, name)
            args.putString(ARG_POINT_COORDS, coords)
            args.putString(ARG_POINT_DESC, description)
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

        view.findViewById<TextView>(R.id.tvPointName).text = name
        view.findViewById<TextView>(R.id.tvCoordinates).text = coords
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
            listener?.onDescriptionChanged(pointId, newDesc)
            dismiss()
        }

        view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            listener?.onDeletePoint(pointId)
            dismiss()
        }
    }
}
