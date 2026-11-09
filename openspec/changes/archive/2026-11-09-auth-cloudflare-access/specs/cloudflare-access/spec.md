## ADDED Requirements

### Requirement: Mode d'accès
Le système SHALL fonctionner en mode d'accès `local` par défaut et en mode `cloudflare` quand le profil de production est actif. Le mode `cloudflare` SHALL refuser de démarrer si le domaine du team, l'audience ou le secret de hachage des emails n'est pas configuré, ou si le secret fait moins de 32 caractères.

#### Scenario: Profil de production
- **WHEN** le profil `prod` est actif
- **THEN** le mode d'accès est `cloudflare`

#### Scenario: Développement local
- **WHEN** aucun profil de production n'est actif
- **THEN** le mode d'accès est `local` et l'authentification par formulaire reste en vigueur

#### Scenario: Configuration manquante
- **WHEN** le mode `cloudflare` démarre sans `CF_ACCESS_TEAM_DOMAIN`, `CF_ACCESS_AUD` ou `EMAIL_HMAC_SECRET`, ou avec un secret trop court
- **THEN** le démarrage échoue

### Requirement: Validation du jeton Cloudflare Access
En mode `cloudflare`, le système SHALL valider à chaque requête non publique le jeton de l'en-tête `Cf-Access-Jwt-Assertion` : signature par les clés publiques du team, émetteur du team, audience configurée, expiration et présence d'une adresse email. Toute requête sans jeton valide SHALL recevoir un statut 401 sans redirection.

#### Scenario: Jeton valide
- **WHEN** une requête porte un jeton valide pour l'audience configurée
- **THEN** la requête est authentifiée

#### Scenario: Jeton absent
- **WHEN** une requête non publique ne porte pas d'en-tête Cloudflare Access
- **THEN** la réponse est 401

#### Scenario: Jeton expiré
- **WHEN** le jeton est expiré
- **THEN** la réponse est 401

#### Scenario: Mauvaise audience ou mauvais émetteur
- **WHEN** le jeton est destiné à une autre application ou émis par un autre team
- **THEN** la réponse est 401

#### Scenario: Signature falsifiée
- **WHEN** le jeton n'est pas signé par une clé du team
- **THEN** la réponse est 401

#### Scenario: Sans adresse email
- **WHEN** le jeton ne contient pas d'adresse email
- **THEN** la réponse est 401

### Requirement: Révocation immédiate
Le système SHALL authentifier chaque requête à partir du jeton de cette requête et SHALL NOT conserver l'authentification dans la session. Un accès retiré dans Cloudflare SHALL cesser de fonctionner dès la requête suivante.

#### Scenario: Accès retiré
- **WHEN** un utilisateur a navigué avec un jeton valide puis envoie une requête sans jeton valide
- **THEN** la réponse est 401 et aucune donnée n'est servie

### Requirement: Chemins publics
Le système SHALL laisser sans jeton les chemins publics (`/p/**`, ressources statiques, `/manifest.webmanifest`, `/sw.js`, `/error`). Pour ces chemins, un jeton valide SHALL authentifier la requête, et un jeton absent SHALL la laisser anonyme sans erreur.

#### Scenario: Lien public sans jeton
- **WHEN** un visiteur ouvre un lien public sans en-tête Cloudflare
- **THEN** la page est servie en anonyme

#### Scenario: Lien public avec jeton valide
- **WHEN** un utilisateur connu ouvre un lien public avec un jeton valide
- **THEN** la page est servie et l'utilisateur est reconnu

### Requirement: Email conservé uniquement haché
Le système SHALL identifier un compte par le HMAC-SHA256 de l'adresse email normalisée (espaces retirés, minuscules) avec le secret configuré, et SHALL NOT stocker, afficher ni journaliser l'adresse email, le jeton ou leurs claims.

#### Scenario: Normalisation
- **WHEN** la même adresse arrive avec des majuscules ou des espaces en bordure
- **THEN** elle désigne le même compte

#### Scenario: Aucune fuite
- **WHEN** un utilisateur parcourt l'application, avant ou après le choix de son pseudo
- **THEN** aucune réponse HTML ni aucun journal ne contient son adresse email

### Requirement: Pseudo choisi à la première connexion
Le système SHALL demander un pseudo à tout utilisateur authentifié qui n'a pas encore de compte, sur la page `/bienvenue`, et SHALL rediriger vers cette page toute autre page applicative tant que le pseudo n'est pas choisi. Le pseudo SHALL compter 4 à 50 caractères parmi lettres, chiffres, point, tiret et underscore, être unique sans tenir compte de la casse, ne pas figurer parmi les pseudos réservés (`admin`, `administrateur`, `root`, `lutin`, `system`, `support`, sans tenir compte de la casse) et ne SHALL PAS pouvoir être modifié ensuite. Le pseudo SHALL servir d'identifiant (`login`) dans toute l'application.

