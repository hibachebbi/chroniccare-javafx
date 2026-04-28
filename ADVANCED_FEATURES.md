#  Fonctionnalités Métier Avancées - ChronicCare

Ce document décrit les 4 fonctionnalités métier avancées implémentées dans l'application.

---

## 1️⃣ **Workflow Commande → Livraison**

###  Statuts de Commande
Les commandes passent par les statuts suivants (en ordre logique):
- `en_attente` : Commande reçue, en attente de confirmation
- `confirmée` : Admin a validé la commande
- `préparée` : Les produits sont préparés
- `expédiée` : La commande a été envoyée
- `livrée` : Le client a reçu la commande
- `annulée` : Commande annulée

###  Statuts de Livraison
- `en_preparation` : Livraison en cours de préparation
- `en_transit` : En route vers le client
- `livree` : Livrée au client
- `retard` : Livraison en retard
- `annulee` : Livraison annulée

###  Utilisation
```java
CommandeWorkflowService workflow = new CommandeWorkflowService();

// Transitionner le statut
workflow.transitionStatut(commandeId, CommandeWorkflowService.CommandeStatut.CONFIRMEE);

// Compter par statut
int enAttente = workflow.countByStatut(CommandeWorkflowService.CommandeStatut.EN_ATTENTE);
```

---

## 2️⃣ **Dashboard Admin Avancé**

###  Vue d'Ensemble
Le dashboard affiche en temps réel:

####  **Chiffre d'Affaires (CA)**
- Total CA vs CA déjà livré
- Barre de progression

####  **Commandes**
- Total commandes
- Commandes d'aujourd'hui
- Commandes en attente (en_attente + confirmée)

####  **Livraisons**
- Total livraisons
- Livraisons en retard

####  **Produits**
- Total produits
- Produits en stock faible (< 5)
- Produits en rupture (stock = 0)

####  **Alertes Dynamiques**
Une section d'alertes s'met à jour automatiquement avec:
- ⚠️ Commandes en attente
-  Livraisons en retard
-  Stock faible
-  Ruptures de stock
- ✅ Tout est normal

###  Accès
- Fichier FXML: `/com/chroniccare/Admin/AdminDashboard.fxml`
- Contrôleur: `AdminDashboardController.java`
- Bouton dans l'interface: " Tableau de Bord"

---

## 3️⃣ **Alerte Stock Faible & Rupture**

###  Seuils
- **Stock Faible**: `<= 5 unités` → Badge  Orange
- **Rupture**: `0 unité` → Badge  Rouge

### ✅ États
| Statut | Couleur | Emoji |
|--------|---------|-------|
| OK | Vert | ✅ |
| Stock Faible | Orange |  |
| Rupture | Rouge |  |

###  Service
```java
StockAlertService alert = new StockAlertService();

// Vérifier le stock
boolean faible = alert.isStockFaible(produitId);
boolean rupture = alert.isRupture(produitId);

// Compter
int countFaible = alert.countStockFaible();
int countRupture = alert.countRupture();

// Lister les produits problématiques
List<String> problems = alert.getProduitsStockFaible();
List<String> ruptures = alert.getProduitsRupture();
```

###  Affichage
Dans le tableau des produits:
- Colonne "Statut" affiche `✅ OK`, ` Faible`, ou ` Rupture`
- Couleurs adaptées pour visibilité

---

## 4️⃣ **Historique Client**

###  Vue Client
Chaque client peut voir:

####  **Mes Commandes**
Table affichant:
- ID
- Numéro de commande
- Statut
- Total
- Date

####  **Mes Livraisons**
Table affichant:
- ID
- Commande ID
- Statut
- Adresse
- Ville
- Date prévue

####  **Statistiques**
- Total commandes
- Total livraisons
- Total dépensé

###  Accès
- Fichier FXML: `/com/chroniccare/Client/ClientHistorique.fxml`
- Contrôleur: `ClientHistoriqueController.java`
- À ajouter dans le menu client

