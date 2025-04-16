package com.example.quarrymap

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException

/**
 * Gestionnaire des planches favorites
 */
class FavoriteManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    init {
        // Migrer les anciennes données au format String vers StringSet si nécessaire
        migrateFromStringToStringSet()
    }
    
    /**
     * Ajoute une planche aux favoris
     */
    fun addFavorite(imagePath: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.add(imagePath)) { // Retourne true si ajouté
            saveFavorites(favorites)
            Log.d(TAG, "Ajout aux favoris: $imagePath")
        }
    }
    
    /**
     * Supprime une planche des favoris
     */
    fun removeFavorite(imagePath: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.remove(imagePath)) { // Retourne true si supprimé
            saveFavorites(favorites)
            Log.d(TAG, "Suppression des favoris: $imagePath")
        }
    }
    
    /**
     * Vérifie si une planche est dans les favoris
     */
    fun isFavorite(imagePath: String): Boolean {
        return getFavorites().contains(imagePath)
    }
    
    /**
     * Récupère toutes les planches favorites
     */
    fun getFavorites(): Set<String> {
        // Essayer d'abord avec le format Set<String>
        return try {
            prefs.getStringSet(KEY_FAVORITES_SET, emptySet()) ?: emptySet()
        } catch (e: Exception) {
            // En cas d'échec, récupérer depuis l'ancien format et migrer
            Log.w(TAG, "Impossible de récupérer les favoris au format Set, tentative avec le format JSON", e)
            try {
                val oldFavorites = getOldFavorites()
                if (oldFavorites.isNotEmpty()) {
                    // Migrer vers le nouveau format
                    saveFavorites(oldFavorites.toSet())
                    oldFavorites.toSet()
                } else {
                    emptySet()
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Échec total de récupération des favoris", e2)
                // Créer un nouveau jeu vide
                prefs.edit {
                    remove(KEY_FAVORITES)
                    putStringSet(KEY_FAVORITES_SET, emptySet())
                    apply()
                }
                emptySet()
            }
        }
    }
    
    /**
     * Met à jour un chemin dans les favoris (après renommage)
     */
    fun updatePath(oldPath: String, newPath: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.remove(oldPath)) {
            favorites.add(newPath)
            saveFavorites(favorites)
            Log.d(TAG, "Mise à jour du chemin: $oldPath -> $newPath")
        }
    }
    
    /**
     * Sauvegarde la liste des favoris
     */
    private fun saveFavorites(favorites: Set<String>) {
        prefs.edit {
            remove(KEY_FAVORITES) // Supprimer l'ancienne version
            putStringSet(KEY_FAVORITES_SET, favorites)
            apply()
        }
    }
    
    /**
     * Récupère les favoris au format ancien (List<String> stocké en JSON)
     */
    private fun getOldFavorites(): List<String> {
        val jsonFavorites = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(jsonFavorites)
            val favorites = mutableListOf<String>()
            
            for (i in 0 until jsonArray.length()) {
                favorites.add(jsonArray.getString(i))
            }
            
            favorites
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la lecture des favoris au format JSON", e)
            emptyList()
        }
    }
    
    /**
     * Migre les anciennes données au format String vers StringSet
     */
    private fun migrateFromStringToStringSet() {
        try {
            // Vérifier si le nouveau format existe déjà
            if (prefs.contains(KEY_FAVORITES_SET)) {
                return
            }
            
            // Vérifier si l'ancien format existe
            if (prefs.contains(KEY_FAVORITES)) {
                val oldFavorites = getOldFavorites()
                if (oldFavorites.isNotEmpty()) {
                    // Migrer vers le nouveau format
                    prefs.edit {
                        putStringSet(KEY_FAVORITES_SET, oldFavorites.toSet())
                        remove(KEY_FAVORITES)
                        apply()
                    }
                    Log.i(TAG, "Migration des favoris réussie: ${oldFavorites.size} éléments")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la migration des favoris", e)
        }
    }
    
    companion object {
        private const val TAG = "FavoriteManager"
        private const val PREF_NAME = "favorite_prefs"
        private const val KEY_FAVORITES = "favorite_images" // Ancienne clé (JSONArray)
        private const val KEY_FAVORITES_SET = "favorite_images_set" // Nouvelle clé (StringSet)
    }
}
