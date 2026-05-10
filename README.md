# 🩺 ChronicCare JavaFX — Module Événement

![Java](https://img.shields.io/badge/Java-17-red?style=for-the-badge)
![JavaFX](https://img.shields.io/badge/JavaFX-Desktop-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-orange?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-Database-lightblue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-En%20développement-yellow?style=for-the-badge)

---

## 📌 Présentation

**ChronicCare** est une application desktop développée avec **JavaFX** pour le suivi médical, le bien-être et le coaching.

Cette branche est dédiée au **module Événement**.  
Elle permet aux coachs de créer et gérer des événements liés à la santé, au sport et au bien-être, tandis que les patients peuvent consulter les événements disponibles et y participer.

---

## ✨ Fonctionnalités principales

### 👨‍🏫 Côté Coach

- Créer un événement
- Modifier un événement existant
- Supprimer un événement
- Ajouter une image à un événement
- Générer une description avec l’IA
- Consulter les participations des patients
- Gérer les places disponibles

### 🧑‍⚕️ Côté Patient

- Consulter la liste des événements
- Voir les détails d’un événement
- Participer à un événement
- Consulter les informations importantes :
  - Date
  - Lieu
  - Description
  - Capacité
  - Places disponibles

---

## 🚀 Fonctionnalités avancées

| Fonctionnalité | Description |
|---|---|
| 🤖 Suggestions IA | Génération automatique de descriptions avec OpenAI |
| 🌦️ Météo | Affichage de la météo liée aux événements |
| 🖼️ Upload d’images | Ajout d’images pour illustrer les événements |
| 🔳 QR Code | Génération de QR Codes pour les événements |
| 🎨 Interface JavaFX | Interfaces créées avec FXML et CSS |
| 👥 Participation | Gestion des patients inscrits aux événements |

---

## 🛠️ Technologies utilisées

| Technologie | Utilisation |
|---|---|
| Java 17 | Langage principal |
| JavaFX | Interface graphique desktop |
| Maven | Gestion du projet et des dépendances |
| MySQL | Base de données |
| Gson | Manipulation des données JSON |
| OpenAI API | Suggestions de descriptions |
| API météo | Informations météorologiques |
| QR Code Generator | Génération des QR Codes |
| FXML / CSS | Structure et style de l’interface |

---

## 📁 Structure utile du projet

```text
src/main/java          Code source Java
src/main/resources     Fichiers FXML, CSS et ressources UI
config/                Fichiers de configuration locaux
uploads/               Images uploadées
qrcodes/               QR Codes générés
target/                Build Maven généré
```

---

## ▶️ Lancement du projet

### Prérequis

- JDK 17
- Maven
- MySQL actif
- Base de données ChronicCare configurée

### Commandes

```bash
mvn clean compile
mvn javafx:run
```

### Classe principale

```text
com.chroniccare.MainApp
```

---

## ⚙️ Configuration

### Base de données

Copier le fichier :

```text
db.properties.example
```

Puis le renommer en :

```text
db.properties
```

Exemple :

```properties
db.url=jdbc:mysql://localhost:3306/chroniccare?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=
```

---

### OpenAI

L’API OpenAI est utilisée pour générer automatiquement des suggestions de descriptions d’événements.

Fichiers possibles :

```text
openai.properties
openai2.properties
```

Exemple :

```properties
OPENAI_API_KEY=VOTRE_CLE_OPENAI
CC_OPENAI_MODEL=gpt-5.4-mini
OPENAI_API_URL=https://api.openai.com/v1/responses
```

---

## ⚠️ Remarques importantes

- Le projet doit être lancé avec **Java 17**.
- Les clés API doivent rester locales.
- Les fichiers contenant des secrets ne doivent pas être envoyés sur GitHub.
- Le dossier `target/` peut être régénéré avec Maven.
- Les dossiers `uploads/` et `qrcodes/` peuvent contenir des fichiers générés localement.

---

## 👤 Auteur

Module **Événement** développé dans le cadre du projet **ChronicCare**.
