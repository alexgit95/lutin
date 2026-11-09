## ADDED Requirements

### Requirement: Onglet Remboursement
Le système SHALL proposer au propriétaire d'une liste un onglet « Remboursement » qui regroupe tout le suivi : alertes « virement en trop », carte « À rembourser » avec remboursement groupé, et liste paginée des cadeaux remboursables avec leur pastille. La liste SHALL contenir les cadeaux éligibles (statut Commandé ou plus, avec un prix, pas pour un tiers), les non remboursés en premier, puis les remboursés. Un badge sur l'onglet SHALL indiquer le nombre d'alertes en attente quand il est supérieur à zéro. L'onglet et son badge SHALL ne pas exister pour un lecteur, et la route SHALL refuser un lecteur ou tout autre utilisateur (403).

#### Scenario: Contenu de l'onglet
- **WHEN** le propriétaire ouvre l'onglet Remboursement
- **THEN** la page affiche les alertes, la carte « À rembourser » et la liste des cadeaux remboursables

#### Scenario: Ordre de la liste
- **WHEN** la liste contient des cadeaux remboursés et non remboursés
- **THEN** les cadeaux non remboursés apparaissent avant les remboursés

#### Scenario: Cadeaux non éligibles absents
- **WHEN** la liste contient un cadeau À acheter, pour un tiers ou sans prix
- **THEN** il n'apparaît pas dans l'onglet

#### Scenario: Pagination
- **WHEN** plus de 50 cadeaux sont remboursables
- **THEN** la liste est paginée avec « Précédent » et « Suivant »

#### Scenario: Badge d'alertes
- **WHEN** deux alertes sont en attente
- **THEN** l'onglet « Remboursement » porte un badge « 2 » sur les pages de la liste

#### Scenario: Pas de badge
- **WHEN** aucune alerte n'est en attente
- **THEN** l'onglet ne porte pas de badge

#### Scenario: Onglet interdit à un lecteur
- **WHEN** un lecteur ou un autre utilisateur ouvre la route de l'onglet
- **THEN** la requête est refusée (403)

#### Scenario: Onglet absent pour un lecteur
- **WHEN** un lecteur consulte une liste partagée
- **THEN** aucun onglet « Remboursement » ni badge n'est affiché

## MODIFIED Requirements

### Requirement: Pastille de remboursement
L'onglet Remboursement SHALL afficher au propriétaire, dans la ligne de chaque cadeau remboursable, une pastille de remboursement : grise « à rembourser » si la date est nulle, verte « remboursé le jj/mm » sinon. Cliquer la pastille SHALL basculer le remboursement et mettre à jour la ligne et la carte « À rembourser » sans recharger la page. Le dashboard SHALL ne porter aucune pastille.

#### Scenario: Pastille à rembourser
- **WHEN** le propriétaire consulte dans l'onglet un cadeau Commandé non remboursé
- **THEN** la ligne affiche une pastille grise « à rembourser »

#### Scenario: Pastille remboursé
- **WHEN** le propriétaire consulte dans l'onglet un cadeau remboursé le 3 décembre
- **THEN** la ligne affiche une pastille verte « remboursé le 03/12 »

#### Scenario: Mise à jour sans rechargement
- **WHEN** le propriétaire clique la pastille dans l'onglet
- **THEN** la ligne et la carte « À rembourser » sont mises à jour sans recharger la page

#### Scenario: Pas de pastille sur le dashboard
- **WHEN** le propriétaire consulte le dashboard
- **THEN** aucune ligne de cadeau ne porte de pastille de remboursement

### Requirement: Carte À rembourser
L'onglet Remboursement SHALL afficher au propriétaire une carte « À rembourser » indiquant la somme des prix des cadeaux éligibles non remboursés et celle des cadeaux déjà remboursés. La somme des deux SHALL égaler le « Déjà déboursé » du dashboard.

#### Scenario: Montants
- **WHEN** le propriétaire a un cadeau Commandé de 89 € remboursé et un cadeau Livré de 15 € non remboursé
- **THEN** la carte indique 15 € à rembourser et 89 € déjà remboursés

#### Scenario: Cohérence avec le déboursé
- **WHEN** le propriétaire consulte l'onglet Remboursement
- **THEN** le montant à rembourser plus le montant déjà remboursé est égal au « Déjà déboursé » du dashboard

### Requirement: Tout marquer remboursé
Le système SHALL permettre au propriétaire de marquer d'un coup remboursés tous les cadeaux éligibles non remboursés. La carte de l'onglet SHALL afficher d'abord le montant « À virer » et une date préremplie à la date du jour. La date SHALL ne pas pouvoir être dans le futur. Un compte rendu SHALL indiquer le nombre de cadeaux marqués et leur somme, et l'utilisateur SHALL revenir sur l'onglet.

