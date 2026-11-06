## Why

Dans l'onglet Personnes, chaque ligne affiche trois liens d'action (+ cadeau, modifier, supprimer) qui prennent de la place, alors que l'information utile est de savoir où en est le budget de chaque personne. Il faut gagner de l'espace et montrer, pour chaque personne, ce qui est dépensé et ce qui est déjà prévu par rapport à son budget.

## What Changes

- Les actions d'une personne (« + Cadeau », « Modifier », « Supprimer ») passent derrière un bouton `⋯` qui ouvre un sous-menu au clic. La suppression garde sa confirmation.
- Chaque ligne affiche une **barre de budget** : le **dépensé** en teinte pleine (cadeaux au statut Commandé ou plus), le **reste à payer** en teinte plus claire (cadeaux « À acheter » avec prix), le budget comme repère. Les montants sont aussi écrits : « dépensé 85 € · à payer 20 € · budget 120 € ».
- Dépensé et reste à payer suivent les règles du dashboard : cadeaux pour un tiers et cadeaux sans prix exclus. Les cadeaux masqués comptent, l'onglet étant réservé au propriétaire. La somme des dépensés des personnes égale le « Déjà déboursé » du dashboard, et celle des restes à payer égale son « Reste à payer ».
- Budget à 0 : texte seul, sans barre. Total prévu (dépensé plus reste à payer) supérieur au budget : la barre s'adapte, un repère marque le budget et « dépassé de X € » apparaît en rouge.
- La barre porte une description textuelle pour les lecteurs d'écran, la couleur n'étant pas le seul vecteur d'information.
- Pas de récapitulatif global en tête de l'onglet, et aucun changement de schéma.

## Capabilities

### New Capabilities
<!-- Aucune -->

### Modified Capabilities
- `shopping-lists`: modification de l'exigence « Onglet Personnes » (actions dans un sous-menu) et nouvelle exigence « Consommé par personne ».

## Impact

- Code : `CadeauRepository` (requête agrégée par personne), `PersonneService` (objet de vue avec budget, dépensé, reste à payer), `PersonneController.lister`.
- Gabarit et style : `personnes.html`, `app.css`, et un court script dans `app.js` pour refermer le sous-menu.
- Tests : service (règles de calcul et égalité avec le dashboard), web (menu, barre, accessibilité) ; un test existant de l'onglet Personnes est à adapter.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`. Aucun changement de schéma.
