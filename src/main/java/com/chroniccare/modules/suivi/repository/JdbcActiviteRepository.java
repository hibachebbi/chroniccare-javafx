package com.chroniccare.modules.suivi.repository;

import com.chroniccare.modules.suivi.model.Activite;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class JdbcActiviteRepository implements ActiviteRepository {
    @Override
    public List<Activite> findAll() {
        String sql = """
                SELECT id, utilisateur_id, etat_id, type, duree, calories, distance_km,
                       heures_repos, date_activite, notes, created_at
                FROM activite
                ORDER BY date_activite DESC, id DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Activite> rows = new ArrayList<>();
            while (rs.next()) rows.add(mapRow(rs));
            return rows;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB findAll activite.", ex);
        }
    }

    @Override
    public Optional<Activite> findById(Long id) {
        String sql = """
                SELECT id, utilisateur_id, etat_id, type, duree, calories, distance_km,
                       heures_repos, date_activite, notes, created_at
                FROM activite
                WHERE id = ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB findById activite.", ex);
        }
    }

    @Override
    public Activite save(Activite entity) {
        String sql = """
                INSERT INTO activite (utilisateur_id, etat_id, type, duree, calories, distance_km,
                                      heures_repos, date_activite, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableLong(statement, 1, entity.getUtilisateurId());
            setNullableLong(statement, 2, entity.getEtatId());
            statement.setString(3, entity.getType());
            statement.setInt(4, entity.getDuree());
            statement.setInt(5, entity.getCalories());
            setNullableDouble(statement, 6, entity.getDistanceKm());
            setNullableInt(statement, 7, entity.getHeuresRepos());
            statement.setTimestamp(8, Timestamp.valueOf(entity.getDateActivite()));
            setNullableString(statement, 9, entity.getNotes());
            statement.setTimestamp(10, Timestamp.valueOf(entity.getCreatedAt()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) entity.setId(keys.getLong(1));
            }
            return entity;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB save activite.", ex);
        }
    }

    @Override
    public Activite update(Long id, Activite entity) {
        String sql = """
                UPDATE activite
                SET utilisateur_id = ?, etat_id = ?, type = ?, duree = ?, calories = ?,
                    distance_km = ?, heures_repos = ?, date_activite = ?, notes = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            setNullableLong(statement, 1, entity.getUtilisateurId());
            setNullableLong(statement, 2, entity.getEtatId());
            statement.setString(3, entity.getType());
            statement.setInt(4, entity.getDuree());
            statement.setInt(5, entity.getCalories());
            setNullableDouble(statement, 6, entity.getDistanceKm());
            setNullableInt(statement, 7, entity.getHeuresRepos());
            statement.setTimestamp(8, Timestamp.valueOf(entity.getDateActivite()));
            setNullableString(statement, 9, entity.getNotes());
            statement.setLong(10, id);
            if (statement.executeUpdate() == 0) throw new NoSuchElementException("Activite introuvable: " + id);
            entity.setId(id);
            return entity;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB update activite.", ex);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM activite WHERE id = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) throw new NoSuchElementException("Activite introuvable: " + id);
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB delete activite.", ex);
        }
    }

    @Override
    public List<Activite> findByEtatId(Long etatId) {
        if (etatId == null) return List.of();
        String sql = """
                SELECT id, utilisateur_id, etat_id, type, duree, calories, distance_km,
                       heures_repos, date_activite, notes, created_at
                FROM activite
                WHERE etat_id = ?
                ORDER BY date_activite DESC, id DESC
                """;
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setLong(1, etatId);
            try (ResultSet rs = statement.executeQuery()) {
                List<Activite> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapRow(rs));
                return rows;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB findByEtatId activite.", ex);
        }
    }

    private Activite mapRow(ResultSet rs) throws SQLException {
        return new Activite(
                rs.getLong("id"),
                rs.getLong("utilisateur_id"),
                rs.getObject("etat_id") == null ? null : rs.getLong("etat_id"),
                rs.getString("type"),
                rs.getInt("duree"),
                rs.getInt("calories"),
                rs.getObject("distance_km") == null ? null : rs.getDouble("distance_km"),
                rs.getObject("heures_repos") == null ? null : rs.getInt("heures_repos"),
                rs.getTimestamp("date_activite") == null ? null : rs.getTimestamp("date_activite").toLocalDateTime(),
                rs.getString("notes"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value);
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value);
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) statement.setNull(index, Types.INTEGER);
        else statement.setInt(index, value);
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) statement.setNull(index, Types.DOUBLE);
        else statement.setDouble(index, value);
    }
}
