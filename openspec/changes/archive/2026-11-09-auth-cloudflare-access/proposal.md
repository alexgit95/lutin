## Why

L'application est exposée par Cloudflare Tunnel et les adresses autorisées sont déjà gérées côté Cloudflare Access. Maintenir en plus des comptes à mot de passe et une clé d'invitation fait doublon et expose un second point d'entrée. En production, Cloudflare doit être la seule porte d'entrée, sans que l'adresse email d'un utilisateur apparaisse dans l'application. En développement local, le formulaire actuel reste utile.

## What Changes

- En profil **`prod`**, l'identité vient de **Cloudflare Access** : l'en-tête `Cf-Access-Jwt-Assertion` est validé à **chaque requête** (signature par la JWKS du team, émetteur, audience, expiration). Un jeton absent, expiré ou invalide donne un refus et ferme la session de l'application, donc retirer un email côté Cloudflare prend effet immédiatement.
- À la **première connexion**, l'utilisateur choisit un **pseudo** sur une page `/bienvenue` : 4 à 50 caractères parmi lettres, chiffres, point, tiret et underscore, unique sans tenir compte de la casse, immuable, avec une liste de pseudos réservés (`admin`, `administrateur`, `root`, `lutin`, `system`, `support`). Tant qu'il n'est pas choisi, toutes les autres pages redirigent vers `/bienvenue`.
- Le pseudo est la valeur de `login` : tout le reste de l'application (propriété, partages, affichages « liste de ... ») continue de ne voir que ce nom court.
- L'email n'est **jamais affiché ni journalisé** et n'est conservé que sous forme de **HMAC-SHA256** (email normalisé : espaces retirés, minuscules) avec un secret serveur.
- Le **premier compte créé** en production est **administrateur**, avec la garantie d'unicité actuelle.
- En production, `/login`, `/register` et la clé d'invitation disparaissent (404). L'écran **`/admin` est conservé**, réservé au rôle administrateur, avec un message neutre à la place de la clé.
- La déconnexion ferme la session de l'application puis redirige vers la déconnexion Cloudflare.
- Une requête HTMX qui échoue (session Access expirée, erreur réseau) affiche une **erreur visible** au lieu de rester silencieuse.
- En profils **`dev` et `test`**, rien ne change : formulaire de connexion, inscription par clé d'invitation, premier inscrit administrateur.
- Départ **à zéro** : aucune reprise des comptes existants. **BREAKING** : les bases de prod (PostgreSQL) et de dev (SQLite) doivent être recréées.
- Le secret HMAC est critique : le changer ou le perdre empêche de reconnaître les comptes. L'email est supposé immuable.

## Capabilities

### New Capabilities
- `cloudflare-access`: validation du jeton Cloudflare Access par requête, rattachement d'un email haché à un compte, choix du pseudo à la première connexion, pseudos réservés, premier compte administrateur, déconnexion, erreurs HTMX visibles, absence de fuite de l'email.

### Modified Capabilities
- `user-auth`: l'inscription par clé d'invitation, le premier inscrit administrateur, l'écran d'administration et la connexion par session ne s'appliquent plus qu'hors production ; en production l'écran d'administration reste accessible au seul administrateur sans clé d'invitation.

## Impact

- Données : `utilisateur.password_hash` devient facultatif, nouvelles colonnes `email_hmac` (64 caractères hexadécimaux, unique, facultative) et `login_cle` (pseudo en minuscules, unique) ; mise à jour de `docs/database.puml`. Compatible SQLite et PostgreSQL, sur base neuve.
- Code : `SecurityConfig` (deux chaînes selon le profil), nouveau filtre Access et décodeur JWT, `Utilisateur`, `UtilisateurRepository`, `RegistrationService`, nouveau service de pseudo, `AdminController`, `AuthController`, `LayoutAdvice`, gabarits (`bienvenue.html`, `admin.html`, layout), script de gestion des erreurs HTMX, `app.css`.
- Dépendance : décodage et validation JWT par Nimbus (`spring-security-oauth2-jose`).
- Configuration prod : `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` et `EMAIL_HMAC_SECRET` obligatoires (démarrage refusé sinon), `INVITATION_KEY` n'est plus exigé (`application-prod.properties`, `docker-compose.yml`). Le mappage de port et le pipeline GitHub ne changent pas.
- Cloudflare (hors dépôt) : politique Access sur les emails autorisés, politique Bypass pour `/p/*`, `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/css/*` et `/js/*`, et `crossorigin="use-credentials"` sur le manifest.
- Réseau : l'application doit joindre `https://<team>.cloudflareaccess.com/cdn-cgi/access/certs` ; les clés sont mises en cache et rafraîchies.
- Tests : validation du JWT (valide, expiré, mauvais `aud` ou `iss`, signature falsifiée, absent), première connexion, pseudos, premier administrateur, révocation, absence de l'email dans les réponses et les journaux.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`, `copilot-instructions.md` (l'exigence `INVITATION_KEY` en prod).
