## 1. Modèle de données

- [x] 1.1 `Cadeau.rembourseLe` (`LocalDate`, facultative) et entité `AlerteRemboursement` (liste, nom du cadeau, montant, date de création), sans colonne `integer` avant l'identifiant
- [x] 1.2 Repositories : requêtes d'éligibilité (`pourTiers = false`, prix renseigné, statut différent de « À acheter »), `sommeARembourser`, `sommeRembourse`, mise à jour groupée limitée aux cadeaux éligibles non remboursés, listes filtrées « à rembourser » avec et sans tag, alertes d'une liste (20 plus récentes et total), suppression des alertes d'une liste
- [x] 1.3 `ListeAchatService.supprimer` : supprimer aussi les alertes de la liste ; propriété `lutin.fuseau-horaire` (défaut `Europe/Paris`) lue par le composant `DateDuJour`
- [x] 1.4 Mettre à jour `docs/database.puml`
- [x] 1.5 Vérifier le démarrage sur une copie de la base de dev SQLite existante (colonne et table ajoutées)

## 2. Services

- [x] 2.1 `CadeauService` : basculer le remboursement (date du jour dans le fuseau configuré, refus d'un cadeau non éligible), propriétaire seulement
- [x] 2.2 `CadeauService` : « Tout marquer remboursé » (date non future, mise à jour groupée, compte rendu exact calculé avant la mise à jour)
- [x] 2.3 `AlerteRemboursementService` : créer et lister les alertes (20 plus récentes et total), fermer une alerte, propriétaire seulement
- [x] 2.4 Sorties du déboursé d'un cadeau remboursé : retour à « À acheter », suppression du cadeau, passage « pour un tiers » (effacement de la date et alerte) ; suppression d'une personne dans `PersonneService` (relever les cadeaux remboursés avant la suppression groupée)
- [x] 2.5 Verrou du prix d'un cadeau remboursé dans `CadeauService.modifier`, sans toucher à la date lors des autres modifications
- [x] 2.6 `DashboardService` : montants à rembourser et remboursés, alertes et filtre « à rembourser » pour le propriétaire ; paramètre ignoré et champs absents pour un lecteur
- [x] 2.7 Tests de service : éligibilité, date du jour, bascule, remboursement groupé et date future, cohérence avec le déboursé, toutes les sorties du déboursé avec alerte, absence d'alerte pour un cadeau non remboursé, verrou du prix, filtre avec et sans tag, droits (lecteur, étranger)

## 3. Web

- [x] 3.1 Contrôleurs : `POST /listes/{id}/cadeaux/{cid}/rembourse` (HTMX ou redirection), `POST /listes/{id}/remboursements`, `POST /listes/{id}/alertes/{aid}/fermer`, paramètre `aRembourser` sur la liste et le chargement continu
- [x] 3.2 `liste-fragments.html` : pastille de remboursement dans la ligne ; réponse HTMX du changement de statut et du cochage avec la carte et le bloc d'alertes en `hx-swap-oob`
- [x] 3.3 `liste.html` : carte « À rembourser » avec « À virer : X € », date préremplie et bouton inactif si rien à virer, bloc d'alertes avec bouton Fermer, pastille de filtre « À rembourser », tout cela propriétaire seulement
- [x] 3.4 `cadeau-form.html` : champ prix en lecture seule avec indication pour un cadeau remboursé
- [x] 3.5 `app.css` : pastilles, carte, alertes
- [x] 3.6 Tests web : pastille, carte, remboursement groupé et message, filtre et chargement continu, alertes persistantes et fermeture, 403 pour lecteur et étranger, absence de toute donnée de remboursement dans le HTML d'un lecteur (page et lots), filtre ignoré pour un lecteur, champ prix en lecture seule

## 4. Livraison

- [x] 4.1 Mettre à jour `README.md` (remboursement, carte, regroupé, filtre, alertes, fuseau)
- [x] 4.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog
- [x] 4.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
