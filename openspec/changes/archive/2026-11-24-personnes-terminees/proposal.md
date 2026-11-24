## Why

Le propriétaire d'une liste ne peut pas identifier les personnes pour lesquelles tous les cadeaux ont déjà été renseignés. Les statuts individuels des cadeaux ne répondent pas à ce besoin : un cadeau présent dans la liste peut encore être à acheter.

## What Changes

- Permettre au propriétaire de marquer manuellement une personne comme terminée et de retirer ce statut depuis son menu d'actions.
- Signaler les personnes terminées dans l'onglet Personnes et dans l'en-tête de leur groupe de cadeaux sur le dashboard, avec une bordure verte, et afficher dans l'en-tête du dashboard un compteur global des personnes terminées sur le nombre total de personnes.
- Garder ce statut indépendant des statuts et des ajouts de cadeaux. Les personnes importées depuis une autre liste commencent non terminées ; les personnes déjà présentes ne sont pas modifiées.
- Réserver le statut, ses indicateurs et ses actions au propriétaire, sans divulgation au lecteur, et le conserver lors d'une sauvegarde et de sa restauration.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `shopping-lists`: suivi manuel et réversible de l'état terminé des personnes et comportement à l'import.
- `dashboard`: indicateurs de progression réservés au propriétaire, indépendants du filtre par tag.
- `data-transfer`: conservation de l'état terminé dans les sauvegardes et restaurations complètes.

## Impact

- Entité personne, schéma Hibernate compatible SQLite et PostgreSQL, diagramme de données et gestion des données existantes.
- Services, contrôleurs et vues des personnes et du dashboard, y compris les fragments et lots chargés par HTMX, avec contrôles d'accès serveur.
- Format d'export et d'import administrateur, tests et documentation fonctionnelle.