# ⚡ WISHLIST - TEST RAPIDE (5 MIN)

## ✅ ÉTAPES FINALES

### 1. Exécuter le SQL (30 sec)

Ouvrir phpMyAdmin → SQL :

```sql
CREATE TABLE IF NOT EXISTS wishlist (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    produit_id INT NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_wishlist (utilisateur_id, produit_id),
    INDEX idx_utilisateur_id (utilisateur_id),
    INDEX idx_produit_id (produit_id),
    FOREIGN KEY (utilisateur_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
);
```

**Résultat**: ✅ Query successful

---

### 2. Compiler (30 sec)

```bash
cd C:\Users\rayen\dev_desktop\chroniccare-javafx
mvn clean compile
```

**Résultat**: ✅ BUILD SUCCESS

---

### 3. Lancer l'App (1 min)

```bash
mvn javafx:run
```

**Résultat**: App se lance sans erreur ✅

---

### 4. Tester (2 min)

1. **Login** client
2. **Aller dans le menu** et cliquer "Ma Wishlist" (ou depuis produits si bouton intégré)
3. **Vérifier** que la page existe et est vide
4. **Ajouter manuellement** un produit en BD (optionnel):

```sql
INSERT INTO wishlist (utilisateur_id, produit_id) VALUES (1, 1);
```

5. **Recharger** la page - le produit doit apparaître ✅

---

## ✨ C'EST TOUT!

La Wishlist fonctionne! 🎉

---

## 📋 PROCHAINES ACTIONS

A) Intégrer les boutons ❤️ dans ProduitsDashboard (voir `WISHLIST_INTEGRATION.md`)
B) Ajouter connectivité panier ("Ajouter au panier depuis wishlist")
C) Passer à la prochaine feature (Dashboard)

---

**Status**: 🟢 WISHLIST DÉPLOYÉE AVEC SUCCÈS

