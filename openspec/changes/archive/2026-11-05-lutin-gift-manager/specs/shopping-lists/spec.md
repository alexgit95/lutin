## ADDED Requirements

### Requirement: Listes d'achat
Le système SHALL permettre à un utilisateur de créer, renommer et supprimer ses listes d'achat ; chaque liste appartient à un seul utilisateur.

#### Scenario: Création
- **WHEN** l'utilisateur crée une liste avec un nom
- **THEN** la liste apparaît dans ses listes

#### Scenario: Isolation
- **WHEN** un utilisateur demande une liste dont il n'est ni propriétaire ni lecteur
- **THEN** l'accès est refusé

### Requirement: Personnes destinataires et budget
Le système SHALL permettre de créer, modifier et supprimer, dans une liste, des personnes destinataires avec un budget représentant la somme que l'utilisateur souhaite dépenser pour elles.

#### Scenario: Création avec budget
- **WHEN** l'utilisateur crée une personne avec un nom et un budget
- **THEN** la personne est ajoutée à la liste avec ce budget

#### Scenario: Suppression d'une personne ayant des cadeaux
- **WHEN** l'utilisateur supprime une personne qui a des cadeaux
- **THEN** le système demande une confirmation puis supprime la personne et ses cadeaux

### Requirement: Onglet Personnes
La création, la modification (nom, budget) et la suppression des personnes SHALL se faire dans un onglet dédié de la liste, et non sur le dashboard ; cet onglet est réservé au propriétaire.

#### Scenario: Création depuis l'onglet
- **WHEN** le propriétaire remplit le formulaire de l'onglet Personnes
- **THEN** la personne est créée avec son budget et l'onglet est mis à jour

#### Scenario: Dashboard sans gestion des personnes
- **WHEN** l'utilisateur ouvre le dashboard
- **THEN** aucun formulaire ni liste de personnes n'y figure, seul le budget global est affiché

#### Scenario: Onglet interdit à un lecteur
- **WHEN** un lecteur d'un partage ouvre l'onglet Personnes
- **THEN** l'accès est refusé (403)
