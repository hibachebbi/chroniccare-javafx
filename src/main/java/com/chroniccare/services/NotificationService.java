package com.chroniccare.services;

import com.chroniccare.models.Notification;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private static final String TYPE_LIKE_PUBLICATION = "LIKE_PUBLICATION";
    private static final String TYPE_COMMENT_PUBLICATION = "COMMENT_PUBLICATION";

    private final Connection conn = MyDatabase.getInstance().getConnection();

    public NotificationService() {
        ensureTableExists();
    }

    public void notifierLikePublication(int publicationId, int actorUserId) {
        try {
            PublicationOwnerData publicationData = getPublicationOwnerData(publicationId);
            if (publicationData == null || publicationData.ownerId == actorUserId) {
                return;
            }

            UserSummary actor = getUserSummary(actorUserId);
            if (actor == null) {
                return;
            }

            if (notificationExisteDeja(publicationId, actorUserId, publicationData.ownerId, TYPE_LIKE_PUBLICATION)) {
                return;
            }

            String message = actor.fullName + " a aime votre publication \"" + safeTitle(publicationData.title) + "\".";
            insererNotification(publicationData.ownerId, actorUserId, publicationId, null, TYPE_LIKE_PUBLICATION, message);
        } catch (SQLException e) {
            System.err.println("Erreur notification like : " + e.getMessage());
        }
    }

    public void notifierCommentairePublication(int publicationId, int commentaireId, int actorUserId, String contenuCommentaire) {
        try {
            PublicationOwnerData publicationData = getPublicationOwnerData(publicationId);
            if (publicationData == null || publicationData.ownerId == actorUserId) {
                return;
            }

            UserSummary actor = getUserSummary(actorUserId);
            if (actor == null) {
                return;
            }

            String extrait = truncate(contenuCommentaire, 70);
            String message = actor.fullName + " a commente votre publication \"" + safeTitle(publicationData.title)
                    + "\" : \"" + extrait + "\".";
            insererNotification(publicationData.ownerId, actorUserId, publicationId, commentaireId, TYPE_COMMENT_PUBLICATION, message);
        } catch (SQLException e) {
            System.err.println("Erreur notification commentaire : " + e.getMessage());
        }
    }

    public List<Notification> getByRecipient(int recipientUserId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE recipient_user_id = ? ORDER BY created_at DESC, id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture notifications : " + e.getMessage());
        }

        return notifications;
    }

    public int countUnread(int recipientUserId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE recipient_user_id = ? AND is_read = FALSE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur compteur notifications : " + e.getMessage());
        }

        return 0;
    }

    public boolean markAllAsRead(int recipientUserId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE recipient_user_id = ? AND is_read = FALSE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur mise a jour notifications : " + e.getMessage());
            return false;
        }
    }

    private void insererNotification(int recipientUserId, int actorUserId, Integer publicationId,
                                     Integer commentaireId, String type, String message) throws SQLException {
        String sql = "INSERT INTO notification (recipient_user_id, actor_user_id, publication_id, commentaire_id, type, message, is_read, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, FALSE, NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);
            ps.setInt(2, actorUserId);
            if (publicationId != null) {
                ps.setInt(3, publicationId);
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (commentaireId != null) {
                ps.setInt(4, commentaireId);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, type);
            ps.setString(6, message);
            ps.executeUpdate();
        }
    }

    private boolean notificationExisteDeja(int publicationId, int actorUserId, int recipientUserId, String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE publication_id = ? AND actor_user_id = ? AND recipient_user_id = ? AND type = ? AND is_read = FALSE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, publicationId);
            ps.setInt(2, actorUserId);
            ps.setInt(3, recipientUserId);
            ps.setString(4, type);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    private PublicationOwnerData getPublicationOwnerData(int publicationId) throws SQLException {
        String sql = "SELECT id, titre, auteur_id FROM publication WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, publicationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int ownerId = rs.getInt("auteur_id");
                    if (rs.wasNull()) {
                        return null;
                    }

                    PublicationOwnerData data = new PublicationOwnerData();
                    data.title = rs.getString("titre");
                    data.ownerId = ownerId;
                    return data;
                }
            }
        }

        return null;
    }

    private UserSummary getUserSummary(int userId) throws SQLException {
        String sql = "SELECT nom, prenom FROM users WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserSummary user = new UserSummary();
                    String prenom = rs.getString("prenom");
                    String nom = rs.getString("nom");
                    user.fullName = ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim();
                    if (user.fullName.isEmpty()) {
                        user.fullName = "Utilisateur #" + userId;
                    }
                    return user;
                }
            }
        }

        return null;
    }


    private String safeTitle(String title) {
        return title == null || title.isBlank() ? "sans titre" : title;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "commentaire sans contenu";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setRecipientUserId(rs.getInt("recipient_user_id"));
        notification.setActorUserId(rs.getInt("actor_user_id"));

        int publicationId = rs.getInt("publication_id");
        if (!rs.wasNull()) {
            notification.setPublicationId(publicationId);
        }

        int commentaireId = rs.getInt("commentaire_id");
        if (!rs.wasNull()) {
            notification.setCommentaireId(commentaireId);
        }

        notification.setType(rs.getString("type"));
        notification.setMessage(rs.getString("message"));
        notification.setRead(rs.getBoolean("is_read"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            notification.setCreatedAt(ts.toLocalDateTime());
        }

        return notification;
    }

    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS notification (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    recipient_user_id INT NOT NULL,
                    actor_user_id INT NOT NULL,
                    publication_id INT NULL,
                    commentaire_id INT NULL,
                    type VARCHAR(50) NOT NULL,
                    message VARCHAR(255) NOT NULL,
                    is_read BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_notification_recipient (recipient_user_id),
                    INDEX idx_notification_read (recipient_user_id, is_read)
                )
                """;

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erreur creation table notification : " + e.getMessage());
        }
    }

    private static class PublicationOwnerData {
        private int ownerId;
        private String title;
    }

    private static class UserSummary {
        private String fullName;
    }
}
