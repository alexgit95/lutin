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
Le système SHALL permettre au propriétaire de marquer un cadeau comme acheté pour le compte d'un tiers. Le formulaire SHALL proposer un champ « Acheté pour le compte de » distinct du destinataire, limité aux personnes de la même liste. Une nouvelle activation du mode tiers SHALL exiger une attribution valide côté serveur. Un cadeau historique déjà marqué pour tiers sans attribution SHALL pouvoir être conservé et complété ultérieurement. Désactiver le mode tiers SHALL retirer l'attribution sous réserve des règles de protection d'un remboursement reçu. Le cadeau pour compte de tiers SHALL suivre le workflow et rester exclu des budgets et totaux de dépenses.

#### Scenario: Exclusion des totaux
- **WHEN** un cadeau pour compte de tiers a un prix et le statut « Commandé »
- **THEN** ce prix n'est compté ni dans le déboursé, ni dans le reste à payer, ni dans le budget d'une personne

#### Scenario: Attribution distincte du destinataire
- **WHEN** le propriétaire enregistre un cadeau pour Léa en choisissant Marc, rattaché à la même liste, comme tiers
- **THEN** le cadeau reste destiné à Léa et son achat est attribué à Marc, dont le nom est visible dans la vue propriétaire de la liste

#### Scenario: Nouveau cadeau sans tiers valide
- **WHEN** le propriétaire active le mode tiers sans choisir de personne ou soumet une personne d'une autre liste
- **THEN** la demande est refusée sans modification du cadeau

#### Scenario: Cadeau historique
- **WHEN** le propriétaire modifie un cadeau déjà marqué pour compte de tiers avant cette évolution et encore sans attribution
- **THEN** il peut conserver son état sans attribution ou sélectionner une personne de la même liste

#### Scenario: Désactivation avant règlement
- **WHEN** le propriétaire désactive le mode tiers sur un cadeau non réglé par le tiers
- **THEN** l'attribution est retirée et les règles habituelles de budget et de remboursement s'appliquent

#### Scenario: Suppression d'une personne encore référencée
- **WHEN** le propriétaire tente de supprimer une personne encore désignée comme tiers sur un cadeau
- **THEN** la suppression est refusée avec une indication de retirer ou de réattribuer les achats concernés, sans supprimer de cadeau ni d'attribution

### Requirement: Cadeau masqué
Le système SHALL permettre de marquer un cadeau comme masqué pour les lecteurs d'un partage.

#### Scenario: Basculer le masquage
- **WHEN** le propriétaire active l'indicateur masqué
- **THEN** le cadeau est masqué pour les lecteurs et reste entièrement visible pour lui
