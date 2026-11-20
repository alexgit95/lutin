## Context

Le fragment Thymeleaf rend chaque cadeau comme une ligne de tableau : nom, prix, tags, statut et, pour le propriétaire, menu d'actions. Dans la règle CSS mobile actuelle, la ligne devient une grille de deux colonnes ; les tags partagent donc une rangée avec le prix et le statut se retrouve séparé. La présentation desktop utilise les colonnes du tableau et ne doit pas changer.

## Goals / Non-Goals

**Goals:**

- Placer les tags immédiatement sous le nom du cadeau sur les écrans étroits.
- Réserver une colonne plus large au statut, en gardant prix et menu d'actions accessibles.
- Conserver les données, libellés, commandes, droits du lecteur et mise en page desktop.

**Non-Goals:**

- Changer le modèle de données, l'ordre métier, les filtres, le workflow ou les endpoints.
- Modifier l'aspect des cartes budget, de la barre de workflow ou des autres pages.

## Decisions

- Garder la structure générale de ligne/tableau pour les grands écrans. Donner aux cellules des hooks explicites si nécessaires, puis réorganiser leur placement dans la grille uniquement au breakpoint mobile ; éviter de dépendre d'index de cellule fragiles.
- Sur téléphone, disposer les zones dans cet ordre : nom pleine largeur, tags pleine largeur sous le nom, puis prix, statut et bouton d'actions sur une même rangée. La piste du statut reçoit davantage de largeur que celle du prix ; le bouton d'actions garde une piste compacte. Pour un lecteur, le statut reste en lecture seule et peut utiliser l'espace libéré par l'absence du menu.
- Ne pas réserver une rangée vide lorsque le cadeau n'a aucun tag. Garder les libellés de prix, tags et statut et autoriser les valeurs longues à revenir à la ligne sans créer de débordement horizontal.

## Risks / Trade-offs

- [Écrans très étroits et intitulés de statut longs] → Vérifier au minimum 320 px et 375 px, avec commandes propriétaire et libellés de statut réels.
- [Cadeaux sans tag ou sans prix] → Vérifier que la grille reste compacte et que les cellules ne se superposent pas.
- [Lecteur et HTML mis à jour par HTMX] → Vérifier la cellule de statut en lecture seule, l'absence de détails/actions interdits et le rendu des lignes remplacées.
- [Régression desktop] → Confirmer que le breakpoint mobile est le seul à changer le placement.

## Migration Plan

Aucune migration de données. Le retour à la version précédente restaure l'ordre mobile précédent.

## Open Questions

Aucune décision bloquante identifiée.