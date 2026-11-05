# PWA Shell

## Purpose
Make the application installable and specify its local and production runtime profiles.

## Requirements

### Requirement: Application installable
Le système SHALL exposer un manifeste web, des icônes et un service worker minimal rendant l'application installable sur Android.

#### Scenario: Manifeste
- **WHEN** le navigateur charge l'application en HTTPS
- **THEN** le manifeste avec nom, icônes, `display: standalone` et couleur de thème est disponible

#### Scenario: Service worker
- **WHEN** l'application est chargée
- **THEN** un service worker est enregistré sans mise en cache hors ligne des données

### Requirement: Profils d'exécution
Le système SHALL fournir un profil `dev` (SQLite, HTTP, local) et un profil `prod` (PostgreSQL, Docker, cookies sécurisés, en-têtes `X-Forwarded-*` pris en compte).

#### Scenario: Démarrage local
- **WHEN** l'application démarre sans profil explicite
- **THEN** le profil `dev` est actif avec une base SQLite locale

#### Scenario: Production
- **WHEN** le profil `prod` est actif
- **THEN** l'application utilise PostgreSQL via variables d'environnement et marque les cookies de session `Secure`
