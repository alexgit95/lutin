## ADDED Requirements

### Requirement: Tags par liste
Le système SHALL permettre de créer des tags dans une liste et de les affecter à plusieurs cadeaux de cette liste.

#### Scenario: Affectation
- **WHEN** l'utilisateur ajoute un tag à un cadeau
- **THEN** le cadeau porte ce tag

#### Scenario: Tag d'une autre liste
- **WHEN** l'utilisateur tente d'affecter un tag d'une autre liste
- **THEN** l'opération est refusée

### Requirement: Filtrage par tag
Le système SHALL permettre de filtrer les cadeaux d'une liste par tag.

#### Scenario: Filtre
- **WHEN** l'utilisateur sélectionne un tag
- **THEN** seuls les cadeaux portant ce tag sont affichés
