# ChronicCare - Resume technique

## 1) Vue d'ensemble
ChronicCare est une application JavaFX qui gere le catalogue produits, le panier, les commandes et la livraison.
Deux roles principaux :
- Admin : CRUD produits, gestion commandes, gestion livraisons.
- Client : catalogue, panier, commande rapide, commande panier, paiement et livraison.

Architecture simple de type MVC :
- FXML = vues
- Controllers = logique d'interface et navigation
- Services = acces base de donnees
- Entities = modeles de donnees


## 2) Structure du projet

### Code Java
- src/main/java/com/chroniccare/controllers
  - Admin/ : ecrans d'administration
  - Client/ : ecrans cote client
  - user/ : navigation centrale (home, login, profil...)
- src/main/java/com/chroniccare/entities
  - Produit, Commande, Livraison, User, etc.
- src/main/java/com/chroniccare/services
  - ProduitsService, CommandeService, LivraisonService, CartService
- src/main/java/com/chroniccare/utils
  - FxNavigator, SessionManager, MyDatabase

### Ressources
- src/main/resources/com/chroniccare
  - FXML principaux (home, login, profils, etc.)
  - Admin/ : FXML admin
  - Client/ : FXML client
  - CSS : styles

### SQL
- sql/create_livraison.sql


## 3) Navigation et role
- Navigation principale via home.fxml + HomeController
- SessionManager determine si l'utilisateur est admin ou client
- FxNavigator centralise la navigation entre FXML


## 4) Flux cote client

### 4.1 Catalogue produits
- Vue : Client/ProduitsDashboard.fxml
- Controller : Client/ProduitsDashboardController.java
- Fonctionnalites :
  - Liste des produits actifs
  - Recherche par nom ou categorie
  - Filtre categorie + pagination
  - Actions par produit :
    - Commander (commande rapide)
    - Ajouter au panier (avec choix quantite)

### 4.2 Commande rapide
- Vue : Client/CommandeRapide.fxml
- Controller : Client/CommandeRapideController.java
- Fonctionnalites :
  - Choisir quantite
  - Choisir paiement
  - Creation commande via CommandeService.createFromCart
  - Stock mis a jour dans la base

### 4.3 Panier
- Vue : Client/Panier.fxml
- Controller : Client/PanierController.java
- Fonctionnalites :
  - Affiche les items du panier (CartService)
  - Total calcule
  - Double clic pour modifier la quantite
  - Retirer / Vider
  - Valider commande -> Checkout

### 4.4 Checkout + Livraison
- Vue : Client/Checkout.fxml
- Controller : Client/CheckoutController.java
- Fonctionnalites :
  - Affiche total du panier ou d'une commande
  - Choix paiement
  - Apres "Confirmer" : section livraison
  - Saisie adresse, ville, code postal, mode livraison
  - Creation commande si panier
  - Creation livraison en base
  - Redirection vers Commandes

### 4.5 Commandes client
- Vue : Client/CommandesDashboard.fxml
- Controller : Client/CommandesDashboardController.java
- Fonctionnalites :
  - Liste des commandes du client
  - Recherche + filtre statut
  - Pagination
  - Bouton Payer -> affiche paiement/total dans Checkout


## 5) Flux cote admin

### 5.1 Produits admin
- Vue : Admin/ProduitsDashboard.fxml
- Controller : Admin/ProduitsDashboardController.java
- CRUD complet : ajouter / modifier / supprimer
- Statistiques : stock total, actifs, stock faible
- Recherche + pagination
- Edition : Admin/edit-produit.fxml
- Validation : champs obligatoires + unicite (nom + categorie)

### 5.2 Commandes admin
- Vue : Admin/CommandesDashboard.fxml
- Controller : Admin/CommandesDashboardController.java
- Modification limitee : statut + paiement uniquement
- Suppression securisee (dependances ligne_commande / livraison)
- Recherche + pagination
- Edition : Admin/edit-commande.fxml

### 5.3 Livraisons admin
- Vue : Admin/LivraisonsDashboard.fxml
- Controller : Admin/LivraisonsDashboardController.java
- CRUD livraisons


## 6) Services (base de donnees)

### ProduitsService
- CRUD + recherche
- Unicite nom + categorie

### CommandeService
- createFromCart :
  - verifie stock
  - decremente stock
  - insere commande
- updateAdminStatusAndPayment : update securise statut/paiement
- delete : supprime dependances puis commande (transaction)

### LivraisonService
- CRUD livraisons
- findByUtilisateurId (via jointure commande.utilisateur_id)

### CartService
- Panier en memoire (singleton)
- add/setQuantity/remove/clear
- total items + total prix


