# 🎨 Flux Visual - Annulation de Commande (CORRIGÉ)

## Statuts en Base de Données

```
┌─────────────────────────────────────────────────────┐
│           STATUTS DISPONIBLES EN BD                 │
├─────────────────────────────────────────────────────┤
│  en_attente  →  validee  →  livree                 │
│                      ↓                              │
│                  annulee  (peut venir de n'importe │
│                           quel statut avant livree) │
└─────────────────────────────────────────────────────┘
```

## Flux d'Annulation Corrigé

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT ACTIONS                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│         1. AFFICHAGE DU BOUTON "ANNULER"                   │
│  (CommandesDashboardController - updateItem method)        │
│                                                              │
│  Statut = en_attente → Bouton VISIBLE + ACTIVÉ (100%)     │
│  Statut = validee    → Bouton VISIBLE + ACTIVÉ (100%)      │
│  Statut = annulee    → Bouton MASQUÉ ❌                     │
│  Statut = livree     → Bouton MASQUÉ ❌                      │
│                                                              │
│  💡 Normalisation: trim() + toLowerCase()                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
            Utilisateur clique "Annuler"?
                            ↓
            ┌──────────────┴──────────────┐
            ↓ OUI                      NON ↓
      CONFIRMATION POPUP          (Rien ne
      (voir détails ci-dessous)    se passe)
            │
            ↓
┌─────────────────────────────────────────────────────────────┐
│  2. POPUP DE CONFIRMATION                                   │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Annuler la commande?                                 │  │
│  │  ─────────────────────────────────────────────────── │  │
│  │  Commande n°: 12345                                  │  │
│  │  Montant: 125.99€                                    │  │
│  │  Statut actuel: en_attente                          │  │
│  │                                                       │  │
│  │  Après annulation:                                   │  │
│  │  • Le statut passera à 'annulée'                    │  │
│  │  • Vous serez remboursé automatiquement              │  │
│  │  • Le stock sera restitué                            │  │
│  │                                                       │  │
│  │  [Annuler cette action] [CONFIRMER]                │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
        ↙ N'annule pas        ↘ Confirme
    Ferme popup          Appelle service
        ↓                       ↓
    (Rien)        ┌─────────────────────────────────────────────────┐
                  │ 3. VÉRIFICATION MÉTIER (SERVICE)               │
                  │ (AnnulationCommandeService)                     │
                  │                                                 │
                  │ Vérifier statut = "en_attente" OU "validee"?  │
                  │                                                 │
                  ├──────────────┬──────────────┬──────────────┐  │
                  │ NON-Annulee  │ NON-Livree   │ OUI-Valide   │  │
                  ↓              ↓              ↓              │  │
         ❌ Erreur:     ❌ Erreur:      ✅ CONTINUE →  │
         "Déjà            "Déjà                      │  │
          annulée"        livrée"                    │  │
                                                     ↓  │
                  ┌─────────────────────────────────────────────┐  │
                  │ 4. EXÉCUTION DE L'ANNULATION              │  │
                  │                                             │  │
                  │ a) Restituer le stock                     │  │
                  │    FOR chaque ligne_commande:             │  │
                  │      UPDATE produit                        │  │
                  │      SET stock = stock + quantite         │  │
                  │                                             │  │
                  │ b) Mettre à jour statut                   │  │
                  │    UPDATE commande                         │  │
                  │    SET statut = 'annulee'                │  │
                  │    SET motif_annulation = 'Annulée par   │  │
                  │                            le client'    │  │
                  │                                             │  │
                  │ c) Enregistrer en audit                    │  │
                  │    INSERT INTO annulation_commande         │  │
                  │    (commande_id, utilisateur_id,          │  │
                  │     raison, statut_avant_annulation,      │  │
                  │     montant_rembourse, date_annulation)   │  │
                  │                                             │  │
                  └────────┬────────────────────────────────────┘  │
                           ↓                                       │
                  ✅ SUCCESS - Continue                          │
                  └─────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  5. MESSAGE DE SUCCÈS                                       │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ✅ Commande annulée                                  │  │
