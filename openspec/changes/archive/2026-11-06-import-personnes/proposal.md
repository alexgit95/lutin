## Why

Chaque année ou pour chaque occasion, les mêmes destinataires reviennent avec des budgets proches. Aujourd'hui il faut recréer à la main chaque personne et son budget dans la nouvelle liste. Importer les personnes d'une autre liste évite cette saisie répétitive.

## What Changes

- Dans l'onglet Personnes d'une liste d'achat, un panneau « Importer depuis une autre liste » propose un menu des **autres listes dont l'utilisateur est propriétaire**, avec leur nombre de personnes. Le panneau n'apparaît que s'il existe au moins une autre liste.
- Un **aperçu** liste les personnes de la liste source avec leur budget. Elles sont **cochées par défaut** ; celles dont le nom existe déjà dans la liste cible (comparaison sans tenir compte de la casse ni des espaces en bordure) sont grisées « déjà présente », non cochables et jamais importées. Un total du budget importé est affiché.
- La validation copie les personnes cochées (nom et budget) dans la liste cible, puis affiche un compte rendu : personnes importées, budget total ajouté, personnes ignorées.
- Seules les personnes sont copiées, jamais les cadeaux. La copie est indépendante de la source.
- Seules les listes dont l'utilisateur est propriétaire sont concernées, pour la source comme pour la cible. Une liste simplement partagée ne peut pas servir de source.
- Le serveur ne fait pas confiance à l'aperçu : il revérifie les droits, l'appartenance des personnes à la source et les doublons au moment de l'import, et relit les valeurs en base.
- Aucun changement de schéma.

## Capabilities

### New Capabilities
<!-- Aucune -->

### Modified Capabilities
- `shopping-lists`: nouvelle exigence « Import de personnes depuis une autre liste ».

## Impact

- Code : `PersonneService` (aperçu et import), `PersonneController` (routes d'aperçu et d'import), `personnes.html` et un gabarit d'aperçu.
- Aucun changement de schéma, d'API REST ni de configuration.
- Tests : service (règle des doublons, droits, identifiants forgés, budget, cadeaux non copiés, listes vides) et web.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`.
