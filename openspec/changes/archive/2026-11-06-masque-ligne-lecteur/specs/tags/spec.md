## MODIFIED Requirements

### Requirement: Filtrage par tag
Le système SHALL permettre de filtrer les cadeaux d'une liste par tag. Pour un lecteur, un cadeau masqué SHALL être traité comme s'il n'avait aucun tag.

#### Scenario: Filtre
- **WHEN** l'utilisateur sélectionne un tag
- **THEN** seuls les cadeaux portant ce tag sont affichés

#### Scenario: Filtre par un lecteur
- **WHEN** un lecteur filtre par un tag porté par un cadeau masqué
- **THEN** le cadeau masqué n'est pas affiché et rien ne permet de déduire ses tags

#### Scenario: Filtre par le propriétaire
- **WHEN** le propriétaire filtre par un tag porté par un cadeau masqué
- **THEN** le cadeau masqué est affiché avec ses informations complètes
