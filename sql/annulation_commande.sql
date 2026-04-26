-- Créer la table pour les annulations de commande
CREATE TABLE IF NOT EXISTS annulation_commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id INT NOT NULL UNIQUE,
    raison VARCHAR(500),
    date_annulation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utilisateur_id INT NOT NULL,
    statut_avant_annulation VARCHAR(50),
    montant_rembourse DOUBLE,
    stock_restitue TINYINT DEFAULT 0,
    FOREIGN KEY (commande_id) REFERENCES commande(id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ajouter colonne si elle n'existe pas
ALTER TABLE commande ADD COLUMN IF NOT EXISTS motif_annulation VARCHAR(500);

-- Index pour rapidité
CREATE INDEX idx_commande_id_annulation ON annulation_commande(commande_id);
CREATE INDEX idx_utilisateur_id_annulation ON annulation_commande(utilisateur_id);

