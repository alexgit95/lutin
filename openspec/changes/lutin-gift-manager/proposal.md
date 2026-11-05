## Why

Gérer des achats de cadeaux en masse (plusieurs destinataires, budgets, étapes de commande/livraison/emballage) est aujourd'hui fait à la main. Il faut une application web installable sur Android (PWA), multi-utilisateurs, avec suivi de workflow et budget. Le dépôt ne contient pas de code applicatif (le `pom.xml` vient d'un autre projet) : c'est un démarrage à zéro.

## What Changes

- Nouvelle application Spring Boot 4 / Java 25 avec Thymeleaf + HTMX (stack 100 % Java + HTML, pas de SPA).
- Inscription par login + mot de passe + clé d'invitation fixe (variable d'environnement) ; le premier inscrit est administrateur et peut consulter la clé dans un écran d'administration.
- Authentification par session cookie avec CSRF.
- Listes d'achat par utilisateur, contenant des personnes destinataires (avec budget) et des cadeaux.
- Cadeau : nom obligatoire (déduit du `<title>` de l'URL si absent), URL facultative, prix facultatif, destinataire, tags, indicateur « masqué », indicateur « pour compte de tiers ».
- Workflow : À acheter → Commandé → Livré → Emballé → Remis.
- Dashboard par liste : budget global, déboursé (statut Commandé ou plus), reste à payer, statuts de tous les cadeaux avec changement de statut direct ; IHM inspirée de la maquette (cartes KPI colorées, navigation, pastilles de statut).
- Tags sur les cadeaux (par liste) et filtrage.
- Partage d'une liste en lecture seule avec un autre utilisateur choisi dans une liste déroulante ; les cadeaux masqués n'apparaissent que par leur nom.
- PWA installable (manifest, icônes, service worker minimal, sans hors ligne).
- Deux profils : `dev` (SQLite, local) et `prod` (Docker + PostgreSQL, HTTPS fourni par Cloudflare).
- Nettoyage du `pom.xml` (retrait de JJWT et des dépendances inutiles) et mise à jour de `.github/copilot-instructions.md` pour deux profils.
- **BREAKING** : remplace le contenu hérité du projet « Lorcana » (aucun code existant à migrer).

## Capabilities

### New Capabilities
- `user-auth`: inscription avec clé d'invitation, connexion par session, rôle administrateur (premier inscrit), écran d'administration affichant la clé.
- `shopping-lists`: listes d'achat, personnes destinataires et leur budget.
- `gifts`: cadeaux (CRUD, prix facultatif, titre depuis l'URL avec protection SSRF, pour compte de tiers, masqué) et workflow de statuts.
- `tags`: tags par liste, affectation aux cadeaux, filtrage.
- `dashboard`: indicateurs budgétaires, vue des statuts, changement de statut direct, IHM.
- `list-sharing`: partage en lecture seule d'une liste, affichage des cadeaux masqués.
- `pwa-shell`: manifest, icônes, service worker minimal, installation Android.

### Modified Capabilities

(aucune : `openspec/specs/` est vide)

## Impact

- Code : création complète de l'application (`com.alexgit95.lutin`), entités JPA, contrôleurs MVC/HTMX, templates Thymeleaf, ressources statiques.
- Dépendances : ajout Thymeleaf, Thymeleaf Spring Security, Jsoup, PostgreSQL, springdoc ; retrait JJWT.
- Configuration : `application.properties` avec profils `dev` et `prod`, variables `INVITATION_KEY` et paramètres de base de données.
- Déploiement : `Dockerfile` et `docker-compose.yml` adaptés (app + PostgreSQL).
- Documentation : `docs/database.puml`, `README.md`, `CHANGELOG.md`, instructions Copilot.
