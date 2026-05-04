package com.chroniccare.services;

import com.chroniccare.models.Livraison;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LivraisonService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    public List<Livraison> findAll() throws SQLException {
        String sql = "SELECT * FROM livraison ORDER BY created_at DESC, id DESC";
        List<Livraison> livraisons = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                livraisons.add(map(rs));
            }
        }
        return livraisons;
    }

    public List<Livraison> findByUtilisateurId(int utilisateurId) throws SQLException {
        String sql = "SELECT l.* FROM livraison l JOIN commande c ON c.id = l.commande_id WHERE c.utilisateur_id = ? ORDER BY l.created_at DESC, l.id DESC";
        List<Livraison> livraisons = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    livraisons.add(map(rs));
                }
            }
        }
        return livraisons;
    }

    public Livraison findById(int id) throws SQLException {
        String sql = "SELECT * FROM livraison WHERE id = ?";
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

    public Livraison save(Livraison livraison) throws SQLException {
        if (livraison.getStatut() == null || livraison.getStatut().isBlank()) {
            livraison.setStatut("en_preparation");
        }
        if (livraison.getCreatedAt() == null) {
            livraison.setCreatedAt(LocalDateTime.now());
        }
        String sql = "INSERT INTO livraison (commande_id, statut, adresse, ville, code_postal, date_prevue, date_livraison, tracking_code, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, livraison, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    livraison.setId(keys.getInt(1));
                }
            }
        }
        return livraison;
    }

    public void update(Livraison livraison) throws SQLException {
        String sql = "UPDATE livraison SET commande_id = ?, statut = ?, adresse = ?, ville = ?, code_postal = ?, date_prevue = ?, date_livraison = ?, tracking_code = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            bind(ps, livraison, true);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM livraison WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countAll() throws SQLException {
        return countQuery("SELECT COUNT(*) FROM livraison");
    }

    public int countByStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM livraison WHERE statut = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countLate() throws SQLException {
        String sql = "SELECT COUNT(*) FROM livraison WHERE date_prevue < NOW() AND date_livraison IS NULL";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void updateStatutByCommandeId(int commandeId, String statut) throws SQLException {
        if (commandeId <= 0) {
            throw new IllegalArgumentException("commandeId invalide");
        }
        if (statut == null || statut.isBlank()) {
            throw new IllegalArgumentException("statut obligatoire");
        }
        String sql = "UPDATE livraison SET statut = ?, updated_at = ? WHERE commande_id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, commandeId);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Livraison livraison, boolean isUpdate) throws SQLException {
        ps.setInt(1, livraison.getCommandeId());
        ps.setString(2, livraison.getStatut());
        ps.setString(3, livraison.getAdresse());
        ps.setString(4, livraison.getVille());
        ps.setString(5, livraison.getCodePostal());
        setNullableTimestamp(ps, 6, livraison.getDatePrevue());
        setNullableTimestamp(ps, 7, livraison.getDateLivraison());
        setNullableString(ps, 8, livraison.getTrackingCode());
        if (!isUpdate) {
            ps.setTimestamp(9, Timestamp.valueOf(livraison.getCreatedAt()));
            setNullableTimestamp(ps, 10, livraison.getUpdatedAt());
        } else {
            setNullableTimestamp(ps, 9, livraison.getUpdatedAt() != null ? livraison.getUpdatedAt() : LocalDateTime.now());
            ps.setInt(10, livraison.getId());
        }
    }

    private Livraison map(ResultSet rs) throws SQLException {
        Livraison livraison = new Livraison();
        livraison.setId(rs.getInt("id"));
        livraison.setCommandeId(rs.getInt("commande_id"));
        livraison.setStatut(rs.getString("statut"));
        livraison.setAdresse(rs.getString("adresse"));
        livraison.setVille(rs.getString("ville"));
        livraison.setCodePostal(rs.getString("code_postal"));
        Timestamp datePrevue = rs.getTimestamp("date_prevue");
        livraison.setDatePrevue(datePrevue != null ? datePrevue.toLocalDateTime() : null);
        Timestamp dateLivraison = rs.getTimestamp("date_livraison");
        livraison.setDateLivraison(dateLivraison != null ? dateLivraison.toLocalDateTime() : null);
        livraison.setTrackingCode(rs.getString("tracking_code"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        livraison.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        livraison.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        return livraison;
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

    private void setNullableTimestamp(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
