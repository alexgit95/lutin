## Why

La valeur d'une wishlist n'est pas visible sans additionner ses articles. Un total commun aux differentes vues permet de connaitre le montant des prix renseignes sans compromettre la surprise du proprietaire.

## What Changes

- Afficher en haut de chaque wishlist le total des prix renseignes, sur les vues proprietaire, lecteur partage et publique.
- Additionner tous les articles, quelle que soit la page et qu'ils soient pris ou non, et signaler le nombre d'articles sans prix.
- Mettre a jour le total apres ajout, modification ou suppression, sans exposer l'etat des reservations au proprietaire.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `wishlists`: affichage du total des prix et des articles sans prix sur les trois vues, independamment des reservations et de la pagination.

## Impact

- Service et repository des souhaits, DTO des vues et gabarits Thymeleaf des trois parcours.
- Tests Java et web, documentation fonctionnelle, changelog et version selon les conventions du projet.
- Aucun changement de schema de donnees, de pipeline ou de droits d'acces.