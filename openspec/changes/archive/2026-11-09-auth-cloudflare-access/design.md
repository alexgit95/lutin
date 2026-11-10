## Context

L'authentification actuelle est un formulaire login et mot de passe avec session Spring, une inscription par clé d'invitation (`RegistrationService`, `AuthController`) et un premier inscrit administrateur (`admin_marker` unique). `Principal.getName()` est la clé de propriété dans toute l'application, et le layout affiche `utilisateurCourant`. `WishlistPubliqueController` lit l'`Authentication` de façon facultative sur `/p/**`.

En production, l'application tourne derrière Cloudflare Tunnel et les emails autorisés sont gérés dans Cloudflare Access. Access ajoute à chaque requête protégée l'en-tête `Cf-Access-Jwt-Assertion` (JWT RS256 signé, clés publiques sur `https://<team>.cloudflareaccess.com/cdn-cgi/access/certs`).

Contraintes : pas d'outil de migration (Hibernate `update`, qui n'assouplit jamais un `NOT NULL`) ; entités compatibles SQLite et PostgreSQL ; SQLite ne fait pas respecter certains index uniques, donc les services vérifient aussi ; départ à zéro décidé, sans reprise des comptes existants.

## Goals / Non-Goals

**Goals:**
- En production, Cloudflare Access est la seule porte d'entrée et sa révocation s'applique immédiatement.
- L'email n'apparaît ni dans l'application, ni dans les journaux, ni en clair en base.
- Le reste du code continue de ne manipuler qu'un nom court (le pseudo) comme identité.
- En développement et en test, le comportement actuel est inchangé.

**Non-Goals:**
- Changement d'email, rattachement d'un ancien compte, récupération de compte : l'email est supposé immuable et le secret HMAC conservé.
- Changer de pseudo après coup, supprimer un compte, fonctions d'administration autres que l'écran existant.
- Jetons de service Cloudflare, mise en cache du résultat de validation d'un JWT, limitation de débit.
- Modifier le pipeline GitHub ou le mappage de port de `docker-compose.yml`.

## Decisions

### Mode choisi par propriété, activé par le profil `prod`
`lutin.acces.mode` vaut `local` par défaut et `cloudflare` dans `application-prod.properties`. Les deux chaînes de sécurité, le filtre Access et les contrôleurs propres à chaque mode sont portés par `@ConditionalOnProperty` plutôt que par `@Profile("prod")`. Les tests du mode Cloudflare tournent ainsi sur la base de test SQLite avec `@TestPropertySource`, sans PostgreSQL. Un test lit `application-prod.properties` pour garantir que `prod` active bien le mode `cloudflare`.
Alternative écartée : `@Profile("prod")` directement, impossible à tester sans activer toute la configuration PostgreSQL.

### Authentification à chaque requête, rien dans la session
Le filtre `CloudflareAccessFilter` valide le JWT à chaque requête, place l'`Authentication` dans le contexte de la requête et ne l'enregistre pas en session (`RequestAttributeSecurityContextRepository`). La session ne sert qu'au jeton CSRF et aux messages flash. Retirer un email dans Cloudflare coupe donc l'accès à la requête suivante, sans session à invalider. Le coût est une validation de signature locale (clés en cache) et une recherche indexée par requête.
Alternative écartée : conserver l'authentification en session, qui survivrait à la révocation.

### Validation du JWT
`NimbusJwtDecoder.withJwkSetUri("https://<team>/cdn-cgi/access/certs")` en RS256, avec les validateurs : émetteur `https://<team>`, expiration, audience contenant l'AUD configuré. Le décodeur est un bean séparé que les tests remplacent par un décodeur à clé locale. Les clés sont mises en cache et rafraîchies par Nimbus ; si Cloudflare est injoignable au démarrage, l'application démarre et refuse l'accès tant que les clés manquent. Le domaine du team doit se terminer par `.cloudflareaccess.com`. La dépendance ajoutée est `spring-security-oauth2-jose`.
Le contrôle de l'audience est indispensable : un JWT d'une autre application Access du même team est signé par les mêmes clés.

### Comportement du filtre selon le chemin
- Chemins publics (`/p/**`, statiques, `/error`) : authentification **facultative**. Sans en-tête, la requête reste anonyme ; avec un JWT valide, l'utilisateur est authentifié (la page publique lit l'`Authentication`). Jamais de 401.
- Autres chemins : en-tête absent, JWT invalide ou sans claim `email` donne **401** avec un court texte, sans redirection ni page d'erreur détaillée. Le détail n'est ni écrit dans la réponse ni journalisé avec l'email.

