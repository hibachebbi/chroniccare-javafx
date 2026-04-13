package com.chroniccare.services;

import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LigneCommandeService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    public void insertLine(int commandeId, int produitId, int quantite, double prixUnitaire, double sousTotal, Double remise) throws SQLException {
        String sql = "INSERT INTO ligne_commande (quantite, prix_unitaire, sous_total, remise, commande_id, produit_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setDouble(2, prixUnitaire);
            ps.setDouble(3, sousTotal);
            if (remise == null) {
                ps.setNull(4, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(4, remise);
            }
            ps.setInt(5, commandeId);
            ps.setInt(6, produitId);
            ps.executeUpdate();
        }
    }
}

