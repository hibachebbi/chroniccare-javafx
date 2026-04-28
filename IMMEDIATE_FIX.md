# ⚡ ACTION IMMÉDIATE - Unknown Column Fix

## ❌ PROBLEM
```
lors de l'annulation de commande:
Unknown column 'motif_annulation' in 'field list'
```

## ✅ SOLUTION IMMÉDIATE (5 MIN)

### FAIRE MAINTENANT:

1. **Ouvrir phpMyAdmin**: http://localhost/phpmyadmin
2. **Sélectionner**: Base `chroniccare` → Onglet SQL
3. **Copier-coller cette commande**:

```sql
ALTER TABLE commande ADD COLUMN motif_annulation VARCHAR(255) NULL AFTER statut;
```

4. **Clique**: Bouton "Exécuter"
5. **Voir**: "Requête exécutée avec succès" ✅

---

### PUIS TESTER:

1. Fermer et redémarrer l'app: `mvn javafx:run`
2. Login client → Mes commandes
3. Cliquer "Annuler" sur `en_attente`
4. **Doit fonctionner maintenant!** ✅

---

##  SI BESOIN DE PLUS DE DÉTAILS

Lire: **QUICK_FIX_5MIN.md** (guide complet)
Ou: **FIX_DATABASE_COLUMNS.md** (avec troubleshooting)

---

## ⏱️ TEMPS: 5 minutes max

C'est tout! Allez-y! 
