## ADDED Requirements

### Requirement: Cadeaux remboursables
Le système SHALL permettre au propriétaire de marquer comme remboursé un cadeau qui a quitté son compte courant, c'est-à-dire dont le statut est Commandé ou plus, qui a un prix, et qui n'est pas pour un tiers. Le remboursement SHALL enregistrer la date du jour. Marquer comme remboursé un cadeau non éligible SHALL être refusé.

#### Scenario: Cadeau éligible
- **WHEN** le propriétaire coche le remboursement d'un cadeau Commandé avec un prix
- **THEN** le cadeau est marqué remboursé avec la date du jour

#### Scenario: Cadeau À acheter
- **WHEN** le propriétaire tente de cocher un cadeau au statut « À acheter »
- **THEN** la demande est refusée et le cadeau n'est pas modifié

#### Scenario: Cadeau pour un tiers ou sans prix
- **WHEN** le propriétaire tente de cocher un cadeau pour un tiers ou sans prix
- **THEN** la demande est refusée

#### Scenario: Décochage
- **WHEN** le propriétaire décoche un cadeau remboursé
- **THEN** sa date de remboursement est effacée et le cadeau redevient à rembourser

#### Scenario: Date du jour
- **WHEN** un cadeau est coché
- **THEN** la date enregistrée est celle du jour dans le fuseau horaire configuré, par défaut Europe/Paris

### Requirement: Pastille de remboursement
Le dashboard SHALL afficher au propriétaire, dans la ligne de chaque cadeau éligible, une pastille de remboursement : grise « à rembourser » si la date est nulle, verte « remboursé le jj/mm » sinon. Cliquer la pastille SHALL basculer le remboursement et mettre à jour la ligne et la carte « À rembourser » sans recharger la liste. Un cadeau non éligible SHALL ne pas porter de pastille.

#### Scenario: Pastille à rembourser
- **WHEN** le propriétaire consulte un cadeau Commandé non remboursé
- **THEN** la ligne affiche une pastille grise « à rembourser »

#### Scenario: Pastille remboursé
- **WHEN** le propriétaire consulte un cadeau remboursé le 3 décembre
- **THEN** la ligne affiche une pastille verte « remboursé le 03/12 »

#### Scenario: Mise à jour sans rechargement
- **WHEN** le propriétaire clique la pastille
- **THEN** la ligne et la carte « À rembourser » sont mises à jour sans recharger la liste ni perdre le défilement

#### Scenario: Cadeau non éligible
- **WHEN** le propriétaire consulte un cadeau À acheter, pour un tiers ou sans prix
- **THEN** la ligne n'affiche aucune pastille de remboursement

### Requirement: Carte À rembourser
Le dashboard SHALL afficher au propriétaire une carte « À rembourser » indiquant la somme des prix des cadeaux éligibles non remboursés et celle des cadeaux déjà remboursés. La somme des deux SHALL égaler le « Déjà déboursé » du dashboard.

#### Scenario: Montants
- **WHEN** le propriétaire a un cadeau Commandé de 89 € remboursé et un cadeau Livré de 15 € non remboursé
- **THEN** la carte indique 15 € à rembourser et 89 € déjà remboursés

#### Scenario: Cohérence avec le déboursé
- **WHEN** le propriétaire consulte le dashboard
- **THEN** le montant à rembourser plus le montant déjà remboursé est égal au « Déjà déboursé »

### Requirement: Tout marquer remboursé
Le système SHALL permettre au propriétaire de marquer d'un coup remboursés tous les cadeaux éligibles non remboursés. La carte SHALL afficher d'abord le montant « À virer » et une date préremplie à la date du jour. La date SHALL ne pas pouvoir être dans le futur. Un compte rendu SHALL indiquer le nombre de cadeaux marqués et leur somme.

