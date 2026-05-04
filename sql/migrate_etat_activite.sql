-- Tables pour le module État / Activité (com.chroniccare.modules.suivi)
-- Exécuter une fois sur la base MySQL utilisée par ChronicCare.

CREATE TABLE IF NOT EXISTS etat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL,
    traitement_en_cours VARCHAR(500) NULL,
    remarques_cliniques TEXT NULL,
    temperature_corporelle VARCHAR(50) NULL,
    niveau_hydratation VARCHAR(50) NULL,
    date_releve DATETIME NOT NULL,
    INDEX idx_etat_user (utilisateur_id),
    INDEX idx_etat_date (date_releve),
    CONSTRAINT fk_etat_user FOREIGN KEY (utilisateur_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS activite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL,
    etat_id BIGINT NULL,
    type VARCHAR(100) NOT NULL,
    duree INT NOT NULL,
    calories INT NOT NULL,
    distance_km DOUBLE NULL,
    heures_repos INT NULL,
    date_activite DATETIME NOT NULL,
    notes TEXT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_activite_user (utilisateur_id),
    INDEX idx_activite_etat (etat_id),
    INDEX idx_activite_date (date_activite),
    CONSTRAINT fk_activite_user FOREIGN KEY (utilisateur_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_activite_etat FOREIGN KEY (etat_id) REFERENCES etat (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
