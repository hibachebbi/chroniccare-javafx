package com.chroniccare.services;

import com.chroniccare.models.Commentaire;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService {

    private final Connection conn = MyDatabase.getInstance().getConnection();
    private final NotificationService notificationService = new NotificationService();

    // ─────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────
    public boolean ajouter(Commentaire c) {
        String sql = "INSERT INTO commentaire (contenu, is_anonymous, like_count, created_at, publication_id, auteur_id, parent_id) "
                + "VALUES (?, ?, 0, NOW(), ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getContenu());
            ps.setBoolean(2, c.isAnonymous());
            ps.setInt(3, c.getPublicationId());
            ps.setObject(4, c.getAuteurId());
            ps.setObject(5, c.getParentId()); // null si commentaire racine
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next() && c.getAuteurId() != null) {
                        notificationService.notifierCommentairePublication(
                                c.getPublicationId(),
                                keys.getInt(1),
                                c.getAuteurId(),
                                c.getContenu()
                        );
                    }
                }
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Erreur ajouter commentaire : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────
    // READ - retourne arbre commentaires/reponses
    // ─────────────────────────────────────────
    public List<Commentaire> getByPublication(int publicationId) {
        // 1. Charger tous les commentaires de la publication
        List<Commentaire> tous = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE publication_id=? ORDER BY created_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, publicationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tous.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getByPublication : " + e.getMessage());
        }

        // 2. Construire l'arbre : racines + leurs reponses
        List<Commentaire> racines = new ArrayList<>();
        for (Commentaire c : tous) {
            if (c.getParentId() == null) {
                racines.add(c);
            }
        }
        for (Commentaire racine : racines) {
            for (Commentaire c : tous) {
                if (c.getParentId() != null && c.getParentId() == racine.getId()) {
                    racine.addReponse(c);
                }
            }
        }
        return racines;
    }

    // ─────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────
    public boolean modifier(Commentaire c) {
        String sql = "UPDATE commentaire SET contenu=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getContenu());
            ps.setInt(2, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modifier commentaire : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────
    // DELETE (supprime aussi les reponses via CASCADE)
    // ─────────────────────────────────────────
    public boolean supprimer(int id) {
        String sql = "DELETE FROM commentaire WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur supprimer commentaire : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────
    // LIKES
    // ─────────────────────────────────────────
    public boolean hasLiked(int commentaireId, int userId) {
        String sql = "SELECT liked_by FROM commentaire WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String likedBy = rs.getString("liked_by");
                if (likedBy == null) return false;
                for (String id : likedBy.split(",")) {
                    if (id.trim().equals(String.valueOf(userId))) return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur hasLiked commentaire : " + e.getMessage());
        }
        return false;
    }

    public boolean liker(int commentaireId, int userId) {
        if (hasLiked(commentaireId, userId)) return false;
        String sql = "UPDATE commentaire SET like_count = like_count + 1, "
                + "liked_by = CASE WHEN liked_by IS NULL THEN ? ELSE CONCAT(liked_by, ',', ?) END "
                + "WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(userId));
            ps.setString(2, String.valueOf(userId));
            ps.setInt(3, commentaireId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur liker commentaire : " + e.getMessage());
            return false;
        }
    }

    public boolean unliker(int commentaireId, int userId) {
        if (!hasLiked(commentaireId, userId)) return false;
        String sql = "SELECT liked_by FROM commentaire WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commentaireId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String likedBy = rs.getString("liked_by");
                if (likedBy == null) return false;
                StringBuilder newLikedBy = new StringBuilder();
                for (String id : likedBy.split(",")) {
                    if (!id.trim().equals(String.valueOf(userId))) {
                        if (newLikedBy.length() > 0) newLikedBy.append(",");
                        newLikedBy.append(id.trim());
                    }
                }
                String updateSql = "UPDATE commentaire SET like_count = like_count - 1, "
                        + "liked_by = ? WHERE id=? AND like_count > 0";
                try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                    ps2.setString(1, newLikedBy.length() > 0 ? newLikedBy.toString() : null);
                    ps2.setInt(2, commentaireId);
                    return ps2.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur unliker commentaire : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────
    private Commentaire mapRow(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();
        c.setId(rs.getInt("id"));
        c.setContenu(rs.getString("contenu"));
        c.setAnonymous(rs.getBoolean("is_anonymous"));
        c.setLikeCount(rs.getInt("like_count"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        c.setPublicationId(rs.getInt("publication_id"));
        int auteurId = rs.getInt("auteur_id");
        if (!rs.wasNull()) c.setAuteurId(auteurId);
        int parentId = rs.getInt("parent_id");
        if (!rs.wasNull()) c.setParentId(parentId);
        return c;
    }
}