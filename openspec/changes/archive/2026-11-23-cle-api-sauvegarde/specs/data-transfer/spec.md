## MODIFIED Requirements

### Requirement: Export de la base
Le système SHALL permettre à l'administrateur d'exporter toute la base dans un fichier JSON téléchargeable, depuis l'écran d'administration, par une requête `POST` protégée par CSRF. Le même contenu SHALL aussi être servi à un porteur de clé d'API de sauvegarde par `GET /api/sauvegarde` (voir la capacité `backup-api`). Le fichier SHALL contenir toutes les lignes des tables utilisateurs, listes, personnes, tags, cadeaux et leurs tags, partages, wishlists, souhaits, partages de wishlists, avis, alertes de remboursement et clés d'API de sauvegarde. Il SHALL être produit en flux, lu par lots, sans charger la base en mémoire, avec `Cache-Control: no-store` et un nom `lutin-export-AAAA-MM-JJ.json`.

#### Scenario: Export complet
- **WHEN** l'administrateur lance l'export
- **THEN** un fichier JSON est téléchargé avec toutes les lignes de toutes les tables, clés d'API comprises

#### Scenario: Export non mis en cache
- **WHEN** l'administrateur lance l'export
- **THEN** la réponse porte `Cache-Control: no-store` et un nom de fichier daté

#### Scenario: Export d'une grande base
- **WHEN** une table compte plus d'un lot de lignes
- **THEN** toutes ses lignes figurent dans le fichier et aucune n'est dupliquée

#### Scenario: Même contenu par l'API
- **WHEN** un porteur de clé appelle `GET /api/sauvegarde`
- **THEN** il reçoit un fichier de même structure que l'export de l'administrateur

### Requirement: Export et import réservés à l'administrateur
Le système SHALL réserver l'export depuis l'écran d'administration et l'import aux administrateurs, avec protection CSRF ; l'export SHALL aussi être accessible par l'endpoint de sauvegarde à un porteur de clé d'API valide, alors que l'import SHALL rester réservé aux administrateurs. Le contenu du fichier SHALL ne jamais être écrit dans les journaux ; seuls l'événement, le pseudo ou le login de l'administrateur (ou le nom de la clé d'API) et les totaux le SHALL.

#### Scenario: Accès refusé
- **WHEN** un utilisateur non administrateur tente l'export ou l'import depuis l'application
- **THEN** la requête est refusée (403)

#### Scenario: CSRF
- **WHEN** une requête d'export ou d'import de l'écran d'administration arrive sans jeton CSRF
- **THEN** elle est refusée (403)

#### Scenario: Import refusé à une clé d'API
- **WHEN** une requête d'import présente une clé d'API de sauvegarde et aucune session administrateur
- **THEN** elle est refusée et la base n'est pas modifiée

#### Scenario: Journaux
- **WHEN** un export ou un import a lieu
- **THEN** les journaux ne contiennent aucune donnée du fichier
