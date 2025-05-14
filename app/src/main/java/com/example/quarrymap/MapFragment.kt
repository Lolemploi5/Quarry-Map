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
import org.json.JSONObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

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
            builtInZoomControls = true
            displayZoomControls = false
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
                        function addMarkers(markersJson) {
                            const markers = JSON.parse(markersJson);
                            markers.forEach(marker => {
                                L.marker([marker.lat, marker.lng])
                                    .addTo(map)
                                    .bindPopup(marker.title);
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
            Log.d(TAG, "WebView non initialisée, impossible d'ajouter des marqueurs")
            return
        }
        
        val markersJson = markers.map { marker ->
            """{"lat":${marker.latitude},"lng":${marker.longitude},"title":"${marker.title}"}"""
        }.joinToString(",", "[", "]")
        
        val jsCode = "addMarkers('$markersJson');"
        mapWebView.evaluateJavascript(jsCode, null)
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
        val title: String
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
}
