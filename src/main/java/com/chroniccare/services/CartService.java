package com.chroniccare.services;

import com.chroniccare.entities.Produit;
import com.chroniccare.entities.User;
import com.chroniccare.utils.SessionManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CartService avec persistance en base de données
 * - Charge le panier depuis la BD au démarrage
 * - Sauvegarde automatiquement les changements
 * - Supporte le panier multi-utilisateur
 */
public final class CartService {

    private static final CartService INSTANCE = new CartService();

    private final Map<Integer, CartItem> items = new LinkedHashMap<>();
    private final PanierPersistantService panierService = new PanierPersistantService();

    private CartService() {
    }

    public static CartService getInstance() {
        return INSTANCE;
    }

    /**
     * Charge le panier d'un utilisateur depuis la base de données
     * À appeler après une connexion réussie
     */
    public void chargerPanierUtilisateur(int utilisateurId) {
        items.clear();
        try {
            var panierItems = panierService.findByUtilisateurId(utilisateurId);
            for (var item : panierItems) {
                // Recréer un CartItem avec les données de la BD
                // Note: On ne peut pas récréer le Produit complet ici sans une requête supplémentaire
                // Donc on stocke juste les informations nécessaires
                CartItem cartItem = new CartItem(null, item.getQuantite());
                cartItem.setId(item.getId());
                cartItem.setPrixUnitaire(item.getPrixUnitaire());
                cartItem.setProduitId(item.getProduitId());
                items.put(item.getProduitId(), cartItem);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement panier: " + e.getMessage());
        }
    }

    /**
     * Vide le panier actuel (en mémoire)
     */
    public void clearCache() {
        items.clear();
    }

    public Map<Integer, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Ajoute un produit au panier et sauvegarde en BD
     */
    public void add(Produit produit, int qty) {
        if (produit == null) {
            throw new IllegalArgumentException("produit is null");
        }
        if (qty <= 0) {
            return;
        }
        if (produit.getStock() <= 0) {
            throw new IllegalArgumentException("Ce produit est en rupture de stock.");
        }

        CartItem existing = items.get(produit.getId());
        if (existing == null) {
            if (qty > produit.getStock()) {
                throw new IllegalArgumentException("Stock insuffisant. Stock disponible: " + produit.getStock());
            }
            CartItem newItem = new CartItem(produit, qty);
            items.put(produit.getId(), newItem);
            // Sauvegarder en BD
            sauvegarderEnBD(produit.getId(), qty, produit.getPrix());
        } else {
            int newQuantity = existing.getQuantity() + qty;
            if (newQuantity > produit.getStock()) {
                throw new IllegalArgumentException("Stock insuffisant. Stock disponible: " + produit.getStock());
            }
            existing.setQuantity(newQuantity);
            // Mettre à jour en BD
            double prix = existing.getPrixUnitaire() > 0 ? existing.getPrixUnitaire() : produit.getPrix();
            mettreAJourEnBD(existing.getId(), newQuantity, prix);
        }
    }

    /**
     * Modifie la quantité d'un produit et sauvegarde en BD
     */
    public void setQuantity(int produitId, int qty) {
        CartItem existing = items.get(produitId);
        if (existing == null) {
            return;
        }

        if (qty <= 0) {
            items.remove(produitId);
            // Supprimer de la BD
            if (existing.getId() > 0) {
                supprimerDeBD(existing.getId());
            }
            return;
        }

        Produit produit = existing.getProduit();
        int maxStock = produit == null ? 0 : produit.getStock();
        if (maxStock <= 0) {
            throw new IllegalArgumentException("Ce produit est en rupture de stock.");
        }
        if (qty > maxStock) {
            throw new IllegalArgumentException("Stock insuffisant. Stock disponible: " + maxStock);
        }

        existing.setQuantity(qty);
        // Mettre à jour en BD
        double prix = existing.getPrixUnitaire() > 0 ? existing.getPrixUnitaire() : (produit != null ? produit.getPrix() : 0);
        mettreAJourEnBD(existing.getId(), qty, prix);
    }

    /**
     * Supprime un produit du panier et de la BD
     */
    public void remove(int produitId) {
        CartItem item = items.remove(produitId);
        if (item != null && item.getId() > 0) {
            supprimerDeBD(item.getId());
        }
    }

    public void clear() {
        // Vider la BD pour l'utilisateur actuel
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                panierService.clearPanier(currentUser.getId());
            }
        } catch (Exception e) {
            System.err.println("Erreur vidage panier BD: " + e.getMessage());
        }
        items.clear();
    }

    public int getTotalItems() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getTotalPrice() {
        return items.values().stream()
                .mapToDouble(i -> {
                    // Si produit est null, utiliser prixUnitaire (cas du panier chargé de la BD)
                    if (i.getProduit() == null) {
                        return i.getPrixUnitaire() * i.getQuantity();
                    }
                    // Sinon, utiliser le prix du produit
                    return i.getProduit().getPrix() * i.getQuantity();
                })
                .sum();
    }

    public static final class CartItem {
        private int id; // ID de panier en BD
        private final Produit produit;
        private int produitId; // ID du produit (pour cas sans Produit object)
        private int quantity;
        private double prixUnitaire; // Prix sauvegardé au moment de l'ajout

        public CartItem(Produit produit, int quantity) {
            this.produit = produit;
            this.quantity = quantity;
            this.prixUnitaire = produit != null ? produit.getPrix() : 0;
            this.produitId = produit != null ? produit.getId() : 0;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getProduitId() {
            return produitId;
        }

        public void setProduitId(int produitId) {
            this.produitId = produitId;
        }

        public Produit getProduit() {
            return produit;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrixUnitaire() {
            return prixUnitaire;
        }

        public void setPrixUnitaire(double prixUnitaire) {
            this.prixUnitaire = prixUnitaire;
        }

        public double getLineTotal() {
            return prixUnitaire * quantity;
        }
    }

    // ====== Méthodes privées de persistance ======

    /**
     * Sauvegarde un nouvel article en BD
     */
    private void sauvegarderEnBD(int produitId, int quantite, double prixUnitaire) {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                int id = panierService.add(currentUser.getId(), produitId, quantite, prixUnitaire);
                // Mettre à jour l'ID en mémoire
                CartItem item = items.get(produitId);
                if (item != null) {
                    item.setId(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde panier BD: " + e.getMessage());
        }
    }

    /**
     * Met à jour un article en BD
     */
    private void mettreAJourEnBD(int panierItemId, int quantite, double prixUnitaire) {
        try {
            panierService.update(panierItemId, quantite, prixUnitaire);
        } catch (Exception e) {
            System.err.println("Erreur mise à jour panier BD: " + e.getMessage());
        }
    }

    /**
     * Supprime un article de la BD
     */
    private void supprimerDeBD(int panierItemId) {
        try {
            panierService.delete(panierItemId);
        } catch (Exception e) {
            System.err.println("Erreur suppression panier BD: " + e.getMessage());
        }
    }
}