###  Intégration Session
⚠️ **TODO**: Actuellement, `UTILISATEUR_ID = 2` est hardcodé.
À remplacer par la vraie ID de session:
```java
private static int UTILISATEUR_ID = SessionManager.getCurrentUserId();
```

---

##  Fichiers Créés/Modifiés

### Nouveaux Services
| Fichier | Description |
|---------|-------------|
| `CommandeWorkflowService.java` | Gestion des workflows de commande |
| `LivraisonWorkflowService.java` | Gestion des workflows de livraison |
| `StockAlertService.java` | Alertes et seuils de stock |

### Nouveaux Contrôleurs
| Fichier | Description |
|---------|-------------|
| `AdminDashboardController.java` | Dashboard admin |
| `ClientHistoriqueController.java` | Historique client |

### Nouveuses Interfaces (FXML)
| Fichier | Description |
|---------|-------------|
| `AdminDashboard.fxml` | Dashboard admin UI |
| `ClientHistorique.fxml` | Historique client UI |

### Modifiés
| Fichier | Changements |
|---------|-------------|
| `ProduitsDashboardController.java` | Ajout de `StockAlertService` + meilleur affichage statut |

---

##  Liens Entre Fonctionnalités

```
Commande (client)
    ↓
Workflow Commande (validation admin)
    ↓
Livraison créée (dashboard visible)
    ↓
Workflow Livraison (suivi)
    ↓
Livraison livrée
    ↓
Historique Client (visible dans "Mes Livraisons")

+ Alertes Stock en parallèle (lors de la commande)
+ Dashboard Admin avec vue globale
```

---

##  Prochaines Étapes

Pour aller plus loin:

1. **Notifications en temps réel**
   - Alerte email quand commande confirmée
   - Alerte SMS livraison en retard

2. **Annulation/Retour de Commande**
   - Workflow d'annulation avec remboursement
   - Remise à jour du stock

3. **Rapports & Export**
   - Export PDF des statistiques
   - Graphiques de tendances CA

4. **Gestion Utilisateur Session**
   - Remplacer hardcoded userId par vrai session
   - Rôles admin/client différenciés

---

##  Tests Recommandés

### Test 1: Workflow Commande
```
1. Client passe 1 commande
2. Admin voir dans dashboard "1 en attente"
3. Admin clique "Confirmer" → statut passe à "confirmée"
4. Admin clique "Préparer" → statut passe à "préparée"
5. Admin clique "Expédier" → livraison créée
6. Livraison créée visible dans "Mes Livraisons" client
```

### Test 2: Alert Stock
```
1. Produit A: stock = 5 → affiche " Faible" (orange)
2. Produit B: stock = 0 → affiche " Rupture" (rouge)
3. Produit C: stock = 10 → affiche "✅ OK" (vert)
```

### Test 3: Dashboard Admin
```
1. Ouvrir AdminDashboard
2. Cliquer "Actualiser"
3. Voir CA, commandes, livraisons, stocks
4. Voir les alertes correspondantes en bas
```

### Test 4: Historique Client
```
1. Client clique "Mon Historique"
2. Voir toutes les commandes (status, total, date)
3. Voir toutes les livraisons (adresse, status, date prévue)
4. Voir totaux statistiques
```

---

##  Dépannage

### Dashboard Admin vide?
- Vérifier que la DB a du data (commandes, livraisons)
- Vérifier que les utilisateurs existent

### Client ne voit pas ses livraisons?
- Vérifier UTILISATEUR_ID dans `ClientHistoriqueController`
- Vérifier les livraisons existent en DB pour cet utilisateur

### Stock faible n'affiche pas?
- Vérifier que `StockAlertService.STOCK_FAIBLE_SEUIL` = 5
- Vérifier que produit.stock <= 5

---

**Dernière mise à jour**: 2024
**Version**: 1.0
