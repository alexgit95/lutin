## Why

Il n'existe aucun moyen de sauvegarder ou de restaurer les données de l'application depuis l'application elle-même. L'administrateur doit pouvoir exporter toute la base dans un fichier, puis la restaurer par un import qui remplace tout, sur la même instance ou après une réinstallation.

## What Changes

- L'écran **Admin** gagne une section « Sauvegarde » avec un **export** : un fichier JSON contenant toutes les tables (utilisateurs, listes, personnes, cadeaux, tags, partages, wishlists, souhaits, avis, alertes de remboursement), écrit en flux pour ne pas charger la base en mémoire.
- L'écran Admin gagne un **import** : il **remplace tout** (wipe total, utilisateurs compris) par le contenu d'un fichier exporté, en une seule transaction.
- Avant de toucher à la base, le fichier est **vérifié en entier** : format, version, mode d'accès (`local` ou `cloudflare`), tables et colonnes connues, types, références entre tables. Tout fichier invalide est refusé et la base reste intacte.
- **Garde-fou administrateur** : l'import est refusé si le compte de l'administrateur qui le lance ne figure pas dans le fichier avec le rôle administrateur (empreinte d'email en mode `cloudflare`, login en mode `local`), pour éviter un wipe qui verrouille tout le monde.
- **Confirmation explicite** : il faut saisir « IMPORTER » pour lancer l'import.
- Les **identifiants** sont conservés et les compteurs d'identifiants recalés, donc les adresses `/listes/{id}` et les liens publics `/p/{jeton}` restent valides.
- Un seul import à la fois. Un compte rendu indique le nombre de lignes par table.
- La limite d'envoi de fichier est relevée (100 Mo ; elle est de 1 Mo par défaut).
- L'export contient des données sensibles (empreintes d'emails, hachés de mots de passe en mode local, jetons des liens publics) : il est réservé à l'administrateur, protégé par CSRF, non mis en cache et son contenu n'est jamais journalisé.

## Capabilities

### New Capabilities
- `data-transfer`: export et import complets de la base par l'administrateur, format versionné, validation préalable, remplacement total transactionnel, garde-fous.

### Modified Capabilities
- `user-auth`: l'écran d'administration affiche la section de sauvegarde dans les deux modes, et n'affiche plus le message « aucune fonction d'administration » en mode `cloudflare`.

## Impact

- Code : nouveaux services d'export et d'import et leurs descripteurs de tables, contrôleur d'administration (`POST` d'export et d'import), `AdminController`, gabarit `admin.html`, `app.css`.
- Configuration : `spring.servlet.multipart.max-file-size` et `max-request-size` (100 Mo).
- Données : aucune modification du schéma ni de `docs/database.puml`. Un test garantit que les descripteurs de tables suivent le modèle JPA.
- Sécurité : route réservée au rôle ADMIN, CSRF, `Cache-Control: no-store`, aucune donnée du fichier dans les journaux.
- Tests : export, aller-retour complet, fichiers invalides, garde-fous, droits, import concurrent, les deux modes d'accès.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`.