│  │  ─────────────────────────────────────────────────── │  │
│  │  La commande n°12345 a été annulée avec succès       │  │
│  │                                                       │  │
│  │  Remboursement: 125.99€                              │  │
│  │  Délai: 3-5 jours ouvrables                          │  │
│  │                                                       │  │
│  │  [OK]                                                 │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  6. RAFRAÎCHISSEMENT AUTO                                   │
│                                                              │
│  • Recharge la liste des commandes depuis BD               │
│  • La commande passe à statut "annulee"                   │
│  • Le bouton "Annuler" disparaît pour cette commande      │
│  • L'UI est à jour                                         │
└─────────────────────────────────────────────────────────────┘
```

## Cas d'Erreur - Flux Alternatif

```
┌──────────────────────────────────────────────────────────┐
│  CAS 1: Tentative double annulation                      │
│                                                           │
│  Commande = statut "annulee"                            │
│                                                           │
│  Bouton "Annuler" → MASQUÉ (invisible)                   │
│                                                           │
│  Si accès par edge case:                                │
│  → Service retourne: "Cette commande est déjà annulée."  │
│  → Message d'erreur clair affiché                        │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  CAS 2: Tentative annulation commande livrée             │
│                                                           │
│  Commande = statut "livree"                              │
│                                                           │
│  Bouton "Annuler" → MASQUÉ (invisible)                   │
│                                                           │
│  Si accès par edge case:                                │
│  → Service retourne: "Impossible d'annuler une          │
│                       commande déjà livrée."            │
│  → Message d'erreur clair affiché                        │
└──────────────────────────────────────────────────────────┘
```

## Comparaison: Avant vs Après

```
╔══════════════════════════════════════════════════════════════╗
║ AVANT (Bugué)                    │ APRÈS (Corrigé)          ║
╠══════════════════════════════════════════════════════════════╣
║ • Statut "confirmee" inexistant  │ • Statuts: en_attente,   ║
║   dans BD                        │   validee, annulee,      ║
║                                  │   livree (corrects)      ║
║                                  │                          ║
║ • Bouton visible pour statuts    │ • Bouton masqué pour     ║
║   non annulables                 │   statuts non annulables ║
║                                  │                          ║
║ • Message d'erreur générique:    │ • Messages contextualisés:║
║   "Impossible d'annuler une      │   "Cette commande est    ║
║   commande avec le statut:       │   déjà annulée."         ║
║   annulee"                       │   "Impossible d'annuler  ║
║                                  │   une commande déjà      ║
║                                  │   livrée."               ║
║                                  │                          ║
║ • Pas de normalisation du statut │ • Normalisation stricte: ║
║   (problèmes de casse/espaces)   │   trim() + toLowerCase() ║
║                                  │                          ║
║ • Vérification simple             │ • Vérification robuste:  ║
║                                  │   null check, normalisa- ║
║                                  │   tion, exceptions       ║
║                                  │   séparées               ║
╚══════════════════════════════════════════════════════════════╝
```

## Architecture Corrigée

```
┌─────────────────────────────────────────────────────────────┐
│                     USER INTERFACE                           │
│        (CommandesDashboardController)                        │
│                                                              │
│  • Affichage conditionnel du bouton (statut-aware)         │
│  • Normalisation du statut (pour UI safe)                  │
│  • Gestion des exceptions métier vs techniques              │
└──────────────────────┬──────────────────────────────────────┘
                       ↑
        Appel service.annulerCommande()
                       ↓
┌──────────────────────┴──────────────────────────────────────┐
│              BUSINESS LAYER SERVICE                          │
│        (AnnulationCommandeService)                          │
│                                                              │
│  • Vérification métier stricte (statuts valides)           │
│  • Restitution du stock                                     │
│  • Mise à jour du statut                                    │
│  • Enregistrement pour audit                                │
│  • Gestion cohérente des erreurs                            │
└──────────────────────┬──────────────────────────────────────┘
                       ↑
        Exécution requêtes SQL
                       ↓
┌──────────────────────┴──────────────────────────────────────┐
│              DATABASE LAYER                                  │
│                                                              │
│  • commande → statut="annulee"                             │
│  • annulation_commande → audit trail                        │
│  • produit → stock restitué                                 │
└─────────────────────────────────────────────────────────────┘
```

## Les 3 Couches de Défense

```
                DÉFENSE EN PROFONDEUR
    
Couche 1: INTERFACE
  ✓ Bouton masqué pour statuts non annulables
  ✓ Normalisation du statut
  ✓ Messages explicites en popup
    
Couche 2: SERVICE (Business Logic)
  ✓ Vérification stricte des statuts
  ✓ Normalisation du statut
  ✓ Gestion des cas remarquables
  ✓ Exceptions métier explicites
    
Couche 3: DATABASE (Intégrité Data)
  ✓ Contraintes FK
  ✓ Types de données
  ✓ Triggered actions
  
    → Même si une couche "fail", les autres
      attraperont et géreront l'erreur
```

---

**Résumé**: Le flux est maintenant **sécurisé, robuste et user-friendly** ✅

