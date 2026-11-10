## ADDED Requirements

### Requirement: Dashboard sans suivi de remboursement
Le dashboard SHALL afficher tous les cadeaux de la liste, regroupés par personne, avec leur statut modifiable, le filtre par tag et la barre de workflow, et SHALL ne contenir aucune donnée ni action de remboursement : ni carte, ni alerte, ni pastille, ni filtre « À rembourser ». Le paramètre `aRembourser` SHALL être ignoré. Le suivi du remboursement SHALL se faire dans l'onglet Remboursement.

#### Scenario: Cadeaux et statuts conservés
- **WHEN** le propriétaire consulte le dashboard
- **THEN** tous les cadeaux, leur statut modifiable, le filtre par tag et la barre de workflow en bas sont présents

#### Scenario: Aucune donnée de remboursement
- **WHEN** le propriétaire consulte le dashboard d'une liste contenant des cadeaux remboursés et des alertes
- **THEN** la page et les lots de chargement continu ne contiennent ni carte, ni alerte, ni pastille, ni filtre de remboursement

#### Scenario: Paramètre ignoré
- **WHEN** l'adresse du dashboard porte le paramètre `aRembourser`
- **THEN** la liste n'est pas filtrée

#### Scenario: Changement de statut sans mise à jour du remboursement
- **WHEN** le propriétaire change le statut d'un cadeau depuis le dashboard
- **THEN** seules la ligne, la barre de workflow et le badge de l'onglet Remboursement sont mis à jour
