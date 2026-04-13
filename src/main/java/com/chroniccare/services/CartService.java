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

    public void add(Produit produit, int qty) {
        if (produit == null) {
            throw new IllegalArgumentException("produit is null");
        }
        if (qty <= 0) {
            return;
        }

        CartItem existing = items.get(produit.getId());
        if (existing == null) {
            items.put(produit.getId(), new CartItem(produit, qty));
        } else {
            existing.setQuantity(existing.getQuantity() + qty);
        }
    }

    public void setQuantity(int produitId, int qty) {
        if (qty <= 0) {
            items.remove(produitId);
            return;
        }
        CartItem existing = items.get(produitId);
        if (existing != null) {
            existing.setQuantity(qty);
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

