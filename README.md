# Lutin - gestion des achats de cadeaux

Application web (PWA installable sur Android) pour gérer des achats de cadeaux en masse : listes d'achat, destinataires avec budget, workflow de statut, tags et partage en lecture seule. Multi-utilisateurs.

Stack : Java 25, Spring Boot 4, Spring Security (session cookie + CSRF), Spring Data JPA, Thymeleaf + HTMX. Pas de SPA ni de chaîne de build front.

## Fonctionnalités

### Compte
- **Inscription** avec un login, un mot de passe (8 à 72 caractères) et la **clé d'invitation** (variable `INVITATION_KEY`).
- Le **premier inscrit est administrateur**. L'écran `/admin` affiche la clé d'invitation, réservé à l'administrateur.

### Listes d'achat
Un utilisateur crée plusieurs listes (ex. « Noël 2026 »). Chaque liste s'ouvre sur quatre onglets :

| Onglet | Contenu |
|---|---|
| **Dashboard** | Budget global, déboursé, reste à payer, répartition par statut, liste des cadeaux (filtre par tag, changement de statut direct) |
| **Personnes** | Création, modification (nom, budget) et suppression des destinataires. Supprimer une personne supprime ses cadeaux |
| **Partage** | Partage de la liste en lecture seule avec un autre utilisateur choisi dans une liste déroulante |
| **Réglages** | Renommer ou supprimer la liste |

### Cadeaux
- Nom obligatoire, URL facultative, prix facultatif, destinataire, tags.
- Si le nom est vide et qu'une URL est indiquée, le nom est le `<title>` de la page (récupération protégée contre la SSRF : http/https uniquement, adresses internes refusées, redirections revalidées, timeout et taille limitée).
- **Workflow** : À acheter → Commandé → Livré → Emballé → Remis.
- **Masqué** : un lecteur voit seulement une ligne avec le nom du cadeau. URL, prix, statut, tags et destinataire ne sont jamais envoyés.
- **Pour compte de tiers** : le cadeau suit le workflow mais son prix est exclu de tous les totaux.

### Calcul du dashboard
| Indicateur | Règle |
|---|---|
| Budget global | Somme des budgets des personnes |
| Déboursé | Prix des cadeaux au statut Commandé ou plus (hors tiers, prix renseigné) |
| Reste à payer | Prix des cadeaux « À acheter » (hors tiers, prix renseigné) |

Les cadeaux masqués restent comptés dans les totaux. Pour un lecteur, la répartition par statut exclut les cadeaux masqués.

### Partage en lecture seule
Le lecteur voit le dashboard complet de la liste partagée. Toute tentative de modification est refusée (403).

### PWA
Manifeste, icônes et service worker minimal : l'application est installable sur Android (HTTPS requis, sauf `localhost`). Pas de mode hors ligne.

## Profils Spring

| Profil | Usage | Base | Particularités |
|---|---|---|---|
| `dev` (défaut) | Développement local | SQLite (`lutin-dev.db`) | HTTP, clé d'invitation `lutin-dev` si `INVITATION_KEY` absent |
| `prod` | Docker | PostgreSQL | HTTPS terminé par Cloudflare (`X-Forwarded-*`), cookies de session `Secure` |

### Variables d'environnement

| Variable | Profil | Rôle |
|---|---|---|
| `INVITATION_KEY` | `prod` (obligatoire), `dev` (facultative) | Clé exigée à l'inscription |
| `DB_URL` | `prod` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `prod` | Utilisateur de la base |
| `DB_PASSWORD` | `prod` | Mot de passe de la base |

Le schéma de base est généré par Hibernate (`ddl-auto=update`), sans outil de migration. Le modèle est décrit dans [docs/database.puml](docs/database.puml).

## Lancement

### Développement (sans Docker)
```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn spring-boot:run
```
L'application écoute sur http://localhost:8181.

### Production (Docker)
```bash
INVITATION_KEY=une-cle-secrete DB_PASSWORD=un-mot-de-passe docker compose up -d --build
```
Le port publié est `APP_PORT` (2424 par défaut).

## Tests
```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn test
```
Les tests utilisent SQLite (profil `test`).

## Limites connues
- Le dialecte SQLite n'applique pas les contraintes d'unicité composites (tag nom + liste, partage liste + lecteur) : elles sont vérifiées par les services et déclarées pour PostgreSQL.
- La protection SSRF ne couvre pas le DNS rebinding.
- Pas de mode hors ligne.

## Spécifications
Le dossier `openspec/` fait foi (proposition, conception, spécifications, tâches).
