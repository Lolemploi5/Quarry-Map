package com.example.quarrymap.ui.theme

import android.graphics.Typeface
import android.content.Context

/**
 * Classe utilitaire pour gérer les polices dans l'application
 */
object Typography {
    // Obtenir une police normale
    fun getNormalTypeface(context: Context): Typeface {
        return Typeface.create("sans-serif", Typeface.NORMAL)
    }
    
    // Obtenir une police en gras
    fun getBoldTypeface(context: Context): Typeface {
        return Typeface.create("sans-serif", Typeface.BOLD)
    }
    
    // Obtenir une police légère
    fun getLightTypeface(context: Context): Typeface {
        return Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
}