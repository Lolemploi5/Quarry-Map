package com.example.quarrymap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Classe utilitaire pour gérer les notifications avec barre de progression
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "quarry_map_download_channel"
        private const val DOWNLOAD_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    /**
     * Crée le canal de notification pour Android 8.0 (API 26) et versions supérieures
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Téléchargements"
            val descriptionText = "Notifications pour les téléchargements d'images"
            val importance = NotificationManager.IMPORTANCE_LOW // Importance basse pour éviter les sons/vibrations
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false) // Ne pas afficher de badge sur l'icône de l'app
            }
            
            // Enregistrer le canal auprès du système
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Affiche ou met à jour une notification avec barre de progression
     * @param title Titre de la notification
     * @param content Contenu/description de la notification
     * @param progress Valeur actuelle de la progression (0-100)
     * @param max Valeur maximale de la progression
     * @param indeterminate Si la barre doit être indéterminée
     */
    fun showProgressNotification(title: String, content: String, progress: Int, max: Int, indeterminate: Boolean = false) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle(title)
                setContentText(content)
                setSmallIcon(R.drawable.ic_download) // Assurez-vous d'avoir cette icône dans votre projet
                setOngoing(true) // La notification ne peut pas être balayée
                priority = NotificationCompat.PRIORITY_LOW
                
                // Configurer la barre de progression
                setProgress(max, progress, indeterminate)
            }

            // Afficher la notification
            with(NotificationManagerCompat.from(context)) {
                notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            // Gérer les erreurs de permission ou autres
            e.printStackTrace()
        }
    }

    /**
     * Met à jour la notification pour indiquer que le téléchargement est terminé
     * @param title Titre de la notification
     * @param content Contenu/description de la notification
     */
    fun showCompletedNotification(title: String, content: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle(title)
                setContentText(content)
                setSmallIcon(R.drawable.ic_download_complete) // Assurez-vous d'avoir cette icône
                setProgress(0, 0, false) // Supprimer la barre de progression
                setOngoing(false) // La notification peut être balayée
                priority = NotificationCompat.PRIORITY_DEFAULT
                
                // Auto-cancel lorsque l'utilisateur tape dessus
                setAutoCancel(true)
            }

            // Afficher la notification
            with(NotificationManagerCompat.from(context)) {
                notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Annule la notification de téléchargement
     */
    fun cancelNotification() {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancel(DOWNLOAD_NOTIFICATION_ID)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
