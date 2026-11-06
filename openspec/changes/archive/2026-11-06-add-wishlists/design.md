## Context

Les listes d'achat (`ListeAchat`, `Partage`, `AccesListeService`) donnent au lecteur un accès en lecture seule et au propriétaire une vue complète. Les wishlists inversent cette logique : le lecteur écrit un seul état (« pris »), et le propriétaire est aveugle à cet état. Le schéma est généré par Hibernate (`ddl-auto=update`), doit rester compatible SQLite et PostgreSQL, et SQLite n'applique pas les contraintes composites (vérifications faites dans les services, comme dans `PartageService`).

## Goals / Non-Goals

**Goals:**
- Aggrégat wishlist indépendant des listes d'achat.
- Aveuglement du propriétaire garanti côté serveur.
- Avis de modification et de suppression pour les lecteurs connectés.
- Schéma prêt pour une future wishlist publique avec lecteurs invités.

**Non-Goals:**
- Wishlist publique, invités, suppression de comptes.
- Lien automatique avec les listes d'achat.
- Notification de prise ou de décochage.

## Decisions

### Entités dédiées
`Wishlist(id, nom, nom_cle, proprietaire_id)`, `Souhait(id, nom, url, prix, pris_par_nom, pris_le, wishlist_id)`, `WishlistPartage(id, wishlist_id, lecteur_id)`, `Avis(id, destinataire_id, type, souhait_id, wishlist_id, wishlist_nom, article_nom, pris_par_nom, nombre, cree_le)`.
Alternative écartée : réutiliser `ListeAchat` et `Partage` avec un drapeau, qui aurait semé des conditions « wishlist » dans tout le code de droits.

### Preneur en texte, sans clé vers `utilisateur`
`pris_par_nom` (login copié à la prise) suffit : tout lecteur peut décocher, l'identité ne sert pas aux droits. Un compte supprimé ou un invité n'exige aucune réécriture.
Alternative écartée : `pris_par_id` nullable, rendu inutile par la règle « tout le monde peut décocher ».

### Prise et décochage atomiques
Requêtes `@Modifying` conditionnelles : `UPDATE souhait SET pris_par_nom=:n, pris_le=:t WHERE id=:id AND wishlist_id=:w AND pris_par_nom IS NULL` pour cocher, `... IS NOT NULL` pour décocher. Le nombre de lignes modifiées dit qui a gagné ; en cas d'échec le nom du preneur actuel est relu pour le message. Valable sur SQLite et PostgreSQL, sans verrou optimiste.

### Avertissement de décochage imposé par le serveur
Le décochage est un POST avec un paramètre `confirme`. Si le login courant égale `pris_par_nom`, l'article est libéré. Sinon (autre preneur ou lecteur non identifié) et sans `confirme=true`, la réponse est une page d'avertissement nommant le preneur avec un bouton qui repose la requête avec confirmation. Pas de dépendance à un `confirm()` JavaScript ni à HTMX : les actions sont des formulaires classiques suivis d'une redirection.
Risque connu : un futur invité qui saisirait le login d'un compte serait considéré comme ce compte ; l'identification de l'invité devra être traitée avec la wishlist publique.

### Table d'avis sans clés étrangères
`souhait_id` et `wishlist_id` sont de simples colonnes : un avis survit à la suppression de l'article ou de la wishlist. Un avis est un instantané (noms copiés). Dédoublonnage des avis `MODIFIE` par `(destinataire, souhait_id)` vérifié dans le service.
Alternative écartée : suppression logique des articles, qui oblige toute requête du propriétaire à filtrer et fait courir un risque de fuite.

Règles de génération (dans la même transaction que l'action du propriétaire) :
- Modification d'un article pris : un `MODIFIE` par lecteur, sans doublon.
- Suppression d'un article pris : suppression des `MODIFIE` de l'article puis un `SUPPRIME` par lecteur.
- Suppression d'une wishlist avec au moins un article pris : suppression des avis d'articles de cette wishlist puis un `WISHLIST_SUPPRIMEE` (avec le nombre d'articles pris) par lecteur.
- Décochage : suppression des `MODIFIE` de l'article.
- Retrait de partage : suppression des avis `(lecteur, wishlist_id)`.
Les destinataires sont les `WishlistPartage` de la wishlist, jamais le propriétaire ni l'administrateur.

### Expiration à 90 jours
Chaque lecture (liste des avis, compteur) filtre `cree_le >= maintenant - 90 jours`. La purge physique (`deleteByCreeLeBefore`) s'exécute à l'affichage de la page des wishlists, sans tâche planifiée. Un `Clock` injecté rend la règle testable.

### Compteur dans la barre de navigation
`LayoutAdvice` ajoute `nombreAvis` (un `count` filtré sur la date, index sur `avis(destinataire_id)`), uniquement pour un utilisateur connecté.

### Aveuglement du propriétaire
- Services distincts par rôle : la vue propriétaire utilise un DTO sans champ « pris » ; la vue lecteur un DTO avec preneur.
- Le contrôleur de prise et décochage exige le rôle lecteur (403 pour le propriétaire et pour tout non-destinataire, administrateur compris).
- Tri par date de création puis identifiant, jamais par état ; aucun compteur « pris » dans la vue propriétaire.
- Les avis ne sont jamais chargés côté propriétaire.

### Unicité du nom de wishlist
`nom_cle` = nom nettoyé en minuscules ; contrainte `unique(proprietaire_id, nom_cle)` en base (effective sous PostgreSQL) plus vérification dans le service (SQLite ne l'applique pas), avec message d'erreur clair.

### Réutilisation
`AccesWishlistService` suit le modèle de `AccesListeService` (liste inexistante ou inaccessible = même refus). La validation d'URL reprend la règle http/https de `CadeauService`, le titre automatique reprend `UrlTitleService` (SSRF déjà protégée). Pagination JPA sur les articles et les avis, suppressions en cascade par requêtes groupées.

### Navigation
Entrée « Wishlists » dans la barre de `fragments.html` ; routes sous `/wishlists`. Contrôleurs MVC rendant du HTML, donc pas d'annotations OpenAPI.

## Risks / Trade-offs

- [Un lecteur libère par erreur l'article d'un autre] → avertissement important avec confirmation obligatoire.
- [Un lecteur retiré garde ses articles pris et ne peut plus les libérer] → limite assumée ; le rétablissement du partage permet de les libérer.
- [Accumulation d'avis pour un lecteur inactif] → expiration à 90 jours et un seul `MODIFIE` par article.
- [Contraintes composites ignorées par SQLite] → vérifications dans les services, contraintes en base pour PostgreSQL.
- [Fuite par oubli d'un filtre côté propriétaire] → DTO distincts et tests vérifiant l'absence de données du preneur dans le HTML du propriétaire.
- [Contournement par un second compte du propriétaire] → hors périmètre technique.
