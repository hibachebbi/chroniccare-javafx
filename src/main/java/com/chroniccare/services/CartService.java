package com.chroniccare.services;

import com.chroniccare.entities.Produit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CartService {

    private static final CartService INSTANCE = new CartService();

    private final Map<Integer, CartItem> items = new LinkedHashMap<>();

    private CartService() {
    }

    public static CartService getInstance() {
        return INSTANCE;
    }

    public Map<Integer, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Quantité déjà présente dans le panier (stock "réservé" côté UI).
     */
    public int getReservedQty(int produitId) {
        CartItem item = items.get(produitId);
        return item == null ? 0 : item.getQuantity();
    }

    /**
     * Stock disponible = stock total produit - stock réservé dans le panier.
     */
    public int getAvailableStock(Produit produit) {
        if (produit == null) {
            return 0;
        }
        return Math.max(0, produit.getStock() - getReservedQty(produit.getId()));
    }

    public void add(Produit produit, int qty) {
        if (produit == null) {
            throw new IllegalArgumentException("produit is null");
        }
        if (qty <= 0) {
            return;
        }

        // Empêche de dépasser le stock (en tenant compte du stock déjà réservé dans le panier)
        int available = getAvailableStock(produit);
        if (qty > available) {
            qty = available;
        }
        if (qty <= 0) {
            return;
        }

        CartItem existing = items.get(produit.getId());
        if (existing == null) {
            // qty est déjà clamp à "available" donc <= stock
            items.put(produit.getId(), new CartItem(produit, qty));
        } else {
            // Ajouts successifs: re-clamp vs stock total
            setQuantity(produit.getId(), existing.getQuantity() + qty);
        }
    }

    public void setQuantity(int produitId, int qty) {
        if (qty <= 0) {
            items.remove(produitId);
            return;
        }
        CartItem existing = items.get(produitId);
        if (existing != null) {
            // Clamp vs stock total du produit pour éviter de réserver plus que le stock
            int max = existing.getProduit() == null ? qty : Math.max(0, existing.getProduit().getStock());
            existing.setQuantity(Math.min(qty, max));
        }
    }

    public void remove(int produitId) {
        items.remove(produitId);
    }

    public void clear() {
        items.clear();
    }

    public int getTotalItems() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getTotalPrice() {
        return items.values().stream().mapToDouble(i -> i.getProduit().getPrix() * i.getQuantity()).sum();
    }

    public static final class CartItem {
        private final Produit produit;
        private int quantity;

        public CartItem(Produit produit, int quantity) {
            this.produit = produit;
            this.quantity = quantity;
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

        public double getLineTotal() {
            return produit.getPrix() * quantity;
        }
    }
}

