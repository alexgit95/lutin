## ADDED Requirements

### Requirement: Indicateurs budgétaires
Le dashboard d'une liste SHALL afficher le budget global (somme des budgets des personnes), le budget déboursé (cadeaux hors tiers au statut « Commandé » ou plus, avec prix) et le reste à payer (cadeaux hors tiers au statut « À acheter », avec prix).

#### Scenario: Calcul
- **WHEN** une liste contient un cadeau à 20 € « Commandé », un à 30 € « À acheter » et un à 50 € pour compte de tiers « Commandé »
- **THEN** le déboursé est 20 € et le reste à payer est 30 €

#### Scenario: Prix absent
- **WHEN** un cadeau n'a pas de prix
- **THEN** il n'est compté dans aucun montant

### Requirement: Vue des statuts
Le dashboard SHALL afficher tous les cadeaux avec leur statut et une répartition par statut.

#### Scenario: Affichage
- **WHEN** l'utilisateur ouvre le dashboard
- **THEN** chaque cadeau est listé avec son destinataire, son prix et son statut

### Requirement: Changement de statut depuis le dashboard
Le dashboard SHALL permettre au propriétaire de changer le statut d'un cadeau sans quitter la page.

#### Scenario: Mise à jour en place
- **WHEN** le propriétaire choisit un nouveau statut sur une ligne
- **THEN** le statut est enregistré et les indicateurs sont recalculés

### Requirement: Interface adaptée au mobile
L'interface SHALL s'inspirer de la maquette (cartes d'indicateurs colorées, navigation, pastilles de statut) et rester utilisable sur écran de téléphone.

#### Scenario: Écran étroit
- **WHEN** le dashboard est affiché sur un écran de téléphone
- **THEN** la navigation et les cartes restent lisibles sans défilement horizontal
