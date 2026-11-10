## Context

L'export complet est produit par `ExportService.exporter(OutputStream, String)` et servi par `AdminTransfertController` (`POST /admin/export`, session ou jeton Cloudflare, CSRF). En mode `cloudflare`, `CloudflareAccessFilter` répond 401 à toute requête sans jeton hors `PUBLICS`. Le conteneur publie déjà son port (`2424:8181`), donc un client du LAN joint l'application sans passer par Cloudflare. La table `Tables` décrit chaque entité pour l'export et un test échoue si une entité ou colonne manque. `ImportService` supprime et recharge les tables à partir de `Tables.TOUTES`, et recale les séquences PostgreSQL de façon générique. Les dates sont stockées en `Instant` (horloge UTC) et affichées dans `lutin.fuseau-horaire` (Europe/Paris par défaut).

Décisions déjà prises avec le demandeur : appel depuis le réseau local, sans Cloudflare ni Bypass ; plusieurs clés actives dès le départ ; `api_key` incluse dans l'export (une restauration peut ressusciter une clé révoquée, risque accepté) ; `GET /api/sauvegarde` avec `Authorization: Bearer` ; révocation possible.

## Goals / Non-Goals

**Goals:**
- Permettre à un script du LAN de récupérer la sauvegarde avec une clé révocable, sans session ni Cloudflare.
- Donner à l'administrateur la date de création et de dernière utilisation de chaque clé.
- Ne jamais stocker ni journaliser une clé en clair, ne l'afficher qu'une fois.

**Non-Goals:**
- Restriction par adresse IP, TLS sur le LAN, expiration automatique, limitation de débit.
- Authentifier par clé une autre route que `GET /api/sauvegarde` (pas d'import, pas d'écran).
- Exposer une documentation OpenAPI en ligne (pas de `/v3/api-docs`).

## Decisions

**D1 — Chaîne de sécurité dédiée `/api/**`.** Nouvelle `SecurityApiConfig` (indépendante du mode) avec un bean `SecurityFilterChain` `@Order(1)` limité par `securityMatcher("/api/**")` : sans état (`SessionCreationPolicy.STATELESS`, dépôt de contexte par attribut de requête comme la chaîne cloudflare), CSRF désactivé, formulaire et basic désactivés, `requestMatchers("/api/sauvegarde").hasRole("SAUVEGARDE")` puis `anyRequest().denyAll()`, point d'entrée `HttpStatusEntryPoint(401)` (pas de `sendError`, donc pas de passage par `/error`). Les chaînes existantes, sans `securityMatcher`, ne sont pas modifiées et ne voient jamais `/api/**` : `CloudflareAccessFilter` n'intervient pas et `PUBLICS` n'est pas touché. Alternative écartée : ajouter le chemin à `PUBLICS` et vérifier la clé dans le contrôleur, ce qui mélangerait authentification et traitement et laisserait CSRF et session actifs.

**D2 — Filtre `CleApiFilter`.** `OncePerRequestFilter` placé avant `AuthorizationFilter`. Il lit `Authorization`, n'accepte que le schéma `Bearer` (insensible à la casse) et un jeton d'au plus 128 caractères (borne le coût du hachage), appelle `CleApiService.authentifier(jeton)` et, si une clé correspond, pose une `UsernamePasswordAuthenticationToken(nomDeLaCle, null, ROLE_SAUVEGARDE)`. Sinon il laisse passer sans authentification : le point d'entrée répond 401, identique pour absente, mal formée, inconnue ou révoquée. Une clé valide sur une autre route de l'application n'est jamais lue, car ces routes sont servies par les chaînes existantes.

**D3 — Modèle `api_key`.** Colonnes : `id`, `nom` (varchar 100), `nom_cle` (varchar 100, nom en minuscules sans espaces de bordure, unique), `empreinte` (varchar 64, SHA-256 hexadécimal, unique), `cree_le` (timestamp, obligatoire), `derniere_utilisation` (timestamp, nul tant que jamais utilisée). Aucune clé étrangère. La clé en clair, `lutin_` suivi de 32 octets de `SecureRandom` encodés en base64url sans remplissage, n'est jamais persistée. Une recherche par empreinte unique remplace la comparaison de secrets : avec 256 bits d'entropie, la fuite de timing sur l'empreinte n'est pas exploitable ; un hachage lent n'apporterait rien. Alternative écartée : conserver les 8 premiers caractères pour identifier la clé, inutile puisque le nom suffit.

