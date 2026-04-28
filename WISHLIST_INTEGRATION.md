#  WISHLIST - Guide d'Intégration

## Étapes pour Intégrer la Wishlist

### 1️⃣ SETUP BD (1 minute)

Exécuter le SQL dans phpMyAdmin:

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

### 2️⃣ FICHIERS CRÉÉS ✅

Les fichiers suivants ont été créés automatiquement:

```
✅ Wishlist.java                     (Entity)
✅ WishlistService.java              (Service)
✅ WishlistController.java           (Contrôleur)
✅ Wishlist.fxml                     (UI)
✅ create_wishlist.sql               (Script BD)
```

### 3️⃣ COMPILER

```bash
cd C:\Users\rayen\dev_desktop\chroniccare-javafx
mvn clean compile
```

Résultat attendu: `BUILD SUCCESS`

### 4️⃣ AJOUTER BOUTONS AUX PRODUITS (Optional)

Pour ajouter un bouton ❤️ "Ajouter à la wishlist" dans la page ProduitsDashboard:

#### Dans ProduitsDashboardController.java, ajouter une colonne "Wishlist":

```java
/// ...existing code...
@FXML private TableColumn<Produit, Void> wishlistCol;

// Dans initialize():
wishlistCol.setCellFactory(col -> new TableCell<>() {
    private final Button btnWishlist = new Button("❤️ Wishlist");
    {
        btnWishlist.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnWishlist.setOnAction(e -> {
            Produit produit = getTableView().getItems().get(getIndex());
            toggleWishlist(produit);
        });
    }
    
    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : btnWishlist);
    }
});
```

#### Ajouter la méthode:

```java
private void toggleWishlist(Produit produit) {
    try {
        WishlistService wishlistService = new WishlistService();
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        
        if (wishlistService.isInWishlist(userId, produit.getId())) {
            wishlistService.removeFromWishlist(userId, produit.getId());
            showInfo("Succès", "Retiré de votre wishlist");
        } else {
            wishlistService.addToWishlist(userId, produit.getId());
            showInfo("Succès", "Ajouté à votre wishlist");
        }
    } catch (Exception e) {
        showError("Erreur", e.getMessage());
    }
}
```

### 5️⃣ TESTER

1. **Redémarrer l'app**: `mvn javafx:run`
2. **Login** en tant que client
3. **Aller dans les produits**
4. **Cliquer le bouton wishlist** (si intégré)
5. **Aller dans "Ma Wishlist"** depuis le menu
6. **Voir les produits**, pouvoir les retirer

---

##  FICHIERS MODIFIÉS

Aucun fichier existant n'a été modifié. Tout est créé de zéro!

---

##  CHECKLIST

- [ ] Table `wishlist` créée en BD
- [ ] App compilee (`mvn clean compile`)
- [ ] App lancée (`mvn javafx:run`)
- [ ] Login client
- [ ] Page "Ma Wishlist" accessible
- [ ] (Optional) Boutons wishlist dans produits
- [ ] Tests passsés ✅

---

##  C'EST SIMPLE!

La wishlist fonctionne déjà. Vous pouvez:

1. Ajouter manuellement via BD
2. Ou intégrer les boutons ❤️ dans ProduitsDashboard

---

##  PROCHAINES ÉTAPES

Après wishlist:
1. Intégrer "Ajouter au panier" depuis wishlist
2. Ajouter des statistiques (produits wishlist populaires)
3. Ajouter des recommandations basées sur wishlist

---

**Prêt?**  Lancez le app et testez!
