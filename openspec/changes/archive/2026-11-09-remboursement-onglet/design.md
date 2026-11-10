## Context

Le changement `rembourse-cadeaux` a placé tout le suivi sur le dashboard : carte et alertes (`liste-fragments.html` et `liste.html`), pastille dans chaque ligne (`ligneCadeau`), filtre `aRembourser` (`DashboardService`, `ListeController`, `CadeauRepository`), et des blocs `hx-swap-oob` renvoyés après un changement de statut ou un cochage. `VueListe` porte `aRembourser`, `rembourse`, `aujourdhui`, `filtreARembourser`, `alertes`, `totalAlertes` ; `VueCadeau` porte `remboursable` et `rembourseLe`. Les onglets d'une liste sont le fragment `onglets(listeId, actif, lecteur)` (Dashboard, Personnes, Partage, Réglages), le propriétaire seul voyant les trois derniers. `POST /listes/{id}/remboursements` existe déjà (remboursement groupé).

## Goals / Non-Goals

**Goals:**
- Un onglet « Remboursement » pour le propriétaire qui contient tout le suivi.
- Un dashboard sans aucune trace de remboursement, qui garde cadeaux, statuts, filtre par tag et barre de workflow.
- Ne pas perdre de vue une alerte créée depuis le dashboard.

**Non-Goals:**
- Aucun changement de règle métier, de schéma de données, de droits ni de fuseau.
- Pas de nouveau filtre ni de recherche dans l'onglet.
- Pas d'historique des virements.

## Decisions

### Route et contrôleur
`GET /listes/{id}/remboursements` rend l'onglet (propriétaire seulement, 403 sinon). Le `POST` du même chemin reste le remboursement groupé. Les redirections du remboursement groupé, du cochage sans HTMX et de la fermeture d'une alerte pointent vers l'onglet. Le paramètre `page` pagine la liste.

### Contenu de l'onglet
Une vue dédiée porte l'identité de la liste, les deux montants, la date du jour, les 20 alertes les plus récentes avec le total, et une page de cadeaux. La liste affiche les cadeaux remboursables (`pourTiers = false`, prix renseigné, statut différent de « À acheter »), **les non remboursés d'abord**, puis les remboursés, puis par personne, nom et identifiant, par pages de 50 avec « Précédent » et « Suivant ». Le tri s'exprime en JPQL portable SQLite et PostgreSQL (`case when c.rembourseLe is null then 0 else 1 end`). Chaque ligne montre nom, destinataire, prix, statut en lecture seule et la pastille.
Alternative écartée : chargement continu comme le dashboard, inutile pour une liste de cadeaux déjà filtrée.

### Mises à jour par HTMX dans l'onglet
Le cochage d'une pastille renvoie la ligne remplacée et, en `hx-swap-oob`, la carte (montants à virer et remboursés) ; la ligne reste dans la page, elle ne change pas de place avant rechargement. Le fragment de réponse est propre à l'onglet.

### Dashboard allégé
Disparaissent du dashboard : carte, bloc d'alertes, filtre et pastille, le paramètre `aRembourser`, les sommes de remboursement, `aujourdhui` et la liste des alertes de `VueListe`, les champs `remboursable` et `rembourseLe` de `VueCadeau`, et les requêtes de filtre du dépôt (la requête des cadeaux remboursables de l'onglet les remplace). Le changement de statut ne met plus à jour que la ligne et la barre de workflow, plus un badge (voir ci-dessous). Les sommes `sommeARembourser` et `sommeRembourse` restent pour l'onglet.

### Badge d'alertes
Le nombre d'alertes en attente s'affiche en badge sur l'onglet « Remboursement » pour le propriétaire (comme le badge des avis sur « Wishlists »). Il est calculé pour les pages affichées en GET du propriétaire (hors HTMX, hors actions) à partir de l'identifiant de liste de l'adresse, par un comptage indexé ; il vaut 0 pour un lecteur. Le changement de statut du dashboard renvoie aussi le badge en `hx-swap-oob`, pour qu'une alerte créée par un retour à « À acheter » soit visible tout de suite. Aucune alerte ne s'affiche sur le dashboard.

### Confidentialité
Inchangée : la vue de l'onglet n'est jamais construite pour un lecteur, qui reçoit 403 ; le lien de l'onglet et le badge ne sont pas rendus pour lui ; plus aucune donnée de remboursement ne figure dans les vues du dashboard, ce qui renforce la garantie.

## Risks / Trade-offs

- [Alerte non vue après un retour à « À acheter »] → badge visible sur l'onglet, mis à jour dans la réponse HTMX du changement de statut.
- [Perte de la pastille sur le dashboard] → décision assumée : le cochage se fait dans l'onglet où se trouve aussi le montant à virer.
- [Tests existants liés au filtre et à la carte du dashboard] → adaptés : le filtre devient la liste de l'onglet, et un test vérifie l'absence de toute donnée de remboursement sur le dashboard.
- [Comptage d'alertes à chaque page de liste] → requête de comptage indexée sur la liste, limitée aux GET du propriétaire.
- [Deux tris différents (dashboard et onglet)] → requêtes distinctes, sans effet sur le dashboard.
