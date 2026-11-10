## 1. Modèle et dépôt

- [x] 1.1 Créer l'entité `ApiKey` (`api_key` : `id`, `nom` 100, `nom_cle` 100 unique, `empreinte` 64 unique, `cree_le` obligatoire, `derniere_utilisation` nullable) avec `ApiKeyRepository` (`findByEmpreinte`, `existsByNomCle`, `count`, mise à jour atomique de `derniereUtilisation`)
- [x] 1.2 Mettre à jour `docs/database.puml` avec l'entité `api_key`

## 2. Service

- [x] 2.1 Créer `CleApiService.generer(nom)` : nom nettoyé et borné, unicité sans casse, plafond de 20, clé `lutin_` + 32 octets `SecureRandom` en base64url, empreinte SHA-256 seule conservée, renvoi de `CleGeneree(nom, cle)`
- [x] 2.2 Ajouter `lister()` (`VueCleApi` triée par création, dates `dd/MM/yyyy HH:mm` dans `lutin.fuseau-horaire`) et `revoquer(id)` (suppression, identifiant inconnu ignoré)
- [x] 2.3 Ajouter `authentifier(jeton)` : jeton borné à 128 caractères, recherche par empreinte, mise à jour de `derniere_utilisation` avec l'horloge de l'application, renvoi du nom de la clé ; aucune modification en cas d'échec

## 3. Sécurité et endpoint

- [x] 3.1 Créer `CleApiFilter` (schéma `Bearer`, jeton borné, authentification `ROLE_SAUVEGARDE` portant le nom de la clé, sans état)
- [x] 3.2 Créer `SecurityApiConfig` : chaîne `@Order(1)` limitée à `/api/**`, sans session ni CSRF, `/api/sauvegarde` réservé à `ROLE_SAUVEGARDE`, tout le reste refusé, point d'entrée 401 ; vérifier que les chaînes locale et cloudflare ne sont pas modifiées
- [x] 3.3 Ajouter la dépendance `io.swagger.core.v3:swagger-annotations-jakarta` (version explicite) au `pom.xml`
- [x] 3.4 Créer `ApiSauvegardeController` (`GET /api/sauvegarde`, `Cache-Control: no-store`, nom de fichier daté, `ExportService.exporter` avec le nom de la clé comme libellé de journal) avec `@Tag`, `@Operation` et `@ApiResponse` (200, 401)

## 4. Administration

- [x] 4.1 Ajouter à `AdminController` `POST /admin/cles-api` (génération rendue directement dans la vue `admin` avec `no-store`, erreurs en flash) et `POST /admin/cles-api/{id}/revoquer` (redirection avec message)
- [x] 4.2 Étendre `admin.html` : formulaire du nom, bandeau « copiez cette clé maintenant », tableau (nom, créée le, dernière utilisation ou « Jamais utilisée », bouton Révoquer avec confirmation), exemple `curl`, avertissement de sensibilité

## 5. Export et import

- [x] 5.1 Ajouter la table `api_key` à `Tables.TOUTES` et passer `Tables.VERSION` à 3
- [x] 5.2 Ajouter une clé au jeu de données `BaseTransfertTest` et adapter les tests de transfert (version 3, compteurs, aller-retour des clés, refus d'un fichier de version 2)

## 6. Tests

- [x] 6.1 Tests de `CleApiService` : génération (préfixe, aléa, empreinte seule en base), nom absent / trop long / doublon sans casse / plafond, liste avec « jamais utilisée », révocation, authentification valide ou refusée, dates de dernière utilisation (première, successives, une seule clé, refus sans effet)
- [x] 6.2 Tests web de l'endpoint : 200 avec clé valide (JSON, `no-store`, nom daté), 401 identique pour absente / mal formée / inconnue / révoquée, aucun cookie de session, aucun besoin de CSRF, 405 en `POST`, clé refusée sur `/admin/import`, `/admin` et les autres routes
- [x] 6.3 Tests en mode cloudflare : appel sans `Cf-Access-Jwt-Assertion` servi avec une clé valide, aucune régression sur les chemins publics et les autres routes
- [x] 6.4 Tests web de l'administration : génération affichant la clé une seule fois, absente après rechargement, révocation, accès non administrateur et absence de CSRF refusés (403), liste des dates
- [x] 6.5 Vérifier que les journaux ne contiennent ni clé, ni empreinte, ni valeur de l'en-tête `Authorization`
- [x] 6.6 Mettre à jour `TablesConformiteTest` si nécessaire et lancer la suite complète avec Maven (`C:\USINE_LOGICIELLE\apache-maven\bin`)

## 7. Documentation et version

- [x] 7.1 Mettre à jour `README.md` : génération et révocation des clés, exemple `curl` sur le LAN, limite HTTP en clair, interdiction de créer un Bypass Cloudflare pour `/api/sauvegarde`, effet d'une restauration sur les clés, export à 13 tables
- [x] 7.2 Ajouter l'entrée 0.14.0 dans `CHANGELOG.md` ([Added], [Changed]) et passer la version à 0.14.0 dans `pom.xml`
