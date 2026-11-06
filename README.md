# Lutin - gestion des achats de cadeaux

Application web (PWA installable sur Android) pour gérer des achats de cadeaux en masse : listes d'achat, destinataires avec budget, workflow de statut, tags et partage en lecture seule, ainsi que des wishlists partageables. Multi-utilisateurs.

Stack : Java 25, Spring Boot 4, Spring Security (session cookie + CSRF), Spring Data JPA, Thymeleaf + HTMX. Pas de SPA ni de chaîne de build front.

## Fonctionnalités

### Compte
- **Inscription** avec un login, un mot de passe (8 à 72 caractères) et la **clé d'invitation** (variable `INVITATION_KEY`).
- Le **premier inscrit est administrateur**. L'écran `/admin` affiche la clé d'invitation, réservé à l'administrateur.

### Listes d'achat
La landing page (`/`) propose deux grandes tuiles : **Mes listes d'achat** (`/listes`) et **Mes wishlists** (`/wishlists`, avec le nombre d'avis en attente). Les listes se gèrent depuis `/listes`.

Un utilisateur crée plusieurs listes (ex. « Noël 2026 »). Chaque liste s'ouvre sur quatre onglets :

| Onglet | Contenu |
|---|---|
| **Dashboard** | Budget global, déboursé, reste à payer, cadeaux regroupés par personne (filtre par tag, changement de statut direct) et barre de workflow persistante |
| **Personnes** | Création, modification (nom, budget) et suppression des destinataires. Supprimer une personne supprime ses cadeaux |
| **Partage** | Partage de la liste en lecture seule avec un autre utilisateur choisi dans une liste déroulante |
| **Réglages** | Renommer ou supprimer la liste |

### Cadeaux
- Nom obligatoire, URL facultative, prix facultatif, destinataire, tags.
- Si le nom est vide et qu'une URL est indiquée, le nom est le `<title>` de la page (récupération protégée contre la SSRF : http/https uniquement, adresses internes refusées, redirections revalidées, timeout et taille limitée).
- **Workflow** : À acheter → Commandé → Livré → Emballé → Remis.
- **Masqué** : un lecteur voit tout comme le propriétaire sauf les cadeaux masqués, réduits à une ligne « Cadeau masqué ». Nom, URL, prix, statut, tags et destinataire ne sont jamais envoyés. Pour un lecteur, un cadeau masqué est traité comme s'il n'avait aucun tag : il ne ressort d'aucun filtre.
- **Pour compte de tiers** : le cadeau suit le workflow mais son prix est exclu de tous les totaux.

### Calcul du dashboard
| Indicateur | Règle |
|---|---|
| Budget global | Somme des budgets des personnes |
| Déboursé | Prix des cadeaux au statut Commandé ou plus (hors tiers, prix renseigné) |
| Reste à payer | Prix des cadeaux « À acheter » (hors tiers, prix renseigné) |

Les cadeaux masqués restent comptés dans les totaux. Pour un lecteur, la répartition par statut exclut les cadeaux masqués.

### Liste des cadeaux du dashboard
- Les cadeaux sont regroupés par personne destinataire. Deux personnes portant le même nom restent dans des groupes distincts. Pour un lecteur, tous les cadeaux masqués forment un seul groupe « Cadeaux masqués » placé après les autres, dans l'ordre de création : leur place ne révèle ni leur nom ni leur destinataire.
- Pas de pagination visible : les cadeaux se chargent par lots de 50 au fil du défilement. Un groupe qui s'étend sur deux lots continue sans répéter son en-tête. Si un chargement échoue, un bouton « Réessayer » apparaît et les cadeaux déjà affichés sont conservés.
- Le filtre par tag s'applique à la liste, pas aux compteurs.

### Barre de workflow
Une barre fixée en bas de l'écran affiche le nombre de cadeaux par statut (À acheter, Commandé, Livré, Emballé, Remis). Ces nombres portent sur toute la liste : ils ne dépendent ni du filtre par tag ni des lots chargés. Pour un lecteur, ils excluent les cadeaux masqués.

Quand le propriétaire change le statut d'un cadeau, seule sa ligne et la barre sont mises à jour, sans recharger la liste ni perdre la position de défilement. Un jeton portant le nom du cadeau rejoint alors l'étape cible et les deux compteurs concernés s'animent. Avec la préférence système « réduire les animations », les compteurs changent sans mouvement.

