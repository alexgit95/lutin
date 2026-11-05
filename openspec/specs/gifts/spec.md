# Gifts

## Purpose
Define gift lifecycle, URL-based title suggestions, third-party purchases, and visibility controls.

## Requirements

### Requirement: Gestion des cadeaux
Le système SHALL permettre de créer, modifier et supprimer un cadeau d'une liste avec : nom (obligatoire), URL (facultative), prix (facultatif), personne destinataire, indicateur masqué et indicateur pour compte de tiers.

#### Scenario: Création minimale
- **WHEN** l'utilisateur crée un cadeau avec un nom et un destinataire
- **THEN** le cadeau est créé au statut « À acheter » sans prix

#### Scenario: Nom manquant
- **WHEN** l'utilisateur soumet un cadeau sans nom et sans URL exploitable
- **THEN** la création est refusée

#### Scenario: Prix renseigné plus tard
- **WHEN** l'utilisateur modifie un cadeau pour ajouter son prix
- **THEN** le prix est enregistré

### Requirement: Nom déduit de l'URL
Le système SHALL proposer comme nom le titre de la page de l'URL indiquée lorsque le nom est vide.

#### Scenario: Titre récupéré
- **WHEN** l'utilisateur indique une URL publique valide sans nom
- **THEN** le nom du cadeau est le titre de la page

#### Scenario: URL interdite
- **WHEN** l'URL cible un schéma autre que http/https ou une adresse privée, loopback ou link-local
- **THEN** aucune requête n'est émise et le nom doit être saisi manuellement

#### Scenario: Récupération impossible
- **WHEN** la page est injoignable, trop lente ou trop volumineuse
- **THEN** le système demande de saisir le nom manuellement

### Requirement: Workflow de statut
Le système SHALL gérer le workflow « À acheter », « Commandé », « Livré », « Emballé », « Remis » et permettre de changer le statut d'un cadeau.

#### Scenario: Changement de statut
- **WHEN** le propriétaire change le statut d'un cadeau
- **THEN** le nouveau statut est enregistré

### Requirement: Cadeau pour compte de tiers
Le système SHALL permettre de marquer un cadeau comme acheté pour le compte d'un tiers ; il suit le workflow mais est exclu des budgets et totaux.

#### Scenario: Exclusion des totaux
- **WHEN** un cadeau pour compte de tiers a un prix et le statut « Commandé »
- **THEN** ce prix n'est compté ni dans le déboursé, ni dans le reste à payer, ni dans le budget d'une personne

### Requirement: Cadeau masqué
Le système SHALL permettre de marquer un cadeau comme masqué pour les lecteurs d'un partage.

#### Scenario: Basculer le masquage
- **WHEN** le propriétaire active l'indicateur masqué
- **THEN** le cadeau est masqué pour les lecteurs et reste entièrement visible pour lui
