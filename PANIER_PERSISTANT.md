#  Panier Persistant - Documentation

## Vue d'ensemble

Le **Panier Persistant** permet de sauvegarder automatiquement les articles du panier dans la base de données. Cela signifie que :

✅ Les clients ne perdent pas leur panier s'ils ferment l'application  
✅ Le panier est synchronisé entre les sessions  
✅ Les données du panier sont persistantes et récupérables  

---

## Architecture

### Components Créés

1. **PanierItem.java** (Entity)
   - Représente un article du panier en BD
   - Propriétés: id, utilisateurId, produitId, quantite, prixUnitaire, dateAjout, dateModification

2. **PanierPersistantService.java** (Service)
   - Opérations CRUD sur la table `panier`
   - Méthodes principales:
     - `findByUtilisateurId(int)` - Récupère tous les articles d'un utilisateur
     - `addOrUpdate(...)` - Ajoute ou met à jour un article
     - `getTotalPanier(int)` - Calcule le total
     - `clearPanier(int)` - Vide le panier

3. **CartService.java** (Modified)
   - Intègre la persistance BD
   - Méthodes nouvelles:
     - `chargerPanierUtilisateur(int)` - Charge le panier depuis la BD
     - `clearCache()` - Vide le cache en mémoire
   - Toutes les modifications (`add`, `remove`, `setQuantity`) sauvegardent automatiquement

4. **Table `panier`** (DDL)
   - Créée dans `sql/create_panier.sql`
   - Exécuter le script pour créer la table

---

## Flux d'utilisation

### 1. Initialisation à la connexion

```javascript
// Dans LoginController.handleLogin()
SessionManager.getInstance().setCurrentUser(user);
CartService.getInstance().chargerPanierUtilisateur(user.getId());
```

Le panier de l'utilisateur est chargé depuis la BD au moment de la connexion.

### 2. Ajouter un article

```javascript
CartService cartService = CartService.getInstance();
cartService.add(produitObject, quantite);
// ✅ Automatiquement sauvegardé en BD
```

### 3. Modifier la quantité

```javascript
cartService.setQuantity(produitId, nouvelleQuantite);
// ✅ Automatiquement mis à jour en BD
```

### 4. Supprimer un article

```javascript
cartService.remove(produitId);
// ✅ Automatiquement supprimé de la BD
```

### 5. Vider le panier

```javascript
cartService.clear();
// ✅ Tous les articles supprimés de la BD et cache vidé
```

---

## Structure de la table `panier`

```sql
CREATE TABLE panier (
    id INT PRIMARY KEY AUTO_INCREMENT,
    utilisateur_id INT NOT NULL,
    produit_id INT NOT NULL,
    quantite INT NOT NULL DEFAULT 1,
    prix_unitaire DECIMAL(10, 2) NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE,
    UNIQUE KEY unique_panier (utilisateur_id, produit_id),
    INDEX idx_utilisateur_id (utilisateur_id),
    INDEX idx_produit_id (produit_id)
);
```

**Points importants:**
- `UNIQUE KEY` sur (utilisateur_id, produit_id) : Un utilisateur ne peut avoir qu'une ligne par produit
- `ON DELETE CASCADE` : Si un utilisateur/produit est supprimé, le panier est aussi supprimé
- `date_modification` : Mis à jour automatiquement à chaque modification

---

## Intégration dans les contrôleurs

### PanierController (à créer)

```javascript
// Afficher le panier
CartService cartService = CartService.getInstance();
Map<Integer, CartService.CartItem> items = cartService.getItems();

// Afficher dans TableView
ObservableList<CartService.CartItem> panierItems = 
    FXCollections.observableArrayList(items.values());
panierTable.setItems(panierItems);

// Total du panier
double total = cartService.getTotalPrice();
totalLabel.setText("Total: " + total + " €");
```

### CheckoutController

```javascript
// Avant de créer la commande, vider le panier
CartService.getInstance().clear();
```

---

## Avantages vs Inconvénients

### ✅ Avantages
- Données persistantes
- Synchronisation multi-session
- Pas de perte de données
- Historique des ajouts (dateAjout)
- Traçabilité des modifications

### ⚠️ Considérations
- Surcharge BD si beaucoup d'utilisateurs
- Latence réseau pour chaque opération (mineure)
- Solution : Implémenter un caching plus avancé si nécessaire

---

## Amélioration future

Vous pourriez ajouter:
-  Sync asynchrone (ne pas bloquer l'UI)
-  Cache local avec sync en arrière-plan
-  Analytics du panier (produits populaires, taux d'abandon)
- ⏰ Nettoyage automatique des vieux paniers (> 30 jours)

---

## Tests à faire

1. ✅ Se connecter > Ajouter article > Fermer l'app > Se reconnecter > Vérifier panier
2. ✅ Ajouter article > Modifier quantité > Vérifier BD
3. ✅ Ajouter article > Vider panier > Vérifier suppression BD
4. ✅ Plusieurs utilisateurs > Chacun a son panier isolé
5. ✅ Vérifier les contraintes UNIQUE (ne pas dupliquer les articles)

---

**Status:** ✅ Feature implémentée et intégrée
