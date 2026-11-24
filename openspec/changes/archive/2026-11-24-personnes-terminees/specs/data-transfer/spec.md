## ADDED Requirements

### Requirement: Sauvegarde de la complétion des personnes
Les exports complets administrateur et API SHALL inclure l'état de complétion de chaque personne dans le format versionné courant. L'import d'une sauvegarde compatible SHALL restaurer cet état exactement, aussi bien pour les personnes terminées que pour celles à compléter, tout en conservant les garanties existantes de validation et de remplacement transactionnel.

#### Scenario: Export
- **WHEN** une sauvegarde complète est produite pour une liste contenant une personne terminée et une personne à compléter
- **THEN** le fichier conserve la valeur de l'état de chacune et porte la nouvelle version du format

#### Scenario: Restauration
- **WHEN** une sauvegarde compatible contenant les deux états est importée
- **THEN** chaque personne retrouve son état enregistré dans la sauvegarde

#### Scenario: Export API
- **WHEN** une sauvegarde est produite via l'API de sauvegarde
- **THEN** elle contient les mêmes états de complétion et utilise le même format versionné que l'export administrateur