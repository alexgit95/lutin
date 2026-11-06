## 1. Vue et requêtes du lecteur

- [x] 1.1 `VueCadeau.depuis` : pour un lecteur, un cadeau masqué ne contient plus aucun champ (nom compris)
- [x] 1.2 `CadeauRepository` : requêtes pour lecteur (sans et avec tag), tri conditionnel (cadeaux visibles puis masqués par identifiant), filtre par tag excluant les masqués
- [x] 1.3 `DashboardService` : utiliser les requêtes du lecteur dans `construire` et `chargerLot`, le propriétaire gardant ses requêtes actuelles

## 2. Gabarit

- [x] 2.1 `liste-fragments.html` : ligne masquée d'un lecteur affichant « Cadeau masqué » seul, titre du groupe neutre « Cadeaux masqués », sans `data-statut`

## 3. Tests

- [x] 3.1 Adapter les tests existants qui attendaient le nom d'un cadeau masqué (`ListesWebTest`, `ParcoursListesTest`)
- [x] 3.2 Tests : ligne « Cadeau masqué » sans nom ni autre donnée dans le HTML (page et lot de chargement)
- [x] 3.3 Tests : section masquée unique en dernière position, y compris avec plusieurs personnes et plusieurs lots
- [x] 3.4 Tests : ordre des lignes masquées indépendant du nom du cadeau et de la personne
- [x] 3.5 Tests : filtre par tag d'un lecteur sans cadeau masqué, filtre du propriétaire inchangé, totaux inchangés

## 4. Livraison

- [x] 4.1 Mettre à jour `README.md` (masquage total, groupe unique, tags, limite du cadeau masqué unique)
- [x] 4.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog
- [x] 4.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
