-- Script SQL: Ajouter les colonnes manquantes pour l'annulation de commande
-- Date: 2026-04-23
-- Objectif: Corriger les erreurs "Unknown column/field"

-- ========================================
-- 1. Vérifier et ajouter colonne motif_annulation dans commande
-- ========================================

-- Vérifier si la colonne existe déjà
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'commande'
AND COLUMN_NAME = 'motif_annulation'
AND TABLE_SCHEMA = 'chroniccare';

-- Si la requête ci-dessus ne retourne rien, exécuter:
ALTER TABLE commande ADD COLUMN motif_annulation VARCHAR(255) NULL AFTER statut;

-- ========================================
-- 2. Vérifier la table annulation_commande
-- ========================================

-- Vérifier la structure
SHOW CREATE TABLE annulation_commande;

-- Vérifier les colonnes
SHOW COLUMNS FROM annulation_commande;

-- ========================================
-- 3. Si annulation_commande n'existe pas, la créer
-- ========================================

-- Créer la table si elle n'existe pas
CREATE TABLE IF NOT EXISTS annulation_commande (
    id INT AUTO_INCREMENT PRIMARY KEY,
    commande_id INT NOT NULL,
    raison VARCHAR(255),
    utilisateur_id INT NOT NULL,
    statut_avant_annulation VARCHAR(50),
    montant_rembourse DECIMAL(10,2),
    stock_restitue BOOLEAN DEFAULT TRUE,
    date_annulation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (commande_id) REFERENCES commande(id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ========================================
-- 4. Vérifier les statuts en BD
-- ========================================

-- Afficher tous les statuts distincts
SELECT DISTINCT statut FROM commande;

-- Doit afficher: annulee, en_attente, livree, validee
-- Si vous voyez 'confirmee', exécuter:
UPDATE commande SET statut = 'validee' WHERE statut = 'confirmee';

-- ========================================
-- 5. Vérifier l'intégrité des clés étrangères
-- ========================================

-- Afficher les contraintes FK
SELECT CONSTRAINT_NAME, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'chroniccare'
AND CONSTRAINT_TYPE = 'FOREIGN KEY'
AND TABLE_NAME = 'annulation_commande';

-- ========================================
-- RÉSUMÉ: Colonnes vérifiées/créées
-- ========================================

-- Table 'commande':
--   ✓ motif_annulation (VARCHAR 255, NULL)

-- Table 'annulation_commande':
--   ✓ id (INT AUTO_INCREMENT PRIMARY KEY)
--   ✓ commande_id (INT NOT NULL, FK)
--   ✓ raison (VARCHAR 255)
--   ✓ utilisateur_id (INT NOT NULL, FK)
--   ✓ statut_avant_annulation (VARCHAR 50)
--   ✓ montant_rembourse (DECIMAL 10,2)
--   ✓ stock_restitue (BOOLEAN)
--   ✓ date_annulation (TIMESTAMP)

-- ========================================
-- FIN DU SCRIPT
-- ========================================

