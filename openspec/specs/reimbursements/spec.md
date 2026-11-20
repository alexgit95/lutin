# Reimbursements

## Purpose
Track the transfer that reimburses gifts paid from the current account out of the savings account, without altering the spending indicators.

## Requirements

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

### Requirement: Remboursement réservé au propriétaire
Le système SHALL réserver au propriétaire les données et actions de remboursement : onglet, pastille, date, carte « À rembourser », remboursement groupé, alertes, badge. Elles SHALL ne figurer ni dans le HTML ni dans les réponses partielles destinés à un lecteur. Les routes correspondantes SHALL refuser un lecteur ou tout autre utilisateur (403).

#### Scenario: Lecteur sans données de remboursement
- **WHEN** un lecteur consulte la liste partagée
- **THEN** aucun onglet, pastille, date, carte, badge ni alerte de remboursement ne figure dans la réponse

#### Scenario: Actions refusées
- **WHEN** un lecteur ou un autre utilisateur tente de consulter l'onglet, de cocher un cadeau, de marquer tout remboursé ou de fermer une alerte
- **THEN** la requête est refusée (403) et rien n'est modifié

### Requirement: Onglet Remboursement
Le système SHALL proposer au propriétaire d'une liste un onglet « Remboursement » avec deux sous-vues : « Épargne » et « Tiers ». La sous-vue « Épargne », affichée par défaut, SHALL conserver les alertes « virement en trop », la carte « À rembourser » avec remboursement groupé et la liste paginée des cadeaux remboursables avec leur pastille. Cette liste SHALL contenir les cadeaux éligibles au virement depuis l'épargne (statut Commandé ou plus, avec un prix, pas pour un tiers), les non remboursés en premier, puis les remboursés. Un badge sur l'onglet SHALL indiquer le nombre d'alertes en attente quand il est supérieur à zéro. L'onglet et son badge SHALL ne pas exister pour un lecteur, et les routes SHALL refuser un lecteur ou tout autre utilisateur (403).

#### Scenario: Contenu de l'onglet
- **WHEN** le propriétaire ouvre l'onglet Remboursement
- **THEN** la page affiche les sous-vues « Épargne » et « Tiers » et ouvre « Épargne » avec les alertes, la carte « À rembourser » et la liste des cadeaux remboursables

#### Scenario: Ordre de la liste
- **WHEN** la liste contient des cadeaux remboursés et non remboursés
- **THEN** les cadeaux non remboursés apparaissent avant les remboursés dans la sous-vue « Épargne »

#### Scenario: Cadeaux non éligibles absents
- **WHEN** la liste contient un cadeau À acheter, pour un tiers ou sans prix
- **THEN** il n'apparaît pas dans la sous-vue « Épargne »

#### Scenario: Pagination
- **WHEN** plus de 50 cadeaux sont remboursables depuis l'épargne
- **THEN** la liste de la sous-vue « Épargne » est paginée avec « Précédent » et « Suivant »

#### Scenario: Badge d'alertes
- **WHEN** deux alertes sont en attente
- **THEN** l'onglet « Remboursement » porte un badge « 2 » sur les pages de la liste

#### Scenario: Pas de badge
- **WHEN** aucune alerte n'est en attente
- **THEN** l'onglet ne porte pas de badge

#### Scenario: Onglet interdit à un lecteur
- **WHEN** un lecteur ou un autre utilisateur ouvre la route de l'onglet ou de sa sous-vue « Tiers »
- **THEN** la requête est refusée (403)

#### Scenario: Onglet absent pour un lecteur
- **WHEN** un lecteur consulte une liste partagée
- **THEN** aucun onglet « Remboursement » ni badge n'est affiché

### Requirement: Remboursements reçus des tiers
Le système SHALL lister dans la sous-vue « Tiers » les cadeaux avec tiers identifié, prix renseigné et statut « Commandé » ou plus. Il SHALL afficher le reste à recevoir par tiers, calculé sur l'ensemble de ses cadeaux éligibles non réglés, indépendamment de la pagination du détail. Les achats historiques sans tiers identifié SHALL être accessibles dans « Tiers à préciser », hors des totaux et sans action de règlement. Les données de tiers et de règlement SHALL être absentes des réponses HTML et des fragments des lecteurs. Les actions SHALL être réservées au propriétaire et refuser tout cadeau hors de la liste ou inéligible au règlement.

#### Scenario: Reste à recevoir par tiers
- **WHEN** Marc doit 35 € pour un livre et 20 € pour un jeu, et que seul le livre est réglé
- **THEN** le reste à recevoir de Marc est 20 € et le détail paginé permet de retrouver les deux cadeaux, avec le jeu avant le livre

#### Scenario: Achat pas encore éligible
- **WHEN** un cadeau attribué à un tiers est « À acheter » ou n'a pas de prix
- **THEN** il n'entre pas dans les totaux ni dans le détail des avances et ne peut pas être marqué réglé

#### Scenario: Tiers historique non identifié
- **WHEN** un cadeau historique pour compte de tiers n'a pas de personne attribuée
- **THEN** il apparaît dans « Tiers à préciser » et ne contribue à aucun total par personne

#### Scenario: Confidentialité du suivi des tiers
- **WHEN** un lecteur ouvre la liste partagée ou tente une action de règlement
- **THEN** aucun nom de tiers ni état ou montant propre au suivi des tiers ne figure dans sa vue ou ses fragments, et l'action est refusée (403)

### Requirement: Règlement intégral par cadeau
Le propriétaire SHALL pouvoir marquer un cadeau éligible comme intégralement remboursé par le tiers ou annuler ce marquage. Le montant SHALL être le prix complet du cadeau, sans paiement partiel ni saisie de montant, et la date SHALL être celle du jour dans le fuseau configuré. Le règlement tiers SHALL être indépendant de la date, des totaux et des alertes d'épargne. Tant que le cadeau est réglé, le système SHALL refuser la modification du prix ou de l'attribution, toute perte d'éligibilité et toute suppression directe ou indirecte du cadeau au sein de la liste, avec une indication d'annuler d'abord le marquage.

#### Scenario: Encaisser un règlement
- **WHEN** le propriétaire marque le livre de 35 € comme remboursé par Marc
- **THEN** la date est enregistrée, le détail et le reste à recevoir sont actualisés sans rechargement complet et aucun autre cadeau ni virement d'épargne n'est modifié

#### Scenario: Annuler la saisie
- **WHEN** le propriétaire annule le règlement du livre
- **THEN** la date est retirée et les 35 € reviennent dans le reste à recevoir de Marc

#### Scenario: Protéger un cadeau déjà réglé
- **WHEN** le propriétaire tente de changer le prix ou le tiers d'un cadeau réglé, de le rendre inéligible ou de le supprimer, y compris via la suppression de son destinataire
- **THEN** l'opération est refusée sans perte de l'attribution ou de la date de règlement ; le propriétaire est invité à annuler le marquage avant de poursuivre

