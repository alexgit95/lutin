## Why

Sur les écrans étroits, les tags occupent actuellement la même ligne que le prix et réduisent l'espace disponible pour les autres informations du cadeau. Le statut, qui doit rester facilement lisible et utilisable, se retrouve sur une ligne séparée.

## What Changes

- Sur mobile, afficher le nom du cadeau puis ses tags sous le nom.
- Réorganiser le prix, le statut et le bouton d'actions sur une ligne compacte qui donne au statut l'espace nécessaire.
- Conserver la disposition desktop, les libellés accessibles, les actions du propriétaire et la vue en lecture seule.

## Capabilities

### New Capabilities

### Modified Capabilities

- `dashboard`: préciser la disposition des informations des cadeaux sur écran étroit.

## Impact

- CSS responsive du dashboard et, si nécessaire, structure du fragment Thymeleaf des lignes de cadeau.
- Tests web du rendu et vérifications navigateur sur téléphone, sans changement API ni modèle de données.