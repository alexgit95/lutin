## ADDED Requirements

### Requirement: Gestion des clés d'API de sauvegarde
Le système SHALL permettre à l'administrateur, depuis l'écran d'administration et dans les deux modes d'accès, de générer des clés d'API de sauvegarde, de les lister et de les révoquer. Plusieurs clés SHALL pouvoir être actives en même temps, dans la limite de 20. Chaque clé SHALL porter un nom obligatoire, nettoyé des caractères de contrôle, limité à 100 caractères et unique sans tenir compte de la casse ni des espaces en bordure. La clé SHALL être aléatoire (32 octets) et préfixée par `lutin_`. Le système SHALL ne conserver que son empreinte SHA-256 et SHALL afficher la clé en clair une seule fois, dans la réponse à la génération, sans la placer en session, dans une redirection ni dans les journaux. La liste SHALL montrer pour chaque clé son nom, sa date de création et sa date de dernière utilisation, ou « Jamais utilisée ». La génération et la révocation SHALL être réservées aux administrateurs et protégées par CSRF ; la révocation SHALL demander une confirmation dans le navigateur et SHALL rendre la clé immédiatement inutilisable.

#### Scenario: Génération
- **WHEN** l'administrateur saisit le nom « nas-backup » et valide
- **THEN** une clé est créée, sa valeur complète est affichée une seule fois avec l'avertissement de la copier, et la liste montre « nas-backup » avec sa date de création et « Jamais utilisée »

#### Scenario: Clé jamais réaffichée
- **WHEN** l'administrateur recharge l'écran d'administration après la génération
- **THEN** la clé en clair n'apparaît plus nulle part et seule l'empreinte est conservée en base

#### Scenario: Plusieurs clés actives
- **WHEN** l'administrateur génère une deuxième clé sous un autre nom
- **THEN** les deux clés figurent dans la liste et les deux authentifient l'endpoint de sauvegarde

#### Scenario: Nom absent ou trop long
- **WHEN** l'administrateur valide un nom vide, composé uniquement de blancs ou de plus de 100 caractères
- **THEN** la génération est refusée avec un message d'erreur et aucune clé n'est créée

#### Scenario: Nom déjà utilisé
- **WHEN** l'administrateur génère une clé dont le nom, sans tenir compte de la casse ni des espaces en bordure, est celui d'une clé existante
- **THEN** la génération est refusée avec un message d'erreur et aucune clé n'est créée

#### Scenario: Plafond atteint
- **WHEN** 20 clés existent et l'administrateur en génère une autre
- **THEN** la génération est refusée avec un message indiquant le nombre maximal de clés

#### Scenario: Révocation
- **WHEN** l'administrateur révoque une clé après confirmation
- **THEN** la clé disparaît de la liste et un appel qui la présente est refusé (401)

#### Scenario: Accès réservé et CSRF
- **WHEN** un utilisateur non administrateur ou une requête sans jeton CSRF tente de générer ou de révoquer une clé
- **THEN** la requête est refusée (403) et aucune clé n'est créée ni supprimée

### Requirement: Endpoint de sauvegarde par clé d'API
Le système SHALL exposer `GET /api/sauvegarde`, authentifié uniquement par l'en-tête `Authorization: Bearer <clé>`. Avec une clé valide, la réponse SHALL être le fichier JSON de l'export de la base, produit en flux par le même service que l'export de l'administrateur, avec `Cache-Control: no-store`, le type `application/json` et un nom de fichier `lutin-export-AAAA-MM-JJ.json`. Une clé absente, mal formée, inconnue ou révoquée SHALL provoquer une réponse 401 identique, sans indication de la cause et sans donnée. Le endpoint SHALL ne dépendre ni d'une session, ni d'un jeton CSRF, ni du jeton Cloudflare Access, dans les deux modes d'accès, et SHALL ne créer aucune session. Une clé SHALL ne donner accès qu'à cet endpoint : aucune autre route de l'application, y compris l'import, ne SHALL l'accepter.

