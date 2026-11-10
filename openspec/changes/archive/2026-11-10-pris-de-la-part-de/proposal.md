## Why

Un lecteur partagé qui achète un cadeau pour le compte d'un proche (qui n'a pas de compte) ne peut aujourd'hui que cocher « Je le prends » sous son propre login ; le nom du vrai preneur est perdu. Le mode public sait déjà demander un nom via une popup, mais cette possibilité n'existe pas sur la vue d'un lecteur partagé.

## What Changes

- Sur la vue d'une wishlist partagée (`/wishlists/{id}`), chaque article libre porte un second bouton « De la part de… », en plus de « Je le prends ».
- Ce bouton ouvre la même popup de saisie de nom que la wishlist publique (sans préremplissage) ; le nom saisi devient le preneur.
- L'article est enregistré avec le nom du tiers et le login du lecteur connecté qui a pris l'article, et affiché « Pris par Bob (via alice) ».
- Un article pris de cette façon peut être libéré sans avertissement ni confirmation par tout lecteur ou visiteur (lecteur partagé, compte connecté, invité du lien public).
- Nouvelle colonne nullable `souhait.pris_via_login` (login du lecteur qui a pris l'article pour un tiers), prise en compte dans l'export/import et dans `docs/database.puml`.
- Hors périmètre : la wishlist publique ne reçoit pas le bouton « De la part de… » ; les avis au lecteur continuent de n'indiquer que le nom du preneur.

## Capabilities

### New Capabilities
<!-- Aucune -->

### Modified Capabilities
- `wishlists`: ajout de l'exigence « Prise pour un tiers » (bouton, popup, mention « via ») et modification de « Décochage avec avertissement » (libération libre d'une prise pour un tiers).

## Impact

- Domaine / dépôt : `Souhait` (champ `prisViaLogin`, `nomPreneurAffiche()`), `SouhaitRepository.prendre/liberer`.
- Services : `SouhaitService` (nouvelle prise pour un tiers, règle `confirmationRequise`), `WishlistPubliqueService` (libération libre).
- Web : `SouhaitController` (nouvelle route `POST /wishlists/{id}/souhaits/{sid}/pris-pour`), gabarit `wishlist-lecteur.html`, `static/js/app.js`.
- Transfert : `Tables.java` (colonne `pris_via_login`), `docs/database.puml`.
- Compatibilité SQLite et PostgreSQL : simple colonne texte nullable créée par Hibernate (`ddl-auto`).
- Sécurité : droits vérifiés côté serveur (lecteur partagé uniquement), nom assaini et borné comme pour un invité, CSRF inchangé ; la vue propriétaire reste aveugle.
- Documentation : `README.md`, `CHANGELOG.md`, version `pom.xml` (0.13.0).
