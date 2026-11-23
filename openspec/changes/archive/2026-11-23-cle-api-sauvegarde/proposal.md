## Why

La sauvegarde de la base n'est possible que depuis l'écran d'administration, avec une session et un jeton CSRF : impossible de l'automatiser (cron, NAS, script) depuis le réseau local. Il faut un accès machine, sans compte ni Cloudflare, protégé par un secret que l'administrateur peut créer, suivre et révoquer.

## What Changes

- L'administrateur génère, depuis l'écran d'administration, des **clés d'API de sauvegarde** nommées (plusieurs actives en même temps, 20 au maximum). La clé en clair n'est affichée qu'une fois ; seule son empreinte SHA-256 est conservée.
- L'écran liste chaque clé avec son nom, sa **date de création** et sa **date de dernière utilisation** (« Jamais utilisée » tant qu'elle n'a pas servi), et permet de la **révoquer**.
- Nouvel endpoint `GET /api/sauvegarde` authentifié uniquement par `Authorization: Bearer <clé>`, qui renvoie le même fichier JSON que l'export de l'administrateur. Il ne dépend ni de la session, ni du CSRF, ni de Cloudflare Access : il est destiné au réseau local (port publié du conteneur), sans règle Bypass Cloudflare.
- Une clé ne donne accès qu'à cet endpoint, jamais à l'import ni au reste de l'application.
- Nouvelle table `api_key` (id, nom, nom_cle, empreinte, cree_le, derniere_utilisation), **incluse dans l'export** : `Tables.VERSION` passe à 3 et une restauration peut remettre en service une clé révoquée depuis (risque accepté, à documenter).
- **BREAKING** : un fichier exporté en version 2 n'est plus importable (même mécanisme de version que pour les évolutions précédentes).
- Les annotations OpenAPI 3 (`@Tag`, `@Operation`, `@ApiResponse`) documentent le contrôleur ; une dépendance d'annotations OpenAPI est ajoutée au `pom.xml`.
- Hors périmètre : restriction par adresse IP, TLS sur le réseau local, expiration automatique des clés, endpoint d'import par clé.

## Capabilities

### New Capabilities
- `backup-api`: gestion des clés d'API de sauvegarde par l'administrateur (création, liste avec dates, révocation) et endpoint de sauvegarde `GET /api/sauvegarde` authentifié par clé.

### Modified Capabilities
- `data-transfer`: l'export contient aussi la table des clés d'API et reste produit par le même service, ouvert à l'API ; l'export est réservé aux administrateurs et aux porteurs d'une clé valide, l'import aux administrateurs seuls.
- `user-auth`: l'écran d'administration propose en plus la gestion des clés d'API de sauvegarde, dans les deux modes.

## Impact

- Domaine / dépôt : nouvelle entité `ApiKey` et `ApiKeyRepository`.
- Services : `CleApiService` (génération, validation du nom, plafond, empreinte, révocation, mise à jour de la dernière utilisation, vue de liste).
- Sécurité : nouvelle chaîne `SecurityFilterChain` prioritaire limitée à `/api/**`, sans état, sans CSRF, avec un filtre d'authentification par clé ; les chaînes locale et cloudflare existantes ne changent pas.
- Web : `ApiSauvegardeController` (REST, OpenAPI), nouveaux gestionnaires de génération et de révocation dans `AdminController` (MVC HTML), section dans `admin.html`.
- Transfert : `Tables.java` (table `api_key`, `VERSION` = 3), `docs/database.puml`.
- Compatibilité SQLite et PostgreSQL : types standards, contrainte d'unicité sur `empreinte` et `nom_cle`, schéma créé par Hibernate (`ddl-auto`).
- Sécurité : droits vérifiés côté serveur, CSRF conservé pour l'écran d'administration, clé jamais journalisée ni réaffichée, réponse 401 uniforme, aucune requête sortante.
- Documentation : `README.md` (usage, limite HTTP sur le LAN, absence de Bypass Cloudflare, effet d'une restauration), `CHANGELOG.md`, version `pom.xml` (0.14.0).
