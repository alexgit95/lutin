## 1. Service

- [x] 1.1 `PersonneService.apercuImport` : droits propriétaire sur source et cible, source différente de la cible, personnes de la source avec drapeau « déjà présente » (comparaison `trim` + minuscules) et total du budget importable
- [x] 1.2 `PersonneService.importer` : relecture en base des personnes cochées appartenant à la source, doublons recalculés à l'import, création des copies dans une transaction, compte rendu (importées, ignorées, budget total)
- [x] 1.3 Tests de service : copie du nom et du budget, cadeaux non copiés, copie indépendante, règle des doublons (casse, espaces), personne ajoutée entre aperçu et import, identifiant forgé, source ou cible non possédée (y compris liste partagée), source égale à la cible, source vide, tout déjà présent, homonymes dans la source

## 2. Web

- [x] 2.1 `PersonneController` : `GET /listes/{id}/personnes/import` (aperçu) et `POST` (import) avec message flash du compte rendu ; erreurs de validation renvoyées à l'onglet
- [x] 2.2 `personnes.html` : panneau « Importer depuis une autre liste » (menu des autres listes avec nombre de personnes), absent sans autre liste
- [x] 2.3 Gabarit d'aperçu : cases cochées par défaut, personne déjà présente grisée et inactive, total du budget, boutons Importer (désactivé si rien d'importable) et Annuler
- [x] 2.4 Tests web : panneau présent ou absent, aperçu, import et message flash, 403 pour un lecteur, une liste partagée comme source et un utilisateur étranger, CSRF

## 3. Livraison

- [x] 3.1 Mettre à jour `README.md` (import de personnes, règle des doublons, listes concernées)
- [x] 3.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog
- [x] 3.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
