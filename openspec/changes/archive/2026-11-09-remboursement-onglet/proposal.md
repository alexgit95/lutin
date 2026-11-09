## Why

Le suivi du remboursement (carte « À virer », alertes, filtre, pastilles) occupe le dashboard alors que celui-ci doit rester centré sur les cadeaux, leurs statuts et la barre de workflow. Le remboursement est une tâche à part, faite de temps en temps, qui mérite son propre écran.

## What Changes

- Un nouvel onglet **« Remboursement »**, réservé au propriétaire, regroupe tout le suivi : les alertes « virement en trop », la carte « À virer : X € » avec « Tout marquer remboursé » et la date du virement, les montants à rembourser et déjà remboursés, et la **liste des cadeaux remboursables** avec la pastille de cochage (« € à rembourser » / « remboursé le jj/mm »).
- La liste de l'onglet affiche les cadeaux remboursables, les non remboursés en premier, paginés. Un lecteur ne voit ni l'onglet ni aucune donnée de remboursement.
- Un **badge** sur l'onglet indique le nombre d'alertes « virement en trop » en attente, y compris juste après un changement de statut sur le dashboard.
- Le **dashboard** redevient celui d'avant : tous les cadeaux, leurs statuts modifiables, le filtre par tag et la barre de workflow en bas. Il ne porte plus ni carte, ni alertes, ni pastille, ni filtre « À rembourser ».
- Le **filtre « À rembourser » disparaît** : l'onglet liste déjà ces cadeaux. Le paramètre `aRembourser` est supprimé.
- Après « Tout marquer remboursé », le cochage d'une pastille ou la fermeture d'une alerte, l'utilisateur revient sur l'onglet Remboursement.
- Les règles métier ne changent pas : éligibilité, date du jour, prix verrouillé, alertes créées quand un cadeau remboursé cesse d'être éligible.

## Capabilities

### New Capabilities
<!-- Aucune -->

### Modified Capabilities
- `reimbursements`: le suivi se déplace du dashboard vers l'onglet Remboursement (pastille, carte, remboursement groupé, alertes, confidentialité) ; le filtre « À rembourser » est supprimé ; nouvelle exigence de l'onglet.
- `dashboard`: le dashboard ne contient aucune donnée de remboursement.

## Impact

- Code : `ListeController` et `RemboursementController` (nouvelle route `GET /listes/{id}/remboursements`, redirections vers l'onglet), `CadeauController` (réponse du cochage), `DashboardService` (retrait du filtre, des montants et des alertes de la vue), nouveau service ou vue pour l'onglet, `CadeauRepository` (requête paginée des cadeaux remboursables, suppression des requêtes de filtre), `VueListe` et `VueCadeau` (retrait des champs de remboursement), `LayoutAdvice` ou équivalent (badge).
- Gabarits : nouveau `remboursement.html`, `fragments.html` (onglet et badge), `liste.html` et `liste-fragments.html` (retrait de la carte, des alertes, du filtre et de la pastille), `app.css`.
- Données : aucune modification du schéma ni de `docs/database.puml`.
- Tests : adaptation de `RemboursementTest` et `RemboursementWebTest` (le filtre devient la liste de l'onglet), absence de toute donnée de remboursement sur le dashboard.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`.
