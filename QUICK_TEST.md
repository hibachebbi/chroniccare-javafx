# 🟢 TEST RAPIDE - 5 MINUTES

## Ce fichier: Guide ultime de test en 5 minutes

Pas de complications. Juste les étapes essentielles.

---

## ⚡ ÉTAPE 1: Compiler (30 sec)

```bash
cd C:\Users\rayen\dev_desktop\chroniccare-javafx
mvn clean compile
```

**Résultat attendu**: `BUILD SUCCESS`

---

## ⚡ ÉTAPE 2: Lancer l'app (30 sec)

```bash
mvn javafx:run
# Ou: Cliquer Play dans IntelliJ
```

**Résultat attendu**: App se lance sans crash

---

## ⚡ ÉTAPE 3: Tester le scénario basique (3 min)

### Cas 1: Bouton visible pour "en_attente"
1. Login en tant que client
2. Aller dans "Mes commandes"
3. Chercher une commande avec statut **en_attente**
4. ✅ Vérifier: Bouton "🟠 Annuler" est **VISIBLE**

### Cas 2: Bouton visible pour "validee"
1. Chercher une commande avec statut **validee**
2. ✅ Vérifier: Bouton "🟠 Annuler" est **VISIBLE**

### Cas 3: Bouton masqué pour "annulee"
1. Chercher une commande avec statut **annulee**
2. ✅ Vérifier: Bouton "🟠 Annuler" est **MASQUÉ** (invisible)

### Cas 4: Bouton masqué pour "livree"
1. Chercher une commande avec statut **livree**
2. ✅ Vérifier: Bouton "🟠 Annuler" est **MASQUÉ** (invisible)

---

## ⚡ ÉTAPE 4: Annuler une commande (1 min 30 sec)

1. Cliquer sur "🟠 Annuler" d'une commande **en_attente**
2. Popup de confirmation s'affiche
3. ✅ Vérifier: Popup affiche:
   - Numéro de commande
   - Montant total
   - Statut actuel
   - Détails du remboursement
4. Cliquer "CONFIRMER"
5. ✅ Message de succès s'affiche:
   - "Commande annulée avec succès"
   - Montant du remboursement
   - Délai (3-5 jours)

---

## ✅ RÉSULTATS FINAUX

- [ ] **Étape 1**: BUILD SUCCESS
- [ ] **Étape 2**: App se lance
- [ ] **Étape 3.1**: Bouton visible `en_attente` ✅
- [ ] **Étape 3.2**: Bouton visible `validee` ✅
- [ ] **Étape 3.3**: Bouton masqué `annulee` ✅
- [ ] **Étape 3.4**: Bouton masqué `livree` ✅
- [ ] **Étape 4**: Annulation réussie ✅

---

## 🎉 Si TOUS les ✅ passent

**STATUS**: ✅ PRODUCTION-READY

Vous pouvez:
- ✅ Mettre en production
- ✅ Commencer la prochaine feature (Wishlist)
- ✅ Célébrer 🎊

---

## ❌ Si SOMETHING FAILS

1. Noter exactement ce qui ne fonctionne pas
2. Lire **VALIDATION_GUIDE.md** (section "Support")
3. Vérifier la BD avec phpMyAdmin

---

## ⏱️ Temps Total: 5-10 minutes

Pas plus. C'est fait si tout passe.

---

**Date**: 2026-04-23  
**Urgence**: - TEST MAINTENANT -  
**Difficulté**: Aucune

