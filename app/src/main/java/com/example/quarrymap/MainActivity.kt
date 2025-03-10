package com.example.quarrymap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quarrymap.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    // Helper pour les notifications avec barre de progression
    private lateinit var notificationHelper: NotificationHelper

    private lateinit var binding: ActivityMainBinding
    
    // Chemin de téléchargement personnalisé
    private var customDownloadPath: String? = null
    
    // Liste complète des communes
    private var allCommunes: List<String> = emptyList()

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            processFolder(uri)
        } else {
            Toast.makeText(this, "Aucun dossier sélectionné", Toast.LENGTH_SHORT).show()
        }
    }

    private val jsonPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            processJson(uri)
        } else {
            Toast.makeText(this, "Aucun fichier JSON sélectionné", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialiser le helper de notification
        notificationHelper = NotificationHelper(this)
        
        // Configuration de la toolbar
        setSupportActionBar(binding.topAppBar)

        setupRecyclerView()
        setupListeners()
        loadCommunesList()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = CommuneAdapter(emptyList()) { commune ->
            // Lancement de l'activité CommuneActivity avec le nom de la commune
            val intent = Intent(this, CommuneActivity::class.java).apply {
                putExtra(CommuneActivity.EXTRA_COMMUNE, commune)
                putExtra("EXTRA_BASE_PATH", getDownloadPath())
            }
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        binding.fab.setOnClickListener {
            showUploadOptions()
        }
        
        // Configurer la recherche
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCommunes(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCommunes(newText)
                return true
            }
        })
    }
    
    private fun filterCommunes(query: String?) {
        (binding.recyclerView.adapter as? CommuneAdapter)?.filter(query ?: "")
    }
    
    fun onSearchCardClick(view: View) {
        // Animation de l'élévation de la carte
        view.animate()
            .translationZ(8f)
            .setDuration(150)
            .withEndAction {
                binding.searchView.requestFocus()
                binding.searchView.isIconified = false
                
                // Afficher le clavier virtuel
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
                
                view.animate()
                    .translationZ(0f)
                    .setStartDelay(200)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    private fun showUploadOptions() {
        val dialog = UploadOptionsDialog()
        dialog.show(supportFragmentManager, "UploadOptionsDialog")
    }

    private fun processFolder(uri: Uri) {
        Log.d("MainActivity", "Processing folder: $uri")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    // Afficher une notification de début de traitement
                    notificationHelper.showProgressNotification(
                        "Traitement du dossier",
                        "Analyse des fichiers...",
                        0,
                        100,
                        true
                    )
                }
                
                // Obtenir le chemin de base pour les plans triés
                val basePath = getDownloadPath()
                val baseDir = File(basePath)
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }
                
                // Accéder aux documents du dossier sélectionné
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                
                // Première passe : compter le nombre total d'images
                var totalImages = 0
                contentResolver.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE),
                    null,
                    null,
                    null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val mime = c.getString(0)
                        if (mime.startsWith("image/")) totalImages++
                    }
                }
                
                Log.d("MainActivity", "Nombre total d'images trouvées: $totalImages")
                
                // Mettre à jour la notification avec le nombre total d'images
                withContext(Dispatchers.Main) {
                    notificationHelper.showProgressNotification(
                        "Traitement des images",
                        "0/$totalImages images traitées",
                        0,
                        totalImages,
                        false
                    )
                }
                
                // Deuxième passe : traiter les images
                var processedImages = 0
                var successfulCopies = 0
                
                val cursor = contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null,
                    null,
                    null
                )
                
                cursor?.use { c ->
                    val idIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    
                    while (c.moveToNext()) {
                        val id = c.getString(idIndex)
                        val name = c.getString(nameIndex)
                        val mime = c.getString(mimeIndex)
                        
                        // Traiter uniquement les images
                        if (mime.startsWith("image/")) {
                            try {
                                // Extraire le nom de la commune du nom du fichier
                                val communeName = name.split("_").firstOrNull() ?: "Inconnu"
                                
                                // Créer le dossier de la commune s'il n'existe pas
                                val communeDir = File(baseDir, communeName)
                                if (!communeDir.exists()) {
                                    communeDir.mkdirs()
                                }
                                
                                // Copier le fichier
                                val fileUri = DocumentsContract.buildDocumentUriUsingTree(uri, id)
                                val inputStream = contentResolver.openInputStream(fileUri)
                                val destFile = File(communeDir, name)
                                
                                inputStream?.use { input ->
                                    destFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                
                                if (destFile.exists() && destFile.length() > 0) {
                                    successfulCopies++
                                    Log.d("MainActivity", "Copié avec succès: $name vers ${destFile.absolutePath}")
                                } else {
                                    Log.e("MainActivity", "Échec de la copie: $name (fichier vide ou inexistant)")
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Erreur lors de la copie de $name", e)
                            }
                            
                            processedImages++
                            
                            // Mettre à jour la notification toutes les 5 images ou à la fin
                            if (processedImages % 5 == 0 || processedImages == totalImages) {
                                withContext(Dispatchers.Main) {
                                    notificationHelper.showProgressNotification(
                                        "Traitement des images",
                                        "$processedImages/$totalImages images traitées ($successfulCopies réussies)",
                                        processedImages,
                                        totalImages,
                                        false
                                    )
                                }
                            }
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    // Afficher la notification finale
                    notificationHelper.showCompletedNotification(
                        "Importation terminée",
                        "$successfulCopies/$totalImages images importées avec succès"
                    )
                    
                    Toast.makeText(this@MainActivity, "Importation terminée: $successfulCopies/$totalImages images importées", Toast.LENGTH_SHORT).show()
                    loadCommunesList() // Recharger la liste des communes
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors du traitement du dossier", e)
                withContext(Dispatchers.Main) {
                    // Afficher la notification d'erreur
                    notificationHelper.showCompletedNotification(
                        "Erreur de traitement",
                        "Une erreur s'est produite: ${e.message}"
                    )
                    
                    Toast.makeText(this@MainActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun processJson(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Configurer l'interface utilisateur pour le téléchargement
                withContext(Dispatchers.Main) {
                    // Afficher le conteneur de progression
                    binding.progressContainer.visibility = View.VISIBLE
                    binding.progressBar.progress = 0
                    binding.progressText.text = "Lecture du fichier JSON..."
                    
                    // Désactiver le RecyclerView pendant le téléchargement
                    binding.recyclerView.alpha = 0.5f
                    
                    // Afficher la notification de téléchargement
                    notificationHelper.showProgressNotification(
                        "Téléchargement démarré",
                        "Préparation des fichiers...",
                        0,
                        100,
                        true // Indéterminé au début
                    )
                    
                    Toast.makeText(this@MainActivity, "Téléchargement démarré", Toast.LENGTH_SHORT).show()
                }
                
                // Lire le contenu JSON
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.use { it.readText() }
                
                // Analyser le JSON
                val jsonArray = JSONArray(jsonContent)
                val totalFiles = jsonArray.length()
                Log.d("MainActivity", "Nombre total d'images à télécharger: $totalFiles")
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.max = totalFiles
                    binding.progressText.text = "Préparation des téléchargements ($totalFiles fichiers)..."
                    
                    // Mettre à jour la notification avec le nombre total de fichiers
                    notificationHelper.showProgressNotification(
                        "Téléchargement en cours",
                        "Préparation de $totalFiles fichiers...",
                        0,
                        totalFiles,
                        false
                    )
                }

                // Obtenir le chemin de base pour les plans triés
                val basePath = getDownloadPath()
                Log.d("MainActivity", "Chemin de téléchargement: $basePath")

                var processedFiles = 0
                var successfulDownloads = 0
                var failedDownloads = 0
                
                for (i in 0 until totalFiles) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val communes = jsonObject.optJSONArray("commune")
                    val imageInfo = jsonObject.optJSONObject("fichier_image")
                    
                    // Mettre à jour l'UI pour montrer quel fichier est en cours de traitement
                    withContext(Dispatchers.Main) {
                        binding.progressText.text = "Traitement: ${i+1} / $totalFiles"
                    }

                    if (communes != null && imageInfo != null) {
                        try {
                            val communeName = communes.getString(0)
                            val imageUrl = imageInfo.optString("url")
                            val imageFilename = imageInfo.optString("filename")
                            val imageWidth = imageInfo.optInt("width", 0)
                            val imageHeight = imageInfo.optInt("height", 0)

                            Log.d("MainActivity", "Traitement de l'image: $imageFilename pour $communeName")
                            
                            if (communeName.isNotEmpty() && imageUrl.isNotEmpty() && imageFilename.isNotEmpty()) {
                                // Créer le dossier de la commune s'il n'existe pas
                                val communeFolder = File(basePath, communeName)
                                if (!communeFolder.exists()) {
                                    val success = communeFolder.mkdirs()
                                    Log.d("MainActivity", "Création du dossier $communeName: $success")
                                }

                                val imageFile = File(communeFolder, imageFilename)
                                if (!imageFile.exists() || imageFile.length() == 0L) {
                                    // Mettre à jour l'UI pour montrer quelle image est en cours de téléchargement
                                    withContext(Dispatchers.Main) {
                                        binding.progressText.text = "Téléchargement: $imageFilename ($communeName)"
                                    }
                                    
                                    val success = downloadImage(imageUrl, imageFile)
                                    if (success) {
                                        successfulDownloads++
                                        Log.d("MainActivity", "Téléchargement réussi: $imageFilename ($imageWidth x $imageHeight)")
                                    } else {
                                        failedDownloads++
                                        Log.e("MainActivity", "Échec du téléchargement: $imageFilename")
                                    }
                                } else {
                                    Log.d("MainActivity", "L'image existe déjà: $imageFilename (taille: ${imageFile.length()} octets)")
                                }
                            } else {
                                Log.w("MainActivity", "Données manquantes pour l'image: commune=$communeName, url=$imageUrl, filename=$imageFilename")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Erreur lors du traitement d'une entrée JSON", e)
                            failedDownloads++
                        }
                    } else {
                        Log.w("MainActivity", "Entrée JSON invalide: communes=${communes != null}, imageInfo=${imageInfo != null}")
                    }
                    
                    processedFiles++
                    withContext(Dispatchers.Main) {
                        binding.progressBar.progress = processedFiles
                        binding.progressText.text = "Traités: $processedFiles / $totalFiles (Réussis: $successfulDownloads, Échecs: $failedDownloads)"
                        
                        // Mettre à jour la notification avec la progression
                        if (processedFiles % 5 == 0 || processedFiles == totalFiles) { // Mise à jour toutes les 5 images ou à la fin
                            notificationHelper.showProgressNotification(
                                "Téléchargement en cours",
                                "$processedFiles/$totalFiles fichiers traités",
                                processedFiles,
                                totalFiles,
                                false
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    // Masquer le conteneur de progression
                    binding.progressContainer.visibility = View.GONE
                    
                    // Restaurer l'opacité du RecyclerView
                    binding.recyclerView.alpha = 1.0f
                    
                    // Afficher la notification de fin de téléchargement
                    val message = "Téléchargement terminé: $successfulDownloads réussis, $failedDownloads échecs"
                    notificationHelper.showCompletedNotification(
                        "Téléchargement terminé",
                        message
                    )
                    
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    loadCommunesList() // Recharger la liste des communes
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors du traitement du JSON", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Masquer le conteneur de progression
                    binding.progressContainer.visibility = View.GONE
                    
                    // Restaurer l'opacité du RecyclerView
                    binding.recyclerView.alpha = 1.0f
                    
                    // Afficher une notification d'erreur
                    notificationHelper.showCompletedNotification(
                        "Erreur de téléchargement",
                        "Une erreur s'est produite: ${e.message}"
                    )
                    
                    Toast.makeText(this@MainActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadImage(url: String, destination: File): Boolean {
        return try {
            Log.d("MainActivity", "Début du téléchargement: $url")
            
            val connection = URL(url).openConnection().apply {
                connectTimeout = 30000 // 30 secondes de timeout de connexion
                readTimeout = 60000    // 60 secondes de timeout de lecture
                setRequestProperty("User-Agent", "QuarryMap Android App")
            }
            
            connection.connect()
            
            // Vérifier le code de réponse si c'est une connexion HTTP
            if (connection is HttpURLConnection) {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("MainActivity", "Erreur HTTP $responseCode pour $url")
                    return false
                }
            }
            
            // S'assurer que le dossier parent existe
            destination.parentFile?.mkdirs()
            
            // Copier les données avec un buffer de taille raisonnable
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            var totalBytesRead = 0L
            
            connection.getInputStream().use { input ->
                destination.outputStream().use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    output.flush()
                }
            }
            
            // Vérifier que le fichier a bien été créé et n'est pas vide
            val success = destination.exists() && destination.length() > 0
            if (success) {
                Log.d("MainActivity", "Image téléchargée avec succès: $url -> ${destination.absolutePath} (${destination.length()} octets)")
            } else {
                Log.e("MainActivity", "Fichier vide ou inexistant après téléchargement: ${destination.absolutePath}")
            }
            
            success
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur lors du téléchargement de l'image: $url", e)
            e.printStackTrace()
            false
        }
    }
    
    // Méthode pour définir le chemin de téléchargement personnalisé
    fun setCustomDownloadPath(path: String) {
        if (path.isNotEmpty()) {
            customDownloadPath = path
            Log.d("MainActivity", "Chemin de téléchargement défini: $path")
            
            // Créer le dossier s'il n'existe pas
            val directory = File(path)
            if (!directory.exists()) {
                val success = directory.mkdirs()
                if (!success) {
                    Log.e("MainActivity", "Impossible de créer le dossier: $path")
                }
            }
        }
    }
    
    // Méthode pour obtenir le chemin de téléchargement actuel
    private fun getDownloadPath(): String {
        return customDownloadPath ?: "${getExternalFilesDir(null)?.absolutePath}/plans_triés"
    }
    
    // Méthode appelée depuis UploadOptionsDialog pour ouvrir le sélecteur de dossier
    fun openFolderPicker() {
        folderPickerLauncher.launch(null)
    }
    
    // Méthode appelée depuis UploadOptionsDialog pour ouvrir le sélecteur de fichier JSON
    fun openJsonPicker() {
        jsonPickerLauncher.launch("application/json")
    }
    

    
    // Chargement de la liste des communes
    private fun loadCommunesList() {
        // Récupérer la liste des dossiers de communes depuis le stockage
        val basePath = getDownloadPath()
        val communesDir = File(basePath)
        
        if (communesDir.exists() && communesDir.isDirectory) {
            allCommunes = communesDir.listFiles()
                ?.filter { it.isDirectory }
                ?.map { it.name }
                ?.sorted() ?: emptyList()
            
            // Mise à jour de l'adaptateur avec la liste des communes
            (binding.recyclerView.adapter as? CommuneAdapter)?.updateData(allCommunes)
            
            if (allCommunes.isEmpty()) {
                Toast.makeText(this, "Aucune commune trouvée. Importez des données d'abord.", Toast.LENGTH_LONG).show()
            } else {
                Log.d("MainActivity", "${allCommunes.size} communes trouvées")
            }
        } else {
            communesDir.mkdirs()
            Toast.makeText(this, "Aucune commune trouvée. Importez des données d'abord.", Toast.LENGTH_LONG).show()
        }
    }
}
