package com.chroniccare.services;

import com.chroniccare.models.Commande;
import com.chroniccare.models.Produit;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CommandeService {

    private final Random random = new Random();

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Crée une commande à partir d'un panier (sans table details) et décrémente le
     * stock en base.
     * Transactionnel: si un produit n'a pas assez de stock, rien n'est enregistré.
     */
    public com.chroniccare.models.Commande createFromCart(int utilisateurId,
            double total,
            String methodePaiement,
            java.util.Map<Integer, com.chroniccare.services.CartService.CartItem> items) throws SQLException {
        if (utilisateurId <= 0) {
            throw new IllegalArgumentException("utilisateurId invalide");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("panier vide");
        }

        Connection conn = connection();
        boolean oldAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // 1) Vérification + décrément stock
            for (com.chroniccare.services.CartService.CartItem item : items.values()) {
                Produit p = item.getProduit();
                if (p == null) {
                    throw new SQLException("Produit manquant dans le panier");
                }
                int qty = item.getQuantity();
                if (qty <= 0) {
                    continue;
                }

                // UPDATE atomique: décrémente seulement si stock suffisant
                String sqlUpdate = "UPDATE produit SET stock = stock - ? WHERE id = ? AND stock >= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, p.getId());
                    ps.setInt(3, qty);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException(
                                "Stock insuffisant pour le produit: " + (p.getNom() == null ? "" : p.getNom()));
                    }
                }
            }

            // 2) Insert commande
            com.chroniccare.models.Commande commande = new com.chroniccare.models.Commande();
            commande.setUtilisateurId(utilisateurId);
            commande.setTotal(total);
            commande.setStatut("en_attente");
            commande.setMethodePaiement(
                    methodePaiement == null || methodePaiement.isBlank() ? "card" : methodePaiement);
            commande.setCreatedAt(LocalDateTime.now());
            if (commande.getNumeroCommande() == null || commande.getNumeroCommande().isBlank()) {
                commande.setNumeroCommande(generateNumeroCommande());
            }

            String sqlInsert = "INSERT INTO commande (numero_commande, statut, total, created_at, utilisateur_id, methode_paiement, stripe_session_id, stripe_payment_intent_id, ip_address, country, city, region, latitude, longitude, timezone, isp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                // bind version insert (même ordre que la méthode bind)
                ps.setString(1, commande.getNumeroCommande());
                ps.setString(2, commande.getStatut());
                ps.setDouble(3, commande.getTotal());
                ps.setTimestamp(4, Timestamp.valueOf(commande.getCreatedAt()));
                ps.setInt(5, commande.getUtilisateurId());
                ps.setString(6, commande.getMethodePaiement());
                setNullableString(ps, 7, commande.getStripeSessionId());
                setNullableString(ps, 8, commande.getStripePaymentIntentId());
                setNullableString(ps, 9, commande.getIpAddress());
                setNullableString(ps, 10, commande.getCountry());
                setNullableString(ps, 11, commande.getCity());
                setNullableString(ps, 12, commande.getRegion());
                setNullableDouble(ps, 13, commande.getLatitude());
                setNullableDouble(ps, 14, commande.getLongitude());
                setNullableString(ps, 15, commande.getTimezone());
                setNullableString(ps, 16, commande.getIsp());

                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        commande.setId(keys.getInt(1));
                    }
                }
            }

            conn.commit();
            return commande;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    public List<Commande> findAll() throws SQLException {
        String sql = "SELECT * FROM commande ORDER BY created_at DESC, id DESC";
        List<Commande> commandes = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                commandes.add(map(rs));
            }
        }
        return commandes;
    }

    public Commande findById(int id) throws SQLException {
        String sql = "SELECT * FROM commande WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public Commande save(Commande commande) throws SQLException {
        if (commande.getNumeroCommande() == null || commande.getNumeroCommande().isBlank()) {
            commande.setNumeroCommande(generateNumeroCommande());
        }
        if (commande.getStatut() == null || commande.getStatut().isBlank()) {
            commande.setStatut("en_attente");
        }
        if (commande.getMethodePaiement() == null || commande.getMethodePaiement().isBlank()) {
            commande.setMethodePaiement("card");
        }
        if (commande.getCreatedAt() == null) {
            commande.setCreatedAt(LocalDateTime.now());
        }

        String sql = "INSERT INTO commande (numero_commande, statut, total, created_at, utilisateur_id, methode_paiement, stripe_session_id, stripe_payment_intent_id, ip_address, country, city, region, latitude, longitude, timezone, isp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, commande, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    commande.setId(keys.getInt(1));
                }
            }
        }
        return commande;
    }

    public void update(Commande commande) throws SQLException {
        String sql = "UPDATE commande SET numero_commande = ?, statut = ?, total = ?, utilisateur_id = ?, methode_paiement = ?, stripe_session_id = ?, stripe_payment_intent_id = ?, ip_address = ?, country = ?, city = ?, region = ?, latitude = ?, longitude = ?, timezone = ?, isp = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            bind(ps, commande, true);
            ps.executeUpdate();
        }
    }

    /**
     * Update minimaliste pour l'administration : ne modifie que le statut et la
     * méthode de paiement.
     * Permet d'éviter d'écraser des champs sensibles (numero, total,
     * utilisateur_id, etc.).
     */
    public void updateAdminStatusAndPayment(int commandeId, String statut, String methodePaiement) throws SQLException {
        if (commandeId <= 0) {
            throw new IllegalArgumentException("commandeId invalide");
        }
        if (statut == null || statut.isBlank()) {
            throw new IllegalArgumentException("statut obligatoire");
        }
        if (methodePaiement == null || methodePaiement.isBlank()) {
            throw new IllegalArgumentException("methodePaiement obligatoire");
        }

        String sql = "UPDATE commande SET statut = ?, methode_paiement = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, statut.trim());
            ps.setString(2, methodePaiement.trim());
            ps.setInt(3, commandeId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Commande introuvable (id=" + commandeId + ")");
            }
        }
    }

    public void updateStripePaymentIntentId(int commandeId, String stripePaymentIntentId) throws SQLException {
        if (commandeId <= 0) {
            throw new IllegalArgumentException("commandeId invalide");
        }
        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("stripePaymentIntentId obligatoire");
        }

        String sql = "UPDATE commande SET stripe_payment_intent_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, stripePaymentIntentId.trim());
            ps.setInt(2, commandeId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Commande introuvable (id=" + commandeId + ")");
            }
        }
    }

    public void updateStripeReferences(int commandeId, String stripeSessionId, String stripePaymentIntentId)
            throws SQLException {
        if (commandeId <= 0) {
            throw new IllegalArgumentException("commandeId invalide");
        }
        if ((stripeSessionId == null || stripeSessionId.isBlank())
                && (stripePaymentIntentId == null || stripePaymentIntentId.isBlank())) {
            throw new IllegalArgumentException("Au moins une reference Stripe doit etre fournie");
        }

        String sql = "UPDATE commande SET stripe_session_id = ?, stripe_payment_intent_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            setNullableString(ps, 1, stripeSessionId);
            setNullableString(ps, 2, stripePaymentIntentId);
            ps.setInt(3, commandeId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Commande introuvable (id=" + commandeId + ")");
            }
        }
    }

    public void delete(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("id commande invalide");
        }

        Connection conn = connection();
        boolean oldAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            // 1) Supprimer les dépendances (FK) si elles existent
            // ligne_commande -> commande
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ligne_commande WHERE commande_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // livraison -> commande (si la table existe dans votre schéma)
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM livraison WHERE commande_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2) Supprimer la commande
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM commande WHERE id = ?")) {
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                if (deleted != 1) {
                    throw new SQLException("Commande introuvable (id=" + id + ")");
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            // Message plus explicite si contrainte FK
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("foreign key") || msg.contains("constraint")) {
                throw new SQLException(
                        "Impossible de supprimer cette commande car elle est liée à d'autres données (lignes de commande / livraison).",
                        ex);
            }
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    public int countAll() throws SQLException {
        return countQuery("SELECT COUNT(*) FROM commande");
    }

    public int countByStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE statut = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public double sumTotal() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM commande";
        try (PreparedStatement ps = connection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private String generateNumeroCommande() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.FRANCE));
        return "CMD-" + timestamp + "-" + (1000 + random.nextInt(9000));
    }

    private void bind(PreparedStatement ps, Commande commande, boolean isUpdate) throws SQLException {
        ps.setString(1, commande.getNumeroCommande());
        ps.setString(2, commande.getStatut());
        ps.setDouble(3, commande.getTotal());
        if (!isUpdate) {
            ps.setTimestamp(4, Timestamp.valueOf(commande.getCreatedAt()));
            ps.setInt(5, commande.getUtilisateurId());
            ps.setString(6, commande.getMethodePaiement());
            setNullableString(ps, 7, commande.getStripeSessionId());
            setNullableString(ps, 8, commande.getStripePaymentIntentId());
            setNullableString(ps, 9, commande.getIpAddress());
            setNullableString(ps, 10, commande.getCountry());
            setNullableString(ps, 11, commande.getCity());
            setNullableString(ps, 12, commande.getRegion());
            setNullableDouble(ps, 13, commande.getLatitude());
            setNullableDouble(ps, 14, commande.getLongitude());
            setNullableString(ps, 15, commande.getTimezone());
            setNullableString(ps, 16, commande.getIsp());
        } else {
            ps.setInt(4, commande.getUtilisateurId());
            ps.setString(5, commande.getMethodePaiement());
            setNullableString(ps, 6, commande.getStripeSessionId());
            setNullableString(ps, 7, commande.getStripePaymentIntentId());
            setNullableString(ps, 8, commande.getIpAddress());
            setNullableString(ps, 9, commande.getCountry());
            setNullableString(ps, 10, commande.getCity());
            setNullableString(ps, 11, commande.getRegion());
            setNullableDouble(ps, 12, commande.getLatitude());
            setNullableDouble(ps, 13, commande.getLongitude());
            setNullableString(ps, 14, commande.getTimezone());
            setNullableString(ps, 15, commande.getIsp());
            ps.setInt(16, commande.getId());
        }
    }

    private Commande map(ResultSet rs) throws SQLException {
        Commande commande = new Commande();
        commande.setId(rs.getInt("id"));
        commande.setNumeroCommande(rs.getString("numero_commande"));
        commande.setStatut(rs.getString("statut"));
        commande.setTotal(rs.getDouble("total"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        commande.setCreatedAt(timestamp != null ? timestamp.toLocalDateTime() : null);
        commande.setUtilisateurId(rs.getInt("utilisateur_id"));
        commande.setMethodePaiement(rs.getString("methode_paiement"));
        commande.setStripeSessionId(rs.getString("stripe_session_id"));
        commande.setStripePaymentIntentId(rs.getString("stripe_payment_intent_id"));
        commande.setIpAddress(rs.getString("ip_address"));
        commande.setCountry(rs.getString("country"));
        commande.setCity(rs.getString("city"));
        commande.setRegion(rs.getString("region"));
        double latitude = rs.getDouble("latitude");
        commande.setLatitude(rs.wasNull() ? null : latitude);
        double longitude = rs.getDouble("longitude");
        commande.setLongitude(rs.wasNull() ? null : longitude);
        commande.setTimezone(rs.getString("timezone"));
        commande.setIsp(rs.getString("isp"));
        return commande;
    }

    private int countQuery(String sql) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, java.sql.Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }
}
