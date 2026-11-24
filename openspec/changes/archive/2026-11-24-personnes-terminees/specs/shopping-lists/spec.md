## ADDED Requirements

### Requirement: État terminé manuel par personne
Le propriétaire SHALL pouvoir marquer manuellement chaque personne d'une liste comme terminée ou à compléter depuis son menu d'actions. « Terminé » signifie que tous les cadeaux prévus pour cette personne sont présents dans la liste, quel que soit leur statut d'achat. L'état SHALL être propre à la personne dans cette liste, conservé en base et indiqué de façon lisible dans l'onglet Personnes. Les nouvelles personnes et les personnes préexistantes lors de la mise à jour SHALL être à compléter par défaut.

#### Scenario: Marquer une personne terminée
- **WHEN** le propriétaire choisit « Marquer comme terminé » dans le menu d'une personne à compléter
- **THEN** son état terminé est enregistré et signalé dans l'onglet Personnes

#### Scenario: Retirer le statut
- **WHEN** le propriétaire choisit « Marquer comme à compléter » dans le menu d'une personne terminée
- **THEN** son état redevient à compléter et l'indication de complétion disparaît

#### Scenario: Action répétée
- **WHEN** une même demande explicite de passage à un état est envoyée plusieurs fois
- **THEN** la personne reste dans l'état demandé sans basculement involontaire

#### Scenario: Personne nouvelle ou existante
- **WHEN** une personne est créée ou préexistait à l'installation de cette fonctionnalité
- **THEN** elle est à compléter tant que son propriétaire ne l'a pas marquée terminée

#### Scenario: Cadeaux indépendants
- **WHEN** un cadeau d'une personne terminée est ajouté, modifié ou supprimé, ou que son statut change
- **THEN** l'état de la personne reste terminé jusqu'à une modification explicite par le propriétaire

#### Scenario: Aucun changement des cadeaux
- **WHEN** le propriétaire modifie l'état d'une personne
- **THEN** les cadeaux et leurs statuts restent inchangés

#### Scenario: Import depuis une autre liste
- **WHEN** des personnes sont importées d'une autre liste, que les personnes source soient terminées ou non
- **THEN** les personnes nouvellement créées sont à compléter et l'état des personnes déjà présentes dans la liste cible est conservé

#### Scenario: Accès non autorisé
- **WHEN** un lecteur ou un autre utilisateur tente de modifier l'état d'une personne, ou que la personne n'appartient pas à la liste indiquée
- **THEN** la modification est refusée et aucun état n'est changé

#### Scenario: Protection CSRF
- **WHEN** une demande de modification de l'état est envoyée sans jeton CSRF valide
- **THEN** elle est refusée et aucun état n'est changé