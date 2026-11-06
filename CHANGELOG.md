# Changelog

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).

## [0.4.0] - 2026-11-06

### Changed
- Masquage total pour les lecteurs : un cadeau masqué n'est plus qu'une ligne « Cadeau masqué », son nom n'est plus envoyé (`VueCadeau`, `liste-fragments.html`). Impact fonctionnel : **breaking** pour les lecteurs, qui ne voient plus le nom des cadeaux masqués.
- Pour un lecteur, les cadeaux masqués forment une seule section en fin de liste, triée par identifiant via une requête dédiée (`CadeauRepository.findPourLecteur`), y compris entre les lots de chargement continu.
- Pour un lecteur, un cadeau masqué est traité comme sans tag : le filtre par tag l'exclut (`findByListeIdAndTagsIdAndMasqueFalse`). Le propriétaire est inchangé.
- Version du `pom.xml` portée à 0.4.0.

### Fixed
- Les lignes masquées d'un lecteur ne s'intercalent plus entre les groupes de personnes : leur place révélait le rang alphabétique du destinataire.
- Un lecteur ne peut plus deviner les tags d'un cadeau masqué en filtrant par tag.

## [0.3.2] - 2026-11-06

### Added
- Landing page `/` avec deux grandes tuiles (listes d'achat, wishlists) affichant les nombres de listes et de wishlists, dont celles partagées, et le compteur d'avis.

### Changed
- La liste des listes d'achat passe de `/` à `/listes` (`GET`) ; le lien « Mes listes » de la barre et les redirections après erreur de création ou suppression d'une liste pointent vers `/listes`.
- Version du `pom.xml` portée à 0.3.2.

### Fixed
- Aucun.

## [0.3.1] - 2026-11-06

### Changed
- Wishlist du propriétaire : onglets Articles / Partage / Réglages (`GET /wishlists/{id}/reglages`) ; la page principale n'affiche plus que les articles. Renommer revient sur l'onglet Réglages.
- Rendu des wishlists en tuiles (CSS maison, sans dépendance) avec une tuile d'ajout ; les vues d'articles portent l'initiale et le site d'origine (`hote`), calculés côté serveur.
- Pour un lecteur, une tuile prise est grisée avec un ruban « Pris par … » ; le propriétaire n'en voit rien.
- Version du `pom.xml` portée à 0.3.1.

### Fixed
- Aucun.

## [0.3.0] - 2026-11-06

### Added
- Wishlists (`/wishlists`, entrée « Wishlists » de la barre) : création, renommage et suppression avec nom obligatoire et unique par propriétaire (insensible à la casse), articles (nom obligatoire, URL http/https et prix facultatifs, titre automatique depuis l'URL avec protection SSRF, pagination par 50), partage par liste déroulante, sections « Mes wishlists » et « Partagées avec moi ». Impact : nouvelles tables `wishlist`, `souhait`, `wishlist_partage` et `avis` (créées par Hibernate), documentées dans `docs/database.puml`.
- Article « pris » : les lecteurs cochent et voient le nom du preneur (texte, sans lien vers `utilisateur`, prêt pour de futurs invités) ; prise atomique par `UPDATE` conditionnel, un seul preneur même en cas de clics simultanés.
- Décochage par tout lecteur, avec page d'avertissement et confirmation obligatoire quand le lecteur n'est pas le preneur.
- Aveuglement du propriétaire : gabarits et objets de vue distincts, aucune donnée « pris », preneur ou avis envoyée au propriétaire ni à l'administrateur, tri indépendant de l'état « pris ».
- Avis aux lecteurs connectés (modification ou suppression d'un article pris, suppression d'une wishlist contenant des articles pris), fermeture définitive par « OK », expiration à 90 jours, compteur dans la barre de navigation (`LayoutAdvice`, pages GET uniquement).
- Tests JUnit 5 / MockMvc : `WishlistsParcoursTest` (35) et `WishlistsWebTest` (14).

### Changed
- Le retrait d'un partage de wishlist supprime les avis du lecteur pour cette wishlist ; ses articles pris restent « pris par ».
- Version du `pom.xml` portée à 0.3.0.

### Fixed
- Aucun.

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
