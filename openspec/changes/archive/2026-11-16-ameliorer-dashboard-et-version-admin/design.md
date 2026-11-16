## Context

Le dashboard rend un tableau indépendant par personne ; leurs largeurs sont calculées selon leur contenu et la cellule d'actions utilise actuellement le style flex de `.mini`. Les fragments de lignes et de groupes sont également rendus lors des échanges HTMX. La barre de workflow est fixe et utilise cinq colonnes, mais sa présentation mobile et l'absence de débordement doivent être vérifiées en conditions réelles.

Le build Spring Boot fournit déjà `BuildProperties`, avec la version Maven et la propriété `git.commit` transmise par le Dockerfile. Hors build renseigné, cette propriété vaut par défaut `unknown`, et les informations de build peuvent être absentes.

## Goals / Non-Goals

**Goals:**

- Garder des colonnes de cadeaux alignées entre groupes et permettre le retour à la ligne des noms longs.
- Garder les cinq statuts et leurs compteurs visibles sur mobile sans défilement horizontal ni chevauchement du contenu.
- Afficher l'identité du build sur toutes les réponses rendant la page d'administration, y compris après génération d'une clé.

**Non-Goals:**

- Modifier les règles métier, les droits, la pagination, les pipelines, le schéma de base ou le format d'export.
- Déduire le commit à partir du dépôt Git à l'exécution ou ajouter une dépendance dédiée.

## Decisions

1. Conserver les tableaux par groupe, nécessaires au regroupement et au chargement progressif, mais leur appliquer une définition de colonnes commune et stable. Préserver le comportement de cellule de tableau pour les actions et autoriser la coupure des noms longs, y compris sans espaces. Sur écran très étroit, adapter la présentation des lignes si nécessaire pour éviter le débordement tout en gardant les données et actions accessibles. Une mise en page calculée indépendamment pour chaque tableau est écartée, car elle reproduit le décalage.
2. Conserver la barre fixe et ses cinq étapes avec leurs libellés et compteurs, en utilisant des dimensions adaptables et une marge inférieure suffisante pour ne pas masquer les derniers cadeaux, y compris avec la zone de sécurité mobile. Vérifier la largeur réelle de la page et de la barre ; masquer le débordement sans corriger son origine ne constitue pas une solution.
3. Injecter les `BuildProperties` de manière optionnelle dans le contrôleur d'administration, comme dans le service d'export, et alimenter le modèle dans la méthode commune de rendu. Afficher la version Maven suivie d'un SHA hexadécimal raccourci à sept caractères ; afficher « inconnue » ou « inconnu » pour chaque information manquante ou non renseignée. Les propriétés embarquées décrivent le build réellement exécuté, contrairement à un appel Git à l'exécution.

## Risks / Trade-offs

- [Écrans très étroits et titres ou tags longs] → Vérifier l'absence de débordement à plusieurs largeurs, avec des mots sans espaces et des valeurs longues ; garder les commandes utilisables.
- [Contenu masqué par la barre fixe] → Vérifier le dernier cadeau et la zone de sécurité en bas, y compris si les libellés prennent plusieurs lignes.
- [Régression HTMX ou confidentialité] → Tester le chargement de lots, le changement de statut, les compteurs et les vues propriétaire et lecteur, sans exposer de détails sur les cadeaux masqués.
- [Métadonnées absentes ou SHA `unknown`] → Prévoir des valeurs de repli explicites sans empêcher l'administration de fonctionner.

## Migration Plan

Aucune migration de données. Déployer l'application normalement ; le retour à la version précédente restaure l'affichage précédent.

## Open Questions

Aucune décision bloquante identifiée.