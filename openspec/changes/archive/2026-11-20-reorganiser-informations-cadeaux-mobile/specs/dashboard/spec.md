## MODIFIED Requirements

### Requirement: Interface adaptée au mobile
L'interface SHALL s'inspirer de la maquette (cartes d'indicateurs colorées, navigation, pastilles de statut) et rester utilisable sur écran de téléphone. Le dashboard SHALL s'adapter à la largeur disponible sans défilement horizontal, tout en conservant l'accès aux informations et actions des cadeaux. Sur écran étroit, le nom de chaque cadeau SHALL être suivi de ses tags lorsqu'il en possède ; le prix, le statut et les actions SHALL être disposés ensuite sur une rangée où le statut dispose de plus d'espace que le prix. Le lecteur SHALL conserver un statut en lecture seule et ne recevoir aucune action réservée au propriétaire. La mise en page desktop SHALL rester inchangée.

#### Scenario: Écran étroit
- **WHEN** le dashboard est affiché sur un écran de téléphone
- **THEN** la navigation et les cartes restent lisibles sans défilement horizontal

#### Scenario: Cadeaux sur écran étroit
- **WHEN** le dashboard contient des cadeaux avec des noms ou des données longs sur un écran de téléphone
- **THEN** le contenu reste lisible et les actions accessibles sans défilement horizontal

#### Scenario: Tags placés sous le nom
- **WHEN** un cadeau possède un ou plusieurs tags et que le dashboard est affiché sur un écran étroit
- **THEN** ses tags apparaissent sous son nom, avant le prix et le statut

#### Scenario: Statut prioritaire sur mobile
- **WHEN** un cadeau est affiché sur un écran étroit
- **THEN** le prix, le statut et, pour le propriétaire, le bouton d'actions apparaissent après le nom et les tags, avec plus d'espace horizontal accordé au statut qu'au prix

#### Scenario: Cadeau sans tag
- **WHEN** un cadeau n'a aucun tag sur un écran étroit
- **THEN** aucune rangée vide n'est réservée aux tags et le prix, le statut et les actions gardent leur disposition compacte

#### Scenario: Lecteur sur écran étroit
- **WHEN** un lecteur consulte un cadeau sur un écran étroit
- **THEN** les tags sont sous le nom, le statut reste en lecture seule et aucune action de modification ou de suppression n'est affichée

#### Scenario: Présentation desktop
- **WHEN** le dashboard est affiché au-dessus du breakpoint mobile
- **THEN** les colonnes du tableau conservent leur disposition actuelle