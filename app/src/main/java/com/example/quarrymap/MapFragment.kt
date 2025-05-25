package com.example.quarrymap

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.quarrymap.databinding.FragmentMapBinding
import org.json.JSONObject as JsonObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import org.json.JSONArray

private const val TAG = "MapFragment"

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mapWebView: WebView
    private lateinit var coordinatesTextView: TextView
    private lateinit var coordinatesContainer: LinearLayout
    private var isWebViewInitialized = false
    
    // Variables pour stocker les valeurs de latitude et longitude actuelles
    var currentLatitude: Double = 0.0
        private set

    var currentLongitude: Double = 0.0
        private set

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val pendingMarkers = mutableListOf<MarkerData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coordinatesTextView = binding.coordinatesText
        coordinatesContainer = binding.coordinatesContainer

        // Configurer le conteneur des coordonnées pour le clic
        coordinatesContainer.setOnClickListener {
            copyCoordinatesToClipboard()
        }

        // Ajouter une animation de pression sur le conteneur
        coordinatesContainer.apply {
            isClickable = true
            isFocusable = true

            setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.alpha = 0.7f
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.alpha = 1.0f
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }

        // Initialiser le client de localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Configurer le callback de localisation
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateMapWithCurrentLocation(location)
                }
            }
        }

        // Démarrer la localisation dès le lancement de l'application
        checkAndRequestLocationPermission()

        // Ajouter un bouton pour centrer sur la position actuelle
        binding.currentLocationButton.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    centerMapOnLocation(it)
                }
            }
        }

        setupWebView()
        loadSavedPoints()
    }

    private fun centerMapOnLocation(location: Location) {
        val jsCode = "map.setView([${location.latitude}, ${location.longitude}], 15);"
        mapWebView.evaluateJavascript(jsCode, null)
    }

    private fun setupWebView() {
        mapWebView = binding.webView
        
        // Configuration de la WebView
        mapWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            setSupportZoom(true)
            setBuiltInZoomControls(true)
            setDisplayZoomControls(false)
        }
        
        // Ajouter une interface JavaScript pour communiquer avec la WebView
        mapWebView.addJavascriptInterface(WebAppInterface(), "Android")
        
        // Configurer le WebViewClient pour intercepter les chargements de pages
        mapWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Garder tous les chargements dans la WebView
                return false
            }
            
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page chargée: $url")
                isWebViewInitialized = true

                // Vérifier si la fonction addMarkers est définie dans le contexte JavaScript
                val jsCheckCode = "if (typeof addMarkers === 'undefined') { console.error('addMarkers is not defined'); } else { console.log('addMarkers is defined'); }"
                mapWebView.evaluateJavascript(jsCheckCode, null)

                // Ajouter les marqueurs en attente immédiatement après l'initialisation
                if (pendingMarkers.isNotEmpty()) {
                    Log.d(TAG, "Ajout des marqueurs en attente: ${pendingMarkers.size} marqueurs")
                    addMarkers(pendingMarkers)
                    pendingMarkers.clear()
                } else {
                    Log.d(TAG, "Aucun marqueur en attente à ajouter")
                }
            }
        }
        
        // Chargement de la carte OpenStreetMap
        loadMap()
    }
    
    private fun loadMap() {
        try {
            // Charger une carte OpenStreetMap de base via Leaflet
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <title>Carte des sites</title>
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <style>
                        body { margin: 0; padding: 0; }
                        #map { position: absolute; top: 0; bottom: 0; width: 100%; height: 100%; }
                        .center-marker {
                            position: absolute;
                            top: 50%;
                            left: 50%;
                            width: 20px;
                            height: 20px;
                            margin-left: -10px;
                            margin-top: -10px;
                            z-index: 1000;
                            pointer-events: none;
                        }
                        .center-marker:before, .center-marker:after {
                            content: "";
                            position: absolute;
                            background-color: rgba(255, 0, 0, 0.7);
                        }
                        .center-marker:before {
                            left: 9px;
                            top: 0;
                            width: 2px;
                            height: 20px;
                        }
                        .center-marker:after {
                            top: 9px;
                            left: 0;
                            width: 20px;
                            height: 2px;
                        }
                        .marker-pulse {
                            border: 3px solid rgba(255, 0, 0, 0.5);
                            background: rgba(255, 0, 0, 0.2);
                            border-radius: 50%;
                            height: 14px;
                            width: 14px;
                            position: absolute;
                            left: 0;
                            top: 0;
                            animation: pulsate 1.5s ease-out;
                            animation-iteration-count: infinite;
                        }
                        @keyframes pulsate {
                            0% { transform: scale(0.1); opacity: 0; }
                            50% { opacity: 1; }
                            100% { transform: scale(1.2); opacity: 0; }
                        }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <div class="center-marker">
                        <div class="marker-pulse"></div>
                    </div>
                    <script>
                        // Initialiser la carte
                        var map = L.map('map').setView([46.603354, 1.888334], 6); // Vue centrée sur la France
                        
                        // Ajouter les tuiles OpenStreetMap
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        }).addTo(map);
                        
                        // Fonction pour ajouter des marqueurs (à appeler depuis Android)
                        var leafletMarkers = [];
                        function addMarkers(markersJson) {
                            // Supprimer les anciens marqueurs
                            if (leafletMarkers && leafletMarkers.length > 0) {
                                leafletMarkers.forEach(function(m) { map.removeLayer(m); });
                                leafletMarkers = [];
                            }
                            const markers = JSON.parse(markersJson);
                            markers.forEach((marker, idx) => {
                                var leafletMarker = L.marker([marker.lat, marker.lng])
                                    .addTo(map)
                                    .bindPopup(marker.title);
                                leafletMarker.on('click', function() {
                                    if (Android && Android.onMarkerClicked) {
                                        Android.onMarkerClicked(marker.lat + ',' + marker.lng);
                                    }
                                });
                                leafletMarkers.push(leafletMarker);
                            });
                        }
                        
                        // Fonction pour obtenir les coordonnées du centre de la carte
                        function updateCenterCoordinates() {
                            var center = map.getCenter();
                            Android.onCenterCoordinatesChanged(center.lat, center.lng);
                        }
                        
                        // Mettre à jour les coordonnées au démarrage et lors du déplacement
                        map.on('load', updateCenterCoordinates);
                        map.on('move', updateCenterCoordinates);
                        map.on('moveend', updateCenterCoordinates);
                        
                        // Mettre à jour après un court délai pour s'assurer que la carte est entièrement chargée
                        setTimeout(updateCenterCoordinates, 500);
                    </script>
                </body>
                </html>
            """.trimIndent()
            
            mapWebView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement de la carte", e)
            Toast.makeText(context, "Erreur lors du chargement de la carte: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Interface JavaScript pour communiquer avec la WebView
    inner class WebAppInterface {
        @JavascriptInterface
        fun onCenterCoordinatesChanged(latitude: Double, longitude: Double) {
            activity?.runOnUiThread {
                // Stocker les valeurs actuelles
                currentLatitude = latitude
                currentLongitude = longitude
                
                // Afficher les coordonnées formatées pour l'utilisateur
                val formattedCoordinates = String.format("%.6f, %.6f", latitude, longitude)
                coordinatesTextView.text = formattedCoordinates
                coordinatesContainer.visibility = View.VISIBLE
            }
        }

        @JavascriptInterface
        fun onMarkerClicked(coordId: String) {
            activity?.runOnUiThread {
                // coordId format: "lat,lng"
                val parts = coordId.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        val points = PointsStorage.loadPoints(requireContext())
                        val point = points.find { it.latitude == lat && it.longitude == lng }
                        if (point != null) {
                            val fragment = PointBottomSheetDialogFragment.newInstance(
                                coordId,
                                point.title,
                                String.format("%.6f, %.6f", point.latitude, point.longitude),
                                point.description,
                                point.latitude,
                                point.longitude,
                                ArrayList(point.photoUris) // passage de la liste d'images
                            )
                            fragment.setPointActionListener(object : PointBottomSheetDialogFragment.PointActionListener {
                                override fun onDescriptionChanged(pointId: String, newDescription: String) {
                                    updatePointDescriptionByCoordId(pointId, newDescription)
                                }
                                override fun onDeletePoint(pointId: String) {
                                    deletePointByCoordId(pointId)
                                }
                                override var onNameChanged: ((String, String) -> Unit)?
                                    get() = { pointId, newName -> updatePointNameByCoordId(pointId, newName) }
                                    set(_) {}
                                override var onPhotoChanged: ((String, ArrayList<String>) -> Unit)?
                                    get() = { pointId, photoUris -> updatePointPhotoUriByCoordId(pointId, photoUris) }
                                    set(_) {}
                            })
                            fragment.show(parentFragmentManager, "PointBottomSheetDialogFragment")
                        }
                    }
                }
            }
        }
    }

    private fun deletePointByCoordId(coordId: String) {
        val parts = coordId.split(",")
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) {
                val points = PointsStorage.loadPoints(requireContext()).toMutableList()
                val removed = points.removeIf { it.latitude == lat && it.longitude == lng }
                if (removed) {
                    PointsStorage.savePoints(requireContext(), points)
                    loadSavedPoints()
                    Toast.makeText(requireContext(), "Point supprimé", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePointDescriptionByCoordId(coordId: String, newDescription: String) {
        val parts = coordId.split(",")
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) {
                val points = PointsStorage.loadPoints(requireContext()).toMutableList()
                val idx = points.indexOfFirst { it.latitude == lat && it.longitude == lng }
                if (idx != -1) {
                    points[idx] = points[idx].copy(description = newDescription)
                    PointsStorage.savePoints(requireContext(), points)
                    loadSavedPoints()
                    Toast.makeText(requireContext(), "Description enregistrée", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePointNameByCoordId(coordId: String, newName: String) {
        val parts = coordId.split(",")
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) {
                val points = PointsStorage.loadPoints(requireContext()).toMutableList()
                val idx = points.indexOfFirst { it.latitude == lat && it.longitude == lng }
                if (idx != -1) {
                    points[idx] = points[idx].copy(title = newName)
                    PointsStorage.savePoints(requireContext(), points)
                    loadSavedPoints()
                    Toast.makeText(requireContext(), "Nom modifié", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Nouvelle fonction pour mettre à jour la liste d'images
    private fun updatePointPhotoUriByCoordId(coordId: String, photoUris: ArrayList<String>?) {
        val parts = coordId.split(",")
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) {
                val points = PointsStorage.loadPoints(requireContext()).toMutableList()
                val idx = points.indexOfFirst { it.latitude == lat && it.longitude == lng }
                if (idx != -1) {
                    points[idx] = points[idx].copy(photoUris = photoUris ?: arrayListOf())
                    PointsStorage.savePoints(requireContext(), points)
                    loadSavedPoints()
                    Toast.makeText(requireContext(), "Images enregistrées", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Fonction pour copier les coordonnées dans le presse-papier au format Google Maps
    private fun copyCoordinatesToClipboard() {
        // Format Google Maps: "latitude,longitude" (sans espace)
        val googleMapsFormat = "$currentLatitude,$currentLongitude"
        
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Coordonnées GPS pour Google Maps", googleMapsFormat)
        clipboard.setPrimaryClip(clip)
        
        // Afficher un message de confirmation
        Toast.makeText(requireContext(), "Coordonnées copiées pour Google Maps", Toast.LENGTH_SHORT).show()
        
        // Animation de feedback
        coordinatesContainer.animate()
            .alpha(0.5f)
            .setDuration(100)
            .withEndAction {
                coordinatesContainer.animate()
                    .alpha(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    // Ajouter des marqueurs à la carte (à appeler depuis l'activité principale)
    fun addMarkers(markers: List<MarkerData>) {
        if (!isWebViewInitialized) {
            val newMarkers = markers.filterNot { it in pendingMarkers }
            Log.d(TAG, "WebView non initialisée, mise en file d'attente de "+newMarkers.size+" nouveaux marqueurs")
            pendingMarkers.addAll(newMarkers)
            return
        }

        val markersJson = markers.map { marker ->
            """{"lat":${marker.latitude},"lng":${marker.longitude},"title":"${marker.title}","description":${org.json.JSONObject.quote(marker.description ?: "")}}"""
        }.joinToString(",", "[", "]")

        val jsCode = """
            if (typeof addMarkers !== 'undefined') {
                addMarkers('$markersJson');
            } else {
                console.error('addMarkers is not defined');
            }
        """.trimIndent()

        mapWebView.postDelayed({
            mapWebView.evaluateJavascript(jsCode, null)
        }, 500)
    }
    
    // Vérifier si la WebView peut revenir en arrière
    fun canGoBack(): Boolean = mapWebView.canGoBack()
    
    // Revenir en arrière dans la WebView
    fun goBack() {
        if (mapWebView.canGoBack()) {
            mapWebView.goBack()
        }
    }
    
    // Classe de données pour les marqueurs
    data class MarkerData(
        val latitude: Double,
        val longitude: Double,
        val title: String,
        var description: String? = null,
        var photoUris: MutableList<String> = mutableListOf() // Liste des chemins d'images
    )
    
    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).setWaitForAccurateLocation(false)
         .setMinUpdateIntervalMillis(5000)
         .setMaxUpdates(1)
         .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateMapWithCurrentLocation(location: Location) {
        Log.d("MapFragment", "Updating map with location: ${location.latitude}, ${location.longitude}")
        val jsCode = """
            setTimeout(function() {
                if (typeof userCircle !== 'undefined') {
                    map.removeLayer(userCircle);
                }
                console.log('Adding userCircle at:', ${location.latitude}, ${location.longitude});
                userCircle = L.circle([${location.latitude}, ${location.longitude}], {
                    color: '#007bff',
                    fillColor: '#007bff',
                    fillOpacity: 0.5,
                    radius: 10
                }).addTo(map);
            }, 500);
        """
        mapWebView.evaluateJavascript(jsCode, null)
    }

    // Remplacement de onRequestPermissionsResult par ActivityResultContracts
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                requestCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Permission refusée", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestCurrentLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Rendre la méthode executeJavaScript accessible
    fun executeJavaScript(jsCode: String) {
        if (::mapWebView.isInitialized) {
            mapWebView.post {
                mapWebView.evaluateJavascript(jsCode, null)
            }
        }
    }

    // Ajouter une méthode pour charger les points sauvegardés et les afficher
    fun loadSavedPoints() {
        val savedPoints = PointsStorage.loadPoints(requireContext())
        addMarkers(savedPoints)
    }

    // Modifier la méthode d'ajout de marqueurs pour sauvegarder les points
    fun addMarker(latitude: Double, longitude: Double, title: String, description: String? = null) {
        val currentMarkers = PointsStorage.loadPoints(requireContext()).toMutableList()
        // Vérifie si le point existe déjà
        val exists = currentMarkers.any { it.latitude == latitude && it.longitude == longitude }
        if (!exists) {
            currentMarkers.add(MarkerData(latitude, longitude, title, description))
            PointsStorage.savePoints(requireContext(), currentMarkers)
            // Attendre que la WebView soit bien initialisée avant de recharger les markers
            fun refreshMarkersWithRetry(retry: Int = 0) {
                if (isWebViewInitialized) {
                    loadSavedPoints()
                } else if (retry < 10) {
                    binding.root.postDelayed({ refreshMarkersWithRetry(retry + 1) }, 100)
                }
            }
            refreshMarkersWithRetry()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedPoints()
    }
}

// Classe utilitaire pour gérer les points sauvegardés
object PointsStorage {
    private const val FILE_NAME = "points.json"

    fun savePoints(context: Context, points: List<MapFragment.MarkerData>) {
        val file = File(context.filesDir, FILE_NAME)
        val jsonArray = JSONArray()
        points.forEach { point ->
            val jsonObject = JsonObject()
            jsonObject.put("latitude", point.latitude)
            jsonObject.put("longitude", point.longitude)
            jsonObject.put("title", point.title)
            jsonObject.put("description", point.description ?: "")
            // Sauvegarde la liste des images
            val imagesArray = JSONArray()
            point.photoUris.forEach { imagesArray.put(it) }
            jsonObject.put("photoUris", imagesArray)
            jsonArray.put(jsonObject)
        }
        FileWriter(file).use { it.write(jsonArray.toString()) }
    }

    fun loadPoints(context: Context): List<MapFragment.MarkerData> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            Log.d("PointsStorage", "Aucun fichier de points trouvé.")
            return emptyList()
        }
        val jsonArray = JSONArray(FileReader(file).readText())
        val points = mutableListOf<MapFragment.MarkerData>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val latitude = jsonObject.getDouble("latitude")
            val longitude = jsonObject.getDouble("longitude")
            val title = jsonObject.getString("title")
            val description = if (jsonObject.has("description")) jsonObject.getString("description") else null
            val photoUris = mutableListOf<String>()
            if (jsonObject.has("photoUris")) {
                val imagesArray = jsonObject.getJSONArray("photoUris")
                for (j in 0 until imagesArray.length()) {
                    photoUris.add(imagesArray.getString(j))
                }
            }
            points.add(MapFragment.MarkerData(latitude, longitude, title, description, photoUris))
        }
        Log.d("PointsStorage", "Points chargés: ${points.size} points")
        return points
    }
}