#### Scenario: Remboursement groupé
- **WHEN** le propriétaire valide « Tout marquer remboursé » avec la date du jour
- **THEN** tous les cadeaux éligibles non remboursés portent cette date et un message indique le nombre et la somme

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
Lorsqu'un cadeau remboursé cesse d'être éligible, le système SHALL effacer sa date de remboursement et créer une alerte « virement en trop » indiquant le nom du cadeau et le montant. Cela concerne le retour au statut « À acheter », la suppression du cadeau, son passage en « pour un tiers » et la suppression de sa personne. L'alerte SHALL rester affichée sur le dashboard du propriétaire jusqu'à ce qu'il la ferme, et SHALL être supprimée avec la liste. Le dashboard SHALL afficher les 20 alertes les plus récentes et leur nombre total. Aucune alerte ne SHALL être créée pour un cadeau non remboursé.

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
- **WHEN** le propriétaire revient plus tard sur le dashboard
- **THEN** l'alerte est toujours affichée tant qu'il ne l'a pas fermée

#### Scenario: Fermeture
- **WHEN** le propriétaire ferme une alerte
- **THEN** elle est supprimée définitivement

#### Scenario: Cadeau non remboursé
- **WHEN** un cadeau non remboursé repasse à « À acheter » ou est supprimé
- **THEN** aucune alerte n'est créée

#### Scenario: Alerte en HTMX
- **WHEN** le propriétaire change le statut d'un cadeau remboursé vers « À acheter » depuis le dashboard
- **THEN** l'alerte apparaît sans recharger la page

### Requirement: Prix d'un cadeau remboursé
Le système SHALL refuser la modification du prix d'un cadeau remboursé et SHALL afficher le champ prix en lecture seule dans le formulaire, avec une indication de décocher d'abord le remboursement. Les autres champs SHALL rester modifiables sans effacer la date de remboursement.

#### Scenario: Champ en lecture seule
- **WHEN** le propriétaire ouvre le formulaire d'un cadeau remboursé
- **THEN** le champ prix est en lecture seule avec une indication

#### Scenario: Changement de prix refusé
- **WHEN** une requête modifie le prix d'un cadeau remboursé
- **THEN** elle est refusée avec un message d'erreur et le prix reste inchangé

#### Scenario: Autres modifications
- **WHEN** le propriétaire modifie le nom d'un cadeau remboursé
- **THEN** le nom est modifié et la date de remboursement est conservée

### Requirement: Filtre À rembourser
Le dashboard SHALL proposer au propriétaire un filtre « À rembourser » qui ne liste que les cadeaux éligibles non remboursés, combinable avec un filtre par tag, et reporté par le chargement continu. Le filtre SHALL être ignoré pour un lecteur.

#### Scenario: Filtre
- **WHEN** le propriétaire active le filtre « À rembourser »
- **THEN** seuls les cadeaux éligibles non remboursés sont affichés

#### Scenario: Combiné avec un tag
- **WHEN** le propriétaire active le filtre et un tag
- **THEN** seuls les cadeaux éligibles non remboursés portant ce tag sont affichés

#### Scenario: Chargement continu
- **WHEN** le filtre est actif et que la liste compte plusieurs lots
- **THEN** les lots suivants respectent le filtre

#### Scenario: Lecteur
- **WHEN** un lecteur ajoute le paramètre du filtre à l'adresse
- **THEN** la liste n'est pas filtrée

### Requirement: Remboursement réservé au propriétaire
Le système SHALL réserver au propriétaire les données et actions de remboursement : pastille, date, carte « À rembourser », remboursement groupé, alertes, filtre. Elles SHALL ne figurer ni dans le HTML ni dans les réponses partielles destinés à un lecteur. Les routes correspondantes SHALL refuser un lecteur ou tout autre utilisateur (403).

#### Scenario: Lecteur sans données de remboursement
- **WHEN** un lecteur consulte la liste partagée
- **THEN** aucune pastille, date, carte ni alerte de remboursement ne figure dans la réponse, y compris les lots de chargement continu

#### Scenario: Actions refusées
- **WHEN** un lecteur ou un autre utilisateur tente de cocher un cadeau, de marquer tout remboursé ou de fermer une alerte
- **THEN** la requête est refusée (403) et rien n'est modifié
