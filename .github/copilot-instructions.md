# Instructions Générales du Projet

## Contexte
- **Application :** PWA de gestion d'achats de cadeaux multi-utilisateurs (listes d'achat, personnes, budgets, workflow de statut, partage en lecture seule). Cadre d'entreprise : respecter l'architecture en place et ne pas régresser sur le déploiement.
- **Backend :** Java 25, Spring Boot 4.x, Spring Security (session cookie + CSRF), Spring Data JPA.
- **Front :** Thymeleaf + HTMX, CSS maison, PWA (manifest + service worker minimal). Pas de SPA, pas de chaîne de build front.
- **Conteneurisation :** Docker (Dockerfile + docker-compose.yml).
- **Spécifications :** le dossier `openspec/` fait foi (changes, specs, design, tasks).

## Profils Spring (deux modes uniquement)
- `dev` (défaut) : développement local, **SQLite**, HTTP, sans Docker.
- `prod` : Docker + **PostgreSQL**, HTTPS terminé par Cloudflare (`X-Forwarded-*`), cookies de session `Secure`. Variables : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `INVITATION_KEY` (obligatoire).
- Configuration : `application.properties` (commun), `application-dev.properties`, `application-prod.properties`.

## CI/CD
- Le déploiement est automatisé par la GitHub Action de `.github/workflows/`. Ne propose aucune modification des pipelines sans demande explicite.

## Base de données
- Aucun outil de migration (pas de Liquibase/Flyway) : le schéma est généré par Hibernate (`ddl-auto`).
- Les entités `@Entity` doivent rester 100 % compatibles **SQLite** et **PostgreSQL** (types et SQL standards uniquement).
- **Modélisation (PlantUML) :** à chaque modification de la structure (entités, colonnes, relations, contraintes), mettre à jour `docs/database.puml`.

## Règles de codage
### 1. Sécurité
- CSRF actif, droits vérifiés côté serveur (propriétaire / lecteur / admin). Les données masquées ne doivent jamais être envoyées au lecteur.
- Toute requête sortante vers une URL saisie par l'utilisateur est protégée contre la SSRF.

### 2. Documentation API
- Si un contrôleur REST est créé ou modifié, ajouter les annotations OpenAPI 3 (`@Tag`, `@Operation`, `@ApiResponse`) avec codes HTTP et DTO. Les contrôleurs MVC rendant du HTML n'y sont pas soumis.

### 3. Tests unitaires obligatoires
- À chaque modification ou ajout de code Java, écrire ou mettre à jour les tests (JUnit 5 + Mockito), avec une couverture complète des nouvelles règles métier.
- Exécuter Maven exclusivement avec le binaire de **`C:\USINE_LOGICIELLE\apache-maven\bin`**.

### 4. Documentation fonctionnelle
- À chaque modification de fonctionnalité, mettre à jour `README.md` à la racine, de façon claire et structurée.

### 5. Journal des modifications
- À chaque tâche de code terminée, mettre à jour `CHANGELOG.md` (format Keep a Changelog : `[Added]`, `[Changed]`, `[Fixed]`) en décrivant l'impact technique et fonctionnel, et incrémenter la version dans le `pom.xml` et le changelog.

### 6. Ressources
- Code sobre en mémoire : pagination JPA, pas de chargement massif de collections, pas de fuite mémoire.
