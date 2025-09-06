package com.example.quarrymap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Configuration complète pour une planche avec ses points d'ancrage
 */
@Parcelize
data class PlancheConfiguration(
    val plancheFile: File,
    var latitude: Double,
    var longitude: Double,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var opacity: Float = 0.7f,
    val id: String = java.util.UUID.randomUUID().toString(),
    var anchorPoints: List<AnchorPoint> = listOf(),
    var isAnchored: Boolean = false,
    var anchorMode: AnchorMode = AnchorMode.CENTER
) : Parcelable {
    
    /**
     * Convertit la configuration vers un PlancheOverlay pour la compatibilité
     */
    fun toPlancheOverlay(): PlancheOverlay {
        return PlancheOverlay(
            plancheFile = plancheFile,
            latitude = latitude,
            longitude = longitude,
            rotation = rotation,
            scale = scale,
            opacity = opacity,
            id = id
        )
    }
    
    /**
     * Met à jour la configuration à partir d'un PlancheOverlay
     */
    fun updateFromOverlay(overlay: PlancheOverlay) {
        latitude = overlay.latitude
        longitude = overlay.longitude
        rotation = overlay.rotation
        scale = overlay.scale
        opacity = overlay.opacity
    }
    
    /**
     * Convertit vers JSON pour la persistance
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("fileName", plancheFile.name)
            put("filePath", plancheFile.absolutePath)
            put("latitude", latitude)
            put("longitude", longitude)
            put("rotation", rotation)
            put("scale", scale)
            put("opacity", opacity)
            put("isAnchored", isAnchored)
            put("anchorMode", anchorMode.name)
            
            val anchorsJson = JSONArray()
            anchorPoints.forEach { anchor ->
                anchorsJson.put(anchor.toJson())
            }
            put("anchorPoints", anchorsJson)
        }
    }
    
    companion object {
        /**
         * Crée une configuration à partir d'un JSON
         */
        fun fromJson(json: JSONObject): PlancheConfiguration? {
            return try {
                val filePath = json.getString("filePath")
                val file = File(filePath)
                
                if (!file.exists()) {
                    return null
                }
                
                val anchorPoints = mutableListOf<AnchorPoint>()
                val anchorsJson = json.optJSONArray("anchorPoints")
                anchorsJson?.let { array ->
                    for (i in 0 until array.length()) {
                        AnchorPoint.fromJson(array.getJSONObject(i))?.let { anchor ->
                            anchorPoints.add(anchor)
                        }
                    }
                }
                
                PlancheConfiguration(
                    plancheFile = file,
                    latitude = json.getDouble("latitude"),
                    longitude = json.getDouble("longitude"),
                    rotation = json.getDouble("rotation").toFloat(),
                    scale = json.getDouble("scale").toFloat(),
                    opacity = json.getDouble("opacity").toFloat(),
                    id = json.optString("id", java.util.UUID.randomUUID().toString()),
                    anchorPoints = anchorPoints,
                    isAnchored = json.optBoolean("isAnchored", false),
                    anchorMode = try {
                        AnchorMode.valueOf(json.optString("anchorMode", "CENTER"))
                    } catch (e: IllegalArgumentException) {
                        AnchorMode.CENTER
                    }
                )
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Crée une configuration à partir d'un PlancheOverlay existant
         */
        fun fromOverlay(overlay: PlancheOverlay): PlancheConfiguration {
            return PlancheConfiguration(
                plancheFile = overlay.plancheFile,
                latitude = overlay.latitude,
                longitude = overlay.longitude,
                rotation = overlay.rotation,
                scale = overlay.scale,
                opacity = overlay.opacity,
                id = overlay.id
            )
        }
    }
}

/**
 * Point d'ancrage sur une planche
 */
@Parcelize
data class AnchorPoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var localX: Float = 0f, // Position relative dans la planche (0-1)
    var localY: Float = 0f, // Position relative dans la planche (0-1)
    var latitude: Double = 0.0, // Position géographique absolue
    var longitude: Double = 0.0, // Position géographique absolue
    var isActive: Boolean = false
) : Parcelable {
    
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("localX", localX)
            put("localY", localY)
            put("latitude", latitude)
            put("longitude", longitude)
            put("isActive", isActive)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): AnchorPoint? {
            return try {
                AnchorPoint(
                    id = json.optString("id", java.util.UUID.randomUUID().toString()),
                    name = json.optString("name", ""),
                    localX = json.getDouble("localX").toFloat(),
                    localY = json.getDouble("localY").toFloat(),
                    latitude = json.getDouble("latitude"),
                    longitude = json.getDouble("longitude"),
                    isActive = json.optBoolean("isActive", false)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Mode d'ancrage d'une planche
 */
enum class AnchorMode {
    CENTER,     // Ancrage par le centre (défaut)
    CORNER,     // Ancrage par un coin
    CUSTOM,     // Ancrage par point personnalisé
    MULTI_POINT // Ancrage multi-points
}