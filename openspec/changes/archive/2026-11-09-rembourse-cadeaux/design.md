## Context

Le dashboard calcule déjà « Déjà déboursé » (`CadeauRepository.sommeDebourse`) : cadeaux au statut Commandé ou plus, avec prix, hors cadeaux pour un tiers. Le changement de statut passe par HTMX (`CadeauController.changerStatut`, fragment `miseAJourStatut` avec la barre de workflow en `hx-swap-oob`). Les lecteurs d'une liste partagée reçoivent des objets de vue réduits (`VueCadeau.depuis`, `VueListe`) et ne doivent rien apprendre des finances du propriétaire, d'autant que les totaux incluent les cadeaux masqués. Le schéma est généré par Hibernate (`ddl-auto=update`), sans migration, sur des bases SQLite et PostgreSQL existantes.

## Goals / Non-Goals

**Goals:**
- Savoir ce qui reste à virer du compte épargne et à quelle date chaque cadeau a été remboursé.
- Cocher un cadeau ou tout d'un coup, sans état incohérent.
- Ne jamais perdre la trace d'un virement devenu inutile.

**Non-Goals:**
- Registre de virements avec montants libres.
- Corriger la date d'un remboursement après coup (on décoche puis on recoche).
- Suivi global entre plusieurs listes.
- Visibilité de toute donnée de remboursement pour un lecteur.

## Decisions

### Une date sur le cadeau
`cadeau.rembourse_le` (`LocalDate`, facultative) : nulle = non remboursé. Facultative, donc l'ajout fonctionne sur les tables existantes. La date n'est jamais modifiée par l'édition du cadeau (`CadeauService.appliquer` n'y touche pas).

### Éligibilité
Un seul prédicat, partagé par toutes les requêtes : `pourTiers = false and prix is not null and statut <> A_ACHETER`. C'est la définition de `sommeDebourse`, ce qui garantit que « déjà remboursé plus à rembourser » égale le « Déjà déboursé » du dashboard. Cocher un cadeau non éligible est refusé par le serveur (`RequeteInvalideException`).

### Sommes et carte
`CadeauRepository` gagne `sommeARembourser(listeId)` (éligibles, `rembourse_le` nul) et `sommeRembourse(listeId)` (éligibles, date renseignée). `VueListe` porte ces deux montants pour le propriétaire et des valeurs nulles pour un lecteur ; le gabarit ne rend la carte que si `!vue.lecteur()`.

### Cochage individuel
`POST /listes/{id}/cadeaux/{cid}/rembourse` bascule le remboursement : date du jour si non remboursé (si éligible), effacement sinon. La date du jour est calculée avec le `Clock` et le fuseau configuré `lutin.fuseau-horaire` (défaut `Europe/Paris`, lu par le composant `DateDuJour` via `@Value`), car l'horloge de l'application est en UTC et la date peut sinon différer en soirée. En HTMX la réponse remplace la ligne et, en `hx-swap-oob`, la carte ; sans HTMX, redirection vers le dashboard.

### Tout marquer remboursé
`POST /listes/{id}/remboursements` avec une date (aujourd'hui par défaut, jamais dans le futur) : mise à jour groupée limitée aux cadeaux encore éligibles et non remboursés. Le montant « À virer » affiché dans la carte est `sommeARembourser` ; le compte rendu (nombre de cadeaux et somme) est calculé dans la même transaction, juste avant la mise à jour, pour rester exact en cas de modification concurrente. Message de succès `info` après redirection.

### Sortie du déboursé et alerte persistante
Table `alerte_remboursement(id, liste_id, cadeau_nom, montant, cree_le)` avec clé étrangère vers la liste (supprimée avec elle). Un `AlerteRemboursementService` crée une alerte quand un cadeau remboursé cesse d'être éligible, puis la date est effacée ou le cadeau supprimé :
- retour au statut « À acheter » (`changerStatut`),
- suppression du cadeau (`supprimer`),
- passage en « pour un tiers » (`modifier`),
- suppression d'une personne (`PersonneService.supprimer`), dont les cadeaux remboursés sont relevés avant la suppression groupée.
Le prix d'un cadeau remboursé étant verrouillé, sa suppression par édition ne se produit pas.
Les alertes restent affichées sur le dashboard du propriétaire jusqu'à ce qu'il les ferme (`POST /listes/{id}/alertes/{aid}/fermer`), sans expiration. L'affichage montre les 20 plus récentes et le nombre total, pour ne pas charger une collection sans borne. En HTMX, la réponse d'un changement de statut renvoie aussi le bloc d'alertes en `hx-swap-oob`.
Alternative écartée : un simple message éphémère, trop facile à rater pour un virement déjà effectué.

### Verrou du prix
`CadeauService.modifier` refuse un prix différent (comparaison `compareTo`) sur un cadeau remboursé avec un message invitant à décocher d'abord. Le formulaire du cadeau affiche le champ en lecture seule dans ce cas.

### Filtre « À rembourser »
Paramètre `aRembourser=true` de `GET /listes/{id}` et du chargement continu `GET /listes/{id}/cadeaux` (le paramètre est reporté dans les liens de lot). Requêtes pour le propriétaire seulement, avec et sans tag. Pour un lecteur, `DashboardService` ignore le paramètre : filtrer révélerait quels cadeaux sont remboursés. La pastille de filtre n'est rendue que pour le propriétaire.

### Confidentialité
`VueCadeau` d'un lecteur ne reçoit aucun champ de remboursement ; `VueListe` d'un lecteur ne porte ni montants à rembourser ni alertes. Les routes de remboursement, de remboursement groupé et d'alertes exigent le propriétaire (403 sinon).

## Risks / Trade-offs

- [Alertes qui s'accumulent si elles ne sont jamais fermées] → affichage limité aux 20 plus récentes avec le total, suppression avec la liste.
- [Statut passé par erreur à « À acheter » puis rétabli] → la date de remboursement a été effacée ; le cadeau redevient à rembourser et l'alerte rappelle le montant déjà viré.
- [Date du jour décalée par le fuseau] → fuseau configurable, par défaut Europe/Paris.
- [Fuite d'information vers un lecteur par le filtre ou le HTML] → paramètre ignoré côté serveur, objets de vue sans ces champs, tests d'absence dans le HTML.
- [Colonne ajoutée sur une base existante] → colonne facultative ; démarrage vérifié sur une copie de la base de dev SQLite.
