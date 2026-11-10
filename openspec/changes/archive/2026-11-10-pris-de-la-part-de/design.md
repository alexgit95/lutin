## Context

La prise d'un article est stockée dans `Souhait` : `pris_par_nom` (texte libre), `pris_le`, `pris_par_invite` (booléen nul = faux). `SouhaitRepository.prendre/liberer` sont des `UPDATE` atomiques. La vue lecteur (`wishlist-lecteur.html`, DTO `VueSouhaitLecteur.prisPar`) affiche `Souhait.nomPreneurAffiche()`. Le popup de nom existe uniquement dans `wishlist-publique.html`, piloté par `app.js` (`data-prendre-url`, préremplissage via `localStorage`). La règle de libération sans confirmation est `SouhaitService.confirmationRequise`.

Décisions fonctionnelles déjà prises avec le demandeur : modèle « Bob (via alice) », bouton ajouté à « Je le prends », réservé aux lecteurs partagés, popup sans préremplissage, libération libre pour tout lecteur ou visiteur.

## Goals / Non-Goals

**Goals:**
- Permettre à un lecteur partagé de prendre un article au nom d'un tiers, avec traçabilité du lecteur.
- Réutiliser la popup et la validation de nom existantes.
- Rester compatible SQLite / PostgreSQL sans outil de migration.

**Non-Goals:**
- Bouton « De la part de… » sur la wishlist publique ou pour un compte connecté non partagé.
- Modifier les avis (ils continuent de nommer uniquement le preneur) ou la vue propriétaire.
- Permettre de modifier le tiers après la prise (on libère puis on reprend).

## Decisions

**D1 — Nouvelle colonne `souhait.pris_via_login varchar(50)` nullable** (login mesure au plus 50 caractères). `pris_par_nom` = nom du tiers, `pris_par_invite` = faux, `pris_via_login` = login du lecteur. Alternative écartée : encoder « Bob (via alice) » dans `pris_par_nom`, ce qui brouille le texte, dépasse les 100 caractères et empêche d'identifier une prise pour un tiers. Alternative écartée : réutiliser `pris_par_invite` avec « (invité) », trompeur car l'auteur est un compte. La colonne est nullable : une table existante la reçoit via `ddl-auto` sans migration, et nul vaut « prise ordinaire ».

**D2 — Affichage dans `Souhait.nomPreneurAffiche()`** : renvoie `nom + " (via " + login + ")"` si `prisViaLogin != null`, sinon le comportement actuel. Vue lecteur, message d'erreur de prise concurrente et avertissement de libération en bénéficient sans autre changement. Les avis gardent `souhait.getPrisParNom()` (inchangé, pas de colonne supplémentaire dans `avis`).

**D3 — Dépôt** : `prendre(...)` reçoit un paramètre `via` nullable ; `liberer` remet aussi `prisViaLogin` à nul. Conservation de l'atomicité (clause `prisParNom is null`) ; `countByWishlistIdAndPrisParNomIsNotNull` reste valable.

**D4 — Service** : `SouhaitService.prendrePour(wishlistId, souhaitId, login, nomSaisi)` vérifie `acces.lecteur(wishlistId, login)` (refuse propriétaire et non-lecteur), valide le nom, puis appelle le dépôt avec `invite=false` et `via=login`. La validation de nom (`WishlistPubliqueService.nomInvite` : nettoyage des caractères de contrôle et de format, 1 à 100 caractères, message « Votre nom est obligatoire ») est extraite dans une méthode statique partagée de `SouhaitService`, appelée par les deux services. `confirmationRequise` n'est plus statique sur 3 paramètres seulement : elle reçoit aussi un indicateur « pris pour un tiers » et renvoie faux dans ce cas ; `libererSansControle` le dérive de `souhait.getPrisViaLogin() != null`. La libération publique (invités compris) en profite, sans changement du contrôleur public.

**D5 — Web** : `POST /wishlists/{id}/souhaits/{sid}/pris-pour` dans `SouhaitController` (MVC HTML, pas d'OpenAPI), paramètres `nom` et `page`, erreurs métier affichées en flash comme `prendre`, redirection vers la page courante. CSRF actif par défaut.

**D6 — Front** : `wishlist-lecteur.html` reçoit un second bouton « De la part de… » (attribut `data-prendre-pour-url`) et son propre `<dialog id="dialogue-pour">` avec texte adapté (« Indiquez le nom de la personne de la part de qui vous prenez cet article »). `app.js` gère le nouvel attribut : affecte l'action du formulaire, vide le champ, ouvre la popup ; elle ne lit ni n'écrit `localStorage` (clé d'invité inchangée). Le gestionnaire de fermeture `data-fermer-dialogue` est généralisé à la boîte de dialogue la plus proche (`closest('dialog')`). Le bouton est à côté de « Je le prends » dans `tuile-pied` ; vérifier l'affichage mobile (retour à la ligne via CSS flex-wrap existant ou ajustement mineur).

**D7 — Transfert et documentation** : `Tables.java` ajoute `texte("pris_via_login", "e.prisViaLogin", false, 50)` et `Tables.VERSION` passe à 2, car l'import exige toutes les colonnes et le même numéro de version ; `docs/database.puml` documente la colonne.

## Risks / Trade-offs

- [Un export antérieur n'est plus importable : l'import est strict sur la version et les colonnes] → comportement voulu par le format existant ; le message « Version de format incompatible » s'affiche.
- [Libération libre : n'importe qui, y compris un invité du lien public, peut défaire une prise pour un tiers] → choix assumé ; le texte du spec le dit explicitement.
- [Le lecteur peut saisir n'importe quel nom, y compris celui d'un autre compte] → même risque que le nom d'invité ; le texte affiché porte « (via login) » qui identifie l'auteur.
- [Variation de largeur des tuiles avec deux boutons] → test visuel sur petit écran.
- [Hibernate 7.4 + SQLite : colonne `integer` avant `id`] → non concerné, la colonne ajoutée est un `varchar`.