#### Scenario: Remboursement groupé
- **WHEN** le propriétaire valide « Tout marquer remboursé » avec la date du jour
- **THEN** tous les cadeaux éligibles non remboursés portent cette date, un message indique le nombre et la somme et l'onglet est réaffiché

#### Scenario: Montant à virer affiché
- **WHEN** le propriétaire consulte la carte avec 150 € à rembourser
- **THEN** « À virer : 150,00 € » est affiché avant validation

#### Scenario: Date future
- **WHEN** le propriétaire saisit une date dans le futur
- **THEN** la demande est refusée avec un message d'erreur et rien n'est modifié

#### Scenario: Rien à rembourser
- **WHEN** aucun cadeau éligible n'est à rembourser
- **THEN** le bouton est inactif et aucun cadeau n'est modifié

#### Scenario: Cadeaux déjà remboursés
- **WHEN** des cadeaux sont déjà remboursés avant le remboursement groupé
- **THEN** leur date d'origine est conservée

### Requirement: Alerte de virement en trop
Lorsqu'un cadeau remboursé cesse d'être éligible, le système SHALL effacer sa date de remboursement et créer une alerte « virement en trop » indiquant le nom du cadeau et le montant. Cela concerne le retour au statut « À acheter », la suppression du cadeau, son passage en « pour un tiers » et la suppression de sa personne. L'alerte SHALL rester affichée dans l'onglet Remboursement du propriétaire jusqu'à ce qu'il la ferme, et SHALL être supprimée avec la liste. L'onglet SHALL afficher les 20 alertes les plus récentes et leur nombre total. Aucune alerte ne SHALL être créée pour un cadeau non remboursé. Le dashboard SHALL ne pas afficher les alertes.

#### Scenario: Retour à À acheter
- **WHEN** un cadeau remboursé repasse au statut « À acheter »
- **THEN** sa date de remboursement est effacée et une alerte indique le cadeau et le montant viré en trop

#### Scenario: Suppression d'un cadeau remboursé
- **WHEN** le propriétaire supprime un cadeau remboursé
- **THEN** une alerte indique le cadeau et le montant viré en trop

#### Scenario: Passage pour un tiers
- **WHEN** un cadeau remboursé est marqué « pour un tiers »
- **THEN** sa date est effacée et une alerte est créée

#### Scenario: Suppression d'une personne
- **WHEN** le propriétaire supprime une personne dont des cadeaux sont remboursés
- **THEN** une alerte est créée pour chacun de ces cadeaux

#### Scenario: Persistance
- **WHEN** le propriétaire revient plus tard dans l'onglet Remboursement
- **THEN** l'alerte est toujours affichée tant qu'il ne l'a pas fermée

#### Scenario: Fermeture
- **WHEN** le propriétaire ferme une alerte
- **THEN** elle est supprimée définitivement et l'onglet est réaffiché

#### Scenario: Cadeau non remboursé
- **WHEN** un cadeau non remboursé repasse à « À acheter » ou est supprimé
- **THEN** aucune alerte n'est créée

#### Scenario: Badge mis à jour depuis le dashboard
- **WHEN** le propriétaire change depuis le dashboard le statut d'un cadeau remboursé vers « À acheter »
- **THEN** le badge de l'onglet « Remboursement » est mis à jour sans recharger la page

#### Scenario: Pas d'alerte sur le dashboard
- **WHEN** une alerte est en attente et que le propriétaire consulte le dashboard
- **THEN** le dashboard n'affiche aucune alerte

### Requirement: Remboursement réservé au propriétaire
Le système SHALL réserver au propriétaire les données et actions de remboursement : onglet, pastille, date, carte « À rembourser », remboursement groupé, alertes, badge. Elles SHALL ne figurer ni dans le HTML ni dans les réponses partielles destinés à un lecteur. Les routes correspondantes SHALL refuser un lecteur ou tout autre utilisateur (403).

#### Scenario: Lecteur sans données de remboursement
- **WHEN** un lecteur consulte la liste partagée
- **THEN** aucun onglet, pastille, date, carte, badge ni alerte de remboursement ne figure dans la réponse

#### Scenario: Actions refusées
- **WHEN** un lecteur ou un autre utilisateur tente de consulter l'onglet, de cocher un cadeau, de marquer tout remboursé ou de fermer une alerte
- **THEN** la requête est refusée (403) et rien n'est modifié

## REMOVED Requirements

### Requirement: Filtre À rembourser
**Reason**: L'onglet Remboursement liste déjà les cadeaux éligibles non remboursés en premier, ce qui rend le filtre du dashboard inutile.
**Migration**: Ouvrir l'onglet Remboursement ; le paramètre `aRembourser` de la liste et du chargement continu est supprimé.
