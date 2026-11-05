## ADDED Requirements

### Requirement: Partage d'une liste
Le système SHALL permettre au propriétaire de partager une liste précise avec un autre utilisateur choisi dans la liste déroulante des logins existants, et de retirer ce partage.

#### Scenario: Partage
- **WHEN** le propriétaire choisit un login et valide
- **THEN** cet utilisateur voit la liste dans ses listes partagées

#### Scenario: Retrait
- **WHEN** le propriétaire retire le partage
- **THEN** l'utilisateur perd l'accès à la liste

### Requirement: Lecture seule
Le système SHALL n'autoriser aux lecteurs que la consultation du dashboard complet de la liste partagée.

#### Scenario: Tentative de modification
- **WHEN** un lecteur tente de modifier ou supprimer un élément de la liste
- **THEN** la requête est refusée (403) et rien n'est modifié

### Requirement: Cadeaux masqués pour les lecteurs
Le système SHALL présenter à un lecteur un cadeau masqué uniquement par une ligne portant son nom, sans URL, prix, statut, tags ni destinataire, tout en le conservant dans les totaux.

#### Scenario: Ligne masquée
- **WHEN** un lecteur consulte une liste contenant un cadeau masqué
- **THEN** il voit une ligne avec le nom et aucune autre information du cadeau

#### Scenario: Totaux inchangés
- **WHEN** un lecteur consulte les indicateurs budgétaires
- **THEN** les montants incluent les cadeaux masqués

#### Scenario: Aucune fuite de données
- **WHEN** la réponse est envoyée à un lecteur
- **THEN** les champs masqués ne figurent pas dans le HTML renvoyé
