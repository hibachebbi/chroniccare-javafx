package com.chroniccare.services;

import com.chroniccare.models.Event;
import com.chroniccare.models.EventRegistration;
import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventService {
    private final Connection conn = MyDatabase.getInstance().getConnection();

    public List<Event> getByCoachId(int coachId) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE coach_id=? ORDER BY date_debut DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();
        List<Event> events = new ArrayList<>();
        while (rs.next()) {
            events.add(mapEvent(rs));
        }
        return events;
    }

    public List<Event> getByCoachIdForList(int coachId) throws SQLException {
        String sql = "SELECT id, statut, titre, date_debut, date_fin, lieu, coach_id " +
                "FROM evenement WHERE coach_id=? ORDER BY date_debut DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();
        List<Event> events = new ArrayList<>();
        while (rs.next()) {
            Event event = new Event();
            event.setId(rs.getInt("id"));
            event.setStatut(rs.getString("statut"));
            event.setTitre(rs.getString("titre"));
            event.setDateDebut(toLocalDateTime(rs.getTimestamp("date_debut")));
            event.setDateFin(toLocalDateTime(rs.getTimestamp("date_fin")));
            event.setLieu(rs.getString("lieu"));
            event.setCoachId(rs.getInt("coach_id"));
            events.add(event);
        }
        return events;
    }

    public List<Event> getByCoachIdForList(int coachId, int limit) throws SQLException {
        if (limit <= 0) {
            return getByCoachIdForList(coachId);
        }

        String sql = "SELECT id, statut, titre, date_debut, date_fin, lieu, coach_id " +
                "FROM evenement WHERE coach_id=? ORDER BY date_debut DESC LIMIT ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);
        ps.setInt(2, limit);

        ResultSet rs = ps.executeQuery();
        List<Event> events = new ArrayList<>();
        while (rs.next()) {
            Event event = new Event();
            event.setId(rs.getInt("id"));
            event.setStatut(rs.getString("statut"));
            event.setTitre(rs.getString("titre"));
            event.setDateDebut(toLocalDateTime(rs.getTimestamp("date_debut")));
            event.setDateFin(toLocalDateTime(rs.getTimestamp("date_fin")));
            event.setLieu(rs.getString("lieu"));
            event.setCoachId(rs.getInt("coach_id"));
            events.add(event);
        }
        return events;
    }

    public List<Event> getByCoachIdForSelection(int coachId) throws SQLException {
        String sql = "SELECT id, titre FROM evenement WHERE coach_id=? ORDER BY date_debut DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();
        List<Event> events = new ArrayList<>();
        while (rs.next()) {
            Event event = new Event();
            event.setId(rs.getInt("id"));
            event.setTitre(rs.getString("titre"));
            events.add(event);
        }
        return events;
    }

    public List<Event> getAvailableForPatients() throws SQLException {
        String sql = "SELECT * FROM evenement " +
                "WHERE statut IS NULL OR statut <> 'annule' " +
                "ORDER BY date_debut ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        List<Event> events = new ArrayList<>();
        while (rs.next()) {
            events.add(mapEvent(rs));
        }
        return events;
    }

    public List<Event> getRegisteredEventsByPatientEmail(String email) throws SQLException {
        String sql = "SELECT e.* FROM evenement e " +
                "JOIN inscription_evenement ie ON ie.evenement_id = e.id " +
                "WHERE ie.email=? " +
                "ORDER BY e.date_debut DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        List<Event> events = new ArrayList<>();
        while (rs.next()) {
            events.add(mapEvent(rs));
        }
        return events;
    }

    public boolean isPatientRegistered(int eventId, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription_evenement WHERE evenement_id=? AND email=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setString(2, email);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean registerPatientToEvent(int eventId, User patient) throws SQLException {
        if (patient == null) {
            throw new IllegalArgumentException("Patient introuvable.");
        }
        if (isPatientRegistered(eventId, patient.getEmail())) {
            return false;
        }
        String sql = "INSERT INTO inscription_evenement (nom, prenom, email, telephone, created_at, evenement_id) " +
                "VALUES (?, ?, ?, ?, NOW(), ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, patient.getNom());
        ps.setString(2, patient.getPrenom());
        ps.setString(3, patient.getEmail());
        ps.setString(4, patient.getTelephone());
        ps.setInt(5, eventId);
        ps.executeUpdate();
        return true;
    }

    public boolean cancelPatientRegistration(int eventId, String email) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email patient introuvable.");
        }

        Event event = getById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Evenement introuvable.");
        }
        if (!canCancelRegistration(event)) {
            throw new IllegalStateException("Annulation impossible moins de 24h avant le debut de l'evenement.");
        }

        String sql = "DELETE FROM inscription_evenement WHERE evenement_id=? AND email=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setString(2, email);
        return ps.executeUpdate() > 0;
    }

    public boolean canCancelRegistration(Event event) {
        if (event == null || event.getDateDebut() == null) {
            return false;
        }
        Duration remaining = Duration.between(LocalDateTime.now(), event.getDateDebut());
        return remaining.toHours() >= 24;
    }

    public Event getById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        return rs.next() ? mapEvent(rs) : null;
    }

    public void insert(Event event) throws SQLException {
        insertAndReturnId(event);
    }

    public int insertAndReturnId(Event event) throws SQLException {
        String sql = "INSERT INTO evenement (statut, titre, description, date_debut, date_fin, lieu, created_at, coach_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, event.getStatut());
        ps.setString(2, event.getTitre());
        ps.setString(3, event.getDescription());
        ps.setTimestamp(4, Timestamp.valueOf(event.getDateDebut()));
        ps.setTimestamp(5, Timestamp.valueOf(event.getDateFin()));
        ps.setString(6, event.getLieu());
        ps.setInt(7, event.getCoachId());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }
        throw new SQLException("Impossible de recuperer l'identifiant de l'evenement cree.");
    }

    public void update(Event event) throws SQLException {
        String sql = "UPDATE evenement SET statut=?, titre=?, description=?, date_debut=?, date_fin=?, lieu=? " +
                "WHERE id=? AND coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, event.getStatut());
        ps.setString(2, event.getTitre());
        ps.setString(3, event.getDescription());
        ps.setTimestamp(4, Timestamp.valueOf(event.getDateDebut()));
        ps.setTimestamp(5, Timestamp.valueOf(event.getDateFin()));
        ps.setString(6, event.getLieu());
        ps.setInt(7, event.getId());
        ps.setInt(8, event.getCoachId());
        ps.executeUpdate();
    }

    public void delete(int eventId, int coachId) throws SQLException {
        PreparedStatement deleteExercises = conn.prepareStatement(
                "DELETE FROM exercice WHERE evenement_id=?");
        deleteExercises.setInt(1, eventId);
        deleteExercises.executeUpdate();

        PreparedStatement deleteRegistrations = conn.prepareStatement(
                "DELETE FROM inscription_evenement WHERE evenement_id=?");
        deleteRegistrations.setInt(1, eventId);
        deleteRegistrations.executeUpdate();

        PreparedStatement deleteEvent = conn.prepareStatement(
                "DELETE FROM evenement WHERE id=? AND coach_id=?");
        deleteEvent.setInt(1, eventId);
        deleteEvent.setInt(2, coachId);
        deleteEvent.executeUpdate();
    }

    public List<EventRegistration> getRegistrationsForEvent(int eventId, int coachId) throws SQLException {
        String sql = "SELECT ie.* FROM inscription_evenement ie " +
                "JOIN evenement e ON e.id = ie.evenement_id " +
                "WHERE ie.evenement_id=? AND e.coach_id=? " +
                "ORDER BY ie.created_at DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setInt(2, coachId);

        ResultSet rs = ps.executeQuery();
        List<EventRegistration> registrations = new ArrayList<>();
        while (rs.next()) {
            registrations.add(mapRegistration(rs));
        }
        return registrations;
    }

    public Map<Integer, Integer> getRegistrationCountByEventForCoach(int coachId) throws SQLException {
        String sql = "SELECT e.id AS event_id, COUNT(ie.id) AS registration_count " +
                "FROM evenement e " +
                "LEFT JOIN inscription_evenement ie ON ie.evenement_id = e.id " +
                "WHERE e.coach_id=? " +
                "GROUP BY e.id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();
        Map<Integer, Integer> counts = new HashMap<>();
        while (rs.next()) {
            counts.put(rs.getInt("event_id"), rs.getInt("registration_count"));
        }
        return counts;
    }

    public Map<Integer, Integer> getRegistrationCountByEventIds(List<Integer> eventIds) throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        if (eventIds == null || eventIds.isEmpty()) {
            return counts;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(eventIds.size(), "?"));
        String sql = "SELECT e.id AS event_id, COUNT(ie.id) AS registration_count " +
                "FROM evenement e " +
                "LEFT JOIN inscription_evenement ie ON ie.evenement_id = e.id " +
                "WHERE e.id IN (" + placeholders + ") " +
                "GROUP BY e.id";
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < eventIds.size(); i++) {
            ps.setInt(i + 1, eventIds.get(i));
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            counts.put(rs.getInt("event_id"), rs.getInt("registration_count"));
        }
        return counts;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setStatut(rs.getString("statut"));
        event.setTitre(rs.getString("titre"));
        event.setDescription(rs.getString("description"));
        event.setDateDebut(toLocalDateTime(rs.getTimestamp("date_debut")));
        event.setDateFin(toLocalDateTime(rs.getTimestamp("date_fin")));
        event.setLieu(rs.getString("lieu"));
        event.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        event.setCoachId(rs.getInt("coach_id"));
        return event;
    }

    private EventRegistration mapRegistration(ResultSet rs) throws SQLException {
        EventRegistration registration = new EventRegistration();
        registration.setId(rs.getInt("id"));
        registration.setNom(rs.getString("nom"));
        registration.setPrenom(rs.getString("prenom"));
        registration.setEmail(rs.getString("email"));
        registration.setTelephone(rs.getString("telephone"));
        registration.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        registration.setEvenementId(rs.getInt("evenement_id"));
        return registration;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
