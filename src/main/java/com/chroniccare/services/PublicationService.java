package com.chroniccare.services;

import com.chroniccare.models.Publication;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PublicationService {

    private final Connection conn = MyDatabase.getInstance().getConnection();
    private final NotificationService notificationService = new NotificationService();

    // CREATE
    public boolean ajouter(Publication p) {
        String sql = "INSERT INTO publication (titre, contenu, categorie, nb_vues, nb_likes, statut, created_at, auteur_id, image_path) VALUES (?, ?, ?, 0, 0, ?, NOW(), ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getContenu());
            ps.setString(3, p.getCategorie());
            ps.setString(4, p.getStatut());
            ps.setObject(5, p.getAuteurId());
            ps.setString(6, p.getImagePath());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajouter : " + e.getMessage());
            return false;
        }
    }

    // READ ALL
    public List<Publication> getAll() {
        List<Publication> list = new ArrayList<>();
        String sql = "SELECT * FROM publication ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAll : " + e.getMessage());
        }
        return list;
    }

    // UPDATE
    public boolean modifier(Publication p) {
        String sql = "UPDATE publication SET titre=?, contenu=?, categorie=?, statut=?, image_path=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getContenu());
            ps.setString(3, p.getCategorie());
            ps.setString(4, p.getStatut());
            ps.setString(5, p.getImagePath());
            ps.setInt(6, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modifier : " + e.getMessage());
            return false;
        }
    }

    // DELETE
    public boolean supprimer(int id) {
        try {
            // D'abord supprimer les commentaires liés
            String sqlComm = "DELETE FROM commentaire WHERE publication_id=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlComm)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            // Ensuite supprimer la publication
            String sql = "DELETE FROM publication WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur supprimer publication : " + e.getMessage());
            return false;
        }
    }

    // Vérifier si l'user a liké
    public boolean hasLiked(int publicationId, int userId) {
        String sql = "SELECT liked_by FROM publication WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, publicationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String likedBy = rs.getString("liked_by");
                if (likedBy == null) return false;
                for (String id : likedBy.split(",")) {
                    if (id.trim().equals(String.valueOf(userId))) return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur hasLiked : " + e.getMessage());
        }
        return false;
    }

    // Liker
    public boolean liker(int publicationId, int userId) {
        if (hasLiked(publicationId, userId)) return false;
        String sql = "UPDATE publication SET nb_likes = nb_likes + 1, " +
                "liked_by = CASE WHEN liked_by IS NULL " +
                "THEN ? ELSE CONCAT(liked_by, ',', ?) END WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(userId));
            ps.setString(2, String.valueOf(userId));
            ps.setInt(3, publicationId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                notificationService.notifierLikePublication(publicationId, userId);
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Erreur liker : " + e.getMessage());
            return false;
        }
    }

    // Disliker
    public boolean unliker(int publicationId, int userId) {
        if (!hasLiked(publicationId, userId)) return false;
        String sql = "SELECT liked_by FROM publication WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, publicationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String likedBy = rs.getString("liked_by");
                if (likedBy == null) return false;

                // Retirer l'userId de la liste
                StringBuilder newLikedBy = new StringBuilder();
                for (String id : likedBy.split(",")) {
                    if (!id.trim().equals(String.valueOf(userId))) {
                        if (newLikedBy.length() > 0) newLikedBy.append(",");
                        newLikedBy.append(id.trim());
                    }
                }

                String updateSql = "UPDATE publication SET nb_likes = nb_likes - 1, " +
                        "liked_by = ? WHERE id=? AND nb_likes > 0";
                try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                    ps2.setString(1, newLikedBy.length() > 0 ? newLikedBy.toString() : null);
                    ps2.setInt(2, publicationId);
                    return ps2.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur unliker : " + e.getMessage());
        }
        return false;
    }

    private Publication mapRow(ResultSet rs) throws SQLException {
        Publication p = new Publication();
        p.setId(rs.getInt("id"));
        p.setTitre(rs.getString("titre"));
        p.setContenu(rs.getString("contenu"));
        p.setCategorie(rs.getString("categorie"));
        p.setNbVues(rs.getInt("nb_vues"));
        p.setNbLikes(rs.getInt("nb_likes"));
        p.setStatut(rs.getString("statut"));
        p.setImagePath(rs.getString("image_path"));
        p.setLikedBy(rs.getString("liked_by"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        int auteurId = rs.getInt("auteur_id");
        if (!rs.wasNull()) p.setAuteurId(auteurId);
        return p;
    }
    public String getNomAuteur(int auteurId) {
        String sql = "SELECT nom, prenom FROM users WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auteurId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("prenom") + " " + rs.getString("nom");
            }
        } catch (SQLException e) {
            System.err.println("Erreur getNomAuteur : " + e.getMessage());
        }
        return "Auteur #" + auteurId;
    }
    public boolean titreExiste(String titre) {
        String sql = "SELECT COUNT(*) FROM publication WHERE titre=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
        return false;
    }
}
