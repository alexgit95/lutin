## MODIFIED Requirements

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

## ADDED Requirements

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