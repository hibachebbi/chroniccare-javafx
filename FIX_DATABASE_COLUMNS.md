#  FIX - Colonnes Manquantes pour Annulation de Commande

## Problème
Lors de l'annulation, vous recevez une erreur: `Unknown column 'motif_annulation'`

## Cause
La colonne `motif_annulation` manque dans la table `commande`.

---

## ✅ SOLUTION - 3 Étapes Simples

### Étape 1: Ouvrir phpMyAdmin

1. Allez à: http://localhost/phpmyadmin
2. Sélectionnez la base `chroniccare`
3. Allez dans l'onglet **SQL**

### Étape 2: Exécuter la correction

Copie-colle chacun de ces blocs SQL dans phpMyAdmin et clique "Exécuter" :

#### Bloc 1: Vérifier la structure actuelle

```sql
SHOW COLUMNS FROM commande;
```

**Résultat attendu**: Vous verrez toutes les colonnes. Cherchez `motif_annulation`:
- ✅ Si elle apparaît → Passer à Bloc 3
- ❌ Si elle n'apparaît pas → Exécuter Bloc 2

#### Bloc 2: Ajouter la colonne manquante

```sql
ALTER TABLE commande ADD COLUMN motif_annulation VARCHAR(255) NULL AFTER statut;
```

**Résultat attendu**: `Query successful` ou `0 rows affected`

#### Bloc 3: Vérifier la table annulation_commande

```sql
SHOW CREATE TABLE annulation_commande;
```

**Résultat attendu**: Doit afficher la DDL complète avec les colonnes:
- commande_id
- raison
- utilisateur_id
- statut_avant_annulation
- montant_rembourse
- stock_restitue
- date_annulation

Si la table n'existe pas ou manque des colonnes, exécuter:

```sql
DROP TABLE IF EXISTS annulation_commande;

CREATE TABLE annulation_commande (
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
```

#### Bloc 4: Vérifier les statuts

```sql
SELECT DISTINCT statut FROM commande;
```

**Résultat attendu**: Doit afficher SEULEMENT: `annulee`, `en_attente`, `livree`, `validee`

Si vous voyez `confirmee`, exécuter:

```sql
UPDATE commande SET statut = 'validee' WHERE statut = 'confirmee';
```

---

### Étape 3: Redémarrer l'application

1. Fermer l'application ChronicCare
2. Relancer avec: `mvn javafx:run`
3. Tester l'annulation à nouveau

---

##  Vérifier que c'est corrigé

Après les modifications BD, tester:

1. Se connecter en tant que client
2. Aller dans "Mes commandes"
3. Cliquer "Annuler" sur une commande `en_attente`
4. Confirmer

**✅ Résultat attendu**: 
- Message de succès "Commande annulée avec succès!"
- Pas d'erreur "Unknown column"
- Vérifier en BD que l'annulation a été enregistrée

---

##  Si ça ne fonctionne toujours pas

### Vérification 1: Colonnes dans `commande`

```sql
DESCRIBE commande;
```

Cherchez `motif_annulation` - doit afficher:
```
Field: motif_annulation
Type: varchar(255)
Null: YES
```

### Vérification 2: Colonnes dans `annulation_commande`

```sql
DESCRIBE annulation_commande;
```

Doit afficher 8 colonnes:
1. id
2. commande_id
3. raison
4. utilisateur_id
5. statut_avant_annulation
6. montant_rembourse
7. stock_restitue
8. date_annulation

### Vérification 3: Contraintes FK

```sql
SELECT CONSTRAINT_NAME, TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'chroniccare'
AND TABLE_NAME = 'annulation_commande';
```

Doit afficher 2 FKs:
- fk_annulation_commande (ou similar)
- fk_annulation_user (ou similar)

### Vérification 4: Données existantes

```sql
SELECT * FROM annulation_commande LIMIT 5;
```

Si vide = normal. Si erreur = problème de structure.

---

##  Checklist Finale

- [ ] Colonne `motif_annulation` existe dans `commande`
- [ ] Table `annulation_commande` existe
- [ ] 8 colonnes correctes dans `annulation_commande`
- [ ] 2 clés étrangères en place
- [ ] Statuts: seulement `en_attente`, `validee`, `annulee`, `livree`
- [ ] Application redémarrée
- [ ] Test d'annulation réussit ✅

---

##  Fichiers SQL Utiles

Un script SQL complet est disponible à:
```
C:\Users\rayen\dev_desktop\chroniccare-javafx\sql\fix_annulation_columns.sql
```

Vous pouvez l'exécuter entièrement dans phpMyAdmin.

---

**Status après corrections**:  PRÊT POUR TEST

Tester maintenant avec QUICK_TEST.md
