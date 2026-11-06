## Context

Les wishlists (propriétaire, lecteurs partagés par login) stockent déjà le preneur en texte (`souhait.pris_par_nom`) sans lien vers un compte, et la règle de décochage traite un lecteur non identifié comme devant toujours confirmer (`SouhaitService.confirmationRequise`). Le schéma est généré par Hibernate (`ddl-auto=update`), sans migration, sur des bases existantes (SQLite en dev, PostgreSQL en prod). `SecurityConfig` n'autorise sans connexion que `/login`, `/register`, `/error` et les ressources statiques. En prod, `server.forward-headers-strategy=framework` gère le HTTPS terminé par Cloudflare.

## Goals / Non-Goals

**Goals:**
- Accès par lien secret, sans connexion, à la consultation et à la prise d'articles.
- Révocation immédiate de tous les liens (privée) ou rotation du lien (régénérer).
- Distinguer visuellement et logiquement un nom d'invité d'un login.

**Non-Goals:**
- Notifier les invités (aucun avis).
- Rendre le propriétaire aveugle à sa propre wishlist publique quand il est déconnecté.
- Limiter le débit dans l'application (déléguée à Cloudflare).
- Authentifier ou mémoriser les invités côté serveur.

## Decisions

### Jeton public sur la wishlist
`wishlist.jeton_public varchar(64)` nullable : sa présence signifie « publique ». Généré avec `SecureRandom` (128 bits, base64url sans remplissage). Désactiver met la colonne à nul ; régénérer écrit un nouveau jeton. Une wishlist inexistante, privée ou dont le jeton est révoqué répond de la même façon : 404.
L'unicité passe par un **index unique séparé** (`@Table(indexes = @Index(columnList = "jeton_public", unique = true))`) : SQLite refuse `ADD COLUMN ... UNIQUE` sur une table existante, et plusieurs valeurs nulles restent permises par SQLite comme par PostgreSQL.
Alternative écartée : table séparée de jetons, inutile pour un jeton unique par wishlist.

### Indicateur d'invité
`souhait.pris_par_invite` booléen **nullable**, nul interprété comme faux : un `boolean not null` ajouté à une table existante échoue sous PostgreSQL. L'affichage ajoute « (invité) » au nom. `SouhaitService.confirmationRequise` devient : confirmation exigée si le preneur est un invité, si le visiteur n'est pas identifié ou si son login diffère du preneur. Un invité qui saisit le login d'un compte ne peut donc pas se faire passer pour lui.

### Routes et sécurité
Contrôleur dédié sous `/p/{jeton}` : consultation, prise (`POST`, nom requis pour un invité), libération (`POST` avec confirmation). `SecurityConfig` ajoute `/p/**` à l'accès libre ; le CSRF reste actif (un invité reçoit un jeton lié à une session anonyme). Un filtre ou un intercepteur applique sur `/p/**` : `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex, nofollow`, `Cache-Control: no-store`. Les liens sortants gardent `rel="noopener noreferrer"`.

### Qui voit quoi sur le lien
Un utilisateur connecté propriétaire, ou lecteur partagé, est redirigé vers `/wishlists/{id}` (vue aveugle ou vue lecteur avec avis). Un autre utilisateur connecté voit la vue publique et prend sous son login, sans popup et sans indicateur d'invité. Un visiteur non connecté voit la vue publique et prend après saisie de son nom.

### Vue publique et objet de vue
Troisième gabarit et troisième objet de vue : titre et tuiles, sans login du propriétaire, sans avis, sans onglets, avec un en-tête minimal. Le nom saisi est affiché avec `th:text` (jamais de HTML). Longueur maximale 100 caractères, caractères de contrôle retirés, valeur vide refusée.

### Popup de saisie du nom
Élément `<dialog>` natif ouvert par un court script de `app.js`. Le nom est prérempli depuis `localStorage` et jamais conservé côté serveur. Le formulaire est un vrai `POST` avec le champ CSRF.

### Interface du propriétaire
Onglet Réglages : interrupteur « Rendre publique » (avec un texte indiquant que le propriétaire verra alors l'état « pris » s'il ouvre le lien déconnecté), lien affiché, bouton « Régénérer ». Onglet Articles : bouton Partager visible seulement si la wishlist est publique, qui utilise `navigator.share` quand il existe et copie le lien sinon. Le lien est absolu, construit depuis la requête (les en-têtes `X-Forwarded-*` sont déjà gérés en prod).

### Prise et libération par jeton
Le service résout la wishlist par jeton, puis réutilise les requêtes atomiques existantes `SouhaitRepository.prendre` et `liberer`, sans nouveau contrôle de login. Un `UPDATE` supplémentaire pose le nom et l'indicateur d'invité ensemble pour éviter qu'ils divergent.

## Risks / Trade-offs

- [Le propriétaire déconnecté voit tout via son lien] → limite assumée et affichée sur l'interrupteur et dans le README.
- [Spam par des invités ou prise de tous les articles sous de faux noms] → limitation de débit par Cloudflare, libération possible par tous avec avertissement.
- [Fuite du jeton par Referer, cache ou moteurs de recherche] → `Referrer-Policy`, `noindex`, `no-store`, `noopener noreferrer`.
- [Échec de la mise à jour du schéma sur une base existante] → colonne facultative, index unique séparé ; vérifié sur une copie de la base de dev SQLite (colonnes ajoutées, démarrage sans erreur). SQLite n'applique pas l'index unique, comme les autres contraintes d'unicité du projet : 128 bits aléatoires rendent les collisions négligeables.
- [Un invité qui ouvre ensuite un compte du même nom] → l'indicateur d'invité l'empêche de libérer sans confirmation.
