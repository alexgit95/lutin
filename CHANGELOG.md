# Changelog

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).

## [0.2.0] - 2026-11-05

### Added
- Dashboard : cadeaux regroupés par personne destinataire ; les homonymes restent distincts grâce à l'identifiant de la personne. Pour un lecteur, les cadeaux masqués sont dans un groupe neutre, sans destinataire ni statut.
- Chargement continu des cadeaux par lots de 50 (`GET /listes/{id}/cadeaux`), avec contrôle d'accès serveur, filtre par tag conservé et ordre stable. Reprise sur erreur par un bouton « Réessayer ».
- Barre de workflow fixée en bas de l'écran avec le nombre de cadeaux par statut, indépendante du filtre par tag, accessible (annonce aux lecteurs d'écran) et adaptée au mobile.
- Animation du changement de statut : jeton du cadeau vers l'étape cible et compteurs animés, jouée seulement après confirmation du serveur et désactivée avec `prefers-reduced-motion`.

### Changed
- Le changement de statut HTMX ne renvoie plus tout le dashboard : seule la ligne du cadeau et la barre (swap hors bande) sont remplacées, ce qui préserve les lots chargés et le défilement.
- Les fragments Thymeleaf du dashboard sont déplacés dans `liste-fragments.html`.
- Version du `pom.xml` portée à 0.2.0.

### Fixed
- Aucun.

## [0.1.0] - 2026-11-05

### Added
- Inscription par login, mot de passe et clé d'invitation fixe (`INVITATION_KEY`) ; premier inscrit administrateur de façon atomique (contrainte d'unicité) ; écran `/admin` affichant la clé. Impact : accès limité aux personnes invitées.
- Connexion par session cookie avec CSRF, mots de passe hachés en BCrypt.
- Listes d'achat avec quatre onglets : Dashboard, Personnes, Partage, Réglages.
- Personnes destinataires avec budget (création, modification, suppression en cascade sur leurs cadeaux).
- Cadeaux : nom, URL, prix facultatif, destinataire, tags, indicateurs « masqué » et « pour compte de tiers », workflow À acheter → Commandé → Livré → Emballé → Remis.
- Récupération du titre de l'URL avec protection SSRF (schémas, adresses internes, redirections, timeout, taille).
- Dashboard : budget global, déboursé, reste à payer, répartition par statut, filtre par tag, pagination, changement de statut en HTMX.
- Partage d'une liste en lecture seule ; cadeaux masqués réduits à leur nom côté serveur, sans fuite dans le HTML.
- PWA : manifeste, icônes 192/512, service worker minimal sans cache hors ligne.
- Profils `dev` (SQLite) et `prod` (PostgreSQL, cookies `Secure`, `X-Forwarded-*`).
- Modèle de données documenté dans `docs/database.puml`, wrapper Maven (`mvnw`), tests JUnit 5 / Mockito / MockMvc.

### Changed
- `pom.xml` repris d'un autre projet : retrait de JJWT et de `webclient`, ajout de Thymeleaf, version 0.1.0.
- `Dockerfile` et `docker-compose.yml` : profil `prod`, variable `INVITATION_KEY` obligatoire, retrait des variables JWT et administrateur.
- `.github/copilot-instructions.md` réécrit pour deux profils (`dev` SQLite, `prod` Docker + PostgreSQL).

### Fixed
- Aucun (première version).
