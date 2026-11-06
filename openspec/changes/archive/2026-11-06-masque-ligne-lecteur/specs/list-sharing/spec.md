## MODIFIED Requirements

### Requirement: Cadeaux masqués pour les lecteurs
Le système SHALL présenter à un lecteur un cadeau masqué uniquement par une ligne « Cadeau masqué », sans nom, URL, prix, statut, tags ni destinataire, tout en le conservant dans les totaux. Un lecteur SHALL voir tous les autres cadeaux de la liste comme le propriétaire. Les cadeaux masqués d'un lecteur SHALL être regroupés en une seule section placée après les autres, triée indépendamment du nom des cadeaux et des personnes.

#### Scenario: Ligne masquée
- **WHEN** un lecteur consulte une liste contenant un cadeau masqué
- **THEN** il voit une ligne « Cadeau masqué » sans nom ni aucune autre information du cadeau

#### Scenario: Cadeaux non masqués
- **WHEN** un lecteur consulte une liste contenant des cadeaux non masqués
- **THEN** il voit le nom, l'URL, le prix, le statut, les tags et le destinataire comme le propriétaire

#### Scenario: Section unique en fin de liste
- **WHEN** un lecteur consulte une liste contenant des cadeaux masqués pour plusieurs personnes
- **THEN** les lignes masquées forment une seule section après tous les groupes de personnes, dans l'ordre de leur création

#### Scenario: Ordre indépendant des noms
- **WHEN** le nom d'un cadeau masqué ou celui de sa personne destinataire est modifié
- **THEN** la place de la ligne masquée pour un lecteur ne change pas

#### Scenario: Totaux inchangés
- **WHEN** un lecteur consulte les indicateurs budgétaires
- **THEN** les montants incluent les cadeaux masqués

#### Scenario: Aucune fuite de données
- **WHEN** la réponse est envoyée à un lecteur, page complète ou lot de chargement
- **THEN** ni le nom, ni l'URL, ni le prix, ni le statut, ni les tags, ni le destinataire d'un cadeau masqué ne figurent dans le HTML renvoyé

#### Scenario: Propriétaire inchangé
- **WHEN** le propriétaire consulte une liste contenant des cadeaux masqués
- **THEN** il voit chaque cadeau complet, avec le tri habituel par personne et par nom
