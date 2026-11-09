# Data Transfer

## Purpose
Let the administrator export the whole database to a versioned JSON file and restore it by a total, validated, transactional replacement.

## Requirements

### Requirement: Export de la base
Le système SHALL permettre à l'administrateur d'exporter toute la base dans un fichier JSON téléchargeable, depuis l'écran d'administration, par une requête `POST` protégée par CSRF. Le fichier SHALL contenir toutes les lignes des tables utilisateurs, listes, personnes, tags, cadeaux et leurs tags, partages, wishlists, souhaits, partages de wishlists, avis et alertes de remboursement. Il SHALL être produit en flux, lu par lots, sans charger la base en mémoire, avec `Cache-Control: no-store` et un nom `lutin-export-AAAA-MM-JJ.json`.

#### Scenario: Export complet
- **WHEN** l'administrateur lance l'export
- **THEN** un fichier JSON est téléchargé avec toutes les lignes de toutes les tables

#### Scenario: Export non mis en cache
- **WHEN** l'administrateur lance l'export
- **THEN** la réponse porte `Cache-Control: no-store` et un nom de fichier daté

#### Scenario: Export d'une grande base
- **WHEN** une table compte plus d'un lot de lignes
- **THEN** toutes ses lignes figurent dans le fichier et aucune n'est dupliquée

### Requirement: Format versionné de l'export
Le fichier SHALL être un objet JSON portant `format` (`lutin-export`), `version` (entier de format), `mode` (`local` ou `cloudflare`), la version de l'application et la date d'export, puis les tables dans l'ordre des clés étrangères, chaque ligne ayant pour clés les noms de colonnes. Le numéro de version SHALL changer à chaque modification du schéma exporté. Un test SHALL échouer si une colonne d'entité manque dans les descripteurs de l'export.

#### Scenario: En-tête
- **WHEN** l'administrateur ouvre un fichier exporté
- **THEN** il y trouve le format, la version, le mode, la version de l'application et la date

#### Scenario: Ordre des tables
- **WHEN** le fichier est lu dans l'ordre
- **THEN** chaque table apparaît après les tables qu'elle référence

#### Scenario: Conformité au modèle
- **WHEN** une colonne est ajoutée à une entité sans mettre à jour les descripteurs
- **THEN** le test de conformité échoue

### Requirement: Import par remplacement total
Le système SHALL permettre à l'administrateur d'importer un fichier exporté, ce qui remplace tout le contenu de la base, utilisateurs compris, en une seule transaction : en cas d'erreur, la base SHALL rester dans son état d'origine. Les identifiants du fichier SHALL être conservés et les compteurs d'identifiants SHALL être recalés pour que les créations suivantes ne les réutilisent pas. Les jetons des liens publics, les hachés de mots de passe et les empreintes d'emails SHALL être restaurés à l'identique.

#### Scenario: Restauration complète
- **WHEN** l'administrateur importe un fichier valide
- **THEN** la base contient exactement les lignes du fichier et plus aucune ancienne ligne

#### Scenario: Aller-retour
- **WHEN** l'administrateur exporte puis réimporte le fichier produit
- **THEN** toutes les tables retrouvent les mêmes lignes, valeurs et identifiants

#### Scenario: Compteurs d'identifiants
- **WHEN** un enregistrement est créé après un import
- **THEN** son identifiant est supérieur à tous les identifiants importés de sa table

#### Scenario: Liens conservés
- **WHEN** une wishlist était publique avant l'export
- **THEN** son lien `/p/{jeton}` fonctionne toujours après l'import

#### Scenario: Échec en cours d'import
- **WHEN** une erreur survient pendant l'écriture
- **THEN** la transaction est annulée et la base retrouve son état d'origine

### Requirement: Validation avant écrasement
Le système SHALL vérifier le fichier en entier avant de modifier la base : format, version, mode d'accès, tables et colonnes connues, colonnes obligatoires, types, longueurs, JSON complet, unicité des identifiants et existence des lignes référencées. Tout fichier invalide SHALL être refusé avec un message d'erreur et la base SHALL rester inchangée.

