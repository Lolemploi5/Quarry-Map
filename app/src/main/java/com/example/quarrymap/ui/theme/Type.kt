package com.example.quarrymap.ui.theme

import android.graphics.Typeface

/**
 * Classe utilitaire pour gérer les polices dans l'application
 */
object Typography {
    // Obtenir une police normale
    fun getNormalTypeface(): Typeface {
        return Typeface.create("sans-serif", Typeface.NORMAL)
    }
    
    // Obtenir une police en gras
    fun getBoldTypeface(): Typeface {
        return Typeface.create("sans-serif", Typeface.BOLD)
    }
    
    // Obtenir une police légère
    fun getLightTypeface(): Typeface {
        return Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
}