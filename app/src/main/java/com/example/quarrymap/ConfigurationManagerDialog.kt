package com.example.quarrymap

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialogue pour gérer les configurations d'ancrage sauvegardées
 */
class ConfigurationManagerDialog : DialogFragment() {
    
    interface ConfigurationManagerCallback {
        fun onConfigurationSelected(fileName: String)
        fun onConfigurationSaved(name: String)
    }
    
    private var callback: ConfigurationManagerCallback? = null
    private lateinit var configurationManager: ConfigurationManager
    private lateinit var configurationsAdapter: ConfigurationsAdapter
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        configurationManager = ConfigurationManager(context)
        if (parentFragment is ConfigurationManagerCallback) {
            callback = parentFragment as ConfigurationManagerCallback
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_configuration_manager, null)
        
        setupViews(view)
        
        return MaterialAlertDialogBuilder(context)
            .setTitle("Gestionnaire de configurations")
            .setView(view)
            .setNegativeButton("Fermer") { _, _ -> dismiss() }
            .create()
    }
    
    private fun setupViews(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.configurations_recycler)
        val saveNameEdit = view.findViewById<EditText>(R.id.save_name_edit)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        
        // Configuration du RecyclerView
        configurationsAdapter = ConfigurationsAdapter { configuration ->
            callback?.onConfigurationSelected(configuration.fileName)
            dismiss()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = configurationsAdapter
        
        // Bouton de sauvegarde
        saveButton.setOnClickListener {
            val name = saveNameEdit.text.toString().trim()
            if (name.isNotEmpty()) {
                callback?.onConfigurationSaved(name)
                saveNameEdit.text.clear()
                refreshConfigurations()
                Toast.makeText(context, "Configuration '$name' sauvegardée", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Charger les configurations existantes
        refreshConfigurations()
    }
    
    private fun refreshConfigurations() {
        val configurations = configurationManager.listConfigurations()
        configurationsAdapter.updateConfigurations(configurations)
    }
    
    /**
     * Adaptateur pour la liste des configurations
     */
    private class ConfigurationsAdapter(
        private val onConfigurationSelected: (ConfigurationInfo) -> Unit
    ) : RecyclerView.Adapter<ConfigurationsAdapter.ViewHolder>() {
        
        private var configurations = listOf<ConfigurationInfo>()
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: android.widget.TextView = view.findViewById(R.id.config_name)
            val dateText: android.widget.TextView = view.findViewById(R.id.config_date)
            val sizeText: android.widget.TextView = view.findViewById(R.id.config_size)
            val loadButton: Button = view.findViewById(R.id.load_button)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_configuration, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val config = configurations[position]
            
            holder.nameText.text = config.name
            holder.dateText.text = config.getFormattedDate()
            holder.sizeText.text = config.getFormattedSize()
            
            holder.loadButton.setOnClickListener {
                onConfigurationSelected(config)
            }
        }
        
        override fun getItemCount() = configurations.size
        
        fun updateConfigurations(newConfigurations: List<ConfigurationInfo>) {
            configurations = newConfigurations
            notifyDataSetChanged()
        }
    }
    
    companion object {
        const val TAG = "ConfigurationManagerDialog"
        
        fun newInstance(): ConfigurationManagerDialog {
            return ConfigurationManagerDialog()
        }
    }
}