// Fonction pour ajouter des marqueurs sur une carte
function addMarkers(map, markers) {
    markers.forEach(marker => {
        const markerElement = document.createElement('div');
        markerElement.className = 'marker';
        markerElement.style.backgroundImage = `url(${marker.icon})`;
        markerElement.style.width = '30px';
        markerElement.style.height = '30px';

        new mapboxgl.Marker(markerElement)
            .setLngLat([marker.longitude, marker.latitude])
            .addTo(map);
    });
}