### Identité : email haché, pseudo = `login`
`utilisateur` gagne `email_hmac` (64 caractères hexadécimaux, unique, facultative), `login_cle` (pseudo en minuscules, unique, renseignée par l'entité à chaque `login`) et `password_hash` devient facultatif. L'email est normalisé (`trim`, minuscules) puis haché en HMAC-SHA256 avec `lutin.cloudflare.email-hmac-secret` (32 caractères minimum, sinon démarrage refusé). `login_cle` renforce côté PostgreSQL l'unicité sans casse ; le service vérifie aussi, car SQLite peut ignorer l'index.
Alternatives écartées : `sub` du JWT (stabilité entre fournisseurs d'identité non vérifiée) ; email en clair en base ; élargir `login` (impossible sans migration).

### `Authentication` du mode Cloudflare
Une classe `AccesAuthentication` porte le pseudo comme principal, donc `getName()` renvoie toujours le pseudo, jamais l'email. Avant la création du compte, `getName()` renvoie une chaîne vide, le seul rôle est `ROLE_ONBOARDING`, et le HMAC est conservé dans les détails. Le rôle `USER` (et `ADMIN` si besoin) n'est attribué qu'aux comptes existants. `LayoutAdvice.nombreAvis` renvoie 0 pour un compte sans pseudo.

### Première connexion
`BienvenueController` (`GET` et `POST /bienvenue`, rôle `ONBOARDING` seulement) et `PseudoService`. Les règles : 4 à 50 caractères, `[A-Za-z0-9._-]`, refus d'un pseudo déjà pris sans tenir compte de la casse (vérification du service puis contrainte `login_cle`, avec message clair pour le perdant d'une course), liste réservée `admin`, `administrateur`, `root`, `lutin`, `system`, `support` comparée sans casse. Un compte déjà créé pour ce HMAC rend l'envoi idempotent (redirection vers l'accueil). Le `AccessDeniedHandler` redirige un compte sans pseudo vers `/bienvenue` (en-tête `HX-Redirect` pour HTMX) et un compte existant qui ouvre `/bienvenue` vers l'accueil.
La page `/bienvenue` n'affiche ni la barre habituelle ni aucune information de compte, seulement le formulaire et un bouton de déconnexion.

### Premier compte administrateur
`PseudoService` reprend la logique de `RegistrationService` : administrateur si `count() == 0`, avec repli non administrateur si l'unicité d'`admin_marker` est violée par une création concurrente. L'administrateur est donc le premier qui **termine** le choix du pseudo.

### Contrôleurs et écrans selon le mode
`AuthController` (`/login`, `/register`) n'existe qu'en mode `local`, donc 404 en `cloudflare`. `AdminController` existe dans les deux modes et reste réservé au rôle ADMIN ; il passe au gabarit un indicateur de mode. `admin.html` affiche la clé d'invitation en `local` et un message neutre en `cloudflare` (« Aucune fonction d'administration disponible pour l'instant »). En `cloudflare`, `RegistrationService` n'est pas utilisé et `INVITATION_KEY` est facultatif.

### Déconnexion
En `cloudflare`, `POST /logout` (CSRF conservé) vide le contexte puis redirige vers `https://<team>/cdn-cgi/access/logout`.

### Erreurs HTMX visibles
`app.js` écoute `htmx:responseError` et `htmx:sendError` et affiche une bannière fermable : le code de statut quand il existe (par exemple « Session expirée (401), rechargez la page »), sinon « Connexion impossible ou session expirée, rechargez la page » (la redirection cross-origin de Cloudflare apparaît comme une erreur d'envoi sans statut). Le comportement vaut pour tous les modes.

### Manifest et PWA
Le `<link rel="manifest">` du fragment `head` reçoit `crossorigin="use-credentials"` pour que le navigateur envoie le cookie Access. La politique Bypass de Cloudflare couvre `/p/*`, `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/css/*` et `/js/*` (configuration hors dépôt, documentée dans le README).

### Configuration
`application-prod.properties` : `lutin.acces.mode=cloudflare`, `lutin.cloudflare.team-domain=${CF_ACCESS_TEAM_DOMAIN}`, `lutin.cloudflare.audience=${CF_ACCESS_AUD}`, `lutin.cloudflare.email-hmac-secret=${EMAIL_HMAC_SECRET}`, `lutin.invitation-key=${INVITATION_KEY:}`. Les trois premières n'ont pas de valeur par défaut : le démarrage échoue si elles manquent. `docker-compose.yml` transmet ces variables et ne force plus `INVITATION_KEY`.

## Risks / Trade-offs

- [Secret HMAC changé ou perdu : plus personne n'est reconnu, chacun repasse par le choix d'un pseudo et le premier arrivé devient administrateur] → secret obligatoire de 32 caractères minimum, sauvegardé comme le mot de passe de la base, avertissement dans le README.
- [Politique Access trop large au premier accès : un visiteur indésirable devient administrateur] → restreindre la politique avant de se connecter et se connecter le premier.
- [Cloudflare injoignable ou clés en rotation] → clés en cache avec rafraîchissement, démarrage non bloquant, 401 tant qu'aucune clé n'est disponible.
- [Fuite de l'email] → principal limité au pseudo, pas d'email en claim exposé aux vues, aucun journal de claims, tests sur les réponses et les journaux.
- [Port 2424 joignable hors tunnel] → le JWT est exigé et vérifié (signature, émetteur, audience), un en-tête falsifié est refusé ; durcissement facultatif du mappage de port documenté.
- [Une requête HTMX échoue en silence après expiration de session Access] → bannière d'erreur visible.
- [Départ à zéro] → opération manuelle et destructive : arrêt, suppression du volume PostgreSQL, suppression de `lutin-dev.db`.
- [Mode `local` actif en production par oubli de profil] → `application-prod.properties` active le mode `cloudflare` et un test le vérifie.

## Migration Plan

1. Créer l'application Access dans Cloudflare (emails autorisés, politique Bypass pour les chemins publics) et noter le domaine du team et l'AUD.
2. Définir `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` et `EMAIL_HMAC_SECRET` sur le serveur ; sauvegarder le secret.
3. Arrêter l'application et supprimer le volume PostgreSQL (décision : aucune reprise de données). Pour la dev, supprimer `lutin-dev.db`.
4. Déployer par la GitHub Action existante, puis se connecter en premier pour devenir administrateur et choisir son pseudo.
5. Retour arrière : redéployer l'ancienne image avec une sauvegarde de l'ancienne base ; les nouvelles données ne sont pas reprises.