**D4 — `CleApiService`.** `generer(nom)` nettoie le nom (caractères de contrôle et de format remplacés par une espace, espaces multiples réduits, bornes 1 à 100), refuse un nom existant (`nom_cle`, vérifié avant l'insertion, la contrainte d'unicité servant de filet comme pour les wishlists) et refuse la 21e clé ; il renvoie un enregistrement `CleGeneree(nom, cle)`. `lister()` renvoie des `VueCleApi(id, nom, creeLe, derniereUtilisation)` triés par date de création, avec dates déjà formatées `dd/MM/yyyy HH:mm` dans le fuseau configuré. `revoquer(id)` supprime la ligne ; un identifiant inconnu est ignoré. `authentifier(jeton)` hache, cherche, puis met à jour `derniere_utilisation` par une requête atomique `update ApiKey set derniereUtilisation = :le where id = :id` avec l'horloge de l'application, et renvoie le nom. Le plafond de 20 évite toute pagination de la liste.

**D5 — Endpoint.** `ApiSauvegardeController` (`@RestController`, `GET /api/sauvegarde`, `produces = application/json`) écrit directement dans la réponse comme `AdminTransfertController` (pas de `StreamingResponseBody`, pour rester dans la même requête de sécurité), avec `Cache-Control: no-store` et `Content-Disposition` `lutin-export-AAAA-MM-JJ.json`, et appelle `ExportService.exporter(flux, "clé d'API « nom »")`. Les annotations `@Tag`, `@Operation` et `@ApiResponse` (200 : fichier JSON ; 401 : clé absente ou invalide) décrivent le contrat, comme l'exige la règle du projet. Le nom de la clé est nettoyé à la création, ce qui évite l'injection dans les journaux.

**D6 — Administration.** `AdminController` reçoit deux gestionnaires : `POST /admin/cles-api` (nom) et `POST /admin/cles-api/{id}/revoquer`, sous les règles existantes `/admin/**` (administrateur, CSRF) dans les deux chaînes. Une génération réussie rend directement la vue `admin` avec la clé en clair dans le modèle, sans redirection ni flash, pour qu'elle ne passe ni par l'URL ni par la session ; `Cache-Control: no-store` est posé. Une erreur (nom absent, trop long, déjà pris, plafond) redirige vers `/admin` avec le message en flash, comme les autres actions de la page. La révocation redirige avec un message d'information. `admin.html` gagne, dans la section Sauvegarde, un formulaire de nom, le bandeau « copiez cette clé maintenant », le tableau (nom, créée le, dernière utilisation ou « Jamais utilisée », bouton Révoquer avec `confirm`) et un exemple `curl`. L'avertissement existant sur la sensibilité de l'export s'applique aussi à la clé.

**D7 — Export.** `Tables` déclare `api_key` après `alerte_remboursement` (aucune dépendance) et `Tables.VERSION` passe à 3. La fixture de transfert crée une clé pour que le test « chaque table est non vide » reste vrai. `docs/database.puml` décrit l'entité.

**D8 — Dépendance OpenAPI.** Ajout de `io.swagger.core.v3:swagger-annotations-jakarta` (version explicite, non gérée par la BOM Spring Boot) au `pom.xml`, annotations seulement, sans point d'accès de documentation. Alternative écartée : `springdoc-openapi`, qui exposerait `/v3/api-docs` et une interface, donc une nouvelle surface à protéger.

## Risks / Trade-offs

- [Clé transmise en clair en HTTP sur le LAN] → limite documentée dans le README ; l'usage est réservé à un réseau de confiance.
- [Une restauration peut remettre en service une clé révoquée, car `api_key` est exportée] → risque accepté par le demandeur, signalé dans le README et dans la section de restauration.
- [Aucune règle Cloudflare ne doit ouvrir `/api/sauvegarde` à Internet] → le README précise de ne pas créer de Bypass pour ce chemin ; sans Bypass, Access bloque l'accès public.
- [Export complet donné à qui détient la clé : empreintes d'emails, hachés de mots de passe, jetons de liens publics] → clé longue et aléatoire, révocable, jamais réaffichée ; avertissement affiché à l'administrateur.
- [Pas de limitation de débit : un attaquant du LAN peut multiplier les tentatives] → 401 uniforme et clé de 256 bits rendent la devinette impossible ; le coût d'un hachage SHA-256 est négligeable ; la longueur de l'en-tête est bornée.
- [Écriture en base à chaque appel authentifié] → une ligne mise à jour, volume négligeable pour des sauvegardes.
- [Format d'export en version 3 : un fichier de la version 2 n'est plus importable] → comportement voulu par le format existant ; message « Version de format incompatible ».
- [Ajout d'une dépendance Maven] → annotations seulement ; si le dépôt central est injoignable depuis la station, l'implémentation s'arrête et demande une décision plutôt que d'omettre les annotations.
