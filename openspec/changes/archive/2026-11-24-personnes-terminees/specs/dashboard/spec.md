## ADDED Requirements

### Requirement: Progression des personnes terminées
Le dashboard SHALL afficher au propriétaire dans son en-tête le nombre de personnes terminées sur le nombre total de personnes de la liste. Ces deux nombres SHALL inclure toutes les personnes, y compris celles sans cadeau, et SHALL être indépendants du filtre par tag et des lots chargés. Pour chaque groupe affiché d'une personne terminée, son en-tête SHALL montrer une coche et le texte « Tous les cadeaux dans la liste », et le groupe SHALL comporter une bordure verte. L'indication SHALL rester lisible sur mobile et ne SHALL pas dépendre uniquement de la couleur. Les personnes sans cadeau ou sans cadeau correspondant au filtre SHALL contribuer au compteur sans création de groupe vide.

#### Scenario: Compteur global
- **WHEN** une liste contient cinq personnes dont deux terminées, y compris une personne sans cadeau
- **THEN** l'en-tête affiche « 2 personnes terminées / 5 »

#### Scenario: Liste vide
- **WHEN** une liste ne contient aucune personne
- **THEN** le compteur affiche zéro personne terminée sur zéro

#### Scenario: Filtre ou chargement progressif
- **WHEN** le propriétaire filtre les cadeaux par tag ou charge un lot supplémentaire
- **THEN** le compteur conserve les nombres de toute la liste et les groupes affichés indiquent correctement l'état de leur personne

#### Scenario: Groupe terminé
- **WHEN** le propriétaire consulte un groupe de cadeaux d'une personne terminée
- **THEN** l'en-tête porte la coche et le libellé et le groupe a une bordure verte

#### Scenario: Groupe à compléter
- **WHEN** le propriétaire consulte un groupe de cadeaux d'une personne à compléter
- **THEN** le groupe ne porte pas l'indication ni la bordure de complétion

#### Scenario: Groupe sur plusieurs lots
- **WHEN** les cadeaux d'une personne terminée sont répartis sur plusieurs lots
- **THEN** leur groupe conserve une seule indication de complétion et sa bordure après fusion des lots

#### Scenario: Mise à jour d'un cadeau
- **WHEN** le propriétaire modifie le statut d'un cadeau depuis le dashboard
- **THEN** l'indication de complétion de sa personne et le compteur restent inchangés

### Requirement: Confidentialité de la progression des personnes
Les indications de complétion, leur compteur et leurs données SHALL être réservés au propriétaire. Les réponses destinées à un lecteur SHALL NOT transmettre l'état de complétion d'une personne, y compris dans le HTML, les attributs ou les lots de cadeaux, et SHALL NOT transmettre le compteur correspondant.

#### Scenario: Dashboard du lecteur
- **WHEN** un lecteur consulte une liste contenant des personnes terminées
- **THEN** il ne reçoit ni compteur, ni libellé, ni bordure ou donnée permettant d'identifier ces personnes comme terminées

#### Scenario: Chargement progressif et cadeaux masqués
- **WHEN** un lecteur charge un lot de cadeaux, y compris des cadeaux masqués
- **THEN** la réponse ne contient ni état de complétion ni compteur et le groupe neutre des cadeaux masqués ne révèle aucune identité supplémentaire