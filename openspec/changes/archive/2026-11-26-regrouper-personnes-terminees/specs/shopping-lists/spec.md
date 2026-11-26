## ADDED Requirements

### Requirement: Regroupement des personnes par état de complétion
Dans l'onglet Personnes du propriétaire, le système SHALL afficher les personnes à compléter avant les personnes terminées. Chaque groupe SHALL conserver le tri par nom existant. Les personnes à compléter SHALL être affichées sans en-tête de groupe ; les personnes terminées SHALL être regroupées sous l'en-tête « Terminées », affiché uniquement si au moins une personne est terminée. Les informations et actions existantes de chaque personne SHALL rester disponibles dans les deux groupes.

#### Scenario: Personnes dans les deux états
- **WHEN** la liste contient Zoé et Alice à compléter, et Claire et Bernard terminés
- **THEN** l'onglet affiche Alice puis Zoé sans en-tête de groupe, suivies de l'en-tête « Terminées », puis Bernard et Claire

#### Scenario: Aucune personne terminée
- **WHEN** toutes les personnes sont à compléter
- **THEN** elles sont affichées par nom sans en-tête de groupe et l'en-tête « Terminées » est absent

#### Scenario: Toutes les personnes terminées
- **WHEN** toutes les personnes sont terminées
- **THEN** l'en-tête « Terminées » précède toutes les personnes, affichées par nom, sans en-tête ni groupe vide pour les personnes à compléter

#### Scenario: Aucune personne
- **WHEN** la liste ne contient aucune personne
- **THEN** le message « Aucune personne. » est affiché et aucun en-tête de groupe n'apparaît

#### Scenario: Modification du statut
- **WHEN** le propriétaire marque une personne terminée ou la remet à compléter
- **THEN** au retour sur l'onglet elle apparaît dans le groupe correspondant, à sa place dans l'ordre par nom, et l'en-tête « Terminées » reflète la présence ou l'absence de personnes terminées

#### Scenario: Informations et actions conservées
- **WHEN** le propriétaire consulte une personne dans l'un ou l'autre groupe
- **THEN** ses informations de budget, son indication éventuelle de complétion et ses actions habituelles restent disponibles