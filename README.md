# Lutin - gestion des achats de cadeaux

Application web (PWA installable sur Android) pour gérer des achats de cadeaux en masse : listes d'achat, destinataires avec budget, workflow de statut, tags et partage en lecture seule, ainsi que des wishlists partageables. Multi-utilisateurs.

Stack : Java 25, Spring Boot 4, Spring Security (session cookie + CSRF), Spring Data JPA, Thymeleaf + HTMX. Pas de SPA ni de chaîne de build front.

## Fonctionnalités

### Compte
- **En prod**, l'authentification est assurée par **Cloudflare Access** : pas de formulaire de connexion, pas d'inscription ni de clé d'invitation. À la **première connexion**, l'utilisateur choisit un **pseudo** (4 à 50 caractères : lettres, chiffres, point, tiret et underscore ; unique sans tenir compte de la casse ; immuable ; `admin`, `administrateur`, `root`, `lutin`, `system` et `support` sont réservés). C'est le seul nom visible dans l'application : l'adresse email n'est jamais affichée ni journalisée, et n'est conservée que sous forme de HMAC-SHA256.
- **En dev local**, rien ne change : inscription avec un login, un mot de passe (8 à 72 caractères) et la **clé d'invitation** (`INVITATION_KEY`, `lutin-dev` par défaut).
- Le **premier compte créé est administrateur** (en prod : le premier à avoir choisi son pseudo). L'écran `/admin` est réservé à l'administrateur ; il affiche la clé d'invitation en dev et propose, dans les deux modes, la sauvegarde et la restauration de toute la base (voir « Sauvegarde et restauration »).

### Listes d'achat
La landing page (`/`) propose deux grandes tuiles : **Mes listes d'achat** (`/listes`) et **Mes wishlists** (`/wishlists`, avec le nombre d'avis en attente). Les listes se gèrent depuis `/listes`.

Un utilisateur crée plusieurs listes (ex. « Noël 2026 »). Chaque liste s'ouvre sur quatre onglets :

| Onglet | Contenu |
|---|---|
| **Dashboard** | Budget global, déboursé, reste à payer, cadeaux regroupés par personne (filtre par tag, changement de statut direct) et barre de workflow persistante |
| **Personnes** | Création, modification (nom, budget) et suppression des destinataires, via un sous-menu `⋯` par personne (+ cadeau, modifier, supprimer). Chaque personne affiche une barre de budget : dépensé, reste à payer et budget. Import des personnes d'une autre de vos listes (voir ci-dessous) |
| **Remboursement** | Alertes « virement en trop », carte « À virer » avec « Tout marquer remboursé », liste des cadeaux remboursables avec leur pastille (voir ci-dessous) |
| **Partage** | Partage de la liste en lecture seule avec un autre utilisateur choisi dans une liste déroulante |
| **Réglages** | Renommer ou supprimer la liste |

### Budget par personne
Dans l'onglet **Personnes**, chaque ligne montre une barre : le **dépensé** en teinte pleine (cadeaux au statut Commandé ou plus), le **reste à payer** en teinte claire (cadeaux « À acheter » avec prix) et le **budget**, avec les trois montants écrits et une description pour les lecteurs d'écran.

- Les règles sont celles du dashboard : cadeaux pour un tiers et cadeaux sans prix exclus, cadeaux masqués comptés. La somme des dépensés des personnes égale le « Déjà déboursé » du dashboard et celle des restes à payer son « Reste à payer ».
- Budget à 0 : texte seul, sans barre. Si le dépensé plus le reste à payer dépasse le budget, la barre s'adapte, un repère marque le budget et la mention « dépassé de X € » apparaît en rouge.
- Le sous-menu `⋯` se referme au clic ailleurs, à l'ouverture d'un autre menu et avec Échap ; la suppression garde sa confirmation.

### Import de personnes depuis une autre liste
Dans l'onglet **Personnes**, si vous possédez au moins une autre liste, le panneau « Importer depuis une autre liste » ouvre un **aperçu** des personnes de la liste choisie, avec leur budget.

- Les personnes sont **cochées par défaut**. Celles dont le nom existe déjà dans la liste courante (sans tenir compte de la casse ni des espaces en bordure) sont grisées « déjà présente » et ne sont jamais importées.
- L'import copie le **nom et le budget** uniquement : les cadeaux ne sont pas copiés, et la copie est indépendante de la source. Le budget global de la liste augmente du budget importé.
- Un compte rendu indique les personnes importées, le budget total ajouté et les personnes ignorées.
- Seules **vos** listes sont concernées, source comme cible. Une liste simplement partagée avec vous ne peut pas servir de source (403).

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

