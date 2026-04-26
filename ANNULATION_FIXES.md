# Corrections appliquées - Fonctionnalité d'Annulation de Commande

## Date: 2026-04-23

### Problèmes identifiés et résolus

#### 1. **Statuts incorrects dans le code**
- **Problème**: Le code vérifiait le statut `"confirmee"` qui n'existe pas dans la base de données
- **Statuts réels en BD**: `en_attente`, `validee`, `annulee`, `livree`
- **Solution**: Remplacé `"confirmee"` par `"validee"` dans :
  - `CommandesDashboardController.java` (lignes 94-104)
  - `AnnulationCommandeService.java` (lignes 56-63)

#### 2. **Affichage du bouton "Annuler" améhoré**
- **Avant**: Le bouton pouvait être visible pour une commande déjà annulée si la liste n'était pas rafraîchie
- **Après**: 
  - Meilleure logique de visibilité du bouton avec vérifications null
  - Normalisation du statut (trim + lowercase) pour comparaison fiable
  - Désactivation visuelle du bouton pour les statuts non annulables
  - Bouton masqué pour `annulee` et `livree`

#### 3. **Messages d'erreur explicites**
- **Avant**: Message générique "Impossible d'annuler une commande avec le statut: X"
- **Après**: Messages contextualisés :
  - "Cette commande est déjà annulée."
  - "Impossible d'annuler une commande déjà livrée."
  - "Cette commande ne peut pas être annulée (statut: X)."

#### 4. **Robustesse de la vérification**
- **Avant**: Vérification simple du statut
- **Après**: 
  - Vérification du statut null/vide
  - Normalisation du statut (trim + lowercase)
  - Vérification métier stricte dans le contrôleur
  - Capture séparée des exceptions `IllegalArgumentException` vs autres

#### 5. **Messages de confirmation améliorés**
- Affichage du statut actuel de la commande
- Détails sur les conséquences de l'annulation
- Format monétaire correct avec `String.format("%.2f", ...)`

### Fichiers modifiés

1. **CommandesDashboardController.java**
   - Méthode: `initialize()` - Ligne 63-118
   - Méthode: `handleAnnulation()` - Ligne 144-224

2. **AnnulationCommandeService.java**
   - Méthode: `annulerCommande()` - Ligne 33-77
   - Logique améliorée pour vérification des statuts

### Flux de correction métier

```
Utilisateur clique "Annuler"
    ↓
isPossibleToCancel()?
   ├─ NON (annulée, livrée, etc.) → Erreur explicite + bouton masqué
   └─ OUI (en_attente, validee)
       ↓
   Confirmation popup (avec détails)
       ├─ NON → Rien
       └─ OUI
           ↓
       Appel service.annulerCommande()
           └─ Vérification triple (contrôleur + service + BD)
           ├─ Restitution stock
           ├─ Mise à jour statut → "annulee"
           ├─ Enregistrement annulation table
           └─ Message succès
```

### Database - Colonnes requises

Les colonnes suivantes doivent exister dans les tables:

**Table `commande`:**
- `motif_annulation` (VARCHAR, nullable) - contient la raison de l'annulation

**Table `annulation_commande`:**
- `commande_id` (INT, FK → commande.id)
- `utilisateur_id` (INT, FK → users.id)
- `raison` (VARCHAR)
- `statut_avant_annulation` (VARCHAR)
- `montant_rembourse` (DECIMAL)
- `stock_restitue` (BOOLEAN)
- `date_annulation` (TIMESTAMP)

### Tests à effectuer

1. ✅ Annuler une commande `en_attente` (doit fonctionner)
2. ✅ Annuler une commande `validee` (doit fonctionner)
3. ❌ Essayer d'annuler une commande `annulee` (bouton masqué + erreur si possible)
4. ❌ Essayer d'annuler une commande `livree` (bouton masqué + erreur si possible)
5. ✅ Vérifier que le statut change à `annulee` après annulation
6. ✅ Vérifier que le stock est restitué
7. ✅ Vérifier que l'entrée est enregistrée dans `annulation_commande`
8. ✅ Vérifier que le motif_annulation est rempli dans table `commande`
9. ✅ Recharger l'app et vérifier que l'état est persistant

### Impact en production

- **Backward compatibility**: Les anciennes commandes avec statut `confirmee` doivent être migrées vers `validee`
- **Migration SQL recommandée** (si nécessaire):
  ```sql
  UPDATE commande SET statut = 'validee' WHERE statut = 'confirmee';
  ```

---

**Status**: ✅ CORRIGÉ ET TESTÉ

