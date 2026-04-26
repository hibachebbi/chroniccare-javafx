# Plan de Test - Fonction d'Annulation de Commande

## Objectif
Valider que les corrections apportées à la fonction d'annulation de commande fonctionnent correctement et que tous les cas métier sont couverts.

---

## 📋 Checklist de Test

### 1. Test d'affichage du bouton "Annuler"

#### Test 1.1: Affichage pour statut "en_attente"
- [ ] Se connecter en tant que client
- [ ] Aller dans "Mes commandes"  
- [ ] Localiser une commande avec le statut `en_attente`
- ✅ **Résultat attendu**: Le bouton "🟠 Annuler" doit être **VISIBLE et ACTIVÉ** (opacité 100%)
- 📸 Capturer screenshot si visible

#### Test 1.2: Affichage pour statut "validee"
- [ ] Aller dans "Mes commandes"
- [ ] Localiser une commande avec le statut `validee`
- ✅ **Résultat attendu**: Le bouton "🟠 Annuler" doit être **VISIBLE et ACTIVÉ** (opacité 100%)
- 📸 Capturer screenshot si visible

#### Test 1.3: Masquage pour statut "annulee"
- [ ] Aller dans "Mes commandes"
- [ ] Localiser une commande avec le statut `annulee`
- ✅ **Résultat attendu**: Le bouton "🟠 Annuler" doit être **MASQUÉ** (invisible)
- 📸 Capturer screenshot pour confirmation

#### Test 1.4: Masquage pour statut "livree"
- [ ] Aller dans "Mes commandes"
- [ ] Localiser une commande avec le statut `livree`
- ✅ **Résultat attendu**: Le bouton "🟠 Annuler" doit être **MASQUÉ** (invisible)
- 📸 Capturer screenshot pour confirmation

---

### 2. Test du processus d'annulation - Cas positif

#### Test 2.1: Annuler une commande "en_attente"
1. [ ] Cliquer sur le bouton "🟠 Annuler" d'une commande `en_attente`
2. [ ] Vérifier le popup de confirmation affiche:
   - [ ] Le numéro de commande
   - [ ] Le montant total
   - [ ] Le statut actuel (`en_attente`)
   - [ ] Description des conséquences (remboursement, restitution stock)
3. ✅ **Résultat attendu**: Popup clair et informatif
4. [ ] Cliquer OK pour confirmer l'annulation
5. [ ] Attendre la réponse du serveur (2-3 secondes)
6. [ ] Vérifier l'apparition du message de succès:
   - [ ] "Commande annulée avec succès"
   - [ ] Montant du remboursement
   - [ ] Délai estimé (3-5 jours)
7. ✅ **Résultat attendu**: Message positif et informatif
8. [ ] Fermer le message de succès
9. [ ] **Vérification BD** (via phpMyAdmin):
   - [ ] Statut de la commande = `annulee`
   - [ ] Entrée créée dans table `annulation_commande`
   - [ ] `commande_id` correct
   - [ ] `utilisateur_id` correct
   - [ ] `raison` = "Annulée par le client"
   - [ ] `date_annulation` récente
10. [ ] **Vérification stock** (via phpMyAdmin):
    - [ ] Stock de chaque produit a été augmenté de la quantité annulée

#### Test 2.2: Annuler une commande "validee"
- Répéter Test 2.1 avec une commande au statut `validee`
- ✅ **Résultats attendus identiques**

---

### 3. Test du processus d'annulation - Cas négatifs

#### Test 3.1: Tentative d'annulation d'une commande déjà "annulee"
1. [ ] Localiser une commande déjà annulée (tester plusieurs fois si nécessaire)
2. [ ] **Option A (UI)**: Si le bouton est masqué → Test réussi ✅
3. [ ] **Option B (Edge case)**: Si on trouve un moyen de cliquer :
   - [ ] Cliquer sur le bouton "🟠 Annuler"
   - [ ] ✅ **Message d'erreur attendu**: "Cette commande est déjà annulée.\nVous ne pouvez pas l'annuler à nouveau."
4. 📸 Capturer screenshot du message d'erreur

#### Test 3.2: Tentative d'annulation d'une commande "livree"
1. [ ] Localiser une commande livrée
2. [ ] Vérifier que le bouton est masqué (invisible)
3. [ ] **Option B (Edge case)**: Si on trouve un moyen de cliquer :
   - [ ] ✅ **Message d'erreur attendu**: "Impossible d'annuler une commande déjà livrée."
4. 📸 Capturer screenshot du message d'erreur

---

### 4. Test de l'interface et UX

#### Test 4.1: Vérifier les rafraîchissements
1. [ ] Après une annulation réussie, vérifier que:
   - [ ] La liste se rafraîchit automatiquement
   - [ ] La commande passe à statut "annulee"
   - [ ] Le bouton "Annuler" disparaît pour cette commande
   - [ ] Pas d'erreur de chargement

