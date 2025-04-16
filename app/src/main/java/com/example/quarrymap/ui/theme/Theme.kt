package com.example.quarrymap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * Classe utilitaire pour la gestion des thèmes de l'application.
 * Cette version n'utilise pas Jetpack Compose.
 */
object Theme {
    /**
     * Applique le thème sombre à l'application.
     */
    fun applyDarkTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /**
     * Applique le thème clair à l'application.
     */
    fun applyLightTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    /**
     * Suit les paramètres du système pour le thème.
     */
    fun applySystemTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
