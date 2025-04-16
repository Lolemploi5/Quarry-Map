package com.example.quarrymap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
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
import androidx.fragment.app.Fragment
import com.example.quarrymap.databinding.FragmentMapBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

private const val TAG = "MapFragment"

class MapFragment : Fragment(), PlanchesBottomSheetFragment.PlanchesBottomSheetCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var mapWebView: WebView
    private lateinit var coordinatesTextView: TextView
    private lateinit var coordinatesContainer: LinearLayout
    private lateinit var planchesButton: FloatingActionButton
    
    private var isWebViewInitialized = false
    
    // Variables pour stocker les valeurs de latitude et longitude actuelles
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    // Liste des planches affichées sur la carte
    private val activePlanches = mutableListOf<PlancheOverlay>()
    
    // Instance de la bottom sheet
    private var planchesBottomSheet: PlanchesBottomSheetFragment? = null

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
        planchesButton = binding.planchesButton
        
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
        
        // Configurer le bouton d'affichage des planches
        planchesButton.setOnClickListener {
            showPlanchesBottomSheet()
        }
        
        setupWebView()
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
                
                // Ajouter toutes les planches actives à la carte après chargement
                activePlanches.forEach { planche ->
                    addOverlayToMap(planche)
                }
            }
        }
        
        // Intercepter les événements tactiles pour la manipulation des planches
        mapWebView.setOnTouchListener { _, event ->
            handleMapTouch(event)
            false // Ne pas consommer l'événement - laisser la WebView le traiter normalement
        }
        
        // Chargement de la carte OpenStreetMap
        loadMap()
    }
    
    private fun loadMap() {
        try {
            // Charger une carte OpenStreetMap de base via Leaflet avec support pour les overlays
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
                        body { 
                            margin: 0; 
                            padding: 0; 
                            overflow: hidden;
                        }
                        #map { 
                            position: absolute; 
                            top: 0; 
                            bottom: 0; 
                            width: 100%; 
                            height: 100%; 
                            z-index: 1;
                        }
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
                        
                        #overlay-container {
                            position: absolute;
                            top: 0;
                            left: 0;
                            width: 100%;
                            height: 100%;
                            z-index: 900;
                            pointer-events: none;
                        }
                        
                        /* Styles pour l'overlay de planche */
                        .planche-overlay {
                            position: absolute;
                            transform-origin: center;
                            transition: opacity 0.3s ease;
                            pointer-events: auto;
                            cursor: move;
                            z-index: 950;
                            background-color: rgba(0, 0, 0, 0.1);
                            border-radius: 4px;
                            overflow: hidden;
                            max-width: 80%;
                            max-height: 80%;
                            touch-action: none;
                            border: 2px solid rgba(255, 255, 255, 0.2);
                        }
                        
                        .planche-overlay img {
                            display: block;
                            width: 100%;
                            height: 100%;
                            object-fit: contain;
                            pointer-events: none;
                        }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <div class="center-marker">
                        <div class="marker-pulse"></div>
                    </div>
                    <div id="overlay-container"></div>
                    <script>
                        // Initialiser la carte
                        var map = L.map('map', {
                            zoomControl: false,
                            attributionControl: false
                        }).setView([46.603354, 1.888334], 6); // Vue centrée sur la France
                        
                        // Ajouter les tuiles OpenStreetMap
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        }).addTo(map);
                        
                        // Objet pour stocker les références aux overlays de planches
                        var plancheOverlays = {};
                        
                        // Variables pour le déplacement des planches
                        var activeOverlay = null;
                        var startX, startY;
                        var offsetX, offsetY;
                        
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
                        
                        // Fonction pour ajouter une planche à la carte
                        function addPlancheOverlay(id, imagePath, lat, lng, rotation, scale, opacity) {
                            console.log("Ajout d'une planche:", id, imagePath);
                            
                            try {
                                // Supprimer l'overlay existant avec le même ID s'il existe
                                if (plancheOverlays[id]) {
                                    removePlancheOverlay(id);
                                }
                                
                                const overlayContainer = document.getElementById('overlay-container');
                                
                                // Créer l'élément pour l'overlay
                                const overlay = document.createElement('div');
                                overlay.className = 'planche-overlay';
                                overlay.id = 'planche-' + id;
                                overlay.style.opacity = opacity;
                                overlay.style.transform = 'translate(-50%, -50%) rotate(' + rotation + 'deg) scale(' + scale + ')';
                                overlay.style.width = '300px';  // Taille par défaut avant chargement
                                overlay.style.height = '300px';
                                
                                // Créer l'image
                                const img = document.createElement('img');
                                img.onerror = function(e) {
                                    console.error("Erreur de chargement de l'image:", imagePath, e);
                                    Android.onPlancheError(id, "Erreur de chargement de l'image: " + e.type);
                                    overlay.remove();
                                    delete plancheOverlays[id];
                                    return false;
                                };
                                
                                img.onload = function() {
                                    console.log("Image chargée avec succès:", imagePath);
                                    
                                    // Après le chargement, adapter la taille en fonction de l'image
                                    var aspectRatio = img.naturalWidth / img.naturalHeight;
                                    var maxWidth = window.innerWidth * 0.7; // 70% de la largeur d'écran
                                    var maxHeight = window.innerHeight * 0.7; // 70% de la hauteur d'écran
                                    
                                    var finalWidth, finalHeight;
                                    
                                    if (aspectRatio > 1) {
                                        // Image plus large que haute
                                        finalWidth = Math.min(img.naturalWidth, maxWidth);
                                        finalHeight = finalWidth / aspectRatio;
                                    } else {
                                        // Image plus haute que large
                                        finalHeight = Math.min(img.naturalHeight, maxHeight);
                                        finalWidth = finalHeight * aspectRatio;
                                    }
                                    
                                    overlay.style.width = finalWidth + 'px';
                                    overlay.style.height = finalHeight + 'px';
                                    
                                    // Mettre à jour la position après avoir défini la taille
                                    updateOverlayPosition(id, lat, lng);
                                };
                                
                                img.alt = 'Planche overlay';
                                img.src = imagePath;
                                
                                overlay.appendChild(img);
                                overlayContainer.appendChild(overlay);
                                
                                // Ajouter les gestionnaires d'événements
                                overlay.addEventListener('mousedown', startDrag);
                                overlay.addEventListener('touchstart', startDrag, { passive: false });
                                
                                // Stocker les références
                                plancheOverlays[id] = {
                                    element: overlay,
                                    img: img,
                                    lat: lat,
                                    lng: lng,
                                    rotation: rotation,
                                    scale: scale,
                                    opacity: opacity
                                };
                                
                                // Mettre à jour la position
                                updateOverlayPosition(id, lat, lng);
                                
                                return true;
                            } catch (e) {
                                console.error("Erreur lors de l'ajout de l'overlay:", e);
                                Android.onPlancheError(id, "Erreur: " + e.message);
                                return false;
                            }
                        }
                        
                        // Mettre à jour la position d'une planche
                        function updateOverlayPosition(id, lat, lng) {
                            const overlay = plancheOverlays[id];
                            if (!overlay) return;
                            
                            const point = map.latLngToContainerPoint([lat, lng]);
                            overlay.element.style.left = point.x + 'px';
                            overlay.element.style.top = point.y + 'px';
                            overlay.lat = lat;
                            overlay.lng = lng;
                        }
                        
                        // Démarrer le déplacement d'une planche
                        function startDrag(e) {
                            e.preventDefault();
                            
                            const overlay = this;
                            const id = overlay.id.replace('planche-', '');
                            
                            // Obtenir la position initiale
                            if (e.type === 'touchstart') {
                                startX = e.touches[0].clientX;
                                startY = e.touches[0].clientY;
                            } else {
                                startX = e.clientX;
                                startY = e.clientY;
                            }
                            
                            // Calculer le décalage entre le point de clic et le centre de l'overlay
                            const rect = overlay.getBoundingClientRect();
                            offsetX = startX - (rect.left + rect.width / 2);
                            offsetY = startY - (rect.top + rect.height / 2);
                            
                            // Mettre à jour l'overlay actif
                            activeOverlay = id;
                            
                            // Augmenter le z-index
                            overlay.style.zIndex = '999';
                            
                            // Ajouter les écouteurs pour le mouvement
                            document.addEventListener('mousemove', onDrag);
                            document.addEventListener('touchmove', onDrag, { passive: false });
                            document.addEventListener('mouseup', stopDrag);
                            document.addEventListener('touchend', stopDrag);
                        }
                        
                        // Gérer le déplacement d'une planche
                        function onDrag(e) {
                            if (!activeOverlay) return;
                            
                            e.preventDefault();
                            
                            // Obtenir la position actuelle
                            let currentX, currentY;
                            if (e.type === 'touchmove') {
                                currentX = e.touches[0].clientX;
                                currentY = e.touches[0].clientY;
                            } else {
                                currentX = e.clientX;
                                currentY = e.clientY;
                            }
                            
                            // Calculer la nouvelle position centrale de l'overlay
                            const centerX = currentX - offsetX;
                            const centerY = currentY - offsetY;
                            
                            // Convertir la position en coordonnées géographiques
                            const newLatLng = map.containerPointToLatLng([centerX, centerY]);
                            
                            // Mettre à jour la position
                            updateOverlayPosition(activeOverlay, newLatLng.lat, newLatLng.lng);
                        }
                        
                        // Arrêter le déplacement d'une planche
                        function stopDrag() {
                            if (!activeOverlay) return;
                            
                            // Rétablir le z-index normal
                            const overlay = plancheOverlays[activeOverlay];
                            if (overlay) {
                                overlay.element.style.zIndex = '950';
                                
                                // Notifier Android du changement de position
                                Android.onPlancheMoved(activeOverlay, overlay.lat, overlay.lng);
                            }
                            
                            // Retirer les écouteurs
                            document.removeEventListener('mousemove', onDrag);
                            document.removeEventListener('touchmove', onDrag);
                            document.removeEventListener('mouseup', stopDrag);
                            document.removeEventListener('touchend', stopDrag);
                            
                            activeOverlay = null;
                        }
                        
                        // Mettre à jour les propriétés d'une planche
                        function updatePlancheOverlay(id, lat, lng, rotation, scale, opacity) {
                            const overlay = plancheOverlays[id];
                            if (!overlay) return false;
                            
                            overlay.rotation = rotation;
                            overlay.scale = scale;
                            overlay.opacity = opacity;
                            
                            // Mettre à jour les styles
                            overlay.element.style.opacity = opacity;
                            overlay.element.style.transform = 'translate(-50%, -50%) rotate(' + rotation + 'deg) scale(' + scale + ')';
                            
                            // Mettre à jour la position
                            updateOverlayPosition(id, lat, lng);
                            
                            return true;
                        }
                        
                        // Supprimer une planche
                        function removePlancheOverlay(id) {
                            const overlay = plancheOverlays[id];
                            if (!overlay) return false;
                            
                            // Supprimer l'élément du DOM
                            overlay.element.remove();
                            
                            // Supprimer la référence
                            delete plancheOverlays[id];
                            
                            return true;
                        }
                        
                        // Déplacer une planche au centre de la carte
                        function movePlancheToCenter(id) {
                            const center = map.getCenter();
                            const overlay = plancheOverlays[id];
                            if (!overlay) return null;
                            
                            // Mettre à jour la position
                            updateOverlayPosition(id, center.lat, center.lng);
                            
                            return JSON.stringify({lat: center.lat, lng: center.lng});
                        }
                        
                        // Mettre à jour toutes les positions des planches après un mouvement de carte
                        function updateAllPlanchePositions() {
                            for (const id in plancheOverlays) {
                                if (plancheOverlays.hasOwnProperty(id)) {
                                    const overlay = plancheOverlays[id];
                                    updateOverlayPosition(id, overlay.lat, overlay.lng);
                                }
                            }
                        }
                        
                        // Événements de la carte
                        map.on('load', updateCenterCoordinates);
                        map.on('move', function() {
                            updateCenterCoordinates();
                            updateAllPlanchePositions();
                        });
                        map.on('moveend', updateCenterCoordinates);
                        
                        // Mettre à jour après un court délai pour s'assurer que la carte est entièrement chargée
                        setTimeout(function() {
                            updateCenterCoordinates();
                            console.log("Carte initialisée");
                        }, 500);
                    </script>
                </body>
                </html>
            """.trimIndent()
            
            // Activer la console JavaScript pour le débogage
            mapWebView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadsImagesAutomatically = true
                allowFileAccess = true  // Important pour charger les fichiers locaux
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            
            // Ajouter un utilitaire de débogage JavaScript
            if (BuildConfig.DEBUG) {
                mapWebView.webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                        Log.d("WebView", "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                        return true
                    }
                }
            }
            
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
        fun onPlancheMoved(id: String, lat: Double, lng: Double) {
            Log.d(TAG, "Planche déplacée: ID=$id, Lat=$lat, Lng=$lng")
            activity?.runOnUiThread {
                val planche = activePlanches.find { it.id == id }
                planche?.let {
                    it.latitude = lat
                    it.longitude = lng
                }
            }
        }
        
        @JavascriptInterface
        fun onPlancheError(id: String, errorMessage: String) {
            Log.e(TAG, "Erreur avec la planche $id: $errorMessage")
            activity?.runOnUiThread {
                Toast.makeText(context, "Erreur avec la planche: $errorMessage", Toast.LENGTH_SHORT).show()
                
                // Supprimer la planche problématique de la liste des planches actives
                val plancheToRemove = activePlanches.find { it.id == id }
                plancheToRemove?.let {
                    activePlanches.remove(it)
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
    
    // Afficher la bottom sheet des planches
    private fun showPlanchesBottomSheet() {
        if (planchesBottomSheet == null) {
            planchesBottomSheet = PlanchesBottomSheetFragment.newInstance()
        }
        
        planchesBottomSheet?.show(childFragmentManager, PlanchesBottomSheetFragment.TAG)
    }
    
    // Ajouter une planche overlay à la carte
    fun addOverlayToMap(planche: PlancheOverlay) {
        if (!isWebViewInitialized) {
            Log.d(TAG, "WebView non initialisée, impossible d'ajouter l'overlay")
            return
        }
        
        // Vérifier si le fichier existe
        if (!planche.plancheFile.exists() || planche.plancheFile.length() <= 0) {
            Log.e(TAG, "Erreur: fichier de planche inexistant ou vide: ${planche.plancheFile.absolutePath}")
            Toast.makeText(context, "Erreur: fichier de planche inexistant ou vide", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Utiliser l'URI de contenu pour les accès aux fichiers dans la WebView
        val contentUri = Uri.fromFile(planche.plancheFile).toString()
        Log.d(TAG, "Ajout d'une planche à la carte: ${planche.plancheFile.name}, URI: $contentUri")
        
        val script = """
            addPlancheOverlay(
                "${planche.id}",
                "$contentUri",
                ${planche.latitude},
                ${planche.longitude},
                ${planche.rotation},
                ${planche.scale},
                ${planche.opacity}
            );
        """.trimIndent()
        
        mapWebView.evaluateJavascript(script) { result ->
            Log.d(TAG, "Résultat de l'ajout de planche: $result")
            if (result.trim() == "true") {
                // Ajouter la planche à la liste des planches actives si elle n'y est pas déjà
                if (activePlanches.none { it.id == planche.id }) {
                    activePlanches.add(planche)
                }
            } else {
                Log.e(TAG, "Échec de l'ajout de la planche: ${planche.plancheFile.name}")
                Toast.makeText(context, "Échec de l'ajout de la planche: ${planche.plancheFile.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Mettre à jour une planche sur la carte
    private fun updateOverlayOnMap(planche: PlancheOverlay) {
        if (!isWebViewInitialized) return
        
        val script = """
            updatePlancheOverlay(
                "${planche.id}",
                ${planche.latitude},
                ${planche.longitude},
                ${planche.rotation},
                ${planche.scale},
                ${planche.opacity}
            )
        """.trimIndent()
        
        mapWebView.evaluateJavascript(script) { result ->
            Log.d(TAG, "Résultat de la mise à jour de planche: $result")
        }
    }
    
    // Supprimer une planche de la carte
    private fun removeOverlayFromMap(planche: PlancheOverlay) {
        if (!isWebViewInitialized) return
        
        val script = """
            removePlancheOverlay("${planche.id}")
        """.trimIndent()
        
        mapWebView.evaluateJavascript(script) { result ->
            Log.d(TAG, "Résultat de la suppression de planche: $result")
        }
    }
    
    // Déplacer une planche au centre de la carte
    private fun movePlancheToCenter(planche: PlancheOverlay) {
        if (!isWebViewInitialized) return
        
        val script = """
            movePlancheToCenter("${planche.id}")
        """.trimIndent()
        
        mapWebView.evaluateJavascript(script) { result ->
            if (result != "null") {
                try {
                    // Format attendu: {"lat":12.34,"lng":56.78}
                    val jsonStr = result.trim('"')
                    val latStart = jsonStr.indexOf("lat\":") + 5
                    val latEnd = jsonStr.indexOf(",", latStart)
                    val lngStart = jsonStr.indexOf("lng\":") + 5
                    val lngEnd = jsonStr.indexOf("}", lngStart)
                    
                    val lat = jsonStr.substring(latStart, latEnd).toDouble()
                    val lng = jsonStr.substring(lngStart, lngEnd).toDouble()
                    
                    planche.latitude = lat
                    planche.longitude = lng
                    
                    // Afficher les contrôles de la planche
                    planchesBottomSheet?.showControlsForPlanche(planche)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de l'analyse de la position de la planche", e)
                }
            }
        }
    }
    
    // Gérer les événements tactiles pour la manipulation des planches
    private fun handleMapTouch(event: MotionEvent) {
        // Implémenter la logique de manipulation des planches si nécessaire
        // Par exemple, détecter un long press pour sélectionner une planche
        Log.d(TAG, "Gestion des touches sur la carte")
        // Utilisation de l'événement pour éviter l'avertissement
        if (event.action == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "Toucher détecté à la position: ${event.x}, ${event.y}")
        }
    }
    
    // Callbacks de l'interface PlanchesBottomSheetCallback
    override fun onPlancheSelected(planche: File) {
        // Créer un nouvel overlay avec la position actuelle du centre
        val overlay = PlancheOverlay(
            plancheFile = planche,
            latitude = currentLatitude,
            longitude = currentLongitude
        )
        
        // Ajouter à la liste des planches actives
        activePlanches.add(overlay)
        
        // Ajouter à la carte
        addOverlayToMap(overlay)
        
        // Afficher les contrôles de la planche
        planchesBottomSheet?.showControlsForPlanche(overlay)
    }
    
    override fun onPlancheUpdated(planche: PlancheOverlay) {
        // Mettre à jour la planche sur la carte
        updateOverlayOnMap(planche)
    }
    
    override fun onPlancheRemoved(planche: PlancheOverlay) {
        // Supprimer la planche de la carte
        removeOverlayFromMap(planche)
        
        // Retirer de la liste des planches actives
        activePlanches.remove(planche)
    }
    
    override fun onAddPlancheRequested() {
        // Ouvrir un dialogue de sélection de fichier
        (activity as? MainActivity)?.openFolderPicker()
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