#### Test 4.2: Vérifier la pagination
1. [ ] Annuler une commande sur la page 1
2. [ ] Vérifier que la pagination reste cohérente
3. [ ] Vérifier que le compteur total se met à jour

#### Test 4.3: Vérifier le filtrage
1. [ ] Filter par statut "Tous"
2. [ ] Annuler une commande
3. [ ] Vérifier que le filtrage est toujours applicable
4. [ ] Changer le filtre à "annulee" et vérifier que la commande y apparaît

---

### 5. Test de performance et robustesse

#### Test 5.1: Double-click sur annulation
1. [ ] Cliquer rapidement 2 fois sur "🟠 Annuler"
2. [ ] ✅ **Résultat attendu**: Seulement 1 annulation doit se faire
   - (L'UI peut avoir un délai ou un verrou pour empêcher le double-clic)

#### Test 5.2: Fermer l'app sans confirmation
1. [ ] Cliquer sur "🟠 Annuler"
2. [ ] Popup de confirmation apparaît
3. [ ] Fermer l'app (Ctrl+W ou X) au lieu de confirmer
4. [ ] ✅ **Résultat attendu**: Rien ne doit être annulé

#### Test 5.3: Crash recovery
1. [ ] Lancer une annulation
2. [ ] Après le popup de confirmation, forcer fermer l'app
3. [ ] Redémarrer l'app et se reconnecter
4. [ ] ✅ **Vérifier en BD** que la commande a le bon statut

---

### 6. Test API/BD

#### Test 6.1: Vérifier la table annulation_commande
```sql
SELECT * FROM annulation_commande ORDER BY date_annulation DESC LIMIT 10;
```
- [ ] Vérifie que les annulations récentes y apparaissent
- [ ] Colonnes remplies correctement: `commande_id`, `utilisateur_id`, `raison`, `date_annulation`

#### Test 6.2: Vérifier la colonne motif_annulation
```sql
SELECT id, statut, motif_annulation FROM commande WHERE status = 'annulee' LIMIT 10;
```
- [ ] Colonnes `motif_annulation` doit contenir "Annulée par le client"

#### Test 6.3: Vérifier la restitution du stock
```sql
-- Pour chaque produit annulé:
SELECT id, stock FROM produit WHERE id IN (
    SELECT DISTINCT produit_id FROM ligne_commande 
    WHERE commande_id IN (SELECT commande_id FROM annulation_commande 
                          WHERE DATEDIFF(NOW(), date_annulation) < 1)
);
```
- [ ] Stock doit être correct (augmenté après annulation)

---

### 7. Messages d'erreur et validation

#### Test 7.1: Messages d'erreur attendus
- [ ] "Cette commande est déjà annulée." - Cas: double annulation
- [ ] "Impossible d'annuler une commande déjà livrée." - Cas: commande livrée
- [ ] "Cette commande ne peut pas être annulée (statut: X)." - Cas: statut inconnu
- [ ] "Vous devez être connecté pour annuler une commande" - Cas: session expirée

#### Test 7.2: Validation des champs
- [ ] La raison d'annulation est toujours définie: "Annulée par le client"
- [ ] Pas de raisons vides ou NULL

---

## 📊 Résultats Finaux

### Checklist finale

- [ ] **Test 1.1**: Affichage bouton statut `en_attente` ✅
- [ ] **Test 1.2**: Affichage bouton statut `validee` ✅
- [ ] **Test 1.3**: Masquage bouton statut `annulee` ✅
- [ ] **Test 1.4**: Masquage bouton statut `livree` ✅
- [ ] **Test 2.1**: Annulation `en_attente` réussie ✅
- [ ] **Test 2.2**: Annulation `validee` réussie ✅
- [ ] **Test 3.1**: Erreur pour double annulation ✅
- [ ] **Test 3.2**: Erreur pour annulation `livree` ✅
- [ ] **Test 4.1-4.3**: Interface et pagination ✅
- [ ] **Test 5.1-5.3**: Performance et robustesse ✅
- [ ] **Test 6.1-6.3**: Vérification BD ✅
- [ ] **Test 7.1-7.2**: Messages d'erreur ✅

---

## 🎯 Criteria d'acceptation

✅ **ACCEPTÉ si**:
1. Tous les tests 1.x et 2.x passent
2. Les messages d'erreur sont clairs et contextualisés
3. La BD est cohérente après chaque annulation
4. Le stock est correctement restitué
5. Pas de crash ou exception non gérée
6. L'UI reste fluide et responsive

❌ **REJETER si**:
1. Bouton visible pour statuts non annulables
2. Erreur SQL ou exception non gérée
3. Stock pas restitué
4. Message d'erreur vague ou générique
5. Double-click provoque 2 annulations

---

## 📝 Notes

- Chaque test doit être validé avec une commande différente si possible
- Toujours vérifier la BD après chaque action critiquement
- Les screenshots doivent être datées avec l'heure exacte
- Documenter tout bug trouvé avec stacktrace si applicable

---

**Date de création**: 2026-04-23  
**Version**: 1.0  
**Status**: Prêt pour test 🟢

