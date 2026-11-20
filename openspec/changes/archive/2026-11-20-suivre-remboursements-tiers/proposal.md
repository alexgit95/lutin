## Why

Lutin permet déjà d'exclure du budget un cadeau acheté pour le compte d'un tiers, mais ne permet ni d'identifier qui doit rembourser l'avance ni de savoir si elle a été rendue.

## What Changes

- Choisir le tiers parmi les personnes de la liste d'achats, indépendamment du destinataire du cadeau.
- Ajouter une sous-vue « Tiers » dans Remboursement, réservée au propriétaire, avec le reste à recevoir par personne et le détail des cadeaux.
- Marquer chaque cadeau intégralement remboursé en un clic, avec la date de réception et la possibilité d'annuler la saisie. Le montant est le prix du cadeau ; aucun paiement partiel n'est suivi.
- Conserver les cadeaux historiques sans tiers identifié et permettre de compléter leur attribution, sans les inclure dans les totaux par personne entre-temps.
- Préserver les budgets et le suivi actuel des virements depuis l'épargne.

## Capabilities

### New Capabilities

### Modified Capabilities

- `gifts`: identifier la personne pour le compte de laquelle le cadeau est acheté.
- `reimbursements`: séparer les remboursements reçus des tiers des virements depuis l'épargne et suivre les remboursements intégraux par cadeau.

## Impact

Formulaire et données des cadeaux, gestion des personnes, écrans de remboursement, sauvegarde et restauration, modèle de données et documentation. Aucun changement de dépendance ni de pipeline.