

# 🗺️ Quarry Map
[![Android CI](https://github.com/Lolemploi5/Quarry-Map/actions/workflows/android.yml/badge.svg)](https://github.com/Lolemploi5/Quarry-Map/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Issues](https://img.shields.io/github/issues/Lolemploi5/Quarry-Map)](https://github.com/Lolemploi5/Quarry-Map/issues)
[![Stars](https://img.shields.io/github/stars/Lolemploi5/Quarry-Map?style=social)](https://github.com/Lolemploi5/Quarry-Map)

**Quarry Map** est une application Android développée en **Kotlin**, permettant la visualisation et la gestion de planches cartographiques, avec tri par communes, superpositions, et annotations GPS. Le tout disponible **hors ligne**.

---

## ✨ Fonctionnalités

- 🔍 Recherche de planches par **communes**
- 📥 **Téléchargement automatique** via un fichier `.json`
- 🗺️ **Affichage des planches** avec **zoom infini**
- ➕ Ajout de **points GPS** avec **annotations** et **photos**
- 📤 **Exportation** des superpositions, annotations, et données vers un `.json`
- ⭐ Gestion des **planches favorites**
- 🖌️ **Éditeur de plans intégré** (mode hors-ligne)
- 📄 Support des formats `.jpg`, `.svg`, `.xml`
- 🌐 Fonctionnalités **hors-ligne**

---

## 📦 Installation

Clone le projet :
```bash
git clone https://github.com/Lolemploi5/Quarry-Map.git
cd Quarry-Map
```

Ouvre-le dans **Android Studio** (Kotlin), synchronise les dépendances Gradle, et lance l'application sur un émulateur ou un appareil.

---

## 📁 Structure du projet (Kotlin / Android)

```
QuarryMap/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/Quarry-Map/
│   │   │   │   ├── ui/
│   │   │   │   ├── data/
│   │   │   │   ├── utils/
│   │   │   │   └── ...
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       └── drawable/
│   │   └── AndroidManifest.xml
├── build.gradle
└── README.md
```

---

## ✅ TODO

- [ ] 📡 Intégration de la synchronisation cloud
- [ ] 🧠 Reconnaissance automatique des zones via IA
- [ ] 🗃️ Mode archivage automatique pour les anciennes planches

---

## 🧑‍💻 Contribuer

Les contributions sont les bienvenues 🙌  
Fork, crée une branche, propose des changements et ouvre une **pull request** !

```bash
git checkout -b feat/ma-nouvelle-feature
git commit -m "Ajout de ..."
git push origin feat/ma-nouvelle-feature
```

---

## 📄 Licence

Distribué sous licence **MIT**. Voir [`LICENSE`](LICENSE) pour plus d'informations.

---

🧱 **Quarry Map** – L'outil cartographique conçu pour le terrain, puissant même sans réseau.

