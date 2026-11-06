## MODIFIED Requirements

### Requirement: Onglet Personnes
La création, la modification (nom, budget) et la suppression des personnes SHALL se faire dans un onglet dédié de la liste, et non sur le dashboard ; cet onglet est réservé au propriétaire. Dans cet onglet, les actions d'une personne (ajouter un cadeau, modifier, supprimer) SHALL être regroupées derrière un bouton ouvrant un sous-menu au clic.

#### Scenario: Création depuis l'onglet
- **WHEN** le propriétaire remplit le formulaire de l'onglet Personnes
- **THEN** la personne est créée avec son budget et l'onglet est mis à jour

#### Scenario: Dashboard sans gestion des personnes
- **WHEN** l'utilisateur ouvre le dashboard
- **THEN** aucun formulaire ni liste de personnes n'y figure, seul le budget global est affiché

#### Scenario: Onglet interdit à un lecteur
- **WHEN** un lecteur d'un partage ouvre l'onglet Personnes
- **THEN** l'accès est refusé (403)

#### Scenario: Actions dans un sous-menu
- **WHEN** le propriétaire ouvre l'onglet Personnes
- **THEN** chaque personne n'affiche qu'un bouton de menu ; les actions « + Cadeau », « Modifier » et « Supprimer » ne figurent que dans le sous-menu de la personne

#### Scenario: Suppression avec confirmation
- **WHEN** le propriétaire choisit « Supprimer » dans le sous-menu d'une personne
- **THEN** une confirmation est demandée avant la suppression de la personne et de ses cadeaux

#### Scenario: Fermeture du sous-menu
- **WHEN** un sous-menu est ouvert et que le propriétaire clique ailleurs, ouvre le sous-menu d'une autre personne ou appuie sur Échap
- **THEN** le sous-menu se referme

## ADDED Requirements

### Requirement: Consommé par personne
L'onglet Personnes SHALL afficher, pour chaque personne, son budget, ce qui est dépensé et ce qui reste à payer, sous forme de barre et de texte. Le dépensé SHALL être la somme des prix des cadeaux au statut Commandé ou plus, et le reste à payer celle des cadeaux au statut « À acheter ». Les cadeaux pour compte de tiers et les cadeaux sans prix SHALL être exclus. Les cadeaux masqués SHALL être comptés. La somme des dépensés des personnes SHALL égaler le « Déjà déboursé » du dashboard et celle des restes à payer son « Reste à payer ». La barre SHALL aussi être décrite par un texte accessible, et l'information SHALL ne pas reposer sur la seule couleur.

#### Scenario: Dépensé et reste à payer
- **WHEN** une personne de budget 120 € a un cadeau à 85 € commandé et un cadeau à 20 € à acheter
- **THEN** la ligne indique dépensé 85 €, à payer 20 € et budget 120 €, la barre montrant le dépensé en teinte pleine et le reste à payer en teinte claire

#### Scenario: Règles du dashboard
- **WHEN** une personne a un cadeau pour un tiers, un cadeau sans prix et un cadeau masqué avec prix
- **THEN** les deux premiers ne comptent pas et le cadeau masqué compte

#### Scenario: Cohérence avec le dashboard
- **WHEN** l'on additionne les dépensés et les restes à payer de toutes les personnes
- **THEN** les sommes égalent le « Déjà déboursé » et le « Reste à payer » du dashboard

#### Scenario: Aucun cadeau
- **WHEN** une personne n'a aucun cadeau
- **THEN** la ligne indique dépensé 0 € et le budget, avec une barre vide

#### Scenario: Budget à zéro
- **WHEN** une personne a un budget de 0 €
- **THEN** seul le texte des montants est affiché, sans barre

#### Scenario: Dépassement du budget
- **WHEN** le dépensé plus le reste à payer dépasse le budget
- **THEN** la barre est rouge avec un repère à la position du budget, et la mention « dépassé de X € » est affichée

#### Scenario: Accessibilité
- **WHEN** un lecteur d'écran parcourt la ligne
- **THEN** la barre est décrite par un texte donnant le dépensé, le reste à payer et le budget
