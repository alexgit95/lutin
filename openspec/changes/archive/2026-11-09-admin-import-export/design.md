## Context

La base compte 12 tables : `utilisateur`, `liste_achat`, `personne`, `tag`, `cadeau`, `cadeau_tag` (table de jointure), `partage`, `wishlist`, `souhait`, `wishlist_partage`, `avis`, `alerte_remboursement`. Elle tourne sur SQLite (dev et tests) et PostgreSQL (prod), sans outil de migration. Deux colonnes d'`avis`, `souhait_id` et `wishlist_id`, sont des identifiants sans clé étrangère. Les comptes ont un `login`, un `password_hash` (mode `local` seulement), un `email_hmac` (mode `cloudflare` seulement) et un marqueur d'administrateur unique. L'écran `/admin` est réservé au rôle ADMIN (`/admin/**`).

## Goals / Non-Goals

**Goals:**
- Sauvegarder et restaurer toute la base depuis l'écran Admin.
- Restauration fidèle : mêmes identifiants, mêmes liens, mêmes comptes.
- Aucune restauration partielle ni verrouillage de l'administrateur.
- Consommation mémoire bornée quelle que soit la taille de la base.

**Non-Goals:**
- Fusion ou import partiel, import sélectif d'une liste.
- Export automatique planifié, chiffrement, compression du fichier.
- Migration entre les modes `local` et `cloudflare` : un fichier n'est importable que dans le même mode.
- Export cohérent pendant des écritures concurrentes (voir Risques).

## Decisions

### Format
Un seul objet JSON écrit en flux : `{ "format": "lutin-export", "version": 1, "mode": "local|cloudflare", "versionApplication": "...", "exporteLe": "...", "tables": { "utilisateur": [...], "liste_achat": [...], ... } }`. Les tables y figurent dans l'ordre des clés étrangères (parents avant enfants), chaque ligne est un objet dont les clés sont les noms de colonnes. Dates, instants et décimaux sont des chaînes ISO ou décimales, les identifiants des nombres. `version` est un entier de format, incrémenté à chaque changement du schéma exporté ; l'import refuse une version différente.
Alternatives écartées : dump SQL natif (non portable entre SQLite et PostgreSQL) ; CSV par table (plusieurs fichiers et types perdus).

### Descripteurs de tables
Un descripteur explicite par table (nom, colonnes, type logique : entier, texte, booléen, décimal, date, instant, et références) pilote l'export, la validation et l'import. Un test compare les descripteurs au modèle JPA (métamodèle Hibernate) : une colonne ajoutée à une entité sans descripteur fait échouer le test. Cela évite qu'un export oublie silencieusement des données après une évolution du schéma.

### Export en flux
`POST /admin/export` (CSRF) écrit directement dans le flux de la réponse : Jackson écrit les tables une à une, chaque table lue par lots de 500 lignes triés par identifiant (pagination par décalage), avec libération du contexte de persistance entre les lots. En-têtes : `Content-Disposition: attachment; filename="lutin-export-AAAA-MM-JJ.json"`, `Cache-Control: no-store`. La mémoire reste bornée par la taille d'un lot.

### Import en deux passes
Le fichier envoyé (stocké sur disque par Spring au-delà d'un seuil, jamais lu en entier en mémoire) est parcouru deux fois par un parseur en flux.
1. **Validation**, sans écriture : entête (format, version, mode), tables dans l'ordre attendu, colonnes connues et obligatoires, types, longueurs, JSON complet (un fichier tronqué échoue ici), références entre tables résolues à partir des identifiants collectés (un ensemble d'identifiants par table), unicité des identifiants, garde-fou du compte administrateur.
2. **Import**, dans une transaction unique : suppression des 12 tables dans l'ordre inverse des clés étrangères, puis insertion par lots dans l'ordre du fichier, avec les identifiants du fichier, puis recalage des compteurs d'identifiants. Toute erreur annule tout, y compris la suppression.

