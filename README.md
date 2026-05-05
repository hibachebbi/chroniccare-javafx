# ChronicCare JavaFX

Application desktop JavaFX de suivi medical et bien-etre pour plusieurs roles:
- `Admin`
- `Patient`
- `Coach`
- `Nutritionniste`

Le projet regroupe plusieurs modules autour du suivi patient, des rendez-vous, des consultations nutritionnelles, des evenements, des exercices, du forum, des notifications, du paiement et de l'assistance IA.

## Fonctionnalites principales

- Authentification et gestion des utilisateurs
- Roles multiples: admin, patient, coach, nutritionniste
- Suivi patient
  - etat patient
  - activite patient
  - recommandations et historique
- Module nutrition
  - prise de rendez-vous patient
  - validation des RDV par le nutritionniste
  - consultation nutritionnelle
  - aide Gemini pour generer un brouillon de consultation
  - integration Open Food Facts
- Module coach
  - creation et modification d'evenements
  - creation et modification d'exercices
  - bouton `Suggérer` avec OpenAI pour generer des descriptions
- Forum et publications
- Notifications
- Meteo pour les evenements
- Paiement Stripe
- Email / SMTP
- OAuth Google

## Technologies

- Java 17
- JavaFX
- Maven
- MySQL
- Gson
- OpenAI API
- Gemini API
- Open Food Facts API
- Stripe API
- Jakarta Mail

## Structure du projet

- `src/main/java` : code source Java
- `src/main/resources` : fichiers FXML, CSS et ressources UI
- `config/` : fichiers de configuration locaux
- `sql/` : scripts SQL utilitaires
- `uploads/` : fichiers uploades
- `qrcodes/` : images QR generees
- `target/` : build compile

## Lancement du projet

Prerequis:
- JDK 17
- Maven
- MySQL actif

Commandes utiles:

```bash
mvn clean compile
mvn javafx:run
```

Classe principale:

```text
com.chroniccare.MainApp
```

## Configuration

### Base de donnees

Copier `db.properties.example` en `db.properties`, puis renseigner:

```properties
db.url=jdbc:mysql://localhost:3306/chroniccare?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=
```

### Gemini

Fichier:

```text
config/gemini.properties
```

Exemple:

```properties
gemini.apiKey=VOTRE_CLE_GEMINI
gemini.model=gemini-2.5-flash
```

### OpenAI

Fichiers possibles:
- `openai.properties`
- `openai2.properties`

Exemple:

```properties
OPENAI_API_KEY=VOTRE_CLE_OPENAI
CC_OPENAI_MODEL=gpt-5.4-mini
OPENAI_API_URL=https://api.openai.com/v1/responses
```

### Email / SMTP

Fichiers possibles:
- `smtp.properties`
- `smtp2.properties`

Exemple Gmail SMTP:

```properties
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=votre_email@gmail.com
SMTP_FROM=votre_email@gmail.com
SMTP_PASSWORD=mot_de_passe_application
SMTP_FROM_NAME=ChronicCare
```

Note:
- pour Gmail, utilisez un mot de passe d'application
- ne mettez pas votre mot de passe Gmail principal

### Google OAuth

Fichier:

```text
google-oauth.properties
```

## Roles dans l'application

### Admin
- gestion utilisateurs
- consultations patient
- RDV nutrition
- statistiques
- audit

### Patient
- profil
- prise de RDV
- consultations
- suivi personnel
- participation aux evenements

### Coach
- gestion d'evenements
- gestion d'exercices
- suggestions IA OpenAI

### Nutritionniste
- file des demandes RDV
- validation / refus / replanification
- consultation nutritionnelle
- suggestions Gemini

## IA dans le projet

### OpenAI
Utilise pour:
- suggerer des descriptions d'evenements
- suggerer des descriptions d'exercices
- certains modules d'analyse / rapport selon la configuration

### Gemini
Utilise pour:
- generer un brouillon de consultation nutritionnelle

## Remarques importantes

- Le projet doit etre lance avec Java 17.
- Certains modules dependent de cles API locales non versionnees.
- Les fichiers de configuration contenant des secrets ne doivent pas etre pushes publiquement.
- `target/` contient les classes compilees et peut devoir etre regenere si l'IDE garde une ancienne version.

## Auteurs

Projet ChronicCare realise comme application JavaFX multi-modules de suivi sante, nutrition et coaching.
