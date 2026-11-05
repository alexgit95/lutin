# Changelog

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).

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
