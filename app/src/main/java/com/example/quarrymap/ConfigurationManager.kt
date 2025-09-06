package com.example.quarrymap

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire pour la sauvegarde et le chargement des configurations de planches
 */
class ConfigurationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ConfigurationManager"
        private const val CONFIG_DIR = "overlay_configurations"
        private const val AUTO_SAVE_FILE = "auto_save.json"
        private const val SESSION_FILE = "current_session.json"
    }
    
    private val configDir: File by lazy {
        File(context.getExternalFilesDir(null), CONFIG_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Sauvegarde automatique de la session actuelle
     */
    fun autoSaveSession(configurations: List<PlancheConfiguration>) {
        try {
            val sessionFile = File(configDir, AUTO_SAVE_FILE)
            val json = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("version", "1.0")
                put("configurations", configurationsToJsonArray(configurations))
            }
            
            sessionFile.writeText(json.toString(2))
            Log.d(TAG, "Session auto-sauvegardée: ${configurations.size} configurations")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'auto-sauvegarde", e)
        }
    }
    
    /**
     * Charge la session auto-sauvegardée
     */
    fun loadAutoSavedSession(): List<PlancheConfiguration> {
        return try {
            val sessionFile = File(configDir, AUTO_SAVE_FILE)
            if (!sessionFile.exists()) return emptyList()
            
            val json = JSONObject(sessionFile.readText())
            val configurations = jsonArrayToConfigurations(json.getJSONArray("configurations"))
            
            Log.d(TAG, "Session auto-sauvegardée chargée: ${configurations.size} configurations")
            configurations
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement de la session auto-sauvegardée", e)
            emptyList()
        }
    }
    
    /**
     * Sauvegarde une configuration nommée
     */
    fun saveConfiguration(name: String, configurations: List<PlancheConfiguration>): Boolean {
        return try {
            val fileName = "${sanitizeFileName(name)}.json"
            val configFile = File(configDir, fileName)
            
            val json = JSONObject().apply {
                put("name", name)
                put("timestamp", System.currentTimeMillis())
                put("version", "1.0")
                put("configurations", configurationsToJsonArray(configurations))
            }
            
            configFile.writeText(json.toString(2))
            Log.d(TAG, "Configuration sauvegardée: $name (${configurations.size} planches)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde de la configuration: $name", e)
            false
        }
    }
    
    /**
     * Charge une configuration nommée
     */
    fun loadConfiguration(fileName: String): Pair<String, List<PlancheConfiguration>>? {
        return try {
            val configFile = File(configDir, fileName)
            if (!configFile.exists()) return null
            
            val json = JSONObject(configFile.readText())
            val name = json.getString("name")
            val configurations = jsonArrayToConfigurations(json.getJSONArray("configurations"))
            
            Log.d(TAG, "Configuration chargée: $name (${configurations.size} planches)")
            Pair(name, configurations)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement de la configuration: $fileName", e)
            null
        }
    }
    
    /**
     * Liste toutes les configurations sauvegardées
     */
    fun listConfigurations(): List<ConfigurationInfo> {
        return try {
            configDir.listFiles { _, name -> name.endsWith(".json") && name != AUTO_SAVE_FILE }
                ?.mapNotNull { file ->
                    try {
                        val json = JSONObject(file.readText())
                        ConfigurationInfo(
                            fileName = file.name,
                            name = json.getString("name"),
                            timestamp = json.getLong("timestamp"),
                            plancheCount = json.getJSONArray("configurations").length()
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Configuration corrompue ignorée: ${file.name}", e)
                        null
                    }
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la liste des configurations", e)
            emptyList()
        }
    }
    
    /**
     * Supprime une configuration
     */
    fun deleteConfiguration(fileName: String): Boolean {
        return try {
            val configFile = File(configDir, fileName)
            val deleted = configFile.delete()
            if (deleted) {
                Log.d(TAG, "Configuration supprimée: $fileName")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression de la configuration: $fileName", e)
            false
        }
    }
    
    /**
     * Exporte une configuration vers un fichier externe
     */
    fun exportConfiguration(configurations: List<PlancheConfiguration>, exportFile: File): Boolean {
        return try {
            val json = JSONObject().apply {
                put("name", "Configuration exportée")
                put("timestamp", System.currentTimeMillis())
                put("version", "1.0")
                put("exported_from", "Quarry Map Android")
                put("configurations", configurationsToJsonArray(configurations))
            }
            
            exportFile.writeText(json.toString(2))
            Log.d(TAG, "Configuration exportée vers: ${exportFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'exportation", e)
            false
        }
    }
    
    /**
     * Importe une configuration depuis un fichier externe
     */
    fun importConfiguration(importFile: File): List<PlancheConfiguration>? {
        return try {
            val json = JSONObject(importFile.readText())
            val configurations = jsonArrayToConfigurations(json.getJSONArray("configurations"))
            
            Log.d(TAG, "Configuration importée depuis: ${importFile.absolutePath} (${configurations.size} planches)")
            configurations
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'importation depuis: ${importFile.absolutePath}", e)
            null
        }
    }
    
    /**
     * Crée un nom de fichier unique pour une nouvelle configuration
     */
    fun generateUniqueFileName(baseName: String): String {
        val sanitized = sanitizeFileName(baseName)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${sanitized}_$timestamp.json"
    }
    
    private fun configurationsToJsonArray(configurations: List<PlancheConfiguration>): JSONArray {
        return JSONArray().apply {
            configurations.forEach { config ->
                put(config.toJson())
            }
        }
    }
    
    private fun jsonArrayToConfigurations(jsonArray: JSONArray): List<PlancheConfiguration> {
        val configurations = mutableListOf<PlancheConfiguration>()
        for (i in 0 until jsonArray.length()) {
            val configJson = jsonArray.getJSONObject(i)
            PlancheConfiguration.fromJson(configJson)?.let { config ->
                configurations.add(config)
            }
        }
        return configurations
    }
    
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50)
    }
    
    /**
     * Obtient l'usage de l'espace de stockage des configurations
     */
    fun getStorageUsage(): StorageUsage {
        return try {
            val files = configDir.listFiles() ?: emptyArray()
            val totalSize = files.sumOf { it.length() }
            val configCount = files.count { it.name.endsWith(".json") }
            
            StorageUsage(
                totalSizeBytes = totalSize,
                configurationCount = configCount,
                directoryPath = configDir.absolutePath
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du calcul de l'usage de stockage", e)
            StorageUsage(0, 0, configDir.absolutePath)
        }
    }
}

/**
 * Informations sur une configuration sauvegardée
 */
data class ConfigurationInfo(
    val fileName: String,
    val name: String,
    val timestamp: Long,
    val plancheCount: Int
) {
    fun getFormattedDate(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
    
    fun getFormattedSize(): String {
        return "$plancheCount planche${if (plancheCount > 1) "s" else ""}"
    }
}

/**
 * Informations sur l'usage de stockage
 */
data class StorageUsage(
    val totalSizeBytes: Long,
    val configurationCount: Int,
    val directoryPath: String
) {
    fun getFormattedSize(): String {
        return when {
            totalSizeBytes < 1024 -> "$totalSizeBytes octets"
            totalSizeBytes < 1024 * 1024 -> "${totalSizeBytes / 1024} Ko"
            else -> "${totalSizeBytes / (1024 * 1024)} Mo"
        }
    }
}