### Remboursement des cadeaux (propriétaire seulement)
Les achats passent par le compte courant, puis un virement de remboursement part du compte d'épargne. L'application suit ce virement sans toucher au Déboursé ni au Reste à payer.

- **Onglet « Remboursement »** (entre Personnes et Partage, propriétaire seulement) : tout le suivi s'y fait ; le dashboard ne montre ni carte, ni alerte, ni pastille, ni filtre de remboursement. Un badge rouge sur l'onglet indique le nombre d'alertes en attente, y compris juste après un changement de statut sur le dashboard.
- **Cadeaux remboursables** : statut Commandé ou plus, prix renseigné, pas « pour un tiers ». L'onglet les liste, les non remboursés d'abord (puis par personne et par nom), par pages de 50. Chacun porte une pastille « € à rembourser » ; un clic la passe à « remboursé le jj/MM » (date du jour), un second clic annule.
- **Carte « À rembourser »** (en haut de l'onglet) : montant restant à virer, montant déjà remboursé, et le bouton « Tout marquer remboursé » avec la date du virement préremplie (aujourd'hui, jamais dans le futur). Le bouton est inactif s'il n'y a rien à virer, et le compte rendu indique le nombre de cadeaux et la somme.
- **Prix verrouillé** : le prix d'un cadeau remboursé n'est plus modifiable (champ en lecture seule) ; décochez d'abord le remboursement.
- **Alertes « virement en trop »** : si un cadeau remboursé retourne à « À acheter », est supprimé, passe « pour un tiers » ou voit sa personne supprimée, la date est effacée et une alerte garde le montant déjà viré. Elle reste affichée dans l'onglet jusqu'à sa fermeture par le propriétaire (les 20 plus récentes, avec le total).
- Un lecteur ne reçoit aucune de ces informations ; les actions sont refusées (403) à tout autre que le propriétaire.
- **Fuseau horaire** : la « date du jour » suit `lutin.fuseau-horaire` (défaut `Europe/Paris`).

### Barre de workflow
Une barre fixée en bas de l'écran affiche le nombre de cadeaux par statut (À acheter, Commandé, Livré, Emballé, Remis). Ces nombres portent sur toute la liste : ils ne dépendent ni du filtre par tag ni des lots chargés. Pour un lecteur, ils excluent les cadeaux masqués.

Quand le propriétaire change le statut d'un cadeau, seule sa ligne et la barre sont mises à jour, sans recharger la liste ni perdre la position de défilement. Un jeton portant le nom du cadeau rejoint alors l'étape cible et les deux compteurs concernés s'animent. Avec la préférence système « réduire les animations », les compteurs changent sans mouvement.

### Partage en lecture seule
Le lecteur voit le dashboard complet de la liste partagée. Toute tentative de modification est refusée (403).

### Sauvegarde et restauration (administrateur)
Section « Sauvegarde » de l'écran `/admin`, réservée à l'administrateur et protégée par CSRF, dans les deux modes d'accès.

- **Export** : un fichier JSON `lutin-export-AAAA-MM-JJ.json` contenant les 12 tables (utilisateurs, listes, personnes, tags, cadeaux et leurs tags, partages, wishlists, souhaits, partages de wishlists, avis, alertes de remboursement), écrit en flux par lots de 500 lignes, sans mise en cache. Il porte un en-tête (format, version, mode d'accès, version de l'application, date).
- **Import = remplacement total** : toutes les données actuelles, utilisateurs compris, sont supprimées puis remplacées par celles du fichier, en **une seule transaction** (une erreur annule tout). Les identifiants sont conservés, donc les adresses `/listes/{id}` et les liens publics `/p/{jeton}` restent valides, et les compteurs d'identifiants sont recalés.
- **Garde-fous** : le fichier est d'abord vérifié en entier sans rien écrire (format, version, mode, tables et colonnes, types, longueurs, références, JSON complet) ; **votre compte administrateur doit figurer dans le fichier** avec le rôle administrateur (empreinte d'email en prod, login en dev) ; il faut saisir « IMPORTER » ; un seul import à la fois (409) ; taille limitée à 100 Mo.
- **Un fichier n'est importable que dans le même mode** (dev ou prod) et la même version de format. En prod, restaurer sur une instance dont `EMAIL_HMAC_SECRET` diffère empêche de reconnaître les comptes autres que le vôtre.
- **Données sensibles** : le fichier contient les empreintes d'emails, les hachés de mots de passe (dev) et les jetons des liens publics. Conservez-le en lieu sûr ; son contenu n'est jamais écrit dans les journaux.
- **Limites** : l'export n'est pas garanti cohérent si quelqu'un écrit en même temps ; la requête de recalage des séquences PostgreSQL est validée par relecture (les tests tournent sur SQLite).

