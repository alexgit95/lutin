## 1. Données et configuration

- [x] 1.1 `Utilisateur` : `password_hash` facultatif, `email_hmac` (64 caractères hexadécimaux, unique, facultative) et `login_cle` (pseudo en minuscules, unique, renseignée à chaque `login`) ; `UtilisateurRepository` : recherche par `email_hmac` et existence par `login_cle` ; mettre à jour `docs/database.puml`
- [x] 1.2 Propriétés : `lutin.acces.mode` (`local` par défaut) et bloc `lutin.cloudflare` (`team-domain`, `audience`, `email-hmac-secret`) dans `LutinProperties` ; `application-prod.properties` active `cloudflare`, lit `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD`, `EMAIL_HMAC_SECRET` sans valeur par défaut et rend `INVITATION_KEY` facultatif ; refus de démarrer si le secret fait moins de 32 caractères ou si le domaine ne se termine pas par `.cloudflareaccess.com`
- [x] 1.3 Ajouter la dépendance `spring-security-oauth2-jose` au `pom.xml`
- [x] 1.4 `docker-compose.yml` : transmettre les trois variables et ne plus exiger `INVITATION_KEY`

## 2. Sécurité en mode cloudflare

- [x] 2.1 Service de hachage : normalisation de l'email (`trim`, minuscules) et HMAC-SHA256 en hexadécimal
- [x] 2.2 Décodeur JWT séparé et remplaçable (clés du team, RS256, émetteur, expiration, audience)
- [x] 2.3 `AccesAuthentication` : `getName()` renvoie le pseudo (chaîne vide avant sa création), rôles `USER` et `ADMIN` selon le compte, rôle `ONBOARDING` seul sans compte, HMAC dans les détails
- [x] 2.4 `CloudflareAccessFilter` : validation à chaque requête, contexte porté par la requête sans enregistrement en session, 401 sans redirection ni détail, authentification facultative sur les chemins publics, aucun journal de l'email, du jeton ou des claims
- [x] 2.5 Chaîne de sécurité `cloudflare` : chemins publics, `/bienvenue` pour `ONBOARDING`, `/admin/**` pour `ADMIN`, le reste pour `USER`, CSRF actif, gestionnaire d'accès refusé qui redirige un compte sans pseudo vers `/bienvenue` (`HX-Redirect` pour HTMX) ; la chaîne actuelle devient la chaîne `local`
- [x] 2.6 Déconnexion : redirection vers `https://<team>/cdn-cgi/access/logout`
- [x] 2.7 `AuthController` (`/login`, `/register`) et `RegistrationService` limités au mode `local`

## 3. Pseudo et premier administrateur

- [x] 3.1 `PseudoService` : règles de forme (4 à 50 caractères, `[A-Za-z0-9._-]`), pseudos réservés sans casse, unicité sans casse (service et `login_cle`), idempotence pour un compte existant, premier compte administrateur avec repli en cas de création concurrente
- [x] 3.2 `BienvenueController` et gabarit `bienvenue.html` épuré (formulaire et déconnexion, sans barre ni information de compte) ; redirection d'un compte existant vers l'accueil
- [x] 3.3 `LayoutAdvice.nombreAvis` renvoie 0 pour un compte sans pseudo
- [x] 3.4 `AdminController` et `admin.html` : clé d'invitation en mode `local`, message neutre en mode `cloudflare`

## 4. Interface

- [x] 4.1 `app.js` : bannière fermable sur `htmx:responseError` (avec le code) et `htmx:sendError` (message de session expirée ou de connexion impossible) ; styles dans `app.css`
- [x] 4.2 Fragment `head` : `crossorigin="use-credentials"` sur le lien du manifest

## 5. Tests

- [x] 5.1 Hachage : normalisation, stabilité, secret différent donne un résultat différent
- [x] 5.2 Filtre et JWT avec une paire de clés RSA générée : jeton valide, absent, expiré, mauvais `aud`, mauvais `iss`, signature falsifiée, sans email, chemin public avec et sans jeton, révocation (jeton retiré après une navigation valide)
- [x] 5.3 Première connexion : redirection vers `/bienvenue`, pseudo valide, trop court, caractères interdits, déjà pris (casse différente), réservé, courses de pseudo et de premier administrateur, idempotence, compte existant sur `/bienvenue`
- [x] 5.4 Premier compte administrateur puis comptes suivants ; `/admin` : 403 pour un non-administrateur, clé affichée en `local`, message neutre en `cloudflare`
- [x] 5.5 Absence de l'email dans les réponses HTML et les journaux, avant et après le choix du pseudo
- [x] 5.6 `/login` et `/register` en 404 en mode `cloudflare`, déconnexion vers Cloudflare, CSRF refusé sans jeton, lien du manifest avec `crossorigin`
- [x] 5.7 Configuration : démarrage refusé sans variable ou avec un secret trop court ; `application-prod.properties` active le mode `cloudflare`
- [x] 5.8 Vérifier que les tests existants (mode `local`) passent sans modification

## 6. Livraison

- [x] 6.1 `README.md` : fonctionnement du mode cloudflare, variables, configuration Cloudflare Access (emails autorisés, politique Bypass pour `/p/*`, `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/css/*`, `/js/*`), hypothèses (email immuable, secret HMAC à conserver), premier administrateur, départ à zéro (suppression du volume PostgreSQL et de `lutin-dev.db`), durcissement facultatif du port
- [x] 6.2 `copilot-instructions.md` : retirer l'exigence d'`INVITATION_KEY` en prod et ajouter les nouvelles variables
- [x] 6.3 `CHANGELOG.md` et version du `pom.xml`
- [x] 6.4 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