### Insertion avec identifiants conservés
Insertions SQL natives paramétrées (colonne `id` comprise) liées à des types Java (Long, String, Boolean, BigDecimal, LocalDate, Instant), pour que les liens `/listes/{id}` et les références sans clé étrangère d'`avis` restent valides. Recalage : sur PostgreSQL `setval(pg_get_serial_sequence(table, 'id'), max(id))` pour chaque table ; sur SQLite le compteur suit les identifiants explicites. Un test insère une ligne après l'import et vérifie que son identifiant dépasse le maximum importé.
Alternative écartée : insertion par entités avec nouveaux identifiants et table de correspondance ; elle dépend moins du dialecte mais change tous les identifiants et oblige à remapper à la main les références sans clé étrangère.
Le spike sur SQLite est concluant (aller-retour complet, types et compteur d'identifiants vérifiés par `TransfertTest`) : la lecture passe par des projections JPQL typées, lues comme Hibernate les lit, et l'écriture par des insertions natives dont les paramètres sont liés par type Java. La requête de recalage PostgreSQL n'est validée que par relecture, faute de PostgreSQL dans les tests.

### Garde-fou du compte administrateur
Le compte de l'administrateur qui importe doit figurer dans le fichier avec le rôle administrateur : en mode `cloudflare`, une ligne `utilisateur` dont `email_hmac` égale l'empreinte de l'authentification courante et dont le marqueur d'administrateur est présent ; en mode `local`, une ligne dont le `login` égale celui de l'authentification courante, administrateur. Sinon l'import est refusé avant toute écriture, avec un message clair.

### Confirmation et exclusion
Le champ de confirmation doit valoir exactement « IMPORTER » (vérifié côté serveur). Un verrou applicatif refuse un second import simultané (409). Le formulaire est soumis par `POST` multipart avec CSRF.

### Mode et authentification
Le fichier porte le mode d'accès de l'instance qui l'a produit ; un fichier d'un autre mode est refusé. En mode `cloudflare`, l'authentification étant recalculée à chaque requête à partir du jeton, l'administrateur reste connecté après l'import tant que son compte figure dans le fichier ; en mode `local`, sa session reste valide pour la même raison.

### Compte rendu et journal
Après l'import, l'administrateur revient sur l'écran Admin avec le nombre de lignes par table. Le journal ne garde que l'événement, le pseudo de l'administrateur et les totaux, jamais le contenu.

### Limite d'envoi
`spring.servlet.multipart.max-file-size` et `max-request-size` à 100 Mo (le défaut est de 1 Mo). Un dépassement renvoie un message d'erreur lisible.

## Risks / Trade-offs

- [Wipe qui verrouille l'administrateur] → garde-fou du compte administrateur dans le fichier et validation avant toute écriture.
- [Fichier corrompu ou malveillant] → validation complète en lecture seule, types et longueurs contrôlés, références vérifiées, transaction unique.
- [Données sensibles dans le fichier] → réservé à l'administrateur, CSRF, `no-store`, aucun contenu journalisé, avertissement affiché sur l'écran.
- [Export incohérent pendant des écritures concurrentes] → accepté : application peu sollicitée, isolation non imposée (SQLite n'accepte pas `REPEATABLE_READ`) ; l'écran invite à exporter en dehors des périodes d'usage.
- [Import long qui bloque les écritures] → transaction unique par lots, verrou d'exclusion, taille limitée à 100 Mo.
- [Schéma qui évolue sans mise à jour de l'export] → numéro de version de format et test de conformité des descripteurs au métamodèle.
- [SQL natif non portable] → spike initial, repli sur les entités, types liés côté Java.
- [Secret HMAC différent à la restauration] → les empreintes importées ne correspondent plus ; le garde-fou refuse l'import si l'administrateur ne s'y retrouve pas, mais d'autres comptes pourraient ne plus être reconnus : documenté dans le README.

## Open Questions

- Aucune : le spike a conclu à l'insertion native.
