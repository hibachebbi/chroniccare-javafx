ChronicCare JavaFX — Branche Événement

Branche dédiée au module Événement de l’application desktop JavaFX ChronicCare.

Ce module permet aux coachs de gérer des événements liés à la santé et au bien-être, et aux patients de consulter les événements disponibles et d’y participer.

Fonctionnalités
Création d’événements
Modification d’événements
Suppression d’événements
Affichage de la liste des événements
Consultation des détails d’un événement
Participation des patients aux événements
Gestion des places disponibles
Upload d’images pour les événements
Génération de QR Codes
Suggestions de descriptions avec OpenAI
Affichage de la météo liée aux événements
Rôles concernés
Coach

Le coach peut :

Ajouter un événement
Modifier un événement existant
Supprimer un événement
Ajouter une image
Générer une description avec l’IA
Consulter les participations des patients
Patient

Le patient peut :

Consulter les événements disponibles
Voir les détails d’un événement
Participer à un événement
Technologies utilisées
Java 17
JavaFX
Maven
MySQL
Gson
OpenAI API
API météo
QR Code Generator
FXML / CSS
Structure utile

src/main/java
src/main/resources
config/
uploads/
qrcodes/
target/

Lancement du projet
Prérequis
JDK 17
Maven
MySQL actif
Base de données ChronicCare configurée
Commandes

mvn clean compile

mvn javafx:run

Classe principale

com.chroniccare.MainApp

Configuration base de données

Copier :

db.properties.example

Puis le renommer en :

db.properties

Exemple :

db.url=jdbc:mysql://localhost:3306/chroniccare?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=

Configuration OpenAI

Fichiers possibles :

openai.properties
openai2.properties

Exemple :

OPENAI_API_KEY=VOTRE_CLE_OPENAI
CC_OPENAI_MODEL=gpt-5.4-mini
OPENAI_API_URL=https://api.openai.com/v1/responses

Remarques
Le projet doit être lancé avec Java 17.
Les clés API doivent rester locales.
Les fichiers contenant des secrets ne doivent pas être envoyés sur GitHub.
Le dossier target/ peut être régénéré avec Maven.
Les dossiers uploads/ et qrcodes/ peuvent contenir des fichiers générés localement.
Auteur

Module Événement développé dans le cadre du projet ChronicCare.
