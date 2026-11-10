## Why

Ajouter un article depuis une autre application impose aujourd'hui de copier son lien, ouvrir Lutin et retrouver une wishlist. Sur Android, recevoir le partage natif dans la PWA permet de rejoindre directement un formulaire pre-rempli tout en gardant la confirmation de l'utilisateur.

## What Changes

- Declarer Lutin comme cible de partage de liens pour la PWA installee sur Android avec un navigateur compatible, Chrome etant la cible de validation.
- Recevoir une URL, un titre et du texte, puis proposer le choix parmi les wishlists appartenant a l'utilisateur connecte.
- Ouvrir le formulaire d'ajout existant avec l'URL et, si disponible, le titre pre-remplis et modifiables ; laisser le prix vide.
- Retrouver un lien dans le texte partage si le champ URL est absent et tenter de recuperer le titre de la page si aucun titre n'est transmis.
- Traiter les liens absents ou invalides, l'absence de wishlist et la reprise apres authentification sans creation implicite d'article.
- Maintenir les controles de propriete, la protection CSRF et la protection SSRF pour toute recuperation de titre.

## Capabilities

### New Capabilities

- `wishlist-share-target`: reception d'un partage Android, normalisation des donnees, selection d'une wishlist personnelle et pre-remplissage securise du formulaire d'article.

### Modified Capabilities

Aucune. La nouvelle capacite complete le manifeste PWA et reutilise la gestion existante des wishlists et articles sans modifier les exigences de leur parcours manuel.

## Impact

- Manifeste web, nouveaux points d'entree MVC et gabarit de selection, controleur et formulaire d'ajout d'article, reutilisation de `WishlistService`, `AccesWishlistService` et `UrlTitleService`.
- Adaptation minimale du parcours de creation de wishlist pour conserver le contexte du partage lorsqu'aucune wishlist n'existe.
- Verification de la reprise du lien a travers la connexion locale et Cloudflare Access, sans ouverture anonyme des donnees ni modification de pipeline ou de politique Cloudflare.
- Tests Java unitaires et web, verification du manifeste et recette sur une PWA reellement installee sur Android ; mise a jour du README, du changelog et de la version lors de l'implementation.
- Aucun changement de schema de donnees, aucune dependance native, aucune chaine de build front et aucun changement prevu du service worker minimal.
- Hors perimetre : iOS, reception de fichiers ou d'images, liens internes non HTTP(S), ajout automatique, synchronisation ou stockage hors connexion.