## Why

Dans l'onglet Personnes, les personnes terminées sont mélangées aux personnes à compléter, ce qui rend moins visible le travail restant.

## What Changes

- Afficher d'abord les personnes à compléter, sans en-tête, puis les personnes terminées sous l'en-tête « Terminées ».
- Conserver un ordre alphabétique dans chaque groupe et masquer l'en-tête lorsqu'aucune personne n'est terminée.
- Replacer une personne dans le groupe correspondant après une modification de son état, sans modifier la signification du statut ni les actions existantes.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `shopping-lists`: organiser l'affichage des personnes selon leur état de complétion.

## Impact

- Onglet Personnes : récupération et présentation des personnes, tests associés et documentation fonctionnelle.
- Aucun changement prévu du modèle de données, des droits d'accès, du tableau de bord ou des autres écrans.