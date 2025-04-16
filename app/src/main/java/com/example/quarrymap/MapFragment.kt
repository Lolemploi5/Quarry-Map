package com.example.quarrymap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class MapFragment : Fragment() {
    
    private lateinit var webView: WebView
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        webView = view.findViewById(R.id.mapWebView)
        setupWebView()
    }
    
    private fun setupWebView() {
        // Activer JavaScript pour les cartes interactives
        webView.settings.javaScriptEnabled = true
        
        // Activer le zoom
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        
        // Configurer le client WebView pour gérer les redirections à l'intérieur du WebView
        webView.webViewClient = WebViewClient()
        
        // Charger une carte OpenStreetMap comme exemple
        // Dans un cas réel, vous pourriez vouloir charger une carte plus spécifique ou utiliser Google Maps
        loadOpenStreetMap()
    }
    
    private fun loadOpenStreetMap() {
        // Centre de la France par défaut (à ajuster selon vos besoins)
        val latitude = 46.603354
        val longitude = 1.888334
        val zoom = 6
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                <style>
                    html, body, #map {
                        width: 100%;
                        height: 100%;
                        margin: 0;
                        padding: 0;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([$latitude, $longitude], $zoom);
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    }).addTo(map);
                    
                    // Exemple d'ajout d'un marqueur
                    // L.marker([48.856614, 2.3522219]).addTo(map)
                    //    .bindPopup('Paris')
                    //    .openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadData(html, "text/html", "UTF-8")
    }
    
    // Méthode pour gérer le retour en arrière dans le WebView
    fun canGoBack(): Boolean {
        return webView.canGoBack()
    }
    
    fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }
}
