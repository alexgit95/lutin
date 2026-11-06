## MODIFIED Requirements

### Requirement: Cadeaux regroupés par personne
Le dashboard SHALL présenter les cadeaux visibles regroupés par personne destinataire. Deux personnes distinctes portant le même nom SHALL rester dans des groupes distincts. Les cadeaux masqués consultés par un lecteur SHALL apparaître dans un unique groupe neutre, placé après les autres groupes, sans exposer ni nom de cadeau, ni destinataire, ni statut.

#### Scenario: Cadeaux d'une personne
- **WHEN** le propriétaire consulte une liste contenant plusieurs cadeaux pour une personne
- **THEN** ces cadeaux apparaissent sous un même groupe portant le nom de cette personne

#### Scenario: Personnes homonymes
- **WHEN** deux personnes distinctes de la liste portent le même nom
- **THEN** les cadeaux de chacune restent dans leur groupe respectif

#### Scenario: Cadeau masqué vu par un lecteur
- **WHEN** un lecteur consulte un cadeau masqué
- **THEN** il apparaît comme une ligne « Cadeau masqué » dans le groupe neutre, sans nom de cadeau, nom de destinataire ni statut dans le groupe, le HTML ou la réponse de chargement

#### Scenario: Groupe masqué en dernière position
- **WHEN** un lecteur parcourt une liste dont les cadeaux sont chargés en plusieurs lots
- **THEN** les lignes masquées ne se trouvent qu'après tous les cadeaux visibles et ne réapparaissent pas entre deux groupes de personnes
