# 🔧 Guide de Dépannage - Erreurs d'Exécution

## 🚨 Problèmes Courants et Solutions

### ❌ Problème 1: "Cannot find symbol: class StockAlertService"
**Cause**: Import manquant ou classe non trouvée  
**Solution**:
1. Ouvrir IntelliJ → `Build` → `Rebuild Project`
2. Ou: `Ctrl + Shift + O` pour réorganiser les imports

### ❌ Problème 2: "FXML load exception: ... is not a valid location"
**Cause**: Fichier FXML n'existe pas au bon endroit  
**Solution**:
1. Vérifier que les fichiers FXML existent:
   - `resources/com/chroniccare/Admin/AdminDashboard.fxml` ✓
   - `resources/com/chroniccare/Client/ClientHistorique.fxml` ✓
2. Nettoyer et reconstruire:
   - `Build` → `Clean` → `Rebuild Project`

### ❌ Problème 3: "NullPointerException at AdminDashboardController.initialize()"
**Cause**: Champ @FXML non initialisé  
**Solution**:
1. S'assurer que le fichier FXML a `fx:controller="com.chroniccare.controllers.Admin.AdminDashboardController"`
2. Vérifier que tous les `fx:id` du FXML correspondent aux champs @FXML du code

### ❌ Problème 4: Cannot find symbols dans les Services
**Cause**: Imports manquants dans les nouveaux Services  
**Solution**:
1. Ouvrir chaque fichier:
   - `CommandeWorkflowService.java`
   - `StockAlertService.java`
   - `LivraisonWorkflowService.java`
2. Appuyer sur `Alt + Enter` pour auto-importer les classes manquantes

### ❌ Problème 5: "The resource is not on the classpath"
**Cause**: Fichiers FXML non packagés correctement  
**Solution**:
1. Aller dans `Build` → `Build Artifacts`
2. Reconstruire le JAR

---

## ✅ Checklist de Vérification

- [ ] **Imports corrects dans les Services**
  ```java
  import com.chroniccare.utils.MyDatabase;
  import java.sql.*;
  ```

- [ ] **Imports dans les Contrôleurs**
  ```java
  import com.chroniccare.services.StockAlertService;
  import com.chroniccare.services.CommandeWorkflowService;
  ```

- [ ] **Fichiers FXML existent**
  - `AdminDashboard.fxml` dans `Admin/`
  - `ClientHistorique.fxml` dans `Client/`

- [ ] **fx:controller correct dans FXML**
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <!-- Pour AdminDashboard.fxml -->
  <VBox fx:controller="com.chroniccare.controllers.Admin.AdminDashboardController">
  
  <!-- Pour ClientHistorique.fxml -->
  <VBox fx:controller="com.chroniccare.controllers.Client.ClientHistoriqueController">
  ```

- [ ] **Tous les fx:id correspondent aux @FXML**
  ```java
  @FXML private Label caLabel;              <!-- fx:id="caLabel" dans FXML -->
  @FXML private Label commandesTotalLabel;  <!-- fx:id="commandesTotalLabel" -->
  ```

---

## 🔨 Processus de Compilation et Exécution

### 1️⃣ **Nettoyer et Reconstruire**
```
Menu Intellij:
Build → Clean
Build → Rebuild Project
```

### 2️⃣ **Vérifier les Erreurs**
```
Fenêtre "Messages" en bas de l'IDE
Appuyer sur Alt + 0 pour afficher les erreurs
```

### 3️⃣ **Corriger les Imports Manquants**
```
Clic sur le class/method rouge
Alt + Enter → "Import class"
```

### 4️⃣ **Exécuter l'Application**
```
Run → Run 'MainApp'
Ou: Shift + F10
```

---

## 📝 Fichiers Critiques à Vérifier

| Fichier | Vérification |
|---------|--------------|
| `AdminDashboardController.java` | ✓ Import `StockAlertService` |
| `ProduitsDashboardController.java` | ✓ Import `StockAlertService` |
| `CommandeWorkflowService.java` | ✓ Import `MyDatabase` |
| `StockAlertService.java` | ✓ Import `MyDatabase` |
| `AdminDashboard.fxml` | ✓ `fx:controller` correct |
| `ClientHistorique.fxml` | ✓ `fx:controller` correct |

---

## 🐛 Si tu reçois une Exception à l'Runtime

### Stack Trace à partager:
```
1. Copier tout le message d'erreur
2. Chercher la ligne "at com.chroniccare..."
3. Me partager le stack trace complet
```

---

## 💡 Aide Rapide

**Si rien ne marche:**

1. Ouvrir le dossier `target/` et le supprimer
2. Clic droit sur le projet → "Mark Directory as" → "Sources Root" (src/main/java)
3. `Build` → `Rebuild Project`
4. Run application

---

**Version**: 1.0  
**Dernière mise à jour**: 2024