### PWA
Manifeste, icônes et service worker minimal : l'application est installable sur Android (HTTPS requis, sauf `localhost`). Pas de mode hors ligne.

### Wishlists
Section distincte des listes d'achat (entrée « Wishlists » de la barre de navigation, page `/wishlists`) : chacun exprime ses envies et ses proches se coordonnent sans doublon.

- **Wishlist** : nom obligatoire, unique par propriétaire (sans tenir compte de la casse ni des espaces en bordure). La wishlist du propriétaire a trois onglets : **Articles** (uniquement ses articles), **Partage** et **Réglages** (renommer ou supprimer ; la suppression emporte les articles et les partages).
- **Rendu en tuiles** : chaque article est une tuile (initiale sur fond dégradé, nom, site d'origine, prix, un clic sur l'article ouvre son lien dans un nouvel onglet, boutons Copier (copie l'URL) et Modifier ; la suppression se fait depuis l'écran de modification) et une tuile pointillée permet d'en ajouter. Pour un lecteur, une tuile « prise » est grisée, le nom barré, avec un ruban vert « Pris par bob » ; une tuile libre propose « Je le prends ».
- **Articles** : nom obligatoire, URL (http/https) et prix facultatifs. Si le nom est vide et qu'une URL est indiquée, le titre de la page est utilisé (même protection SSRF que les cadeaux). Articles paginés par 50, dans l'ordre de création.
- **Partage** : le propriétaire choisit un login dans une liste déroulante et peut retirer un partage. Seul le propriétaire voit et gère les partages ; l'administrateur n'a aucun accès particulier aux wishlists des autres.
- **Mes wishlists / Partagées avec moi** : deux sections sur la page `/wishlists`.
- **Article pris** : un lecteur coche « Je le prends » ; son login est alors affiché (« Pris par bob ») à tous les lecteurs. Un seul lecteur peut prendre un article, même en cas de clics simultanés.
- **De la part de…** : à côté de « Je le prends », un lecteur partagé peut prendre un article au nom d'un proche sans compte. Une popup demande le nom (obligatoire, 100 caractères maximum, jamais prérempli) ; la tuile affiche « Pris par Marie (via bob) », bob étant le lecteur connecté. Ce bouton n'existe ni sur la page publique ni pour le propriétaire.
- **Aveuglement du propriétaire** : le propriétaire ne voit jamais ce qui est pris, ni par qui, ni les avis. Ces données ne sont pas envoyées par le serveur (gabarit et objets de vue distincts) et ne dépendent pas de l'ordre d'affichage.
- **Décocher** : tout lecteur peut libérer un article. S'il n'est pas celui qui l'a pris, une page d'avertissement nomme le preneur et demande une confirmation avant de libérer. Un article pris « de la part de » quelqu'un se libère sans avertissement, par tout lecteur ou visiteur (invités du lien public compris).
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

#### Wishlist publique
Dans l'onglet **Réglages**, le propriétaire peut rendre sa wishlist publique. Un jeton secret aléatoire (128 bits) donne un lien `/p/{jeton}` qui permet à n'importe qui, sans compte, de consulter la wishlist et de prendre un article. Un bouton **Partager** apparaît alors dans l'onglet Articles (partage natif du téléphone, sinon copie du lien).

- **Régénérer le lien** invalide l'ancien ; **Rendre privée** invalide tous les liens. Les articles déjà pris restent « pris par ». Un lien inconnu ou révoqué renvoie 404.
- **Vue publique** : seulement le titre et les tuiles, sans login du propriétaire ni avis.
- **Invité (non connecté)** : une popup demande son nom (100 caractères maximum, rempli depuis le navigateur la fois suivante) ; l'article est alors « pris par Nom (invité) ». Le nom est affiché comme du texte.
- **Compte connecté** : s'il n'est ni propriétaire ni lecteur partagé, il prend l'article sous son login, sans popup. Le propriétaire et les lecteurs partagés sont redirigés vers leur vue habituelle.
- **Libérer** : tout visiteur le peut. L'avertissement avec confirmation est exigé sauf pour un compte connecté qui libère un article pris sous son propre login (un invité du même nom ne compte pas) et sauf pour un article pris « de la part de » quelqu'un.
- **Protection des pages publiques** : `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex, nofollow`, `Cache-Control: no-store`, CSRF actif. Aucune route d'édition n'existe sous `/p/`.
- **Cloudflare** : l'application ne limite pas le débit. Il est recommandé d'ajouter une règle de limitation de débit sur `/p/*` devant l'application.

## Profils Spring

| Profil | Usage | Base | Particularités |
|---|---|---|---|
| `dev` (défaut) | Développement local | SQLite (`lutin-dev.db`) | HTTP, connexion par formulaire, clé d'invitation `lutin-dev` si `INVITATION_KEY` absent |
| `prod` | Docker | PostgreSQL | HTTPS terminé par Cloudflare (`X-Forwarded-*`), cookies de session `Secure`, authentification par Cloudflare Access (`lutin.acces.mode=cloudflare`) |

### Variables d'environnement

| Variable | Profil | Rôle |
|---|---|---|
| `CF_ACCESS_TEAM_DOMAIN` | `prod` (obligatoire) | Domaine du team Cloudflare Access, par exemple `monteam.cloudflareaccess.com` (sans `https://`) |
| `CF_ACCESS_AUD` | `prod` (obligatoire) | Audience (AUD tag) de l'application Access |
| `EMAIL_HMAC_SECRET` | `prod` (obligatoire) | Secret de hachage des emails, 32 caractères minimum |
| `INVITATION_KEY` | `dev` (facultative) | Clé exigée à l'inscription en dev ; sans effet en prod |
| `DB_URL` | `prod` | URL JDBC PostgreSQL |
| `DB_USERNAME` | `prod` | Utilisateur de la base |
| `DB_PASSWORD` | `prod` | Mot de passe de la base |

### Authentification par Cloudflare Access (prod)
Chaque requête est authentifiée par le jeton de l'en-tête `Cf-Access-Jwt-Assertion` (signature par les clés du team, émetteur, audience et expiration), sans rien garder en session : retirer une adresse dans Cloudflare coupe l'accès à la requête suivante. Sans jeton valide, la réponse est 401.

- **Configuration Cloudflare** : créer une application Access sur le domaine de l'application avec une politique listant les adresses autorisées, puis une politique **Bypass** pour `/p/*` (liens publics), `/manifest.webmanifest`, `/sw.js`, `/icons/*`, `/css/*` et `/js/*`. Relever le domaine du team et l'AUD.
- **Premier accès** : se connecter en premier pour devenir administrateur, après avoir restreint la politique aux adresses voulues.
- **Secret HMAC** : à sauvegarder comme le mot de passe de la base. Le changer ou le perdre empêche de reconnaître les comptes. L'adresse email est supposée immuable ; il n'existe ni changement d'adresse ni rattachement de compte.
- **Déconnexion** : ferme la session de l'application puis redirige vers la déconnexion Cloudflare.
- **Erreurs HTMX** : si la session Access expire, une bannière rouge indique l'erreur et invite à recharger la page.
- **Départ à zéro** : cette version ne reprend aucun compte existant. Supprimer le volume PostgreSQL (`docker compose down -v`) avant le premier démarrage, et `lutin-dev.db` en développement.
- **Port** : le contrôle du jeton protège même si le port publié est joignable hors tunnel ; il peut être restreint à `127.0.0.1` si `cloudflared` tourne sur la même machine.

Le schéma de base est généré par Hibernate (`ddl-auto=update`), sans outil de migration. Le modèle est décrit dans [docs/database.puml](docs/database.puml).

## Lancement

### Développement (sans Docker)
```bash
C:\USINE_LOGICIELLE\apache-maven\bin\mvn spring-boot:run
```
L'application écoute sur http://localhost:8181.

### Production (Docker)
```bash
CF_ACCESS_TEAM_DOMAIN=monteam.cloudflareaccess.com CF_ACCESS_AUD=xxxx EMAIL_HMAC_SECRET=un-secret-de-32-caracteres-minimum DB_PASSWORD=un-mot-de-passe docker compose up -d --build
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
- Quand une wishlist est publique, son propriétaire qui ouvre son propre lien sans être connecté voit l'état « pris » et les noms : l'aveuglement ne tient plus tant que le lien existe. Connecté, il est redirigé vers sa vue aveugle.
- Les invités ne reçoivent pas d'avis lorsque le propriétaire modifie ou supprime un article qu'ils ont pris.
- L'unicité du jeton public est déclarée par un index unique, que SQLite n'applique pas (comme les autres contraintes d'unicité) ; avec 128 bits aléatoires, les collisions sont négligeables.
- La protection SSRF ne couvre pas le DNS rebinding.
- Avec un seul cadeau masqué, un lecteur peut en déduire le prix : les totaux incluent les masqués et bougent quand le propriétaire change leur statut.
- Pas de mode hors ligne.

## Spécifications
Le dossier `openspec/` fait foi (proposition, conception, spécifications, tâches).
