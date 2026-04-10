# CHRONNICCARE JavaFX

Application JavaFX separee du projet Symfony pour gerer uniquement:
- CRUD `Etat`
- CRUD `Activite`

Architecture appliquee:
- `model`
- `repository`
- `service`
- `controller`
- `view` (JavaFX)

## Lancer dans IntelliJ
1. Ouvrir le dossier `CHRONNICCARE` comme projet Maven.
2. Attendre le sync Maven.
3. Lancer `com.chroniccarefx.App`.

## Commandes Maven
- Executer les tests: `mvn test`
- Lancer l'application: `mvn javafx:run`
