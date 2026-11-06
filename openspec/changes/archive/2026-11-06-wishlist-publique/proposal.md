## Why

Une wishlist n'est aujourd'hui consultable que par des comptes avec lesquels le propriétaire la partage. Pour inviter des proches sans compte (famille éloignée, amis), le propriétaire veut diffuser un simple lien : n'importe qui peut alors consulter la wishlist et prendre un article en indiquant son nom.

## What Changes

- Le propriétaire peut rendre une wishlist **publique** (interrupteur dans l'onglet Réglages). Un jeton secret est généré ; le lien `/p/{jeton}` donne accès à la wishlist sans connexion.
- Une wishlist publique affiche un bouton **Partager** dans l'en-tête de l'onglet Articles (partage natif du système sur mobile, copie du lien sinon). Le propriétaire peut **régénérer** le lien (l'ancien meurt) ou **rendre la wishlist privée** (le jeton disparaît, tous les liens meurent).
- Un visiteur non connecté consulte la wishlist (titre et tuiles seulement, sans login du propriétaire) et prend un article après avoir saisi son nom dans une popup. Son nom est affiché « nom (invité) ».
- Un visiteur connecté qui n'est ni propriétaire ni lecteur partagé prend un article sous son login, sans popup. Le propriétaire et les lecteurs partagés sont redirigés vers leur vue habituelle.
- Tout visiteur peut libérer un article. L'avertissement avec confirmation est exigé sauf si le visiteur est connecté, que son login est le nom du preneur et que le preneur n'est pas un invité.
- Pas d'avis pour les invités. Les wishlists déjà partagées par login gardent leur fonctionnement.
- Pages publiques : `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex`, `Cache-Control: no-store`, CSRF maintenu. La limitation de débit est déléguée à Cloudflare.
- Limite connue : un propriétaire déconnecté qui ouvre son propre lien voit l'état « pris » ; l'aveuglement ne tient plus tant que la wishlist est publique.

## Capabilities

### New Capabilities
<!-- Aucune : tout s'ajoute à la capability existante. -->

### Modified Capabilities
- `wishlists`: nouvelles exigences (wishlist publique, accès par lien, prise par un invité, protection des pages publiques) ; modification de « Article pris » et « Décochage avec avertissement » pour les noms d'invités.

## Impact

- Domaine : `wishlist.jeton_public` (nullable, index unique séparé) et `souhait.pris_par_invite` (nullable, nul = faux). Mise à jour de `docs/database.puml`. Les colonnes ajoutées doivent rester compatibles avec des bases existantes SQLite et PostgreSQL.
- Services : jeton public, résolution d'une wishlist par jeton, prise et libération par un visiteur, règle de confirmation.
- Web : `SecurityConfig` (`/p/**` en accès libre), contrôleur et gabarit publics, interrupteur dans Réglages, bouton Partager, popup dans `app.js`, en-têtes de réponse.
- Documentation : `README.md` (limite d'aveuglement, règle Cloudflare recommandée sur `/p/*`), `CHANGELOG.md`, version du `pom.xml`, tests.
