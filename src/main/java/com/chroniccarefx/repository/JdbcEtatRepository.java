package com.chroniccarefx.repository;

import com.chroniccarefx.config.Db;
import com.chroniccarefx.model.Etat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class JdbcEtatRepository implements EtatRepository {
    @Override
    public List<Etat> findAll() {
        String sql = """
                SELECT id, utilisateur_id, traitement_en_cours, remarques_cliniques,
                       temperature_corporelle, niveau_hydratation, date_releve
                FROM etat
                ORDER BY id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Etat> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapRow(rs));
            }
            return rows;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB findAll etat.", ex);
        }
    }

    @Override
    public Optional<Etat> findById(Long id) {
        String sql = """
                SELECT id, utilisateur_id, traitement_en_cours, remarques_cliniques,
                       temperature_corporelle, niveau_hydratation, date_releve
                FROM etat
                WHERE id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB findById etat.", ex);
        }
    }

    @Override
    public Etat save(Etat entity) {
        String sql = """
                INSERT INTO etat (
                    utilisateur_id, traitement_en_cours, remarques_cliniques,
                    temperature_corporelle, niveau_hydratation, date_releve
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableLong(statement, 1, entity.getUtilisateurId());
            setNullableString(statement, 2, entity.getTraitementEnCours());
            setNullableString(statement, 3, entity.getRemarquesCliniques());
            setNullableString(statement, 4, entity.getTemperatureCorporelle());
            setNullableString(statement, 5, entity.getNiveauHydratation());
            statement.setTimestamp(6, Timestamp.valueOf(entity.getDateReleve()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB save etat.", ex);
        }
    }

    @Override
    public Etat update(Long id, Etat entity) {
        String sql = """
                UPDATE etat
                SET utilisateur_id = ?,
                    traitement_en_cours = ?,
                    remarques_cliniques = ?,
                    temperature_corporelle = ?,
                    niveau_hydratation = ?,
                    date_releve = ?
                WHERE id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, entity.getUtilisateurId());
            setNullableString(statement, 2, entity.getTraitementEnCours());
            setNullableString(statement, 3, entity.getRemarquesCliniques());
            setNullableString(statement, 4, entity.getTemperatureCorporelle());
            setNullableString(statement, 5, entity.getNiveauHydratation());
            statement.setTimestamp(6, Timestamp.valueOf(entity.getDateReleve()));
            statement.setLong(7, id);

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new NoSuchElementException("Etat introuvable: " + id);
            }
            entity.setId(id);
            return entity;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB update etat.", ex);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM etat WHERE id = ?";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new NoSuchElementException("Etat introuvable: " + id);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur DB delete etat.", ex);
        }
    }

    private Etat mapRow(ResultSet rs) throws SQLException {
        return new Etat(
                rs.getLong("id"),
                rs.getLong("utilisateur_id"),
                rs.getString("traitement_en_cours"),
                rs.getString("remarques_cliniques"),
                rs.getString("temperature_corporelle"),
                rs.getString("niveau_hydratation"),
                toLocalDateTime(rs.getTimestamp("date_releve"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