## 7) Tables principales (base)
- produit
- commande
- ligne_commande
- livraison (cree via sql/create_livraison.sql)


## 8) Controles de saisie
- Nom, categorie, prix, stock : validations + messages
- Unicite produit (nom + categorie)
- Quantite panier / commande rapide : limites stock
- Livraison : adresse/ville/code postal obligatoires
- Paiement obligatoire


## 9) Fonctionnalites supplementaires
- Recherche + filtre + pagination
- Calcul total automatique
- Scenario complet client (catalogue -> panier -> commande -> livraison)


## 10) Points a presenter en soutenance (simple)
- Projet structure MVC
- Navigation geree par SessionManager + FxNavigator
- CRUD complet cote admin
- Scenario client complet et coherant
- Validation des donnees + unicite produit


## 11) Questions courantes (reponses courtes)

### Q1. Est-ce que j'ai utilise Stage et Scene ?
Oui.
- Dans `src/main/java/com/chroniccare/utils/FxNavigator.java`, on recupere la Scene du Node courant et on met a jour le Stage:
  - `scene = anyNodeInScene.getScene()`
  - `stage = (Stage) scene.getWindow()`
  - `stage.setScene(new Scene(root, scene.getWidth(), scene.getHeight()))`

### Q2. Quelle est la difference entre Stage et Scene ?
- **Stage** = la fenetre principale (l'ecran).
- **Scene** = le contenu affiche dans la fenetre (le layout FXML).
On change la Scene pour naviguer entre interfaces sans fermer la fenetre.

### Q3. Ou j'utilise Scene directement ?
- Navigation via `scene.setRoot(...)` ou `stage.setScene(...)` dans `FxNavigator`.
- Exemples en controllers: `HomeController` utilise `btnX.getScene().setRoot(root)`.

### Q4. Est-ce que j'ai utilise une session ?
Oui, via `SessionManager`.

### Q5. Ou j'utilise la session exactement ?
- `src/main/java/com/chroniccare/utils/SessionManager.java` (gestion utilisateur connecte, roles)
- `HomeController` : choix admin/client + affichage infos utilisateur
- `CheckoutController` / `CommandeRapideController` : recuperer l'utilisateur courant
- `CommandesDashboardController` (client) : filtrer commandes par utilisateur
- `PanierController` / autres pages : logout + navigation

### Q6. Pourquoi la session est importante ici ?
- Elle permet de savoir qui est connecte.
- Elle controle les droits (admin vs client).
- Elle sert a filtrer les donnees (commandes par utilisateur).

### Q7. Pourquoi on utilise FxNavigator ?
- Pour centraliser la navigation FXML.
- Pour eviter de dupliquer `FXMLLoader` dans chaque controller.
- Pour garder une navigation uniforme (Scene/Stage).
- Exemples d'usage : `Client/ProduitsDashboardController`, `Client/PanierController`, `Client/CommandesDashboardController`.

### Q8. Comment fonctionne le panier (singleton) ?
- Le panier est gere par `CartService` en singleton : une seule instance en memoire.
- Il stocke un `Map<produitId, CartItem>`.
- Il permet d'ajouter, modifier quantite, supprimer et calculer le total.
- Utilise par : `ProduitsDashboardController` (ajout), `PanierController` (affichage/modif), `CheckoutController` (total).

### Q9. Flux complet en 5 etapes (client)
1) Catalogue produits : choisir produit.
2) Ajouter au panier (quantite) OU commande rapide.
3) Panier : modifier quantite, voir total.
4) Checkout : paiement + adresse.
5) Livraison : mode + infos, creation commande + livraison.

### Q10. Flux complet en 5 etapes (admin)
1) Connexion admin.
2) Gestion produits (CRUD).
3) Gestion commandes (statut/paiement).
4) Gestion livraisons.
5) Suivi et statistiques (stock, commandes).

### Q11. Pourquoi on a choisi MVC ?
- Pour separer clairement la vue (FXML), la logique (controllers) et les donnees (services/entities).
- Cela rend le code plus lisible, maintenable et facile a expliquer en soutenance.

### Q12. Comment on a securise la suppression commande ?
- Suppression en transaction.
- Suppression d'abord des dependances (`ligne_commande`, `livraison`) puis de la commande.
- Message clair si une contrainte FK bloque la suppression.

### Q13. Pourquoi on a filtre les commandes par utilisateur ?
- Pour que chaque client ne voie que ses propres commandes.
- Cela respecte la logique metier et la confidentialite.
- Filtre applique dans `Client/CommandesDashboardController` via `SessionManager`.
