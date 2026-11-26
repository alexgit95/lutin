## Context

L'onglet Personnes reçoit de `PersonneService.lister` une liste triée par nom par le dépôt, avec l'état de complétion et les montants de chaque personne. Le changement de statut est manuel et redirige ensuite vers cet onglet. Le même tri du dépôt sert aussi à l'import et ne doit pas voir son comportement modifié.

## Goals / Non-Goals

**Goals:**

- Séparer les personnes à compléter et les personnes terminées dans l'onglet, en conservant le tri par nom existant dans chaque groupe.
- Afficher uniquement un titre « Terminées », lorsqu'il y a des personnes terminées, et conserver les informations et actions de chaque ligne.
- Recalculer la présentation lors du retour sur l'écran après un changement de statut.

**Non-Goals:**

- Modifier le statut manuel, les cadeaux, les droits d'accès, le modèle de données ou les autres écrans.
- Introduire une nouvelle règle de classement linguistique ou modifier le tri des écrans d'import.

## Decisions

- Partitionner pour l'onglet la liste déjà triée par nom en deux groupes ordonnés, sans modifier la méthode de dépôt partagée. Cela préserve l'ordre existant et évite de changer les usages de l'import. Un tri par statut directement dans la méthode de dépôt existante aurait un impact sur ces autres usages.
- Rendre les groupes sous forme de listes avec un titre sémantique « Terminées » pour le second groupe seulement. Réutiliser le rendu d'une ligne pour préserver budgets et actions sans duplication divergente. Un simple changement d'ordre sans titre ne rendrait pas la raison du regroupement explicite.
- Garder la soumission et la redirection existantes pour les changements d'état : le rendu suivant place la personne dans son nouveau groupe. Une mise à jour dynamique côté client ajouterait de la complexité sans être nécessaire.

## Risks / Trade-offs

- [Tri des noms] L'ordre dépend du tri existant de la base, notamment pour la casse et les accents → préserver ce comportement et tester l'ordre dans les deux groupes sans promettre une nouvelle collation.
- [Cas limites] Un groupe vide pourrait laisser apparaître un titre ou une liste vide inutile → tester les cas aucun terminé, tous terminés et aucune personne.
- [Régression du rendu] La séparation pourrait perdre une action ou les informations de budget → réutiliser le rendu des lignes et vérifier les informations et actions dans les deux groupes.