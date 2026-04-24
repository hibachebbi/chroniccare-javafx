package com.chroniccare.services;

import com.chroniccare.models.Exercise;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExerciseService {
    private final Connection conn = MyDatabase.getInstance().getConnection();

    public List<Exercise> getByCoachId(int coachId) throws SQLException {
        String sql = "SELECT ex.*, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ev.coach_id=? " +
                "ORDER BY ex.id DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();
        List<Exercise> exercises = new ArrayList<>();
        while (rs.next()) {
            exercises.add(mapExercise(rs));
        }
        return exercises;
    }

    public List<Exercise> getByCoachId(int coachId, int limit) throws SQLException {
        if (limit <= 0) {
            return getByCoachId(coachId);
        }

        String sql = "SELECT ex.*, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ev.coach_id=? " +
                "ORDER BY ex.id DESC " +
                "LIMIT ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);
        ps.setInt(2, limit);

        ResultSet rs = ps.executeQuery();
        List<Exercise> exercises = new ArrayList<>();
        while (rs.next()) {
            exercises.add(mapExercise(rs));
        }
        return exercises;
    }

    public List<Exercise> getByCoachIdForSelection(int coachId) throws SQLException {
        String sql = "SELECT ex.id, ex.nom, ex.duree, ex.repetitions, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ev.coach_id=? " +
                "ORDER BY ex.id DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);

        ResultSet rs = ps.executeQuery();
        List<Exercise> exercises = new ArrayList<>();
        while (rs.next()) {
            Exercise exercise = new Exercise();
            exercise.setId(rs.getInt("id"));
            exercise.setNom(rs.getString("nom"));
            exercise.setDuree(rs.getInt("duree"));
            int repetitions = rs.getInt("repetitions");
            exercise.setRepetitions(rs.wasNull() ? null : repetitions);
            exercise.setEvenementTitre(rs.getString("evenement_titre"));
            exercises.add(exercise);
        }
        return exercises;
    }

    public List<Exercise> getByCoachIdForSelection(int coachId, int limit) throws SQLException {
        if (limit <= 0) {
            return getByCoachIdForSelection(coachId);
        }

        String sql = "SELECT ex.id, ex.nom, ex.duree, ex.repetitions, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ev.coach_id=? " +
                "ORDER BY ex.id DESC " +
                "LIMIT ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, coachId);
        ps.setInt(2, limit);

        ResultSet rs = ps.executeQuery();
        List<Exercise> exercises = new ArrayList<>();
        while (rs.next()) {
            Exercise exercise = new Exercise();
            exercise.setId(rs.getInt("id"));
            exercise.setNom(rs.getString("nom"));
            exercise.setDuree(rs.getInt("duree"));
            int repetitions = rs.getInt("repetitions");
            exercise.setRepetitions(rs.wasNull() ? null : repetitions);
            exercise.setEvenementTitre(rs.getString("evenement_titre"));
            exercises.add(exercise);
        }
        return exercises;
    }

    public List<Exercise> getByEventId(int eventId, int coachId) throws SQLException {
        String sql = "SELECT ex.*, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ex.evenement_id=? AND ev.coach_id=? " +
                "ORDER BY ex.id ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setInt(2, coachId);

        ResultSet rs = ps.executeQuery();
        List<Exercise> exercises = new ArrayList<>();
        while (rs.next()) {
            exercises.add(mapExercise(rs));
        }
        return exercises;
    }

    public List<Exercise> getByEventIdPublic(int eventId) throws SQLException {
        String sql = "SELECT ex.*, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ex.evenement_id=? " +
                "ORDER BY ex.id ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);

        ResultSet rs = ps.executeQuery();
        List<Exercise> exercises = new ArrayList<>();
        while (rs.next()) {
            exercises.add(mapExercise(rs));
        }
        return exercises;
    }

    public Exercise getById(int id, int coachId) throws SQLException {
        String sql = "SELECT ex.*, ev.titre AS evenement_titre " +
                "FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ex.id=? AND ev.coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.setInt(2, coachId);

        ResultSet rs = ps.executeQuery();
        return rs.next() ? mapExercise(rs) : null;
    }

    public void insert(Exercise exercise) throws SQLException {
        String sql = "INSERT INTO exercice (nom, description, duree, repetitions, evenement_id, video_url) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, exercise.getNom());
        ps.setString(2, exercise.getDescription());
        ps.setInt(3, exercise.getDuree());
        if (exercise.getRepetitions() != null) {
            ps.setInt(4, exercise.getRepetitions());
        } else {
            ps.setNull(4, java.sql.Types.INTEGER);
        }
        ps.setInt(5, exercise.getEvenementId());
        if (exercise.getVideoUrl() != null && !exercise.getVideoUrl().isEmpty()) {
            ps.setString(6, exercise.getVideoUrl());
        } else {
            ps.setNull(6, java.sql.Types.VARCHAR);
        }
        ps.executeUpdate();
    }

    public void update(Exercise exercise, int coachId) throws SQLException {
        String sql = "UPDATE exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "SET ex.nom=?, ex.description=?, ex.duree=?, ex.repetitions=?, ex.evenement_id=?, ex.video_url=? " +
                "WHERE ex.id=? AND ev.coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, exercise.getNom());
        ps.setString(2, exercise.getDescription());
        ps.setInt(3, exercise.getDuree());
        if (exercise.getRepetitions() != null) {
            ps.setInt(4, exercise.getRepetitions());
        } else {
            ps.setNull(4, java.sql.Types.INTEGER);
        }
        ps.setInt(5, exercise.getEvenementId());
        if (exercise.getVideoUrl() != null && !exercise.getVideoUrl().isEmpty()) {
            ps.setString(6, exercise.getVideoUrl());
        } else {
            ps.setNull(6, java.sql.Types.VARCHAR);
        }
        ps.setInt(7, exercise.getId());
        ps.setInt(8, coachId);
        ps.executeUpdate();
    }

    public void delete(int exerciseId, int coachId) throws SQLException {
        String sql = "DELETE ex FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ex.id=? AND ev.coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, exerciseId);
        ps.setInt(2, coachId);
        ps.executeUpdate();
    }

    public void deleteByEventId(int eventId, int coachId) throws SQLException {
        String sql = "DELETE ex FROM exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "WHERE ex.evenement_id=? AND ev.coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setInt(2, coachId);
        ps.executeUpdate();
    }

    public void clearAssignmentsForEvent(int eventId, int coachId) throws SQLException {
        String sql = "UPDATE exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "SET ex.evenement_id=NULL " +
                "WHERE ex.evenement_id=? AND ev.coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setInt(2, coachId);
        ps.executeUpdate();
    }

    public void assignExerciseToEvent(int exerciseId, int eventId, int coachId) throws SQLException {
        String sql = "UPDATE exercice ex " +
                "JOIN evenement ev ON ev.id = ex.evenement_id " +
                "SET ex.evenement_id=? " +
                "WHERE ex.id=? AND ev.coach_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, eventId);
        ps.setInt(2, exerciseId);
        ps.setInt(3, coachId);
        ps.executeUpdate();
    }

    private Exercise mapExercise(ResultSet rs) throws SQLException {
        Exercise exercise = new Exercise();
        exercise.setId(rs.getInt("id"));
        exercise.setNom(rs.getString("nom"));
        exercise.setDescription(rs.getString("description"));
        exercise.setDuree(rs.getInt("duree"));
        int repetitions = rs.getInt("repetitions");
        exercise.setRepetitions(rs.wasNull() ? null : repetitions);
        int evenementId = rs.getInt("evenement_id");
        exercise.setEvenementId(rs.wasNull() ? null : evenementId);
        exercise.setEvenementTitre(rs.getString("evenement_titre"));
        String videoUrl = rs.getString("video_url");
        exercise.setVideoUrl(videoUrl);
        return exercise;
    }
}
