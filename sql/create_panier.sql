-- Table pour le Panier Persistant
CREATE TABLE IF NOT EXISTS panier (
    id INT PRIMARY KEY AUTO_INCREMENT,
    utilisateur_id INT NOT NULL,
    produit_id INT NOT NULL,
    quantite INT NOT NULL DEFAULT 1,
    prix_unitaire DECIMAL(10, 2) NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE,
    UNIQUE KEY unique_panier (utilisateur_id, produit_id),
    INDEX idx_utilisateur_id (utilisateur_id),
    INDEX idx_produit_id (produit_id)
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_panier_date ON panier(date_ajout);

