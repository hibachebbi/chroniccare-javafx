#  FIX: Erreur FXML "Panier.fxml não trovata" - RÉSOLU

## ❌ **Problème**

```
Navigation echouee
FXML introuvable ou invalide: /com/chroniccare/Client/Panier.fxml
```

##  **Cause**

Le `pom.xml` d'énergissait **pas** configuré pour copier les fichiers FXML du répertoire `src/main/resources` vers `target/classes`.

Résultat:
- Source: `src/main/resources/com/chroniccare/Client/Panier.fxml` ✅ Existe
- Build: `target/classes/com/chroniccare/Client/Panier.fxml` ❌ Manquant!

## ✅ **Solution appliquée**

Mis à jour `pom.xml` avec:

1. **Section `<resources>`**: Copie les FXML et CSS
2. **Plugin `maven-resources-plugin`**: Gère la copie
3. **Plugin `maven-compiler-plugin`**: Compil avec encoding UTF-8

##  **Prochaines étapes (IMPORTANT!)**

### **1. Nettoie le projet**

**IntelliJ IDEA:**
```
Build → Clean Project
```

Ou depuis terminal:
```bash
cd C:\Users\rayen\dev_desktop\chroniccare-javafx
mvn clean
```

### **2. Reconstruit le projet**

**IntelliJ IDEA:**
```
Build → Rebuild Project
```

Ou depuis terminal:
```bash
cd C:\Users\rayen\dev_desktop\chroniccare-javafx
mvn clean compile
```

### **3. Redémarre l'app**

Puis teste:
1. Connecte-toi (client, PAS admin)
2. Ajoute un article au panier
3. Ferme l'app
4. Rouvre → **Le panier doit être restauré!** ✅

##  **Fichiers modifiés**

- ✅ `pom.xml` - Configuration build complète

## ✨ **Si ça marche toujours pas**

Dis-moi l'erreur complète! 

---

**Status:** ✅ Corrigé - À tester
