## MODIFIED Requirements

### Requirement: Cadeau pour compte de tiers
Le système SHALL permettre au propriétaire de marquer un cadeau comme acheté pour le compte d'un tiers. Le formulaire SHALL proposer un champ « Acheté pour le compte de » distinct du destinataire, limité aux personnes de la même liste. Une nouvelle activation du mode tiers SHALL exiger une attribution valide côté serveur. Un cadeau historique déjà marqué pour tiers sans attribution SHALL pouvoir être conservé et complété ultérieurement. Désactiver le mode tiers SHALL retirer l'attribution sous réserve des règles de protection d'un remboursement reçu. Le cadeau pour compte de tiers SHALL suivre le workflow et rester exclu des budgets et totaux de dépenses.

#### Scenario: Exclusion des totaux
- **WHEN** un cadeau pour compte de tiers a un prix et le statut « Commandé »
- **THEN** ce prix n'est compté ni dans le déboursé, ni dans le reste à payer, ni dans le budget d'une personne

#### Scenario: Attribution distincte du destinataire
- **WHEN** le propriétaire enregistre un cadeau pour Léa en choisissant Marc, rattaché à la même liste, comme tiers
- **THEN** le cadeau reste destiné à Léa et son achat est attribué à Marc, dont le nom est visible dans la vue propriétaire de la liste

#### Scenario: Nouveau cadeau sans tiers valide
- **WHEN** le propriétaire active le mode tiers sans choisir de personne ou soumet une personne d'une autre liste
- **THEN** la demande est refusée sans modification du cadeau

#### Scenario: Cadeau historique
- **WHEN** le propriétaire modifie un cadeau déjà marqué pour compte de tiers avant cette évolution et encore sans attribution
- **THEN** il peut conserver son état sans attribution ou sélectionner une personne de la même liste

#### Scenario: Désactivation avant règlement
- **WHEN** le propriétaire désactive le mode tiers sur un cadeau non réglé par le tiers
- **THEN** l'attribution est retirée et les règles habituelles de budget et de remboursement s'appliquent

#### Scenario: Suppression d'une personne encore référencée
- **WHEN** le propriétaire tente de supprimer une personne encore désignée comme tiers sur un cadeau
- **THEN** la suppression est refusée avec une indication de retirer ou de réattribuer les achats concernés, sans supprimer de cadeau ni d'attribution