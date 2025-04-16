package com.example.quarrymap

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import java.io.File
import java.util.Locale

class PlanchesBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlancheBottomSheetAdapter
    private lateinit var addPlancheButton: MaterialButton
    private lateinit var controlsSection: ConstraintLayout
    
    // Contrôles de la planche
    private lateinit var rotationSlider: Slider
    private lateinit var scaleSlider: Slider
    private lateinit var opacitySlider: Slider
    private lateinit var rotationValue: TextView
    private lateinit var scaleValue: TextView
    private lateinit var opacityValue: TextView
    private lateinit var savePositionButton: Button
    private lateinit var removePlancheButton: Button
    private lateinit var closeControlsButton: ImageButton
    
    // Callback pour interagir avec le fragment parent
    private var callback: PlanchesBottomSheetCallback? = null
    
    // Planche actuellement sélectionnée
    private var selectedPlanche: PlancheOverlay? = null

    interface PlanchesBottomSheetCallback {
        fun onPlancheSelected(planche: File)
        fun onPlancheUpdated(planche: PlancheOverlay)
        fun onPlancheRemoved(planche: PlancheOverlay)
        fun onAddPlancheRequested()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Vérifier si le parent implémente l'interface de callback
        if (parentFragment is PlanchesBottomSheetCallback) {
            callback = parentFragment as PlanchesBottomSheetCallback
        } else if (context is PlanchesBottomSheetCallback) {
            callback = context
        } else {
            throw RuntimeException("$context doit implémenter PlanchesBottomSheetCallback")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        // Configurer le bottom sheet pour rester en état étendu (expanded)
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialiser les vues
        recyclerView = view.findViewById(R.id.planches_recycler_view)
        addPlancheButton = view.findViewById(R.id.add_planche_button)
        controlsSection = view.findViewById(R.id.planche_controls_section)
        
        // Initialiser les contrôles
        rotationSlider = view.findViewById(R.id.rotation_slider)
        scaleSlider = view.findViewById(R.id.scale_slider)
        opacitySlider = view.findViewById(R.id.opacity_slider)
        rotationValue = view.findViewById(R.id.rotation_value)
        scaleValue = view.findViewById(R.id.scale_value)
        opacityValue = view.findViewById(R.id.opacity_value)
        savePositionButton = view.findViewById(R.id.save_position_button)
        removePlancheButton = view.findViewById(R.id.remove_planche_button)
        closeControlsButton = view.findViewById(R.id.close_controls_button)
        
        // Configurer le RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = PlancheBottomSheetAdapter(emptyList()) { plancheFile ->
            callback?.onPlancheSelected(plancheFile)
        }
        recyclerView.adapter = adapter
        
        // Configurer le bouton d'ajout
        addPlancheButton.setOnClickListener {
            callback?.onAddPlancheRequested()
        }
        
        // Configurer les écouteurs de changement pour les sliders
        setupSliderListeners()
        
        // Configurer les boutons d'action
        setupActionButtons()
        
        // Charger la liste des planches
        loadPlanches()
    }

    private fun setupSliderListeners() {
        rotationSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                rotationValue.text = String.format(Locale.getDefault(), "%.0f°", value)
                selectedPlanche?.let {
                    it.rotation = value
                    callback?.onPlancheUpdated(it)
                }
            }
        }
        
        scaleSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                scaleValue.text = String.format(Locale.getDefault(), "%d%%", (value * 100).toInt())
                selectedPlanche?.let {
                    it.scale = value
                    callback?.onPlancheUpdated(it)
                }
            }
        }
        
        opacitySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                opacityValue.text = String.format(Locale.getDefault(), "%d%%", (value * 100).toInt())
                selectedPlanche?.let {
                    it.opacity = value
                    callback?.onPlancheUpdated(it)
                }
            }
        }
    }
    
    private fun setupActionButtons() {
        savePositionButton.setOnClickListener {
            // L'enregistrement est automatique via les callbacks onPlancheUpdated
            // On peut donc simplement cacher les contrôles
            controlsSection.visibility = View.GONE
        }
        
        removePlancheButton.setOnClickListener {
            selectedPlanche?.let {
                callback?.onPlancheRemoved(it)
                selectedPlanche = null
                controlsSection.visibility = View.GONE
            }
        }
        
        closeControlsButton.setOnClickListener {
            controlsSection.visibility = View.GONE
        }
    }
    
    // Charger la liste des planches disponibles
    private fun loadPlanches() {
        val context = context ?: return
        
        // Obtenir le chemin de base des téléchargements (ajuster selon votre app)
        val basePath = (activity as? MainActivity)?.getDownloadPath() 
            ?: "${context.getExternalFilesDir(null)?.absolutePath}/plans_triés"
        
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
            return
        }
        
        // Récupérer toutes les planches récursivement
        val planches = mutableListOf<File>()
        
        // Fonction récursive pour parcourir les dossiers
        fun scanDirectory(dir: File) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    scanDirectory(file)
                } else if (isPlancheFile(file)) {
                    planches.add(file)
                }
            }
        }
        
        scanDirectory(baseDir)
        
        // Mettre à jour l'adapter
        adapter.updatePlanches(planches)
    }
    
    // Vérifier si le fichier est une planche (image)
    private fun isPlancheFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("jpg", "jpeg", "png", "svg", "webp", "gif")
    }
    
    // Afficher les contrôles pour une planche sélectionnée
    fun showControlsForPlanche(planche: PlancheOverlay) {
        selectedPlanche = planche
        
        // Mettre à jour les valeurs des sliders
        rotationSlider.value = planche.rotation
        scaleSlider.value = planche.scale
        opacitySlider.value = planche.opacity
        
        // Mettre à jour les textes des valeurs
        rotationValue.text = String.format(Locale.getDefault(), "%.0f°", planche.rotation)
        scaleValue.text = String.format(Locale.getDefault(), "%d%%", (planche.scale * 100).toInt())
        opacityValue.text = String.format(Locale.getDefault(), "%d%%", (planche.opacity * 100).toInt())
        
        // Afficher la section des contrôles
        controlsSection.visibility = View.VISIBLE
    }
    
    companion object {
        const val TAG = "PlanchesBottomSheetFragment"
        
        fun newInstance(): PlanchesBottomSheetFragment {
            return PlanchesBottomSheetFragment()
        }
    }
}
