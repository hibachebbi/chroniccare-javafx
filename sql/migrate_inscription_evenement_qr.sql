-- Colonnes QR pour les inscriptions aux événements (écran patient « Mes inscriptions »).
-- Exécuter une fois sur la base ChronicCare (MySQL / MariaDB).

ALTER TABLE inscription_evenement
    ADD COLUMN qr_code_token VARCHAR(100) DEFAULT NULL,
    ADD COLUMN qr_code_path VARCHAR(500) DEFAULT NULL;