#### Scenario: Sauvegarde avec une clé valide
- **WHEN** un client appelle `GET /api/sauvegarde` avec `Authorization: Bearer` suivi d'une clé valide
- **THEN** la réponse est 200 avec le fichier d'export complet, `Cache-Control: no-store` et un nom de fichier daté

#### Scenario: Clé absente
- **WHEN** un client appelle l'endpoint sans en-tête `Authorization`
- **THEN** la réponse est 401 sans donnée

#### Scenario: Clé inconnue, mal formée ou révoquée
- **WHEN** un client appelle l'endpoint avec une clé inconnue, avec un schéma autre que `Bearer` ou avec une clé révoquée
- **THEN** la réponse est 401, identique dans les trois cas, sans donnée

#### Scenario: Sans session ni CSRF
- **WHEN** un client appelle l'endpoint avec une clé valide, sans cookie ni jeton CSRF
- **THEN** la sauvegarde est servie et aucun cookie de session n'est émis

#### Scenario: Sans Cloudflare Access
- **WHEN** l'application fonctionne en mode cloudflare et qu'un client du réseau local appelle l'endpoint sans en-tête `Cf-Access-Jwt-Assertion`
- **THEN** la sauvegarde est servie dès que la clé est valide

#### Scenario: Portée limitée à l'endpoint
- **WHEN** un client présente une clé valide sur `POST /admin/import`, `GET /admin` ou toute autre route de l'application
- **THEN** la requête n'est pas authentifiée par la clé et est refusée

#### Scenario: Méthode non autorisée
- **WHEN** un client appelle `POST /api/sauvegarde` avec une clé valide
- **THEN** la sauvegarde n'est pas servie et la réponse est un refus (405)

### Requirement: Dernière utilisation d'une clé
Le système SHALL enregistrer la date de la dernière authentification réussie de chaque clé et l'afficher à l'administrateur. Un appel refusé SHALL ne modifier la date d'aucune clé. La date de création SHALL ne jamais changer.

#### Scenario: Première utilisation
- **WHEN** une clé jamais utilisée authentifie un appel de sauvegarde
- **THEN** sa date de dernière utilisation est renseignée et l'écran n'affiche plus « Jamais utilisée »

#### Scenario: Utilisations successives
- **WHEN** la même clé authentifie un second appel plus tard
- **THEN** sa date de dernière utilisation est celle du second appel et sa date de création est inchangée

#### Scenario: Utilisation d'une seule clé
- **WHEN** une clé authentifie un appel alors qu'une autre clé existe
- **THEN** seule la date de la clé utilisée est modifiée

#### Scenario: Appel refusé
- **WHEN** un appel est refusé (401)
- **THEN** aucune date de dernière utilisation n'est modifiée

### Requirement: Confidentialité des clés d'API
Le système SHALL ne jamais écrire une clé en clair, ni son empreinte, ni le contenu de la sauvegarde dans les journaux. Une sauvegarde servie par une clé SHALL être journalisée avec le nom de la clé et les totaux de lignes uniquement. Les clés d'API SHALL figurer dans l'export de la base sous forme d'empreinte, avec leur nom et leurs dates.

#### Scenario: Journal d'une sauvegarde
- **WHEN** une sauvegarde est servie par la clé « nas-backup »
- **THEN** le journal mentionne « nas-backup » et le total de lignes, sans clé, sans empreinte et sans donnée de la base

#### Scenario: Journal d'un refus
- **WHEN** un appel est refusé
- **THEN** le journal ne contient pas la valeur présentée dans l'en-tête `Authorization`

#### Scenario: Clés dans l'export
- **WHEN** l'administrateur ou un porteur de clé exporte la base
- **THEN** le fichier contient la table des clés d'API avec empreinte, nom, date de création et date de dernière utilisation, et jamais une clé en clair
