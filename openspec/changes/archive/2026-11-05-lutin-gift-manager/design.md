## Context

Dépôt vide côté code : le `pom.xml` hérité (Spring Boot 4.1.2, Java 25, JPA, Security, SQLite, JJWT) vient d'un autre projet. L'application cible est une PWA de gestion d'achats de cadeaux, multi-utilisateurs, déployée en Docker avec PostgreSQL derrière un certificat Cloudflare. Le développement local se fait sans Docker avec SQLite. Pas d'outil de migration : le schéma est généré par Hibernate (`ddl-auto`). Maven : `C:\USINE_LOGICIELLE\apache-maven\bin`.

## Goals / Non-Goals

**Goals:**
- Stack 100 % Java + HTML (Thymeleaf + HTMX), sessions cookie, CSRF actif.
- Modèle JPA identique et compatible SQLite et PostgreSQL.
- Droits vérifiés côté serveur (propriétaire / lecteur / admin) ; masquage appliqué avant le rendu.
- PWA installable sur Android.

**Non-Goals:**
- Mode hors ligne, notifications push.
- Gestion multi-administrateurs, suppression de comptes, clés d'invitation à usage unique.
- Outil de migration de base de données.
- Modification de la GitHub Action existante.

## Decisions

**1. Thymeleaf + HTMX (vs SPA).** Une seule stack Java, un seul jar, empreinte mémoire faible. HTMX (fichier statique) sert aux changements de statut et aux fragments sans rechargement. Alternative écartée : SPA Vue/React.

**2. Session cookie + CSRF (vs JWT).** Adapté au rendu serveur ; JJWT est retiré du `pom.xml`. Mots de passe hachés avec BCrypt. Cookie `Secure`, `HttpOnly`, `SameSite=Lax` en `prod`.

**3. Modèle de données.**

```
Utilisateur(id, login unique, passwordHash, admin)
ListeAchat(id, nom, proprietaire_id)
Personne(id, nom, budget, liste_id)
Cadeau(id, nom, url?, prix?, statut, masque, pourTiers, personne_id, liste_id)
Tag(id, nom, liste_id)  -- N-N avec Cadeau
Partage(id, liste_id, lecteur_id)  -- unique (liste, lecteur)
```

Statut : enum stocké en chaîne (`A_ACHETER`, `COMMANDE`, `LIVRE`, `EMBALLE`, `REMIS`). Prix en `BigDecimal` (`NUMERIC`). Pas de types spécifiques à une base. Un `docs/database.puml` décrit le schéma.

**4. Règles de calcul du dashboard.**
- Budget global = somme des budgets des personnes de la liste.
- Déboursé = somme des prix des cadeaux non `pourTiers`, statut ≥ `COMMANDE`, prix renseigné.
- Reste à payer = somme des prix des cadeaux non `pourTiers`, statut `A_ACHETER`, prix renseigné.
- Les cadeaux `pourTiers` suivent le workflow mais sont exclus de tous les totaux.
- Les cadeaux masqués restent comptés dans les totaux, y compris pour un lecteur.

**5. Premier inscrit = admin.** L'inscription se fait dans une transaction : l'admin est attribué si aucun utilisateur n'existe, avec une contrainte garantissant qu'un seul compte reçoit ce rôle (verrou sur l'attribution, vérification dans la même transaction). La clé d'invitation vient de `INVITATION_KEY` ; l'écran admin l'affiche en lecture seule.

**6. Masquage côté serveur.** Un service de projection transforme les cadeaux en vues : pour un lecteur, un cadeau masqué est réduit à son nom, sans url, prix, statut, tags ni destinataire. Les templates n'accèdent jamais à l'entité brute dans le mode lecture seule. Les endpoints d'écriture exigent que l'utilisateur soit propriétaire de la liste.

**7. Titre depuis l'URL.** Récupération côté serveur avec `java.net.http.HttpClient` et extraction du `<title>` par expression régulière (sans Jsoup, `HtmlUtils` de Spring pour les entités). Garde-fous SSRF : schémas `http`/`https` uniquement, refus des IP privées, loopback et link-local (vérifié sur l'adresse résolue, redirections suivies à la main et revalidées), timeout court, taille de réponse limitée. Risque résiduel : une résolution DNS différente entre la vérification et la connexion (DNS rebinding) n'est pas couverte. En cas d'échec, le nom doit être saisi à la main.

**8. Profils.** `application.properties` avec profil par défaut `dev` (SQLite, HTTP) ; `application-dev.properties` et `application-prod.properties`. `prod` : PostgreSQL via variables d'environnement, cookies `Secure`, prise en compte des en-têtes `X-Forwarded-*` de Cloudflare. `ddl-auto=update`.

**9. PWA.** `manifest.webmanifest`, icônes, et un service worker minimal sans cache hors ligne, servis en statique.

**10. IHM.** Inspirée de la maquette : barre latérale (barre de navigation basse sur mobile), cartes KPI colorées, pastilles de statut cliquables, répartition par statut en CSS/SVG rendu côté serveur. Tailwind n'est pas utilisé : CSS maison pour rester sans chaîne de build front.

**11. Documentation API.** Les contrôleurs MVC rendent du HTML ; springdoc n'est ajouté que si des endpoints REST apparaissent, auquel cas `@Tag`, `@Operation` et `@ApiResponse` sont obligatoires.

## Risks / Trade-offs

- [Deux administrateurs créés par inscriptions simultanées] → attribution atomique en transaction, test de concurrence.
- [SSRF via la récupération du titre] → garde-fous de la décision 7, tests unitaires dédiés.
- [Un lecteur peut déduire un cadeau masqué par différence de totaux] → accepté : plusieurs cadeaux masqués empêchent l'identification.
- [Différences SQLite / PostgreSQL (types, `BigDecimal`)] → types standards uniquement, tests de l'accès aux données en SQLite ; validation PostgreSQL via Docker avant livraison.
- [Le dialecte SQLite ignore les contraintes d'unicité composites (tag nom+liste, partage liste+lecteur) et traduit les violations en `JpaSystemException`] → contraintes déclarées pour PostgreSQL, unicité aussi vérifiée dans les services, exceptions interceptées via `DataAccessException`.
- [Clé d'invitation fixe qui fuit] → rotation par changement de la variable d'environnement.
- [Installation PWA impossible sans HTTPS] → HTTPS fourni par Cloudflare en production.
