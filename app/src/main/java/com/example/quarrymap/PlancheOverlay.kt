package com.example.quarrymap

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Représente une planche (carte, plan, etc.) superposée sur la carte principale.
 * Cette classe est Parcelable pour pouvoir être transmise entre les composants Android.
 */
@Parcelize
data class PlancheOverlay(
    val plancheFile: File,
    var latitude: Double,
    var longitude: Double,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var opacity: Float = 0.7f,
    val id: String = java.util.UUID.randomUUID().toString()
) : Parcelable
