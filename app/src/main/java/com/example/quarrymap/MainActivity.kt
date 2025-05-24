package com.example.quarrymap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
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
    

    private lateinit var notificationHelper: NotificationHelper

    private lateinit var binding: ActivityMainBinding
    
    private var customDownloadPath: String? = null
    
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

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkNetworkStatus()
        }
    }

    private lateinit var tabMap: LinearLayout
    private lateinit var tabCommunes: LinearLayout
    private lateinit var tabFavorites: LinearLayout
    private lateinit var iconMap: ImageView
    private lateinit var iconCommunes: ImageView
    private lateinit var iconFavorites: ImageView
    private lateinit var mapFragment: MapFragment
    private lateinit var communesFragment: CommunesFragment
    private lateinit var favoritesFragment: FavoritesFragment

    private lateinit var locationManager: LocationManager
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("MainActivity", "Location updated: ${location.latitude}, ${location.longitude}")
            // Mettre à jour la carte ou d'autres éléments avec la nouvelle localisation
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialiser le helper de notification
        notificationHelper = NotificationHelper(this)
        
        // Configuration de la toolbar - obtenir par findViewById car elle est dans un include
        val topAppBar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false) // Masquer le titre par défaut
            elevation = resources.getDimension(R.dimen.toolbar_elevation)
        }
        
        // Définir le titre de la toolbar
        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle.text = getString(R.string.app_name)
        
        // Initialiser les vues de la navbar
        tabMap = findViewById(R.id.tab_map)
        tabCommunes = findViewById(R.id.tab_communes)
        tabFavorites = findViewById(R.id.tab_favorites)
        iconMap = findViewById(R.id.icon_map)
        iconCommunes = findViewById(R.id.icon_communes)
        iconFavorites = findViewById(R.id.icon_favorites)
        
        // Initialiser les fragments
        mapFragment = MapFragment()
        communesFragment = CommunesFragment()
        favoritesFragment = FavoritesFragment()
        
        // Configurer les écouteurs d'événements pour la navbar
        setupBottomNavBar()
        
        // Charger le fragment de la carte par défaut
        if (savedInstanceState == null) {
            loadFragment(mapFragment)
            updateNavBarState(true, false, false)
        }

        // Mettre à jour le référencement du bouton d'ajout
        binding.addButton.setOnClickListener {
            showUploadOptions()
        }
        
        checkNetworkStatus()

        // Enregistrer le BroadcastReceiver pour les changements de connectivité
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        } else {
            checkNetworkStatus() // Vérifier l'état initial
        }

        // Initialisation des fragments
        val mapFragment = MapFragment()
        val communesFragment = CommunesFragment()
        val favoritesFragment = FavoritesFragment()

        // Configuration de la navigation
        binding.tabMap.setOnClickListener {
            loadFragment(mapFragment)
            updateNavBarState(true, false, false)
            binding.addButton.visibility = View.VISIBLE // Afficher le menu déroulant sur la page carte
            binding.addButton.setOnClickListener {
                val popupMenu = PopupMenu(this, binding.addButton)
                popupMenu.menuInflater.inflate(R.menu.map_add_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.add_point -> {
                            // Afficher un dialogue pour demander le nom du point
                            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_point, null)
                            val etPointName = dialogView.findViewById<EditText>(R.id.etPointName)
                            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                            val btnValidate = dialogView.findViewById<Button>(R.id.btnValidate)

                            val dialog = AlertDialog.Builder(this)
                                .setView(dialogView)
                                .setCancelable(false)
                                .create()

                            btnCancel.setOnClickListener { dialog.dismiss() }
                            btnValidate.setOnClickListener {
                                val pointName = etPointName.text.toString().trim()
                                if (pointName.isNotEmpty()) {
                                    val localMapFragment = supportFragmentManager.findFragmentById(R.id.container) as? MapFragment
                                    val latitude = localMapFragment?.currentLatitude ?: 0.0
                                    val longitude = localMapFragment?.currentLongitude ?: 0.0

                                    // Ajouter un marqueur sur la carte
                                    localMapFragment?.executeJavaScript(
                                        """
                                        L.marker([$latitude, $longitude]).addTo(map)
                                            .bindPopup('$pointName').openPopup();
                                        """
                                    )

                                    // Sauvegarder le point
                                    val newPoint = MapFragment.MarkerData(latitude, longitude, pointName)
                                    val allPoints = PointsStorage.loadPoints(this).toMutableList()
                                    allPoints.add(newPoint)
                                    PointsStorage.savePoints(this, allPoints)
                                    localMapFragment?.loadSavedPoints()

                                    Toast.makeText(this, "Point ajouté : $pointName", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else {
                                    etPointName.error = "Le nom ne peut pas être vide"
                                }
                            }

                            dialog.show()
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        }

        binding.tabCommunes.setOnClickListener {
            loadFragment(communesFragment)
            updateNavBarState(false, true, false)
            binding.addButton.visibility = View.VISIBLE // Afficher le bouton d'importation sur la page commune
            binding.addButton.setOnClickListener {
                showUploadOptions() // Logique pour l'importation
            }
        }

        binding.tabFavorites.setOnClickListener {
            loadFragment(favoritesFragment)
            updateNavBarState(false, false, true)
            binding.addButton.visibility = View.GONE // Cacher le bouton sur les autres pages
        }

        // Configuration du menu déroulant pour le bouton d'ajout
        binding.addButton.setOnClickListener {
            val popupMenu = PopupMenu(this, binding.addButton)
            popupMenu.menuInflater.inflate(R.menu.map_add_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.add_point -> {
                        // Afficher un dialogue pour demander le nom du point
                        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_point, null)
                        val etPointName = dialogView.findViewById<EditText>(R.id.etPointName)
                        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                        val btnValidate = dialogView.findViewById<Button>(R.id.btnValidate)

                        val dialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)
                            .create()

                        btnCancel.setOnClickListener { dialog.dismiss() }
                        btnValidate.setOnClickListener {
                            val pointName = etPointName.text.toString().trim()
                            if (pointName.isNotEmpty()) {
                                val localMapFragment = supportFragmentManager.findFragmentById(R.id.container) as? MapFragment
                                val latitude = localMapFragment?.currentLatitude ?: 0.0
                                val longitude = localMapFragment?.currentLongitude ?: 0.0

                                // Ajouter un marqueur sur la carte
                                localMapFragment?.executeJavaScript(
                                    """
                                    L.marker([$latitude, $longitude]).addTo(map)
                                        .bindPopup('$pointName').openPopup();
                                    """
                                )

                                // Sauvegarder le point
                                val newPoint = MapFragment.MarkerData(latitude, longitude, pointName)
                                val allPoints = PointsStorage.loadPoints(this).toMutableList()
                                allPoints.add(newPoint)
                                PointsStorage.savePoints(this, allPoints)
                                localMapFragment?.loadSavedPoints()

                                Toast.makeText(this, "Point ajouté : $pointName", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                etPointName.error = "Le nom ne peut pas être vide"
                            }
                        }

                        dialog.show()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L, // Intervalle de mise à jour en millisecondes
                10f,   // Distance minimale en mètres
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Permission de localisation non accordée", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Pour les versions plus récentes d'Android, vérifier régulièrement l'état du réseau
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            checkNetworkStatus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Désenregistrer le BroadcastReceiver uniquement si nécessaire
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            unregisterReceiver(networkReceiver)
        }
        locationManager.removeUpdates(locationListener)
    }

    private fun checkNetworkStatus() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            // Méthode dépréciée mais nécessaire pour les anciennes versions d'Android
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected == true
        }

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        
        if (!isConnected) {
            toolbarTitle.visibility = View.GONE
            binding.offlineBanner.visibility = View.VISIBLE
            binding.addButton.visibility = View.GONE
        } else {
            toolbarTitle.visibility = View.VISIBLE
            binding.offlineBanner.visibility = View.GONE
            binding.addButton.visibility = View.VISIBLE
        }
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
                    notificationHelper.showProgressNotification(
                        "Traitement du dossier",
                        "Analyse des fichiers...",
                        0,
                        100,
                        true
                    )
                }
                
                val basePath = getDownloadPath()
                val baseDir = File(basePath)
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }
                
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                
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
                
                withContext(Dispatchers.Main) {
                    notificationHelper.showProgressNotification(
                        "Traitement des images",
                        "0/$totalImages images traitées",
                        0,
                        totalImages,
                        false
                    )
                }
                
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
                                val communeName = name.split("_").firstOrNull() ?: "Inconnu"
                                
                                val communeDir = File(baseDir, communeName)
                                if (!communeDir.exists()) {
                                    communeDir.mkdirs()
                                }
                                
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
                    
                    // Recharger les communes à travers le fragment actif
                    loadCommunesList()
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
                    
                    // Afficher la notification de téléchargement
                    notificationHelper.showProgressNotification(
                        "Téléchargement démarré",
                        "Préparation des fichiers...",
                        0,
                        100,
                        true
                    )
                    
                    Toast.makeText(this@MainActivity, "Téléchargement démarré", Toast.LENGTH_SHORT).show()
                }
                
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.use { it.readText() }
                
                val jsonArray = JSONArray(jsonContent)
                val totalFiles = jsonArray.length()
                Log.d("MainActivity", "Nombre total d'images à télécharger: $totalFiles")
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.max = totalFiles
                    binding.progressText.text = "Préparation des téléchargements ($totalFiles fichiers)..."
                    
                    notificationHelper.showProgressNotification(
                        "Téléchargement en cours",
                        "Préparation de $totalFiles fichiers...",
                        0,
                        totalFiles,
                        false
                    )
                }

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
                    binding.progressContainer.visibility = View.GONE
                    
                    val message = "Téléchargement terminé: $successfulDownloads réussis, $failedDownloads échecs"
                    notificationHelper.showCompletedNotification(
                        "Téléchargement terminé",
                        message
                    )
                    
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    
                    // Recharger les communes à travers le fragment actif
                    loadCommunesList()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors du traitement du JSON", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressContainer.visibility = View.GONE
                    
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
            
            if (connection is HttpURLConnection) {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("MainActivity", "Erreur HTTP $responseCode pour $url")
                    return false
                }
            }
            
            destination.parentFile?.mkdirs()
            
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
    
    fun setCustomDownloadPath(path: String) {
        if (path.isNotEmpty()) {
            customDownloadPath = path
            Log.d("MainActivity", "Chemin de téléchargement défini: $path")
            
            val directory = File(path)
            if (!directory.exists()) {
                val success = directory.mkdirs()
                if (!success) {
                    Log.e("MainActivity", "Impossible de créer le dossier: $path")
                }
            }
        }
    }
    
    fun getDownloadPath(): String {
        return customDownloadPath ?: "${getExternalFilesDir(null)?.absolutePath}/plans_triés"
    }
    
    fun openFolderPicker() {
        folderPickerLauncher.launch(null)
    }
    
    fun openJsonPicker() {
        jsonPickerLauncher.launch("application/json")
    }
    
    private fun setupBottomNavBar() {
        tabMap.setOnClickListener {
            loadFragment(mapFragment)
            updateNavBarState(true, false, false)
        }
        
        tabCommunes.setOnClickListener {
            loadFragment(communesFragment)
            updateNavBarState(false, true, false)
        }
        
        tabFavorites.setOnClickListener {
            loadFragment(favoritesFragment)
            updateNavBarState(false, false, true)
        }
    }
    
    private fun updateNavBarState(mapSelected: Boolean, communesSelected: Boolean, favoritesSelected: Boolean) {
        // Mettre à jour les icônes
        iconMap.setImageAlpha(if (mapSelected) 255 else 128)
        iconCommunes.setImageAlpha(if (communesSelected) 255 else 128)
        iconFavorites.setImageAlpha(if (favoritesSelected) 255 else 128)
        
        // Mettre à jour les textes
        (tabMap.getChildAt(1) as TextView).setTextColor(
            if (mapSelected) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt()
        )
        (tabCommunes.getChildAt(1) as TextView).setTextColor(
            if (communesSelected) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt()
        )
        (tabFavorites.getChildAt(1) as TextView).setTextColor(
            if (favoritesSelected) 0xFFFFFFFF.toInt() else 0x80FFFFFF.toInt()
        )
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
    
    private fun loadCommunesList() {
        if (::communesFragment.isInitialized && 
            supportFragmentManager.findFragmentById(R.id.container) is CommunesFragment) {
            communesFragment.refreshCommunes()
        }
    }

    // Gérer le bouton retour pour la navigation dans la WebView
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is MapFragment && currentFragment.canGoBack()) {
            currentFragment.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
