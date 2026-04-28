-- Script SQL: Créer la table Wishlist
-- Date: 2026-04-23
-- Objectif: Table pour stocker les listes de souhaits des clients

-- Créer la table wishlist
CREATE TABLE IF NOT EXISTS wishlist (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    produit_id INT NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Index pour recherche rapide
    INDEX idx_utilisateur_id (utilisateur_id),
    INDEX idx_produit_id (produit_id),
    
    -- Constraint pour éviter les doublons (un produit max par utilisateur)
    UNIQUE KEY unique_wishlist (utilisateur_id, produit_id),
    
    -- Clés étrangères
    FOREIGN KEY (utilisateur_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vérifier que la table a été créée
SELECT * FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'chroniccare' AND TABLE_NAME = 'wishlist';

-- Afficher la structure
DESCRIBE wishlist;
