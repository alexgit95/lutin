# Changelog

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).

## [0.9.0] - 2026-11-09

### Added
- Authentification par **Cloudflare Access** en prod (`lutin.acces.mode=cloudflare`, activé par `application-prod.properties`) : `CloudflareAccessFilter` valide à chaque requête le jeton `Cf-Access-Jwt-Assertion` (signature par la JWKS du team, émetteur, audience, expiration, présence d'un email) sans rien enregistrer en session ; sans jeton valide, 401. Chemins publics (`/p/**`, statiques, `/manifest.webmanifest`, `/sw.js`, `/error`) servis sans jeton, avec authentification facultative.
- Identité : `utilisateur.email_hmac` (HMAC-SHA256 de l'email normalisé, `EmailHmacService`), `login_cle` (pseudo en minuscules, unique) ; l'email n'est jamais stocké, affiché ni journalisé. `AccesAuthentication.getName()` renvoie le pseudo.
- Page `/bienvenue` et `PseudoService` : choix du pseudo à la première connexion (4 à 50 caractères `[A-Za-z0-9._-]`, unique sans casse, réservés `admin`, `administrateur`, `root`, `lutin`, `system`, `support`, immuable, création idempotente). Le premier compte créé est administrateur, y compris en cas de créations simultanées.
- Déconnexion vers `https://<team>/cdn-cgi/access/logout`, bannière d'erreur visible pour les requêtes HTMX en échec (code de statut, session expirée, connexion impossible), `crossorigin="use-credentials"` sur le manifest.
- Configuration prod obligatoire : `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD`, `EMAIL_HMAC_SECRET` (32 caractères minimum) ; démarrage refusé sinon. Dépendance `spring-security-oauth2-jose`.
- Tests : `EmailHmacServiceTest` (5), `CloudflareAccessConfigTest` (9), `JetonCloudflareTest` (7), `PseudoServiceTest` (10), `AccesCloudflareWebTest` (17). Total de la suite : 289.

### Changed
- `SecurityConfig` ne porte plus que la chaîne du mode local (`@ModeLocal`) ; `AuthController` et `RegistrationService` ne sont actifs qu'en mode local (`/login` et `/register` répondent 404 en prod).
- `utilisateur.password_hash` devient facultatif ; `docs/database.puml` mis à jour.
- `/admin` reste réservé à l'administrateur : clé d'invitation en dev, message neutre en prod. `INVITATION_KEY` n'est plus exigé en prod (`docker-compose.yml`, `application-prod.properties`).
- **BREAKING** : aucun compte existant n'est repris ; recréer la base de prod (volume PostgreSQL) et `lutin-dev.db`.
- Version du `pom.xml` portée à 0.9.0.

### Fixed
- Aucun.

## [0.8.0] - 2026-11-09

### Added
- Remboursement des cadeaux (propriétaire seulement) : colonne `cadeau.rembourse_le` (nullable) et pastille par cadeau « € à rembourser » / « remboursé le jj/MM » (`POST /listes/{id}/cadeaux/{cid}/rembourse`, HTMX ou redirection). Un cadeau est remboursable s'il est Commandé ou plus, avec un prix, et pas « pour un tiers » ; sinon la requête est refusée.
- Carte « À rembourser » : montants à rembourser et déjà remboursés (`CadeauRepository.sommeARembourser` et `sommeRembourse`), « À virer : X € » et bouton « Tout marquer remboursé » (`POST /listes/{id}/remboursements`) avec date préremplie, refus d'une date future, mise à jour groupée limitée aux cadeaux éligibles et compte rendu (nombre et somme).
- Filtre « À rembourser » (paramètre `aRembourser`, avec ou sans tag, propagé au chargement continu) ; ignoré pour un lecteur.
- Entité `AlerteRemboursement` (table `alerte_remboursement`), `AlerteRemboursementService` et `POST /listes/{id}/alertes/{aid}/fermer` : alerte « virement en trop » quand un cadeau remboursé repasse à « À acheter », est supprimé, devient « pour un tiers » ou voit sa personne supprimée. Elle persiste jusqu'à fermeture (20 plus récentes et total) et est supprimée avec la liste.
- `DateDuJour` : date du jour dans le fuseau `lutin.fuseau-horaire` (défaut `Europe/Paris`).
- `docs/database.puml` mis à jour.
- Tests : `RemboursementTest` (19) et `RemboursementWebTest` (12). Total de la suite : 241.

### Changed
- Le prix d'un cadeau remboursé est verrouillé (refus serveur et champ en lecture seule dans le formulaire) ; la date n'est pas touchée par les autres modifications.
- Le Déboursé et le Reste à payer du dashboard sont inchangés par le remboursement.
- Aucune donnée de remboursement n'est envoyée à un lecteur (page, lots, réponses HTMX).
- Version du `pom.xml` portée à 0.8.0.

### Fixed
- Aucun.

## [0.7.0] - 2026-11-06

### Added
- Onglet Personnes : barre de budget par personne (dépensé en teinte pleine, reste à payer en teinte claire, budget en repère) avec les montants écrits, une description `aria-label`, un cas « budget à 0 » sans barre et un dépassement affiché en rouge avec un repère.
- `CadeauRepository.sommesParPersonne` : une requête agrégée par liste (mêmes règles que le dashboard), `PersonneService.lister` et `VuePersonneBudget`. La somme par personne égale le « Déjà déboursé » et le « Reste à payer » du dashboard.
- Tests : `PersonnesBudgetTest` (8) et `PersonnesBudgetWebTest` (6).

### Changed
- Les actions d'une personne (+ cadeau, modifier, supprimer) passent dans un sous-menu natif `<details>` ouvert par un bouton « ⋯ », refermé par un court script au clic ailleurs et sur Échap ; la confirmation de suppression est conservée.
- Version du `pom.xml` portée à 0.7.0.

### Fixed
- Aucun.

## [0.6.0] - 2026-11-06

### Added
- Import de personnes depuis une autre liste d'achat (onglet Personnes) : panneau listant vos autres listes avec leur nombre de personnes, aperçu (`GET /listes/{id}/personnes/import`) avec les personnes cochées par défaut, puis import (`POST`) qui copie nom et budget. Les cadeaux ne sont pas copiés et la copie est indépendante de la source.
- Règle de doublon : une personne dont le nom existe déjà dans la cible (casse et espaces en bordure ignorés) est signalée « déjà présente » et jamais importée. Compte rendu : personnes importées, budget total ajouté, personnes ignorées.
- Contrôles serveur : source et cible doivent appartenir à l'utilisateur (une liste partagée est refusée en 403), source différente de la cible, identifiants relus en base et limités à la source, doublons recalculés à l'import.
- Message de succès `info` dans le fragment de messages.
- Tests : `ImportPersonnesTest` (11) et `ImportPersonnesWebTest` (8). Aucun changement de schéma.

### Changed
- Version du `pom.xml` portée à 0.6.0.

### Fixed
- Aucun.

## [0.5.0] - 2026-11-06

### Added
- Wishlist publique : jeton secret (`SecureRandom`, 128 bits) par wishlist, lien `/p/{jeton}` accessible sans connexion (`SecurityConfig`, `WishlistPubliqueController`), interrupteur, copie, régénération et retour en privé dans l'onglet Réglages, bouton Partager (partage natif ou copie) dans l'onglet Articles.
- Prise par un invité via une popup `<dialog>` (nom obligatoire, nettoyé, 100 caractères, mémorisé dans `localStorage`) ; affichage « nom (invité) » ; un compte connecté prend sous son login sans popup.
- Colonnes `wishlist.jeton_public` (index unique) et `souhait.pris_par_invite` (facultative, nul = faux), ajout validé sur une copie de la base de dev SQLite existante.
- Filtre `EnTetesPubliquesFilter` : `Referrer-Policy`, `X-Robots-Tag` et `Cache-Control: no-store` sur `/p/**`.
- Tests : `WishlistPubliqueTest` (20) et `WishlistPubliqueWebTest` (17).

### Changed
- La confirmation de libération tient compte de l'indicateur d'invité : un invité homonyme d'un compte ne peut pas être libéré par ce compte sans avertissement.
- Version du `pom.xml` portée à 0.5.0.

### Fixed
- Aucun.

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