#### Scenario: Version incompatible
- **WHEN** le fichier porte une autre version de format
- **THEN** l'import est refusé avec un message et la base est inchangée

#### Scenario: Mode différent
- **WHEN** le fichier a été exporté en mode `local` et l'instance est en mode `cloudflare`, ou l'inverse
- **THEN** l'import est refusé et la base est inchangée

#### Scenario: Fichier tronqué ou illisible
- **WHEN** le fichier n'est pas un JSON complet
- **THEN** l'import est refusé et la base est inchangée

#### Scenario: Table ou colonne inconnue
- **WHEN** le fichier contient une table ou une colonne inconnue
- **THEN** l'import est refusé

#### Scenario: Référence manquante
- **WHEN** une ligne référence un identifiant absent de la table parente
- **THEN** l'import est refusé

#### Scenario: Type ou valeur invalide
- **WHEN** une valeur n'a pas le type ou la longueur attendus
- **THEN** l'import est refusé

### Requirement: Garde-fou du compte administrateur
Le système SHALL refuser l'import si le compte de l'administrateur qui le lance ne figure pas dans le fichier avec le rôle administrateur. En mode `cloudflare`, la correspondance se fait sur l'empreinte de l'email ; en mode `local`, sur le login. Le refus SHALL intervenir avant toute modification et indiquer la cause.

#### Scenario: Administrateur présent
- **WHEN** le fichier contient le compte de l'administrateur courant avec le rôle administrateur
- **THEN** l'import est accepté

#### Scenario: Administrateur absent
- **WHEN** le fichier ne contient pas le compte de l'administrateur courant
- **THEN** l'import est refusé avec le message « Ce fichier ne contient pas votre compte administrateur » et la base est inchangée

#### Scenario: Compte présent sans rôle administrateur
- **WHEN** le fichier contient le compte courant sans le rôle administrateur
- **THEN** l'import est refusé

### Requirement: Confirmation explicite de l'import
Le système SHALL exiger la saisie exacte du mot « IMPORTER » pour lancer un import, vérifiée côté serveur.

#### Scenario: Confirmation correcte
- **WHEN** l'administrateur saisit « IMPORTER » et envoie un fichier valide
- **THEN** l'import est lancé

#### Scenario: Confirmation absente ou incorrecte
- **WHEN** la confirmation est vide ou différente de « IMPORTER »
- **THEN** l'import est refusé et la base est inchangée

### Requirement: Un seul import à la fois
Le système SHALL refuser un import lancé pendant qu'un autre est en cours.

#### Scenario: Import concurrent
- **WHEN** un second import est lancé pendant le premier
- **THEN** il est refusé (409) et le premier se poursuit

### Requirement: Limite de taille et compte rendu
Le système SHALL accepter des fichiers jusqu'à 100 Mo et SHALL refuser un fichier plus gros avec un message lisible. Après un import réussi, il SHALL afficher le nombre de lignes importées par table.

#### Scenario: Fichier trop gros
- **WHEN** l'administrateur envoie un fichier de plus de 100 Mo
- **THEN** l'import est refusé avec un message de taille et la base est inchangée

#### Scenario: Compte rendu
- **WHEN** l'import réussit
- **THEN** l'écran d'administration indique le nombre de lignes importées par table

### Requirement: Export et import réservés à l'administrateur
Le système SHALL réserver l'export et l'import aux administrateurs, avec protection CSRF. Le contenu du fichier SHALL ne jamais être écrit dans les journaux ; seuls l'événement, le pseudo ou le login de l'administrateur et les totaux le SHALL.

#### Scenario: Accès refusé
- **WHEN** un utilisateur non administrateur tente l'export ou l'import
- **THEN** la requête est refusée (403)

#### Scenario: CSRF
- **WHEN** une requête d'export ou d'import arrive sans jeton CSRF
- **THEN** elle est refusée (403)

#### Scenario: Journaux
- **WHEN** un export ou un import a lieu
- **THEN** les journaux ne contiennent aucune donnée du fichier
