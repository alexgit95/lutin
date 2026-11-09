## 1. Données et services

- [x] 1.1 `CadeauRepository` : requête paginée des cadeaux remboursables d'une liste (non remboursés d'abord, puis par personne, nom et identifiant, en JPQL portable) avec chargement de la personne ; suppression de `findARembourser` et `findARembourserAvecTag` ; `sommeARembourser` et `sommeRembourse` conservées
- [x] 1.2 Vue et service de l'onglet : montants, date du jour, 20 alertes récentes avec le total, page de cadeaux avec pastille et statut en lecture seule ; propriétaire seulement (403 sinon)
- [x] 1.3 Comptage des alertes d'une liste pour le badge (réutiliser `countByListeId`), 0 pour un non-propriétaire
- [x] 1.4 Dashboard : retirer le filtre, les montants, `aujourdhui`, la liste d'alertes et `filtreARembourser` de `DashboardService`, `VueListe` et `LotCadeaux`, ainsi que `remboursable` et `rembourseLe` de `VueCadeau` ; le nombre d'alertes pour le badge reste disponible

## 2. Routes

- [x] 2.1 `GET /listes/{id}/remboursements` (propriétaire) avec le paramètre `page`
- [x] 2.2 Redirections vers l'onglet : remboursement groupé, fermeture d'une alerte, cochage sans HTMX
- [x] 2.3 Cochage `POST /listes/{id}/cadeaux/{cid}/rembourse` : réponse HTMX propre à l'onglet (ligne et carte en `hx-swap-oob`)
- [x] 2.4 `ListeController` et chargement continu : supprimer le paramètre `aRembourser` (ignoré s'il est fourni)
- [x] 2.5 Réponse du changement de statut du dashboard : ligne, barre de workflow et badge en `hx-swap-oob`, sans carte ni alertes

## 3. Interface

- [x] 3.1 Nouveau gabarit `remboursement.html` : alertes, carte « À virer » avec date et bouton, liste des cadeaux remboursables avec pastille, pagination, messages
- [x] 3.2 `fragments.html` : onglet « Remboursement » pour le propriétaire dans `onglets(...)`, avec badge conditionnel ; fragment du badge pour la mise à jour en `hx-swap-oob`
- [x] 3.3 `liste.html` et `liste-fragments.html` : retirer carte, alertes, filtre et pastille ; garder cadeaux, statuts, filtre par tag et barre de workflow
- [x] 3.4 Badge sur les pages de la liste : attribut de modèle calculé pour les GET du propriétaire (hors HTMX), selon l'identifiant de liste de l'adresse
- [x] 3.5 `app.css` : styles de la liste de l'onglet et du badge ; retrait des styles devenus inutiles

## 4. Tests

- [x] 4.1 Service : liste de l'onglet (éligibles seulement, non remboursés d'abord, pagination), montants, alertes, droits (lecteur et étranger)
- [x] 4.2 Web propriétaire : onglet visible avec contenu, badge selon le nombre d'alertes, cochage HTMX (ligne et carte), remboursement groupé et fermeture d'alerte avec retour sur l'onglet
- [x] 4.3 Web dashboard : aucune carte, alerte, pastille ni filtre, paramètre `aRembourser` ignoré, changement de statut avec badge mis à jour
- [x] 4.4 Web lecteur : aucun onglet ni badge, route en 403 pour lecteur et étranger
- [x] 4.5 Adapter ou supprimer les tests existants du filtre, de la carte et de la pastille sur le dashboard
- [x] 4.6 Vérifier que le reste de la suite passe

## 5. Livraison

- [x] 5.1 Mettre à jour `README.md` (onglet Remboursement, dashboard sans suivi, filtre supprimé)
- [x] 5.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog (aucune variable d'environnement ni donnée touchée, indiquer la suppression du paramètre `aRembourser`)
- [x] 5.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
