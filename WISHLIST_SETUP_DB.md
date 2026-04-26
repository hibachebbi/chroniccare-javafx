# 🚀 WISHLIST - Installation BD (1 minute)

## Créer la table Wishlist

### Option 1: phpMyAdmin (Rapide)

1. Ouvrir: http://localhost/phpmyadmin
2. Sélectionner: Base `chroniccare` → Onglet SQL
3. Copier-coller:

```sql
CREATE TABLE IF NOT EXISTS wishlist (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    produit_id INT NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_utilisateur_id (utilisateur_id),
    INDEX idx_produit_id (produit_id),
    UNIQUE KEY unique_wishlist (utilisateur_id, produit_id),
    FOREIGN KEY (utilisateur_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

4. Clique **Exécuter** ✅

### Option 2: Fichier SQL

Fichier prêt: `sql/create_wishlist.sql`

---

## Vérifier que ça marche

```sql
DESCRIBE wishlist;
```

Doit afficher 4 colonnes: id, utilisateur_id, produit_id, date_ajout

---

**C'est tout!** La table est créée. Vous êtes prêt pour le code Java.