### Partage en lecture seule
Le lecteur voit le dashboard complet de la liste partagée. Toute tentative de modification est refusée (403).

### PWA
Manifeste, icônes et service worker minimal : l'application est installable sur Android (HTTPS requis, sauf `localhost`). Pas de mode hors ligne.

### Wishlists
Section distincte des listes d'achat (entrée « Wishlists » de la barre de navigation, page `/wishlists`) : chacun exprime ses envies et ses proches se coordonnent sans doublon.

- **Wishlist** : nom obligatoire, unique par propriétaire (sans tenir compte de la casse ni des espaces en bordure). La wishlist du propriétaire a trois onglets : **Articles** (uniquement ses articles), **Partage** et **Réglages** (renommer ou supprimer ; la suppression emporte les articles et les partages).
- **Rendu en tuiles** : chaque article est une tuile (initiale sur fond dégradé, nom, site d'origine, prix, boutons Voir / Modifier / Supprimer) et une tuile pointillée permet d'en ajouter. Pour un lecteur, une tuile « prise » est grisée, le nom barré, avec un ruban vert « Pris par bob » ; une tuile libre propose « Je le prends ».
- **Articles** : nom obligatoire, URL (http/https) et prix facultatifs. Si le nom est vide et qu'une URL est indiquée, le titre de la page est utilisé (même protection SSRF que les cadeaux). Articles paginés par 50, dans l'ordre de création.
- **Partage** : le propriétaire choisit un login dans une liste déroulante et peut retirer un partage. Seul le propriétaire voit et gère les partages ; l'administrateur n'a aucun accès particulier aux wishlists des autres.
- **Mes wishlists / Partagées avec moi** : deux sections sur la page `/wishlists`.
- **Article pris** : un lecteur coche « Je le prends » ; son login est alors affiché (« Pris par bob ») à tous les lecteurs. Un seul lecteur peut prendre un article, même en cas de clics simultanés.
- **Aveuglement du propriétaire** : le propriétaire ne voit jamais ce qui est pris, ni par qui, ni les avis. Ces données ne sont pas envoyées par le serveur (gabarit et objets de vue distincts) et ne dépendent pas de l'ordre d'affichage.
- **Décocher** : tout lecteur peut libérer un article. S'il n'est pas celui qui l'a pris, une page d'avertissement nomme le preneur et demande une confirmation avant de libérer.
- **Retrait de partage** : les articles que le lecteur avait pris restent « pris par » son nom ; ses avis pour cette wishlist sont supprimés.

#### Avis aux lecteurs
Quand le propriétaire agit sur un article **pris**, chaque lecteur avec un compte reçoit un avis :

| Action du propriétaire | Avis |
|---|---|
| Modifie le nom, l'URL ou le prix d'un article pris | « modifié » (un seul par article et par lecteur) ; l'article reste « pris par » |
| Supprime un article pris | « supprimé » (remplace les avis « modifié » de l'article) |
| Supprime une wishlist contenant des articles pris | un seul avis `WISHLIST_SUPPRIMEE` avec le nombre d'articles pris |
| Agit sur un article libre ou sur une wishlist sans article pris | aucun avis |

Les avis s'affichent sur `/wishlists` et sur la page de la wishlist concernée ; un compteur apparaît dans la barre de navigation. Le bouton « OK » supprime définitivement l'avis, qui expire de toute façon 90 jours après sa création. Libérer un article supprime ses avis « modifié ».

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
- Le dialecte SQLite n'applique pas les contraintes d'unicité composites (tag nom + liste, partage liste + lecteur, wishlist nom + propriétaire, partage wishlist + lecteur) : elles sont vérifiées par les services et déclarées pour PostgreSQL.
- Un lecteur retiré d'une wishlist ne peut plus libérer les articles qu'il avait pris, et le propriétaire, aveugle, ne le peut pas non plus : ils restent « pris » tant que le partage n'est pas rétabli.
- Les wishlists ne sont pas encore publiques : seul un utilisateur connecté peut les consulter.
- La protection SSRF ne couvre pas le DNS rebinding.
- Avec un seul cadeau masqué, un lecteur peut en déduire le prix : les totaux incluent les masqués et bougent quand le propriétaire change leur statut.
- Pas de mode hors ligne.

## Spécifications
Le dossier `openspec/` fait foi (proposition, conception, spécifications, tâches).
