## Why

Les colonnes des cadeaux ne sont pas alignées entre les personnes et la présentation mobile du dashboard peut déborder. L'administration ne permet pas non plus d'identifier facilement la version et le commit effectivement déployés.

## What Changes

- Aligner les colonnes des groupes de cadeaux et afficher les titres longs sur plusieurs lignes.
- Adapter la barre persistante des cinq statuts aux écrans mobiles sans défilement horizontal ni contenu masqué.
- Afficher dans l'administration la version Maven et le SHA du commit embarqués dans le build, avec une indication explicite lorsque l'information manque.

## Capabilities

### New Capabilities

- `admin-version`: identification de la version et du commit du build depuis la page d'administration.

### Modified Capabilities

- `dashboard`: alignement des colonnes et présentation mobile des cadeaux et de la barre de workflow.

## Impact

- Templates Thymeleaf et CSS du dashboard, y compris les fragments chargés par HTMX.
- Contrôleur et template de l'administration, informations de build Maven et tests associés.
- Documentation fonctionnelle et journal des modifications ; aucun changement de pipeline ni de schéma de base de données prévu.