#### Scenario: Première connexion
- **WHEN** un utilisateur authentifié sans compte ouvre une page applicative
- **THEN** il est redirigé vers `/bienvenue`

#### Scenario: Pseudo valide
- **WHEN** il soumet un pseudo valide et libre
- **THEN** le compte est créé, rattaché à son adresse hachée, et il est redirigé vers l'accueil

#### Scenario: Pseudo trop court ou caractères interdits
- **WHEN** il soumet un pseudo de moins de 4 caractères, de plus de 50 caractères ou avec un caractère non autorisé
- **THEN** le formulaire est réaffiché avec un message et aucun compte n'est créé

#### Scenario: Pseudo déjà pris
- **WHEN** il soumet un pseudo déjà utilisé, quelle que soit la casse
- **THEN** le formulaire est réaffiché avec un message explicite et aucun compte n'est créé

#### Scenario: Pseudo réservé
- **WHEN** il soumet un pseudo réservé, quelle que soit la casse
- **THEN** le pseudo est refusé

#### Scenario: Pseudos simultanés identiques
- **WHEN** deux utilisateurs soumettent en même temps le même pseudo
- **THEN** un seul compte est créé et l'autre reçoit le message « pseudo déjà pris »

#### Scenario: Compte existant sur la page de bienvenue
- **WHEN** un utilisateur qui a déjà un compte ouvre `/bienvenue`
- **THEN** il est redirigé vers l'accueil

#### Scenario: Page de bienvenue épurée
- **WHEN** un utilisateur sans compte voit `/bienvenue`
- **THEN** la page n'affiche que le formulaire et un bouton de déconnexion

### Requirement: Pseudo seul identifiant visible
Le système SHALL n'exposer aux vues, aux partages et aux autres utilisateurs que le pseudo : `Authentication.getName()` SHALL renvoyer le pseudo, ou une chaîne vide avant sa création.

#### Scenario: Nom affiché
- **WHEN** un utilisateur consulte l'application
- **THEN** son nom et celui des propriétaires de listes affichés sont des pseudos

### Requirement: Premier compte administrateur en mode cloudflare
Le système SHALL attribuer le rôle administrateur au premier compte créé en mode `cloudflare`, et à lui seul, y compris en cas de créations simultanées.

#### Scenario: Premier compte
- **WHEN** aucun compte n'existe et qu'un pseudo valide est soumis
- **THEN** le compte créé est administrateur

#### Scenario: Comptes suivants
- **WHEN** un compte existe déjà et qu'un autre pseudo valide est soumis
- **THEN** le nouveau compte n'est pas administrateur

#### Scenario: Créations simultanées
- **WHEN** deux premiers comptes sont créés en même temps
- **THEN** un seul est administrateur

### Requirement: Formulaire de connexion absent en mode cloudflare
En mode `cloudflare`, le système SHALL NOT proposer `/login`, `/register` ni la clé d'invitation, et SHALL répondre 404 à ces chemins.

#### Scenario: Chemins de connexion
- **WHEN** un utilisateur authentifié demande `/login` ou `/register`
- **THEN** la réponse est 404

### Requirement: Déconnexion Cloudflare
En mode `cloudflare`, la déconnexion SHALL vider le contexte de l'application puis rediriger vers `https://<team>/cdn-cgi/access/logout`, avec la protection CSRF.

#### Scenario: Déconnexion
- **WHEN** l'utilisateur soumet la déconnexion
- **THEN** il est redirigé vers l'adresse de déconnexion Cloudflare du team

### Requirement: Erreurs de requêtes HTMX visibles
Le système SHALL afficher une bannière fermable quand une requête HTMX échoue, avec le code de statut s'il existe (par exemple « Session expirée (401) ») ou un message de connexion impossible ou de session expirée quand la requête est interrompue sans statut.

#### Scenario: Réponse en erreur
- **WHEN** une requête HTMX reçoit un statut 4xx ou 5xx
- **THEN** une bannière affiche l'erreur et le code

#### Scenario: Requête interrompue
- **WHEN** une requête HTMX échoue avant d'obtenir une réponse (redirection Cloudflare refusée par le navigateur, réseau coupé)
- **THEN** une bannière invite à recharger la page

### Requirement: Manifest avec identifiants
Le système SHALL déclarer le manifest de la PWA avec `crossorigin="use-credentials"` pour que le navigateur envoie le cookie Cloudflare Access.

#### Scenario: Lien du manifest
- **WHEN** une page est rendue
- **THEN** le lien du manifest porte `crossorigin="use-credentials"`
