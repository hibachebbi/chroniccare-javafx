-- Create livraison table for ChronicCare
-- Adjust database name if needed before running.

CREATE TABLE IF NOT EXISTS livraison (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id INT NOT NULL,
    statut VARCHAR(50) NOT NULL,
    adresse VARCHAR(255) NOT NULL,
    ville VARCHAR(100) NOT NULL,
    code_postal VARCHAR(20),
    date_prevue DATETIME NULL,
    date_livraison DATETIME NULL,
    tracking_code VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_livraison_commande
        FOREIGN KEY (commande_id) REFERENCES commande(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_livraison_commande ON livraison(commande_id);
CREATE INDEX idx_livraison_statut ON livraison(statut);

