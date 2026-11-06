## 1. Données et service

- [x] 1.1 `CadeauRepository` : requête agrégée par personne renvoyant dépensé et reste à payer, avec les règles du dashboard (hors tiers, prix renseigné, « À acheter » séparé)
- [x] 1.2 `PersonneService.lister` : droit propriétaire, objet de vue `VuePersonneBudget` (budget, dépensé, reste à payer) avec total prévu, échelle de la barre, pourcentages, repère du budget, dépassement et cas budget nul ; personnes sans cadeau complétées à zéro
- [x] 1.3 Tests de service : dépensé et reste à payer, cadeau pour tiers, sans prix et masqué, personne sans cadeau, budget à 0, dépassement, égalité des sommes avec `sommeDebourse` et `sommeResteAPayer` du dashboard, accès refusé à un non-propriétaire

## 2. Web

- [x] 2.1 `PersonneController.lister` : fournir les `VuePersonneBudget` au gabarit
- [x] 2.2 `personnes.html` : sous-menu `<details>` par personne (« + Cadeau », « Modifier », « Supprimer » avec confirmation), barre de budget avec `role="img"` et `aria-label`, texte des montants, mention de dépassement
- [x] 2.3 `app.css` : menu, barre (teinte pleine, teinte claire, repère du budget, rouge en dépassement) ; `app.js` : fermeture du sous-menu au clic ailleurs, à l'ouverture d'un autre et sur Échap
- [x] 2.4 Tests web : actions présentes uniquement dans le sous-menu, barre et montants, description accessible, cas budget nul et dépassement ; adapter le test existant de l'onglet Personnes

## 3. Livraison

- [x] 3.1 Mettre à jour `README.md` (onglet Personnes : sous-menu, barre dépensé / à payer / budget, règles)
- [x] 3.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog
- [x] 3.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
