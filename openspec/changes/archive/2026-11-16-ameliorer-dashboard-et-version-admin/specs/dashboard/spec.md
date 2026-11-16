## ADDED Requirements

### Requirement: Colonnes de cadeaux alignées
Le dashboard SHALL aligner les colonnes correspondantes des groupes de cadeaux dans une même présentation, indépendamment de la longueur des noms et des données de chaque groupe. Le destinataire SHALL être identifié par l'en-tête du groupe, sans colonne « Pour » redondante. Les noms de cadeaux longs SHALL pouvoir être affichés en entier sur plusieurs lignes, y compris lorsqu'ils contiennent un mot sans espace.

#### Scenario: Groupes de tailles différentes
- **WHEN** plusieurs personnes ont des cadeaux dont les noms et les autres données ont des longueurs différentes
- **THEN** les colonnes correspondantes sont alignées entre les groupes et les actions restent dans leur colonne

#### Scenario: Titre long
- **WHEN** un cadeau possède un nom trop long pour sa colonne, avec ou sans espaces
- **THEN** son nom passe sur plusieurs lignes sans chevaucher les autres colonnes ni être tronqué

#### Scenario: Destinataire dans l'en-tête
- **WHEN** un propriétaire ou un lecteur consulte les cadeaux visibles regroupés par personne
- **THEN** le nom du destinataire apparaît dans l'en-tête de son groupe, sans colonne « Pour » dans le tableau

#### Scenario: Chargement et modification en place
- **WHEN** un lot supplémentaire est chargé ou qu'un statut est modifié par HTMX
- **THEN** les lignes ajoutées ou mises à jour respectent le même alignement et le même retour à la ligne

### Requirement: Menu des actions de cadeau
Le dashboard SHALL regrouper les actions de modification et de suppression de chaque cadeau dans un bouton de menu compact réservé au propriétaire. Le menu SHALL être accessible au clavier, utilisable sur mobile et se fermer au clic extérieur, à l'ouverture d'un autre menu ou avec Échap. La suppression SHALL conserver sa confirmation et ses contrôles d'accès.

#### Scenario: Actions du propriétaire
- **WHEN** le propriétaire ouvre le menu d'un cadeau
- **THEN** les actions « Modifier » et « Supprimer » sont disponibles pour ce cadeau

#### Scenario: Suppression confirmée
- **WHEN** le propriétaire sélectionne « Supprimer »
- **THEN** une confirmation est demandée avant l'envoi de la suppression

#### Scenario: Menu près des bords de l'écran
- **WHEN** le propriétaire ouvre le menu près du haut ou du bas d'un écran de téléphone
- **THEN** le menu s'ouvre dans l'espace disponible et les deux actions restent visibles

#### Scenario: Lecture seule
- **WHEN** un lecteur consulte le dashboard ou charge un lot de cadeaux
- **THEN** aucun menu de modification ou de suppression ne lui est transmis

#### Scenario: Mise à jour dynamique
- **WHEN** un lot de cadeaux est chargé ou qu'une ligne est remplacée après un changement de statut
- **THEN** le menu des actions de chaque cadeau du propriétaire reste utilisable

### Requirement: Barre de workflow adaptée au mobile
La barre persistante SHALL présenter les cinq libellés de statut et leurs compteurs lisiblement sur écran étroit, sans défilement horizontal. Elle SHALL rester visible pendant le défilement sans masquer les cadeaux ni leurs actions, y compris avec une zone de sécurité en bas de l'écran.

#### Scenario: Cinq statuts sur téléphone
- **WHEN** le dashboard est affiché sur un téléphone étroit
- **THEN** les cinq libellés et compteurs restent visibles et lisibles, sans chevauchement ni défilement horizontal

#### Scenario: Dernier cadeau accessible
- **WHEN** l'utilisateur atteint le bas de la liste sur téléphone
- **THEN** le dernier cadeau et ses actions restent entièrement visibles au-dessus de la barre

#### Scenario: Compteurs après modification
- **WHEN** le propriétaire change un statut sur téléphone et que le serveur confirme la modification
- **THEN** la barre actualisée reste adaptée à l'écran et affiche les compteurs confirmés

## MODIFIED Requirements

### Requirement: Interface adaptée au mobile
L'interface SHALL s'inspirer de la maquette (cartes d'indicateurs colorées, navigation, pastilles de statut) et rester utilisable sur écran de téléphone. Le dashboard SHALL s'adapter à la largeur disponible sans défilement horizontal, tout en conservant l'accès aux informations et actions des cadeaux.

#### Scenario: Écran étroit
- **WHEN** le dashboard est affiché sur un écran de téléphone
- **THEN** la navigation et les cartes restent lisibles sans défilement horizontal

#### Scenario: Cadeaux sur écran étroit
- **WHEN** le dashboard contient des cadeaux avec des noms ou des données longs sur un écran de téléphone
- **THEN** le contenu reste lisible et les actions accessibles sans défilement horizontal