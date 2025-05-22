# 🗺️ Quarry Map

[![Android CI](https://github.com/Lolemploi5/Quarry-Map/actions/workflows/android.yml/badge.svg)](https://github.com/Lolemploi5/Quarry-Map/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Issues](https://img.shields.io/github/issues/Lolemploi5/Quarry-Map)](https://github.com/Lolemploi5/Quarry-Map/issues)
[![Stars](https://img.shields.io/github/stars/Lolemploi5/Quarry-Map?style=social)](https://github.com/Lolemploi5/Quarry-Map)

---

## 📖 Présentation

**Quarry Map** est une application Android open-source développée en **Kotlin** pour la gestion, la visualisation et l’annotation de cartes et plans géographiques. Elle s’adresse aux professionnels, étudiants, chercheurs ou passionnés ayant besoin de manipuler des plans, d’ajouter des points GPS, de trier par commune, de travailler hors-ligne et de gérer des favoris.

> **Exemples d’usages** :
> - Archéologues ou géographes annotant des sites sur le terrain
> - Collectivités gérant des plans cadastraux ou d’urbanisme
> - Étudiants en géographie préparant des dossiers cartographiques

---

## ✨ Fonctionnalités principales

- 🔍 **Recherche avancée** de planches par commune, nom ou mot-clé
- 📥 **Importation** de plans via fichiers `.json` (multi-format supporté)
- 🗺️ **Affichage interactif** des cartes avec zoom, déplacement, et navigation fluide
- ➕ **Ajout de points GPS** : nom, coordonnées, description, édition et suppression
- ⭐ **Gestion des favoris** : ajoutez, retirez, retrouvez vos plans préférés
- 🖌️ **Annotations et édition** : modifiez le nom, la description, copiez les coordonnées (format Google Maps)
- 📤 **Exportation** des données et annotations vers fichiers `.json`
- 📄 **Support multi-format** : `.jpg`, `.svg`, `.xml`, etc.
- 🌐 **Mode hors-ligne** : toutes les fonctionnalités restent accessibles sans connexion
- 🖼️ **Visionneuse d’images** : zoom haute résolution, partage, navigation
- 🔄 **Synchronisation automatique** des favoris et des plans (à venir)

---

## 🖼️ Captures d’écran


| Carte interactive | Ajout d’un point | Gestion des favoris |
|------------------|------------------|---------------------|
| ![Carte](docs/Map.gif) | ![Ajout](docs/ajout_points.gif) | ![Favoris](docs/favoris.gif) |

---

## 🚀 Installation

1. **Cloner le projet**
   ```bash
   git clone https://github.com/Lolemploi5/Quarry-Map.git
   cd Quarry-Map
   ```
2. **Ouvrir dans Android Studio**
3. **Synchroniser les dépendances Gradle**
4. **Lancer l’application** sur un émulateur ou un appareil physique

---

## 📂 Structure du projet

```
QuarryMap/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/quarrymap/   # Code source Kotlin
│   │   │   ├── res/                         # Layouts, drawables, etc.
│   │   │   └── AndroidManifest.xml
│   └── ...
├── build.gradle
└── README.md
```

---

## 🛠️ Utilisation

### ➕ Ajouter un point GPS
- Appuyez sur le bouton “+” sur la carte
- Saisissez le nom du point, ajoutez une description si besoin
- Le point apparaît instantanément, cliquable pour voir/modifier ses infos

### ⭐ Gérer les favoris
- Depuis la liste ou la carte, marquez une planche comme favorite
- Accédez à vos favoris via l’onglet dédié
- Suppression et renommage possibles

### 📤 Exporter / 📥 Importer
- Exportez vos annotations et plans au format `.json` pour les partager ou les sauvegarder
- Importez des fichiers `.json` pour enrichir votre base de plans

### 🗺️ Navigation et recherche
- Utilisez la barre de recherche pour filtrer par commune ou nom
- Naviguez entre les onglets Carte, Communes, Favoris

### 📋 Copier les coordonnées
- Depuis la fiche d’un point, cliquez sur l’icône “copier” pour obtenir les coordonnées au format Google Maps

---

## ❓ FAQ

**Q : L’application fonctionne-t-elle hors-ligne ?**
> Oui, toutes les fonctionnalités (ajout, annotation, favoris, navigation) sont accessibles sans connexion internet.

**Q : Quels formats d’images sont supportés ?**
> JPG, PNG, SVG, XML, et d’autres formats courants.

**Q : Comment signaler un bug ou proposer une amélioration ?**
> Ouvrez une issue sur [GitHub](https://github.com/Lolemploi5/Quarry-Map/issues) ou faites une pull request.

**Q : Où sont stockées mes données ?**
> Les plans, points et favoris sont stockés localement sur votre appareil. Rien n’est envoyé sans votre accord.

---

## 🤝 Contribuer

Les contributions sont les bienvenues !

1. **Forkez** le dépôt
2. **Créez une branche** pour votre fonctionnalité
3. **Développez et testez** vos modifications
4. **Ouvrez une pull request** descriptive

---

## 📜 Licence

Ce projet est distribué sous la licence **MIT**. Consultez le fichier [`LICENSE`](LICENSE) pour plus de détails.

---

## 📧 Support & Contact

Pour toute question, suggestion ou bug, ouvrez une issue sur GitHub ou contactez l’auteur via le dépôt.

---

🌟 **Quarry Map** – Votre compagnon cartographique, puissant, fiable et libre, même hors réseau.

