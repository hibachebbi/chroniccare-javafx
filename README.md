# 🩺 ChronicCare JavaFX

![Java](https://img.shields.io/badge/Java-17-red?style=for-the-badge)
![JavaFX](https://img.shields.io/badge/JavaFX-Desktop-blue?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-orange?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-Database-lightblue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-En%20développement-yellow?style=for-the-badge)

---

## 📌 Présentation

**ChronicCare** est une application desktop développée avec **JavaFX**, dédiée au suivi médical, au bien-être, à la nutrition et au coaching.

L’application regroupe plusieurs modules autour du suivi patient, des rendez-vous, des consultations nutritionnelles, des événements, des exercices, du forum, des notifications, du paiement et de l’assistance par intelligence artificielle.

---

## 👥 Rôles de l’application

| Rôle | Description |
|---|---|
| `Admin` | Supervise la plateforme, les utilisateurs, les statistiques et l’audit |
| `Patient` | Suit son état de santé, ses activités, ses rendez-vous et ses recommandations |
| `Coach` | Gère les événements, les exercices et les suggestions IA |
| `Nutritionniste` | Gère les rendez-vous nutritionnels et les consultations |

---

## ✨ Fonctionnalités principales

### 🔐 Authentification et gestion des utilisateurs

- Connexion des utilisateurs
- Gestion des rôles
- Accès différencié selon le profil utilisateur
- Administration des comptes

---

### 🧑‍⚕️ Suivi patient

- Suivi de l’état du patient
- Suivi des activités
- Historique personnel
- Recommandations personnalisées
- Consultation du profil patient

---

### 🥗 Module nutrition

- Prise de rendez-vous par le patient
- Validation des rendez-vous par le nutritionniste
- Refus ou replanification des rendez-vous
- Création de consultations nutritionnelles
- Génération d’un brouillon de consultation avec **Gemini**
- Intégration de **Open Food Facts**

---

### 🏃 Module coach

- Création et modification d’événements
- Création et modification d’exercices
- Suggestions de descriptions avec **OpenAI**
- Gestion des activités liées au bien-être et au coaching

---

### 💬 Forum et services intégrés

- Forum et publications
- Notifications
- Météo pour les événements
- Paiement avec Stripe
- Email / SMTP
- OAuth Google

---

## 🤖 Intelligence artificielle

| IA | Utilisation |
|---|---|
| **OpenAI** | Génération de descriptions pour les événements et exercices |
| **Gemini** | Génération de brouillons pour les consultations nutritionnelles |

---

## 🛠️ Technologies utilisées

| Technologie | Utilisation |
|---|---|
| Java 17 | Langage principal |
| JavaFX | Interface desktop |
| Maven | Gestion du projet et des dépendances |
| MySQL | Base de données |
| Gson | Manipulation des données JSON |
| OpenAI API | Suggestions IA |
| Gemini API | Génération de contenu nutritionnel |
| Open Food Facts API | Données nutritionnelles |
| Stripe API | Paiement en ligne |
| Jakarta Mail | Envoi d’emails |
| Google OAuth | Authentification Google |

---

## 📁 Structure du projet

```text
src/main/java          Code source Java
src/main/resources     Fichiers FXML, CSS et ressources UI
config/                Fichiers de configuration locaux
sql/                   Scripts SQL utilitaires
uploads/               Fichiers uploadés
qrcodes/               Images QR générées
target/                Build compilé généré par Maven
```

---

## ▶️ Lancement du projet

### Prérequis

Avant de lancer le projet, il faut avoir :

- JDK 17
- Maven
- MySQL actif
- Une base de données ChronicCare configurée

---

### Commandes utiles

```bash
mvn clean compile
mvn javafx:run
```

---

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

Exemple de configuration :

```properties
db.url=jdbc:mysql://localhost:3306/chroniccare?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=
```

---

### Gemini

Fichier de configuration :

```text
config/gemini.properties
```

Exemple :

```properties
gemini.apiKey=VOTRE_CLE_GEMINI
gemini.model=gemini-2.5-flash
```

---

### OpenAI

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

### Email / SMTP

Fichiers possibles :

```text
smtp.properties
smtp2.properties
```

Exemple Gmail SMTP :

```properties
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=votre_email@gmail.com
SMTP_FROM=votre_email@gmail.com
SMTP_PASSWORD=mot_de_passe_application
SMTP_FROM_NAME=ChronicCare
```

> Pour Gmail, utilisez un mot de passe d’application.  
> Ne mettez jamais votre mot de passe Gmail principal dans le projet.

---

### Google OAuth

Fichier de configuration :

```text
google-oauth.properties
```

---

## 🧩 Modules par rôle

### 👨‍💼 Admin

- Gestion des utilisateurs
- Consultation des données patient
- Gestion des rendez-vous nutritionnels
- Statistiques
- Audit

---

### 🧑‍⚕️ Patient

- Gestion du profil
- Prise de rendez-vous
- Consultation du suivi personnel
- Accès aux consultations
- Participation aux événements

---

### 🏃 Coach

- Gestion des événements
- Gestion des exercices
- Suggestions IA avec OpenAI

---

### 🥗 Nutritionniste

- File des demandes de rendez-vous
- Validation, refus ou replanification des rendez-vous
- Création de consultations nutritionnelles
- Suggestions avec Gemini

---

## ⚠️ Remarques importantes

- Le projet doit être lancé avec **Java 17**.
- Certains modules dépendent de clés API locales non versionnées.
- Les fichiers contenant des secrets ne doivent pas être envoyés sur GitHub.
- Le dossier `target/` contient les classes compilées et peut être régénéré avec Maven.
- Si l’IDE garde une ancienne version du projet, relancer :

```bash
mvn clean compile
```

---

## 👤 Auteurs

Projet **ChronicCare** réalisé comme application JavaFX multi-modules dédiée au suivi santé, à la nutrition, au coaching et au bien-être.
