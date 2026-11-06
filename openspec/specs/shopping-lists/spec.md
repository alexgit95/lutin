# Shopping Lists

## Purpose
Manage purchase lists and their recipient people and budgets.

## Requirements

### Requirement: Listes d'achat
Le système SHALL permettre à un utilisateur de créer, renommer et supprimer ses listes d'achat ; chaque liste appartient à un seul utilisateur.

#### Scenario: Création
- **WHEN** l'utilisateur crée une liste avec un nom
- **THEN** la liste apparaît dans ses listes

#### Scenario: Isolation
- **WHEN** un utilisateur demande une liste dont il n'est ni propriétaire ni lecteur
- **THEN** l'accès est refusé

### Requirement: Personnes destinataires et budget
Le système SHALL permettre de créer, modifier et supprimer, dans une liste, des personnes destinataires avec un budget représentant la somme que l'utilisateur souhaite dépenser pour elles.

#### Scenario: Création avec budget
- **WHEN** l'utilisateur crée une personne avec un nom et un budget
- **THEN** la personne est ajoutée à la liste avec ce budget

#### Scenario: Suppression d'une personne ayant des cadeaux
- **WHEN** l'utilisateur supprime une personne qui a des cadeaux
- **THEN** le système demande une confirmation puis supprime la personne et ses cadeaux

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

### Requirement: Import de personnes depuis une autre liste
Le système SHALL permettre au propriétaire d'une liste d'importer, dans l'onglet Personnes, les personnes et leur budget d'une autre liste dont il est également propriétaire, après un aperçu. Seules les personnes SHALL être copiées, jamais les cadeaux. Une personne dont le nom existe déjà dans la liste cible, sans tenir compte de la casse ni des espaces en bordure, SHALL être signalée « déjà présente » dans l'aperçu et SHALL NOT être importée. Les autres personnes SHALL être cochées par défaut. Le système SHALL contrôler côté serveur les droits sur les deux listes, l'appartenance des personnes cochées à la liste source et les doublons au moment de l'import, et SHALL relire les noms et budgets en base. Une liste dont l'utilisateur n'est que lecteur SHALL NOT servir de source.

#### Scenario: Panneau d'import
- **WHEN** le propriétaire possède au moins une autre liste et ouvre l'onglet Personnes
- **THEN** un panneau propose ses autres listes avec leur nombre de personnes

#### Scenario: Aucune autre liste
- **WHEN** le propriétaire ne possède qu'une seule liste
- **THEN** le panneau d'import n'est pas affiché

#### Scenario: Aperçu
- **WHEN** le propriétaire choisit une liste source et demande l'aperçu
- **THEN** il voit les personnes de la source avec leur budget, cochées par défaut, et le total du budget importable

#### Scenario: Personne déjà présente
- **WHEN** la liste cible contient déjà une personne de même nom (casse et espaces en bordure ignorés)
- **THEN** l'aperçu la grise avec la mention « déjà présente », sa case est inactive et elle n'est jamais importée

#### Scenario: Import
- **WHEN** le propriétaire valide l'aperçu
- **THEN** les personnes cochées sont créées dans la liste cible avec le nom et le budget de la source, et un compte rendu indique les personnes importées, le budget total ajouté et les personnes ignorées

#### Scenario: Copie indépendante
- **WHEN** le budget d'une personne importée est modifié dans l'une des listes
- **THEN** l'autre liste n'est pas modifiée

#### Scenario: Cadeaux non copiés
- **WHEN** des personnes sont importées
- **THEN** aucun cadeau de la liste source n'est copié

#### Scenario: Personne ajoutée entre l'aperçu et l'import
- **WHEN** une personne de même nom est ajoutée à la cible après l'aperçu puis que l'import est validé
- **THEN** elle n'est pas dupliquée et figure parmi les personnes ignorées

#### Scenario: Identifiant forgé
- **WHEN** la requête d'import contient l'identifiant d'une personne qui n'appartient pas à la liste source
- **THEN** cette personne est ignorée et rien n'est copié depuis une autre liste

#### Scenario: Source non possédée
- **WHEN** l'utilisateur demande un aperçu ou un import depuis une liste dont il n'est pas propriétaire, y compris une liste partagée avec lui
- **THEN** la requête est refusée (403) et rien n'est copié

#### Scenario: Cible non possédée
- **WHEN** un lecteur de la liste cible ou un autre utilisateur tente un aperçu ou un import
- **THEN** la requête est refusée (403)

#### Scenario: Source identique à la cible
- **WHEN** la source choisie est la liste cible
- **THEN** la demande est refusée avec un message d'erreur

#### Scenario: Rien à importer
- **WHEN** la source n'a aucune personne, ou que toutes sont déjà présentes
- **THEN** l'aperçu l'indique et l'import ne crée aucune personne

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
